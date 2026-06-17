package com.cafemanagement.dao;

import com.cafemanagement.database.DatabaseConnection;
import com.cafemanagement.model.Order;
import com.cafemanagement.model.OrderItem;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderDAOImpl implements OrderDAO {

    @Override
    public boolean add(Order order) {
        String sql = "INSERT INTO orders (table_id, user_id, total_price, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, order.getTableId());
            pstmt.setInt(2, order.getUserId());
            pstmt.setBigDecimal(3, order.getTotalPrice());
            pstmt.setString(4, order.getStatus());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    order.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean update(Order order) {
        String sql = "UPDATE orders SET table_id = ?, user_id = ?, total_price = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, order.getTableId());
            pstmt.setInt(2, order.getUserId());
            pstmt.setBigDecimal(3, order.getTotalPrice());
            pstmt.setString(4, order.getStatus());
            pstmt.setInt(5, order.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM orders WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Order findById(int id) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return extractOrder(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Order> findAll() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                orders.add(extractOrder(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    @Override
    public List<Order> findByTableId(int tableId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE table_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tableId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                orders.add(extractOrder(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    @Override
    public List<Order> findByStatus(String status) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE status = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                orders.add(extractOrder(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    @Override
    public List<Order> search(String keyword) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.* FROM orders o JOIN cafe_tables t ON o.table_id = t.id " +
                     "WHERE t.table_number LIKE ? OR o.status LIKE ? ORDER BY o.created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                orders.add(extractOrder(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    private Order extractOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setTableId(rs.getInt("table_id"));
        order.setUserId(rs.getInt("user_id"));
        order.setTotalPrice(rs.getBigDecimal("total_price"));
        order.setStatus(rs.getString("status"));
        order.setCreatedAt(rs.getTimestamp("created_at"));
        return order;
    }


    @Override
    public boolean createOrder(Order order, List<OrderItem> items) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            // 1. TẮT AUTO COMMIT ĐỂ BẮT ĐẦU TRANSACTION NÂNG CAO
            conn.setAutoCommit(false);

            // 2. Insert Hóa Đơn (Order)
            String orderSql = "INSERT INTO orders (table_id, user_id, total_price, status) VALUES (?, ?, ?, ?)";
            try (PreparedStatement orderPstmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                orderPstmt.setInt(1, order.getTableId());
                orderPstmt.setInt(2, order.getUserId());
                orderPstmt.setBigDecimal(3, order.getTotalPrice());
                orderPstmt.setString(4, order.getStatus());
                int affectedRows = orderPstmt.executeUpdate();

                if (affectedRows == 0) {
                    conn.rollback();
                    return false;
                }

                // Lấy ID hóa đơn tự động tăng
                try (ResultSet rs = orderPstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        order.setId(rs.getInt(1));
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // 3. Insert Chi Tiết Hóa Đơn (OrderItems) - Dùng Batch để tăng hiệu suất
            String itemSql = "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
            try (PreparedStatement itemPstmt = conn.prepareStatement(itemSql)) {
                for (OrderItem item : items) {
                    item.setOrderId(order.getId());
                    itemPstmt.setInt(1, item.getOrderId());
                    itemPstmt.setInt(2, item.getProductId());
                    itemPstmt.setInt(3, item.getQuantity());
                    itemPstmt.setBigDecimal(4, item.getPrice());
                    itemPstmt.addBatch(); // Gom lệnh để chạy 1 lần
                }
                itemPstmt.executeBatch();
            }

            // 4. CẬP NHẬT TRẠNG THÁI BÀN THÀNH 'TRỐNG' SAU KHI THANH TOÁN XONG (Chuẩn file DOCX)
            String updateTableSql = "UPDATE cafe_tables SET status = 'TRỐNG' WHERE id = ?";
            try (PreparedStatement updateTablePstmt = conn.prepareStatement(updateTableSql)) {
                updateTablePstmt.setInt(1, order.getTableId());
                updateTablePstmt.executeUpdate();
            }

            // 5. NẾU KHÔNG CÓ LỖI NÀO -> XÁC NHẬN LƯU (COMMIT)
            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            // NẾU CÓ BẤT KỲ LỖI NÀO XẢY RA -> QUAY XE, KHÔNG LƯU GÌ CẢ (ROLLBACK)
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        } finally {
            // Mở lại Auto Commit để không ảnh hưởng các chức năng khác
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public BigDecimal getTotalRevenue() {
        String sql = "SELECT SUM(total_price) AS total_revenue FROM orders";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getBigDecimal("total_revenue") != null ? rs.getBigDecimal("total_revenue") : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    @Override
    public int getTotalOrders() {
        String sql = "SELECT COUNT(*) AS total_orders FROM orders";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total_orders");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public List<Map<String, Object>> getTopSellingProducts() {
        List<Map<String, Object>> products = new ArrayList<>();
        String sql = "SELECT p.name, SUM(oi.quantity) AS total_quantity, SUM(oi.quantity * oi.price) AS total_revenue " +
                     "FROM order_items oi " +
                     "JOIN products p ON oi.product_id = p.id " +
                     "GROUP BY p.id, p.name " +
                     "ORDER BY total_quantity DESC " +
                     "LIMIT 10";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> product = new HashMap<>();
                product.put("name", rs.getString("name"));
                product.put("total_quantity", rs.getInt("total_quantity"));
                product.put("total_revenue", rs.getBigDecimal("total_revenue"));
                products.add(product);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }
}
