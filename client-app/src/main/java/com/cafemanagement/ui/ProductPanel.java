package com.cafemanagement.ui;

import com.cafemanagement.client.ClientConnection;
import com.cafemanagement.dto.Request;
import com.cafemanagement.dto.Response;
import com.cafemanagement.model.Product;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductPanel extends JPanel {
    private JTable productTable;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private JButton btnAdd, btnUpdate, btnDelete, btnRefresh, btnSearch;
    private ClientConnection clientConnection;

    // Lưu danh sách thật để lấy dữ liệu sửa/xóa chính xác
    private List<Product> currentProductList = new ArrayList<>();

    // Màu sắc chuẩn Sapo
    private final Color COLOR_GREEN = Color.decode("#2ECC71");
    private final Color COLOR_BLUE = Color.decode("#3498DB");
    private final Color COLOR_RED = Color.decode("#E74C3C");

    public ProductPanel() {
        clientConnection = ClientConnection.getInstance();
        initializeUI();
        loadProducts();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(Color.WHITE);

        // ================= TOP PANEL (NÚT BẤM & TÌM KIẾM) =================
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);

        // Các nút thao tác
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setBackground(Color.WHITE);

        btnAdd = createButton("Thêm SP Mới", COLOR_GREEN, Color.WHITE);
        btnUpdate = createButton("Sửa SP Chọn", COLOR_BLUE, Color.WHITE);
        btnDelete = createButton("Xóa SP Chọn", COLOR_RED, Color.WHITE);
        btnRefresh = createButton("Làm Mới", Color.WHITE, Color.BLACK);

        JButton btnExport = createButton("Export CSV", Color.LIGHT_GRAY, Color.BLACK);
        JButton btnImport = createButton("Import CSV", Color.LIGHT_GRAY, Color.BLACK);

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnExport);
        buttonPanel.add(btnImport);

        // Tìm kiếm
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        searchPanel.setBackground(Color.WHITE);
        txtSearch = new JTextField(15);
        txtSearch.setPreferredSize(new Dimension(150, 30));
        btnSearch = new JButton("Tìm");
        btnSearch.setBackground(Color.DARK_GRAY);
        btnSearch.setForeground(Color.WHITE);
        searchPanel.add(new JLabel("Tìm kiếm:"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);

        topPanel.add(buttonPanel, BorderLayout.CENTER);
        topPanel.add(searchPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // ================= CENTER PANEL (BẢNG SẢN PHẨM) =================
        String[] columnNames = {"Mã SP", "Tên Sản Phẩm", "Giá Bán", "Danh mục", "Hiện trên POS"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 4) return Boolean.class; // Hiển thị Checkbox thay vì chữ true/false
                return super.getColumnClass(columnIndex);
            }
        };

        productTable = new JTable(tableModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.setRowHeight(28);
        productTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        productTable.getTableHeader().setBackground(Color.decode("#ECF0F1"));

        productTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        productTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        productTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        productTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        productTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // ================= GÁN SỰ KIỆN NÚT BẤM =================
        btnAdd.addActionListener(e -> showProductDialog(null)); // null = Thêm mới
        btnUpdate.addActionListener(e -> {
            int selectedRow = productTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm cần sửa!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Product p = currentProductList.get(selectedRow);
            showProductDialog(p); // Có dữ liệu = Sửa
        });
        btnDelete.addActionListener(e -> deleteProduct());
        btnRefresh.addActionListener(e -> loadProducts());
        btnSearch.addActionListener(e -> searchProducts());
        btnExport.addActionListener(e -> exportProducts());
        btnImport.addActionListener(e -> importProducts());
    }

    // Hàm tạo nút bấm với màu sắc tùy chỉnh
    private JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(110, 32));
        if (bg.equals(Color.WHITE)) {
            btn.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        } else {
            btn.setBorder(BorderFactory.createEmptyBorder());
        }
        return btn;
    }

    // ================= DIALOG THÊM / SỬA (CHUẨN UI SAPO) =================
    private void showProductDialog(Product product) {
        boolean isEdit = (product != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEdit ? "SỬA SẢN PHẨM" : "THÊM SẢN PHẨM MỚI", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(450, 350);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Các trường nhập liệu
        JTextField txtName = new JTextField(20);
        JTextField txtPrice = new JTextField(20);
        JTextField txtCategory = new JTextField(20);
        JCheckBox chkPos = new JCheckBox("Hiển thị menu bên tab POS", true);

        JButton btnUpload = new JButton("Tải ảnh lên");
        JLabel lblImageName = new JLabel("Chưa có ảnh");
        lblImageName.setFont(new Font("Arial", Font.ITALIC, 11));

        final String[] selectedImagePath = {null}; // Mảng 1 phần tử để chứa link ảnh

        // Nạp dữ liệu nếu là Edit
        if (isEdit) {
            txtName.setText(product.getName());
            txtPrice.setText(product.getPrice().toString());
            txtCategory.setText(product.getCategory());
            chkPos.setSelected(product.isPos());
            if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
                selectedImagePath[0] = product.getImagePath();
                lblImageName.setText("Đã chọn ảnh gốc");
            }
        }

        // Sự kiện Upload Ảnh
        btnUpload.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "png", "jpeg"));
            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                selectedImagePath[0] = file.getAbsolutePath(); // Lưu đường dẫn tuyệt đối
                lblImageName.setText(file.getName());
            }
        });

        // Bố cục Form
        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; formPanel.add(new JLabel("Tên SP:"), gbc);
        gbc.gridx = 1; formPanel.add(txtName, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; formPanel.add(new JLabel("Giá Bán:"), gbc);
        gbc.gridx = 1; formPanel.add(txtPrice, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; formPanel.add(new JLabel("Danh mục:"), gbc);
        gbc.gridx = 1; formPanel.add(txtCategory, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; formPanel.add(new JLabel("Tùy chọn:"), gbc);
        gbc.gridx = 1; formPanel.add(chkPos, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; formPanel.add(new JLabel("Ảnh minh họa:"), gbc);

        JPanel imgPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        imgPanel.add(btnUpload);
        imgPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        imgPanel.add(lblImageName);
        gbc.gridx = 1; formPanel.add(imgPanel, gbc);

        // Buttons
        JPanel btnDialogPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOk = new JButton(isEdit ? "Cập nhật" : "Tạo mới");
        btnOk.setBackground(COLOR_BLUE);
        btnOk.setForeground(Color.WHITE);
        JButton btnCancel = new JButton("Hủy");
        btnDialogPanel.add(btnOk);
        btnDialogPanel.add(btnCancel);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(btnDialogPanel, BorderLayout.SOUTH);

        // Sự kiện OK
        btnOk.addActionListener(e -> {
            if (txtName.getText().trim().isEmpty() || txtPrice.getText().trim().isEmpty() || txtCategory.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập đủ Tên, Giá và Danh mục!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                BigDecimal price = new BigDecimal(txtPrice.getText().trim());
                if (price.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();

                Product p = isEdit ? product : new Product();
                p.setName(txtName.getText().trim());
                p.setPrice(price);
                p.setCategory(txtCategory.getText().trim());
                p.setPos(chkPos.isSelected());
                p.setImagePath(selectedImagePath[0]);

                saveProductToServer(p, isEdit, dialog);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Giá bán phải là số lớn hơn 0!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            }
        });

        btnCancel.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    // ================= XỬ LÝ SERVER (TẠO/SỬA/XÓA/TÌM KIẾM) =================

    private void saveProductToServer(Product p, boolean isEdit, JDialog dialog) {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                String action = isEdit ? "PRODUCT_UPDATE" : "PRODUCT_CREATE";
                Response response = clientConnection.sendRequest(new Request(action, p));
                if (!response.isSuccess()) throw new Exception(response.getMessage());
                return true;
            }
            @Override
            protected void done() {
                try {
                    get();
                    dialog.dispose();
                    JOptionPane.showMessageDialog(ProductPanel.this, isEdit ? "Cập nhật thành công!" : "Thêm mới thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    loadProducts();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Lỗi server: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public void loadProducts() {
        new SwingWorker<List<Product>, Void>() {
            @Override
            protected List<Product> doInBackground() throws Exception {
                Response response = clientConnection.sendRequest(new Request("PRODUCT_FIND_ALL", null));
                if (response.isSuccess()) return (List<Product>) response.getData();
                throw new Exception(response.getMessage());
            }
            @Override
            protected void done() {
                try {
                    currentProductList = get();
                    updateTableData(currentProductList);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi tải sản phẩm: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void searchProducts() {
        final String keyword = txtSearch.getText().trim();
        new SwingWorker<List<Product>, Void>() {
            @Override
            protected List<Product> doInBackground() throws Exception {
                Response response = clientConnection.sendRequest(new Request("PRODUCT_SEARCH", keyword));
                if (response.isSuccess()) return (List<Product>) response.getData();
                throw new Exception(response.getMessage());
            }
            @Override
            protected void done() {
                try {
                    currentProductList = get();
                    updateTableData(currentProductList);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi tìm kiếm: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void deleteProduct() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm cần xóa!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int productId = currentProductList.get(selectedRow).getId();

        int confirm = JOptionPane.showConfirmDialog(this, "Bạn chắc chắn muốn xóa sản phẩm này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    Response response = clientConnection.sendRequest(new Request("PRODUCT_DELETE", productId));
                    if (!response.isSuccess()) throw new Exception(response.getMessage());
                    return true;
                }
                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(ProductPanel.this, "Xóa thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        loadProducts();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi xóa: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void updateTableData(List<Product> products) {
        tableModel.setRowCount(0);
        for (Product product : products) {
            tableModel.addRow(new Object[]{
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    product.getCategory(),
                    product.isPos() // Điền thẳng boolean vào bảng để nó hiện Checkbox
            });
        }
    }

    // ================= XỬ LÝ CSV (ĐÃ CẬP NHẬT TRƯỜNG ẢNH VÀ POS) =================

    private void exportProducts() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn đường dẫn lưu file CSV");
        fileChooser.setSelectedFile(new File("products.csv"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".csv")) filePath += ".csv";
            final String finalFilePath = filePath;

            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(finalFilePath))) {
                        writer.write("ID,Tên sản phẩm,Giá,Danh mục,Đường dẫn ảnh,Hiển thị POS");
                        writer.newLine();
                        for (Product product : currentProductList) {
                            String img = product.getImagePath() != null ? product.getImagePath() : "";
                            writer.write(product.getId() + "," + product.getName() + "," +
                                    product.getPrice().toString() + "," + product.getCategory() + "," +
                                    img + "," + product.isPos());
                            writer.newLine();
                        }
                    }
                    return finalFilePath;
                }
                @Override
                protected void done() {
                    try {
                        JOptionPane.showMessageDialog(ProductPanel.this, "Xuất file CSV thành công tại:\n" + get(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi xuất file CSV: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void importProducts() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn file CSV để nhập");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToImport = fileChooser.getSelectedFile();
            new SwingWorker<Integer, Void>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    int count = 0;
                    try (BufferedReader reader = new BufferedReader(new FileReader(fileToImport))) {
                        String line;
                        boolean isFirst = true;
                        while ((line = reader.readLine()) != null) {
                            if (isFirst) { isFirst = false; continue; }
                            String[] parts = line.split(",");
                            if (parts.length >= 4) {
                                Product p = new Product();
                                p.setName(parts[1].trim());
                                p.setPrice(new BigDecimal(parts[2].trim()));
                                p.setCategory(parts[3].trim());
                                p.setImagePath(parts.length > 4 ? parts[4].trim() : "");
                                p.setPos(parts.length > 5 ? Boolean.parseBoolean(parts[5].trim()) : true);

                                Response res = clientConnection.sendRequest(new Request("PRODUCT_CREATE", p));
                                if (res.isSuccess()) count++;
                            }
                        }
                    }
                    return count;
                }
                @Override
                protected void done() {
                    try {
                        JOptionPane.showMessageDialog(ProductPanel.this, "Nhập thành công " + get() + " sản phẩm!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        loadProducts();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi nhập file CSV: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }
}