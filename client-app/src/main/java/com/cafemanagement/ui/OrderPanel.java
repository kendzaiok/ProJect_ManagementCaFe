package com.cafemanagement.ui;

import com.cafemanagement.client.ClientConnection;
import com.cafemanagement.dto.Request;
import com.cafemanagement.dto.Response;
import com.cafemanagement.model.Product;
import com.cafemanagement.model.CafeTable;
import com.cafemanagement.model.Order;
import com.cafemanagement.model.OrderItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderPanel extends JPanel {

    // Cột 1: Bàn
    private JPanel tablePanel;
    private JPanel takeawayPanel;
    private DefaultListModel<String> takeawayListModel;
    private List<CafeTable> listTables = new ArrayList<>(); // Lưu danh sách bàn
    private JButton currentSelectedTableBtn = null; // Theo dõi nút bàn đang chọn

    // Cột 2: Sản phẩm
    private JPanel productPanel;
    private JTextField txtSearch;
    private List<Product> allProducts = new ArrayList<>(); // Bộ nhớ đệm cho Live Search

    // Cột 3: Giỏ hàng
    private JTable cartTable;
    private DefaultTableModel cartTableModel;
    private JLabel lblTotalAmount;
    private JLabel lblHeader;
    private int currentSelectedTableId = -1;

    // Màu sắc chuẩn POS
    private final Color COLOR_PRIMARY = Color.decode("#2C3E50");
    private final Color COLOR_GREEN = Color.decode("#2ECC71");
    private final Color COLOR_RED = Color.decode("#E74C3C");
    private final Color COLOR_BLUE = Color.decode("#3498DB");
    private final Color COLOR_PURPLE = Color.decode("#8E44AD");
    private final Color COLOR_ORANGE = Color.decode("#F39C12");

    public OrderPanel() {
        setLayout(new GridLayout(1, 3, 10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        initTableColumn();
        initProductColumn();
        initCartColumn();

        loadTablesFromServer();
        loadProductsFromServer();
    }

    // ================= 1. CỘT TRÁI: BÀN =================
    private void initTableColumn() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));

        // KHẮC PHỤC LỖI NÚT KHỔNG LỒ: Dùng JPanel bọc BorderLayout.NORTH
        tablePanel = new JPanel(new GridLayout(0, 2, 10, 10));
        tablePanel.setBackground(Color.WHITE);
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBackground(Color.WHITE);
        tableWrapper.setBorder(new EmptyBorder(10, 10, 10, 10));
        tableWrapper.add(tablePanel, BorderLayout.NORTH);

        JScrollPane scrollTable = new JScrollPane(tableWrapper);
        scrollTable.setBorder(BorderFactory.createLineBorder(COLOR_PRIMARY, 1));
        scrollTable.getVerticalScrollBar().setUnitIncrement(16);

        // Tab Mang Đi
        takeawayPanel = new JPanel(new BorderLayout(10, 10));
        takeawayPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        takeawayPanel.setBackground(Color.WHITE);

        JButton btnTaoDonMoi = new JButton("+ TẠO ĐƠN MỚI");
        btnTaoDonMoi.setBackground(COLOR_ORANGE);
        btnTaoDonMoi.setForeground(Color.WHITE);
        btnTaoDonMoi.setFont(new Font("Arial", Font.BOLD, 14));

        takeawayListModel = new DefaultListModel<>();
        JList<String> listTakeaway = new JList<>(takeawayListModel);
        JScrollPane scrollTakeaway = new JScrollPane(listTakeaway);
        scrollTakeaway.setBorder(BorderFactory.createTitledBorder("Đang lên món"));

        takeawayPanel.add(btnTaoDonMoi, BorderLayout.NORTH);
        takeawayPanel.add(scrollTakeaway, BorderLayout.CENTER);

        tabbedPane.addTab("TẠI BÀN", scrollTable);
        tabbedPane.addTab("MANG ĐI", takeawayPanel);

        add(tabbedPane);
    }

    // ================= 2. CỘT GIỮA: THỰC ĐƠN & TÌM KIẾM =================
    private void initProductColumn() {
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setBackground(Color.WHITE);

        // Thanh tìm kiếm
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.add(new JLabel(" TÌM MÓN: "), BorderLayout.WEST);

        txtSearch = new JTextField();
        txtSearch.setFont(new Font("Arial", Font.PLAIN, 14));
        txtSearch.setPreferredSize(new Dimension(100, 35));

        // CHỨC NĂNG LIVE SEARCH (Gõ chữ hiện món lập tức)
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterProducts(); }
            public void removeUpdate(DocumentEvent e) { filterProducts(); }
            public void changedUpdate(DocumentEvent e) { filterProducts(); }
        });

        searchPanel.add(txtSearch, BorderLayout.CENTER);

        // Lưới sản phẩm chống kéo giãn
        productPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        productPanel.setBackground(Color.WHITE);
        JPanel productWrapper = new JPanel(new BorderLayout());
        productWrapper.setBackground(Color.WHITE);
        productWrapper.setBorder(new EmptyBorder(10, 10, 10, 10));
        productWrapper.add(productPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(productWrapper);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel);
    }

    // ================= 3. CỘT PHẢI: GIỎ HÀNG =================
    private void initCartColumn() {
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setBackground(Color.WHITE);

        lblHeader = new JLabel("CHƯA CHỌN ĐƠN NÀO", SwingConstants.CENTER);
        lblHeader.setFont(new Font("Arial", Font.BOLD, 18));
        lblHeader.setOpaque(true);
        lblHeader.setBackground(COLOR_PRIMARY);
        lblHeader.setForeground(Color.WHITE);
        lblHeader.setPreferredSize(new Dimension(100, 50));

        String[] columns = {"ID", "Tên món", "SL", "Tiền", "Ghi chú"};
        cartTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; } // Khóa không cho sửa trực tiếp trên bảng
        };
        cartTable = new JTable(cartTableModel);
        cartTable.setFont(new Font("Arial", Font.PLAIN, 14));
        cartTable.setRowHeight(25);

        cartTable.getColumnModel().getColumn(0).setMinWidth(0);
        cartTable.getColumnModel().getColumn(0).setMaxWidth(0);
        cartTable.getColumnModel().getColumn(0).setWidth(0);

        JScrollPane tableScroll = new JScrollPane(cartTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JPanel bottomPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        bottomPanel.setBackground(Color.WHITE);

        JButton btnGhiChu = new JButton("THÊM GHI CHÚ");
        btnGhiChu.setBackground(COLOR_PURPLE);
        btnGhiChu.setForeground(Color.WHITE);
        btnGhiChu.setFont(new Font("Arial", Font.BOLD, 14));

        lblTotalAmount = new JLabel("TỔNG TIỀN: 0 VNĐ", SwingConstants.CENTER);
        lblTotalAmount.setFont(new Font("Arial", Font.BOLD, 18));
        lblTotalAmount.setForeground(COLOR_RED);
        lblTotalAmount.setOpaque(true);
        lblTotalAmount.setBackground(Color.WHITE);
        lblTotalAmount.setBorder(BorderFactory.createLineBorder(COLOR_RED, 2));

        JButton btnInBill = new JButton("IN BILL TẠM");
        btnInBill.setBackground(COLOR_BLUE);
        btnInBill.setForeground(Color.WHITE);
        btnInBill.setFont(new Font("Arial", Font.BOLD, 14));

        JButton btnThanhToan = new JButton("CHỐT THANH TOÁN");
        btnThanhToan.setBackground(COLOR_RED);
        btnThanhToan.setForeground(Color.WHITE);
        btnThanhToan.setFont(new Font("Arial", Font.BOLD, 16));

        // SỰ KIỆN NÚT BẤM
        btnGhiChu.addActionListener(e -> addNoteToCartItem());
        btnInBill.addActionListener(e -> showTemporaryBillOnScreen());
        btnThanhToan.addActionListener(e -> processCheckout());

        bottomPanel.add(btnGhiChu);
        bottomPanel.add(lblTotalAmount);
        bottomPanel.add(btnInBill);
        bottomPanel.add(btnThanhToan);

        rightPanel.add(lblHeader, BorderLayout.NORTH);
        rightPanel.add(tableScroll, BorderLayout.CENTER);
        rightPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(rightPanel);
    }

    // ================= XỬ LÝ LOGIC UI & SERVER =================

    private void loadTablesFromServer() {
        try {
            Request req = new Request("TABLE_GET_ALL", null);
            Response res = ClientConnection.getInstance().sendRequest(req);
            if (res.isSuccess()) {
                listTables = (List<CafeTable>) res.getData();
                refreshTableUI();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshTableUI() {
        tablePanel.removeAll();
        for (CafeTable t : listTables) {
            JButton btnTable = new JButton(t.getName());
            btnTable.setFont(new Font("Arial", Font.BOLD, 14));
            btnTable.setForeground(Color.WHITE);
            btnTable.setPreferredSize(new Dimension(100, 100));

            // Xanh lá là mặc định. Đỏ nếu đang chọn hoặc Database báo có khách
            if (t.getId() == currentSelectedTableId || "OCCUPIED".equalsIgnoreCase(t.getStatus())) {
                btnTable.setBackground(COLOR_RED);
            } else {
                btnTable.setBackground(COLOR_GREEN);
            }

            btnTable.addActionListener(e -> {
                currentSelectedTableId = t.getId();
                lblHeader.setText(t.getName().toUpperCase());
                refreshTableUI(); // Cập nhật lại màu toàn bộ bàn
            });
            tablePanel.add(btnTable);
        }
        tablePanel.revalidate();
        tablePanel.repaint();
    }

    private void loadProductsFromServer() {
        try {
            Request req = new Request("PRODUCT_FIND_ALL", null);
            Response res = ClientConnection.getInstance().sendRequest(req);
            if (res.isSuccess()) {
                allProducts = (List<Product>) res.getData();
                filterProducts(); // Hiển thị sản phẩm lần đầu
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Logic Live Search (Tìm kiếm thời gian thực)
    private void filterProducts() {
        String keyword = txtSearch.getText().toLowerCase().trim();
        productPanel.removeAll();

        for (Product p : allProducts) {
            if (p.getName().toLowerCase().contains(keyword)) {
                String cardHtml = "<html><div style='text-align:center;'>"
                        + "<b style='font-size:12px; color:#2C3E50;'>" + p.getName() + "</b><br>"
                        + "<font style='font-size:14px; color:#E74C3C;'><b>" + p.getPrice().longValue() + "đ</b></font>"
                        + "</div></html>";

                JButton btnProduct = new JButton(cardHtml);
                btnProduct.setBackground(Color.WHITE);
                btnProduct.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
                btnProduct.setPreferredSize(new Dimension(130, 100)); // Cố định kích thước

                btnProduct.addActionListener(e -> addProductToCart(p));
                productPanel.add(btnProduct);
            }
        }
        productPanel.revalidate();
        productPanel.repaint();
    }

    // Logic Thêm món và CỘNG DỒN SỐ LƯỢNG
    private void addProductToCart(Product p) {
        boolean found = false;
        // Quét xem món đã có trong giỏ chưa
        for (int i = 0; i < cartTableModel.getRowCount(); i++) {
            int existingId = (int) cartTableModel.getValueAt(i, 0);
            if (existingId == p.getId()) {
                // Đã tồn tại -> Cộng dồn
                int currentQty = (int) cartTableModel.getValueAt(i, 2);
                cartTableModel.setValueAt(currentQty + 1, i, 2);
                found = true;
                break;
            }
        }
        // Nếu chưa có thì thêm dòng mới
        if (!found) {
            cartTableModel.addRow(new Object[]{p.getId(), p.getName(), 1, p.getPrice(), ""});
        }
        updateTotalAmount();
    }

    // Chức năng Thêm ghi chú
    private void addNoteToCartItem() {
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng click chọn 1 món trong giỏ hàng để thêm ghi chú!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String currentNote = cartTableModel.getValueAt(selectedRow, 4).toString();
        String itemName = cartTableModel.getValueAt(selectedRow, 1).toString();

        String newNote = JOptionPane.showInputDialog(this, "Nhập ghi chú cho món [" + itemName + "]:", currentNote);
        if (newNote != null) {
            cartTableModel.setValueAt(newNote, selectedRow, 4);
        }
    }

    private void updateTotalAmount() {
        double total = 0;
        for (int i = 0; i < cartTableModel.getRowCount(); i++) {
            int quantity = (int) cartTableModel.getValueAt(i, 2);
            BigDecimal price = (BigDecimal) cartTableModel.getValueAt(i, 3);
            total += (quantity * price.doubleValue());
        }
        lblTotalAmount.setText("TỔNG TIỀN: " + (long)total + " VNĐ");
    }

    // ================= XUẤT BILL TẠM LÊN MÀN HÌNH =================
    private void showTemporaryBillOnScreen() {
        if (currentSelectedTableId == -1 || cartTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn bàn và thêm món trước khi in bill!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Tạo nội dung Bill Text
        StringBuilder billText = new StringBuilder();
        billText.append("================================================\n");
        billText.append("                 CAFE ÔNG GIÁO\n");
        billText.append("     ĐC: 123 Đường Bờ Biển, Đà Nẵng\n");
        billText.append("          SĐT: 0987.654.321\n");
        billText.append("================================================\n");
        billText.append("               HÓA ĐƠN TẠM TÍNH\n");
        billText.append("Bàn số: ").append(currentSelectedTableId).append("\n");
        billText.append("Ngày: ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date())).append("\n");
        billText.append("------------------------------------------------\n");
        billText.append(String.format("%-25s %-5s %s\n", "Tên món", "SL", "Thành tiền"));
        billText.append("------------------------------------------------\n");

        double total = 0;
        for (int i = 0; i < cartTableModel.getRowCount(); i++) {
            String name = cartTableModel.getValueAt(i, 1).toString();
            int quantity = (int) cartTableModel.getValueAt(i, 2);
            BigDecimal price = (BigDecimal) cartTableModel.getValueAt(i, 3);
            String note = cartTableModel.getValueAt(i, 4).toString();
            double subtotal = quantity * price.doubleValue();
            total += subtotal;

            if (name.length() > 22) name = name.substring(0, 19) + "...";
            billText.append(String.format("%-25s %-5d %d\n", name, quantity, (long)subtotal));
            if (!note.trim().isEmpty()) {
                billText.append("  (Ghi chú: ").append(note).append(")\n");
            }
        }

        billText.append("------------------------------------------------\n");
        billText.append(String.format("%-29s %d VNĐ\n", "TỔNG CỘNG:", (long)total));
        billText.append("================================================\n");
        billText.append("        CẢM ƠN QUÝ KHÁCH & HẸN GẶP LẠI!\n");
        billText.append("================================================\n");

        // Tạo Popup Dialog hiển thị Bill
        JDialog billDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Hóa Đơn Tạm Tính", true);
        billDialog.setLayout(new BorderLayout());

        JTextArea txtArea = new JTextArea(billText.toString());
        txtArea.setFont(new Font("Monospaced", Font.PLAIN, 14)); // Font chữ đánh máy chuẩn Bill
        txtArea.setEditable(false);
        txtArea.setMargin(new Insets(10, 10, 10, 10));

        JButton btnExport = new JButton("Xuất file TXT");
        btnExport.setBackground(COLOR_GREEN);
        btnExport.setForeground(Color.WHITE);
        btnExport.addActionListener(e -> {
            exportBillToFile(billText.toString());
        });

        billDialog.add(new JScrollPane(txtArea), BorderLayout.CENTER);
        billDialog.add(btnExport, BorderLayout.SOUTH);
        billDialog.setSize(400, 550);
        billDialog.setLocationRelativeTo(this);
        billDialog.setVisible(true);
    }

    // Lưu file TXT (Đáp ứng tiêu chí 9.6 trong PDF)
    private void exportBillToFile(String content) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File("HoaDon_Ban_" + currentSelectedTableId + ".txt"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(fileChooser.getSelectedFile()))) {
                writer.print(content);
                JOptionPane.showMessageDialog(this, "Xuất file thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi lưu file: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void processCheckout() {
        if (currentSelectedTableId == -1 || cartTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn bàn và thêm món!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double total = 0;
        List<OrderItem> items = new ArrayList<>();
        for (int i = 0; i < cartTableModel.getRowCount(); i++) {
            int quantity = (int) cartTableModel.getValueAt(i, 2);
            BigDecimal price = (BigDecimal) cartTableModel.getValueAt(i, 3);
            total += (quantity * price.doubleValue());

            OrderItem item = new OrderItem();
            item.setProductId((int) cartTableModel.getValueAt(i, 0));
            item.setQuantity(quantity);
            item.setPrice(price);
            items.add(item);
        }

        Order order = new Order();
        order.setTableId(currentSelectedTableId);
        order.setUserId(1);
        order.setTotalPrice(BigDecimal.valueOf(total));
        order.setStatus("Đã thanh toán");
        order.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        Map<String, Object> mapData = new HashMap<>();
        mapData.put("order", order);
        mapData.put("items", items);

        try {
            Request req = new Request("ORDER_CREATE", mapData);
            Response res = ClientConnection.getInstance().sendRequest(req);

            if (res.isSuccess()) {
                JOptionPane.showMessageDialog(this, "Thanh toán thành công!\nThu: " + (long)total + " VNĐ", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                cartTableModel.setRowCount(0);
                lblTotalAmount.setText("TỔNG TIỀN: 0 VNĐ");
                lblHeader.setText("CHƯA CHỌN ĐƠN NÀO");
                currentSelectedTableId = -1;
                loadTablesFromServer(); // Cập nhật lại màu toàn bộ bàn
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi thanh toán: " + res.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Mất kết nối đến Server!", "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadTablesAndProducts() {
        loadTablesFromServer();
        loadProductsFromServer();
    }
}