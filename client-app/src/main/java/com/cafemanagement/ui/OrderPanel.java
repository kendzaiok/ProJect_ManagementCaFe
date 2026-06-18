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
import java.awt.image.BufferedImage;
import java.io.File;
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
    private JList<String> listTakeaway;
    private int takeawayCounter = 1;
    private List<CafeTable> listTables = new ArrayList<>();

    // Cột 2: Sản phẩm
    private JPanel productPanel;
    private JTextField txtSearch;
    private List<Product> allProducts = new ArrayList<>();

    // Cột 3: Giỏ hàng
    private JTable cartTable;
    private DefaultTableModel cartTableModel;
    private JLabel lblTotalAmount;
    private JLabel lblHeader;

    // BIẾN QUẢN LÝ TRẠNG THÁI (ĐỂ KHÓA GIỎ HÀNG)
    private int currentSelectedTableId = -1;
    private String currentSelectedTakeaway = null;
    private boolean isUpdatingSelection = false;

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

    // ================= HÀM KHÓA GIỎ HÀNG =================
    private boolean checkAndConfirmSwitch() {
        if (cartTableModel.getRowCount() > 0) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Giỏ hàng đang có món chưa thanh toán.\nNếu chọn bàn/đơn khác, giỏ hàng hiện tại sẽ bị xóa.\nBạn có muốn tiếp tục?",
                    "Xác nhận chuyển",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                cartTableModel.setRowCount(0);
                updateTotalAmount();
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    // ================= 1. CỘT TRÁI: BÀN & MANG ĐI =================
    private void initTableColumn() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));

        tablePanel = new JPanel(new GridLayout(0, 2, 10, 10));
        tablePanel.setBackground(Color.WHITE);
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBackground(Color.WHITE);
        tableWrapper.setBorder(new EmptyBorder(10, 10, 10, 10));
        tableWrapper.add(tablePanel, BorderLayout.NORTH);

        JScrollPane scrollTable = new JScrollPane(tableWrapper);
        scrollTable.setBorder(BorderFactory.createLineBorder(COLOR_PRIMARY, 1));
        scrollTable.getVerticalScrollBar().setUnitIncrement(16);

        takeawayPanel = new JPanel(new BorderLayout(10, 10));
        takeawayPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        takeawayPanel.setBackground(Color.WHITE);

        JButton btnTaoDonMoi = new JButton("+ TẠO ĐƠN MỚI");
        btnTaoDonMoi.setBackground(COLOR_ORANGE);
        btnTaoDonMoi.setForeground(Color.WHITE);
        btnTaoDonMoi.setFont(new Font("Arial", Font.BOLD, 14));
        btnTaoDonMoi.setFocusPainted(false);
        btnTaoDonMoi.setPreferredSize(new Dimension(100, 40));

        btnTaoDonMoi.addActionListener(e -> createNewTakeawayOrder());

        takeawayListModel = new DefaultListModel<>();
        listTakeaway = new JList<>(takeawayListModel);
        listTakeaway.setFont(new Font("Arial", Font.BOLD, 14));
        listTakeaway.setSelectionBackground(COLOR_ORANGE);
        listTakeaway.setSelectionForeground(Color.WHITE);

        listTakeaway.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || isUpdatingSelection) return;

            int selectedIdx = listTakeaway.getSelectedIndex();
            if (selectedIdx == -1) return;

            String selectedOrder = listTakeaway.getSelectedValue();
            if (selectedOrder.equals(currentSelectedTakeaway)) return;

            if (!checkAndConfirmSwitch()) {
                isUpdatingSelection = true;
                if (currentSelectedTakeaway != null) {
                    listTakeaway.setSelectedValue(currentSelectedTakeaway, true);
                } else {
                    listTakeaway.clearSelection();
                }
                isUpdatingSelection = false;
                return;
            }

            currentSelectedTableId = 0;
            currentSelectedTakeaway = selectedOrder;
            lblHeader.setText(selectedOrder.toUpperCase());
            refreshTableUI();
        });

        JScrollPane scrollTakeaway = new JScrollPane(listTakeaway);
        scrollTakeaway.setBorder(BorderFactory.createTitledBorder("Đang lên món"));

        takeawayPanel.add(btnTaoDonMoi, BorderLayout.NORTH);
        takeawayPanel.add(scrollTakeaway, BorderLayout.CENTER);

        tabbedPane.addTab("TẠI BÀN", scrollTable);
        tabbedPane.addTab("MANG ĐI", takeawayPanel);

        add(tabbedPane);
    }

    private void createNewTakeawayOrder() {
        if (!checkAndConfirmSwitch()) return;

        String customerName = JOptionPane.showInputDialog(this, "Nhập tên khách hàng (Ghi chú):", "Tạo Đơn Mang Đi", JOptionPane.PLAIN_MESSAGE);
        if (customerName != null && !customerName.trim().isEmpty()) {
            String orderTitle = "Đơn #" + String.format("%02d", takeawayCounter++) + " - " + customerName.trim();
            takeawayListModel.addElement(orderTitle);
            listTakeaway.setSelectedIndex(takeawayListModel.getSize() - 1);
        }
    }

    // ================= 2. CỘT GIỮA: THỰC ĐƠN ĐÃ CẢI TIẾN UI =================
    private void initProductColumn() {
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setBackground(Color.WHITE);

        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.add(new JLabel(" TÌM MÓN: "), BorderLayout.WEST);

        txtSearch = new JTextField();
        txtSearch.setFont(new Font("Arial", Font.PLAIN, 14));
        txtSearch.setPreferredSize(new Dimension(100, 35));

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterProducts(); }
            public void removeUpdate(DocumentEvent e) { filterProducts(); }
            public void changedUpdate(DocumentEvent e) { filterProducts(); }
        });

        searchPanel.add(txtSearch, BorderLayout.CENTER);

        // Đổi thành GridLayout(0, 2) nhưng gài vào BorderLayout.NORTH để có thanh cuộn mượt và giữ nguyên kích thước
        productPanel = new JPanel(new GridLayout(0, 2, 12, 12));
        productPanel.setBackground(Color.WHITE);
        JPanel productWrapper = new JPanel(new BorderLayout());
        productWrapper.setBackground(Color.WHITE);
        productWrapper.setBorder(new EmptyBorder(10, 10, 10, 10));
        productWrapper.add(productPanel, BorderLayout.NORTH); // Phép màu chống "co rúm" nằm ở đây

        JScrollPane scrollPane = new JScrollPane(productWrapper);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scrollPane.getVerticalScrollBar().setUnitIncrement(20); // Lăn chuột nhanh hơn

        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel);
    }

    private void filterProducts() {
        String keyword = txtSearch.getText().toLowerCase().trim();
        productPanel.removeAll();

        for (Product p : allProducts) {
            // FIX: NẾU KHÔNG ĐÁNH DẤU POS THÌ BỎ QUA KHÔNG HIỂN THỊ
            if (!p.isPos()) continue;

            if (p.getName().toLowerCase().contains(keyword)) {
                // TẠO THẺ SẢN PHẨM MỚI CHUYÊN NGHIỆP HƠN
                JButton btnProduct = new JButton();
                btnProduct.setLayout(new BorderLayout());
                btnProduct.setBackground(Color.WHITE);
                btnProduct.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
                btnProduct.setPreferredSize(new Dimension(150, 180)); // Khóa cứng kích thước thẻ
                btnProduct.setCursor(new Cursor(Cursor.HAND_CURSOR));

                // 1. Load và hiển thị Ảnh
                JLabel lblImg = new JLabel(getScaledImage(p.getImagePath(), 120, 100));
                lblImg.setHorizontalAlignment(SwingConstants.CENTER);
                lblImg.setBorder(new EmptyBorder(5, 5, 5, 5));

                // 2. Định dạng Tên (cắt bớt nếu quá dài) và Giá
                String name = p.getName();
                if (name.length() > 20) name = name.substring(0, 17) + "...";

                String textHtml = "<html><div style='text-align:center;'>"
                        + "<b style='font-size:12px; color:#2C3E50;'>" + name + "</b><br>"
                        + "<font style='font-size:14px; color:#E74C3C;'><b>" + p.getPrice().longValue() + "đ</b></font>"
                        + "</div></html>";
                JLabel lblText = new JLabel(textHtml);
                lblText.setHorizontalAlignment(SwingConstants.CENTER);
                lblText.setBorder(new EmptyBorder(0, 5, 5, 5));

                // Ghép vào nút
                btnProduct.add(lblImg, BorderLayout.CENTER);
                btnProduct.add(lblText, BorderLayout.SOUTH);

                // Hover chuột đổi màu nhẹ
                btnProduct.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        btnProduct.setBackground(Color.decode("#F8F9FA"));
                    }
                    public void mouseExited(java.awt.event.MouseEvent evt) {
                        btnProduct.setBackground(Color.WHITE);
                    }
                });

                btnProduct.addActionListener(e -> addProductToCart(p));
                productPanel.add(btnProduct);
            }
        }
        productPanel.revalidate();
        productPanel.repaint();
    }

    // ================= HÀM HỖ TRỢ XỬ LÝ ẢNH =================
    private ImageIcon getScaledImage(String imagePath, int width, int height) {
        try {
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                File imgFile = new File(imagePath);
                if (imgFile.exists()) {
                    ImageIcon icon = new ImageIcon(imagePath);
                    Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    return new ImageIcon(img);
                }
            }
        } catch (Exception e) {
            // Bỏ qua và trả về ảnh mặc định
        }
        return createDefaultIcon(width, height);
    }

    private ImageIcon createDefaultIcon(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.decode("#ECF0F1")); // Màu nền xám nhạt
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));

        FontMetrics fm = g2d.getFontMetrics();
        String text = "NO IMAGE";
        int x = (width - fm.stringWidth(text)) / 2;
        int y = (height - fm.getHeight()) / 2 + fm.getAscent();

        g2d.drawString(text, x, y);
        g2d.dispose();
        return new ImageIcon(img);
    }

    // ================= 3. CỘT PHẢI: GIỎ HÀNG (GIỮ NGUYÊN) =================
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
            public boolean isCellEditable(int row, int column) { return false; }
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

    // ================= XỬ LÝ LOGIC UI & SERVER (GIỮ NGUYÊN) =================

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

            if (t.getId() == currentSelectedTableId) {
                btnTable.setBackground(COLOR_ORANGE);
            } else if ("OCCUPIED".equalsIgnoreCase(t.getStatus())) {
                btnTable.setBackground(COLOR_RED);
            } else {
                btnTable.setBackground(COLOR_GREEN);
            }

            btnTable.addActionListener(e -> {
                if (currentSelectedTableId == t.getId()) return;

                if (!checkAndConfirmSwitch()) return;

                currentSelectedTableId = t.getId();
                currentSelectedTakeaway = null;
                lblHeader.setText(t.getName().toUpperCase());

                isUpdatingSelection = true;
                listTakeaway.clearSelection();
                isUpdatingSelection = false;

                refreshTableUI();
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
                filterProducts();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addProductToCart(Product p) {
        if (currentSelectedTableId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn Bàn hoặc tạo Đơn Mang Đi trước khi thêm món!", "Hướng dẫn", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        boolean found = false;
        for (int i = 0; i < cartTableModel.getRowCount(); i++) {
            int existingId = (int) cartTableModel.getValueAt(i, 0);
            if (existingId == p.getId()) {
                int currentQty = (int) cartTableModel.getValueAt(i, 2);
                cartTableModel.setValueAt(currentQty + 1, i, 2);
                found = true;
                break;
            }
        }
        if (!found) {
            cartTableModel.addRow(new Object[]{p.getId(), p.getName(), 1, p.getPrice(), ""});
        }
        updateTotalAmount();
    }

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

    private void showTemporaryBillOnScreen() {
        if (currentSelectedTableId == -1 || cartTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn bàn/đơn và thêm món trước khi in bill!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        StringBuilder billText = new StringBuilder();
        billText.append("================================================\n");
        billText.append("                 CAFE ÔNG GIÁO\n");
        billText.append("     ĐC: 123 Đường Bờ Biển, Đà Nẵng\n");
        billText.append("          SĐT: 0987.654.321\n");
        billText.append("================================================\n");
        billText.append("               HÓA ĐƠN TẠM TÍNH\n");

        if (currentSelectedTableId == 0) {
            billText.append("Khách: ").append(currentSelectedTakeaway).append("\n");
        } else {
            billText.append("Bàn số: ").append(currentSelectedTableId).append("\n");
        }

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

        JDialog billDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Hóa Đơn Tạm", true);
        billDialog.setLayout(new BorderLayout());

        JTextArea txtArea = new JTextArea(billText.toString());
        txtArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
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

    private void exportBillToFile(String content) {
        JFileChooser fileChooser = new JFileChooser();
        String fileName = (currentSelectedTableId == 0) ? "HoaDon_MangDi.txt" : "HoaDon_Ban_" + currentSelectedTableId + ".txt";
        fileChooser.setSelectedFile(new java.io.File(fileName));
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
            JOptionPane.showMessageDialog(this, "Vui lòng chọn Bàn/Đơn và thêm món!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
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
        order.setStatus("PAID");
        order.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        Map<String, Object> mapData = new HashMap<>();
        mapData.put("order", order);
        mapData.put("items", items);

        try {
            Request req = new Request("ORDER_CREATE", mapData);
            Response res = ClientConnection.getInstance().sendRequest(req);

            if (res.isSuccess()) {
                JOptionPane.showMessageDialog(this, "Thanh toán thành công!\nThu: " + (long)total + " VNĐ", "Thông báo", JOptionPane.INFORMATION_MESSAGE);

                if (currentSelectedTableId == 0) {
                    int selectedTakeawayIdx = listTakeaway.getSelectedIndex();
                    if (selectedTakeawayIdx != -1) {
                        takeawayListModel.remove(selectedTakeawayIdx);
                    }
                }

                cartTableModel.setRowCount(0);
                updateTotalAmount();
                lblHeader.setText("CHƯA CHỌN ĐƠN NÀO");
                currentSelectedTableId = -1;
                currentSelectedTakeaway = null;

                isUpdatingSelection = true;
                listTakeaway.clearSelection();
                isUpdatingSelection = false;

                loadTablesFromServer();
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