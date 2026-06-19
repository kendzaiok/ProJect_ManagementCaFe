package com.cafemanagement.server;

import com.cafemanagement.dao.UserDAO;
import com.cafemanagement.dao.UserDAOImpl;
import com.cafemanagement.dao.ProductDAO;
import com.cafemanagement.dao.ProductDAOImpl;
import com.cafemanagement.dao.CafeTableDAO;
import com.cafemanagement.dao.CafeTableDAOImpl;
import com.cafemanagement.dao.OrderDAO;
import com.cafemanagement.dao.OrderDAOImpl;
import com.cafemanagement.dto.Request;
import com.cafemanagement.dto.Response;
import com.cafemanagement.model.User;
import com.cafemanagement.model.Product;
import com.cafemanagement.model.CafeTable;
import com.cafemanagement.model.Order;
import com.cafemanagement.model.OrderItem;
import com.cafemanagement.util.CsvExportUtil;
import com.cafemanagement.util.CsvImportUtil;
import com.cafemanagement.util.ServerLogger;
import at.favre.lib.crypto.bcrypt.BCrypt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private UserDAO userDAO;
    private ProductDAO productDAO;
    private CafeTableDAO cafeTableDAO;
    private OrderDAO orderDAO;
    private static final Object orderLock = new Object(); // Đối tượng lock đồng bộ cho tạo đơn

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.userDAO = new UserDAOImpl();
        this.productDAO = new ProductDAOImpl();
        this.cafeTableDAO = new CafeTableDAOImpl();
        this.orderDAO = new OrderDAOImpl();
    }

    @Override
    public void run() {
        // ĐẢO NGƯỢC THỨ TỰ: TẠO OUTPUTSTREAM TRƯỚC, RỒI TẠO INPUTSTREAM
        try (ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())) {

            // Bắt buộc flush header ra stream ngay lập tức để Client có thể đọc
            oos.flush();

            Request request;
            while ((request = (Request) ois.readObject()) != null) {
                Response response = handleRequest(request);
                oos.writeObject(response);
                oos.flush();
                oos.reset(); // RẤT QUAN TRỌNG: Chống lỗi cache dữ liệu khi gửi mảng/list giống nhau nhiều lần
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Response handleRequest(Request request) {
        String action = request.getAction();

        switch (action) {
            case "LOGIN":
                return handleLogin(request);

            case "REGISTER":
                @SuppressWarnings("unchecked")
                Map<String, String> regData = (Map<String, String>) request.getData();
                String regUsername = regData.get("username");
                String regPlainPassword = regData.get("password");

                // 1. Kiểm tra xem username đã có ai dùng chưa
                if (userDAO.findByUsername(regUsername) != null) {
                    return new Response(false, "Tên đăng nhập đã tồn tại! Vui lòng chọn tên khác.", null);
                }

                // 2. Băm mật khẩu bằng BCrypt
                String hashedRegPassword = BCrypt.withDefaults().hashToString(12, regPlainPassword.toCharArray());

                // 3. Tạo User mới, mặc định cấp quyền "staff" (Nhân viên)
                User newUser = new User();
                newUser.setUsername(regUsername);
                newUser.setPassword(hashedRegPassword);
                newUser.setRole("EMPLOYEE");

                // 4. Lưu xuống Database
                boolean registerSuccess = userDAO.add(newUser);
                ServerLogger.getInstance().log(regUsername, "REGISTER", registerSuccess ? "SUCCESS" : "FAILED");

                return new Response(registerSuccess, registerSuccess ? "Đăng ký thành công!" : "Lỗi hệ thống, không thể đăng ký!", null);

            case "PRODUCT_FIND_ALL":
                return new Response(true, "Lấy danh sách sản phẩm thành công", productDAO.findAll());

            case "PRODUCT_CREATE":
                Product productCreate = (Product) request.getData();
                boolean createSuccess = productDAO.add(productCreate);
                ServerLogger.getInstance().log("System", "PRODUCT_CREATE", createSuccess ? "SUCCESS" : "FAILED");
                return new Response(createSuccess, createSuccess ? "Thêm sản phẩm thành công" : "Thêm sản phẩm thất bại", null);

            case "PRODUCT_UPDATE":
                Product productUpdate = (Product) request.getData();
                boolean updateSuccess = productDAO.update(productUpdate);
                ServerLogger.getInstance().log("System", "PRODUCT_UPDATE", updateSuccess ? "SUCCESS" : "FAILED");
                return new Response(updateSuccess, updateSuccess ? "Cập nhật sản phẩm thành công" : "Cập nhật sản phẩm thất bại", null);

            case "PRODUCT_DELETE":
                int productId = (int) request.getData();
                boolean deleteSuccess = productDAO.delete(productId);
                ServerLogger.getInstance().log("System", "PRODUCT_DELETE", deleteSuccess ? "SUCCESS" : "FAILED");
                return new Response(deleteSuccess, deleteSuccess ? "Xóa sản phẩm thành công" : "Xóa sản phẩm thất bại", null);

            case "PRODUCT_SEARCH":
                String keyword = (String) request.getData();
                return new Response(true, "Tìm kiếm sản phẩm thành công", productDAO.search(keyword));

            case "TABLE_GET_ALL":
                return new Response(true, "Lấy danh sách bàn thành công", cafeTableDAO.findAll());

            case "ORDER_CREATE":
                synchronized (orderLock) { // Đồng bộ khi nhiều client cùng tạo đơn
                    @SuppressWarnings("unchecked")
                    Map<String, Object> orderData = (Map<String, Object>) request.getData();
                    Order order = (Order) orderData.get("order");
                    @SuppressWarnings("unchecked")
                    List<OrderItem> items = (List<OrderItem>) orderData.get("items");
                    boolean orderSuccess = orderDAO.createOrder(order, items);
                    ServerLogger.getInstance().log("System", "ORDER_CREATE", orderSuccess ? "SUCCESS" : "FAILED");
                    return new Response(orderSuccess, orderSuccess ? "Tạo đơn hàng thành công" : "Tạo đơn hàng thất bại", null);
                }

            case "PRODUCT_EXPORT":
                try {
                    String filePath = (String) request.getData();
                    CsvExportUtil exportUtil = CsvExportUtil.getInstance();
                    List<Product> productList = productDAO.findAll();
                    String savedPath = exportUtil.exportProducts(productList, filePath);
                    ServerLogger.getInstance().log("System", "PRODUCT_EXPORT", "SUCCESS");
                    return new Response(true, "Xuất file CSV thành công", savedPath);
                } catch (Exception e) {
                    e.printStackTrace();
                    ServerLogger.getInstance().log("System", "PRODUCT_EXPORT", "FAILED");
                    return new Response(false, "Lỗi xuất file CSV: " + e.getMessage(), null);
                }

            case "PRODUCT_IMPORT":
                try {
                    String filePath = (String) request.getData();
                    CsvImportUtil importUtil = CsvImportUtil.getInstance();
                    List<Product> importedProducts = importUtil.importProducts(filePath);
                    for (Product product : importedProducts) {
                        productDAO.add(product);
                    }
                    ServerLogger.getInstance().log("System", "PRODUCT_IMPORT", "SUCCESS");
                    return new Response(true, "Nhập file CSV thành công, đã thêm " + importedProducts.size() + " sản phẩm", importedProducts.size());
                } catch (Exception e) {
                    e.printStackTrace();
                    ServerLogger.getInstance().log("System", "PRODUCT_IMPORT", "FAILED");
                    return new Response(false, "Lỗi nhập file CSV: " + e.getMessage(), null);
                }

                // Thay thế case DASHBOARD_STATS cũ bằng 3 case này
            case "DASHBOARD_GET_INVOICES":
                String dateFilter = (String) request.getData(); // Ví dụ: "2026-05-17"
                return new Response(true, "Thành công", orderDAO.getOrdersByDate(dateFilter));

            case "DASHBOARD_INVOICE_DETAILS":
                int orderId = (int) request.getData();
                return new Response(true, "Thành công", orderDAO.getOrderDetails(orderId));

            case "DASHBOARD_CHART_STATS":
                String period = (String) request.getData(); // Hứng bộ lọc từ giao diện
                Map<String, Object> stats = new HashMap<>();
                stats.put("chart_data", orderDAO.getRevenueByPeriod(period));
                stats.put("top_products", orderDAO.getTopSellingProductsByPeriod(period));
                return new Response(true, "Thành công", stats);

            case "USER_GET_ALL":
                return new Response(true, "Lấy danh sách tài khoản thành công", userDAO.findAll());

            case "USER_CREATE":
                User userCreate = (User) request.getData();
                // Hash password
                String hashedCreatePassword = BCrypt.withDefaults().hashToString(12, userCreate.getPassword().toCharArray());
                userCreate.setPassword(hashedCreatePassword);
                boolean userCreateSuccess = userDAO.add(userCreate);
                ServerLogger.getInstance().log("System", "USER_CREATE", userCreateSuccess ? "SUCCESS" : "FAILED");
                return new Response(userCreateSuccess, userCreateSuccess ? "Thêm tài khoản thành công" : "Thêm tài khoản thất bại", null);

            case "USER_UPDATE":
                User userUpdate = (User) request.getData();
                // Hash password
                String hashedUpdatePassword = BCrypt.withDefaults().hashToString(12, userUpdate.getPassword().toCharArray());
                userUpdate.setPassword(hashedUpdatePassword);
                boolean userUpdateSuccess = userDAO.update(userUpdate);
                ServerLogger.getInstance().log("System", "USER_UPDATE", userUpdateSuccess ? "SUCCESS" : "FAILED");
                return new Response(userUpdateSuccess, userUpdateSuccess ? "Cập nhật tài khoản thành công" : "Cập nhật tài khoản thất bại", null);

            case "USER_DELETE":
                int userId = (int) request.getData();
                boolean userDeleteSuccess = userDAO.delete(userId);
                ServerLogger.getInstance().log("System", "USER_DELETE", userDeleteSuccess ? "SUCCESS" : "FAILED");
                return new Response(userDeleteSuccess, userDeleteSuccess ? "Xóa tài khoản thành công" : "Xóa tài khoản thất bại", null);

            default:
                return new Response(false, "Unknown action: " + action, null);
        }
    }

    private Response handleLogin(Request request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> loginData = (Map<String, String>) request.getData();
            String username = loginData.get("username");
            String password = loginData.get("password");

            User user = userDAO.findByUsername(username);

            if (user != null) {
                // Kiểm tra mật khẩu plaintext hoặc BCrypt
                boolean passwordMatch = password.equals(user.getPassword()) ||
                        BCrypt.verifyer().verify(password.toCharArray(), user.getPassword()).verified;
                if (passwordMatch) {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("username", user.getUsername());
                    userInfo.put("role", user.getRole());
                    ServerLogger.getInstance().log(username, "LOGIN", "SUCCESS");
                    return new Response(true, "Login successful", userInfo);
                }
            }
            ServerLogger.getInstance().log(username, "LOGIN", "FAILED");
            return new Response(false, "Invalid username or password", null);
        } catch (Exception e) {
            e.printStackTrace();
            ServerLogger.getInstance().log("Unknown", "LOGIN", "ERROR");
            return new Response(false, "Error during login: " + e.getMessage(), null);
        }
    }
}