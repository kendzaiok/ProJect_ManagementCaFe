package com.cafemanagement.ui;

import com.cafemanagement.client.ClientConnection;
import com.cafemanagement.dto.Request;
import com.cafemanagement.dto.Response;
import com.cafemanagement.model.Product;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductPanel extends JPanel {
    private JTable productTable;
    private DefaultTableModel tableModel;
    private JTextField txtId, txtName, txtPrice, txtCategory, txtSearch;
    private JButton btnAdd, btnUpdate, btnDelete, btnRefresh, btnSearch;
    private ClientConnection clientConnection;

    public ProductPanel() {
        clientConnection = ClientConnection.getInstance();
        initializeUI();
        loadProducts();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Tìm kiếm theo tên:"));
        txtSearch = new JTextField(20);
        searchPanel.add(txtSearch);
        btnSearch = new JButton("Tìm kiếm");
        searchPanel.add(btnSearch);
        add(searchPanel, BorderLayout.NORTH);

        // Table panel
        String[] columnNames = {"ID", "Tên sản phẩm", "Giá", "Danh mục"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        productTable = new JTable(tableModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.setAutoCreateRowSorter(true);
        productTable.getTableHeader().setReorderingAllowed(false);
        productTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        productTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        productTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        productTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && productTable.getSelectedRow() != -1) {
                int selectedRow = productTable.getSelectedRow();
                txtId.setText(tableModel.getValueAt(selectedRow, 0).toString());
                txtName.setText(tableModel.getValueAt(selectedRow, 1).toString());
                txtPrice.setText(tableModel.getValueAt(selectedRow, 2).toString());
                txtCategory.setText(tableModel.getValueAt(selectedRow, 3).toString());
            }
        });
        add(new JScrollPane(productTable), BorderLayout.CENTER);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Thông tin sản phẩm"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1;
        txtId = new JTextField(15);
        txtId.setEditable(false);
        formPanel.add(txtId, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Tên sản phẩm:"), gbc);
        gbc.gridx = 1;
        txtName = new JTextField(15);
        formPanel.add(txtName, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Giá:"), gbc);
        gbc.gridx = 1;
        txtPrice = new JTextField(15);
        formPanel.add(txtPrice, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Danh mục:"), gbc);
        gbc.gridx = 1;
        txtCategory = new JTextField(15);
        formPanel.add(txtCategory, gbc);

        // Button panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnAdd = new JButton("Thêm");
        btnUpdate = new JButton("Sửa");
        btnDelete = new JButton("Xóa");
        btnRefresh = new JButton("Làm mới");
        JButton btnExport = new JButton("Export CSV");
        JButton btnImport = new JButton("Import CSV");
        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnRefresh);
        btnPanel.add(btnExport);
        btnPanel.add(btnImport);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(btnPanel, gbc);

        add(formPanel, BorderLayout.SOUTH);

        // Button listeners
        btnAdd.addActionListener(e -> addProduct());
        btnUpdate.addActionListener(e -> updateProduct());
        btnDelete.addActionListener(e -> deleteProduct());
        btnRefresh.addActionListener(e -> loadProducts());
        btnSearch.addActionListener(e -> searchProducts());
        btnExport.addActionListener(e -> exportProducts());
        btnImport.addActionListener(e -> importProducts());
    }

    public void loadProducts() {
        new SwingWorker<List<Product>, Void>() {
            @Override
            protected List<Product> doInBackground() throws Exception {
                Response response = clientConnection.sendRequest(new Request("PRODUCT_FIND_ALL", null));
                if (response.isSuccess()) {
                    return (List<Product>) response.getData();
                } else {
                    throw new Exception(response.getMessage());
                }
            }

            @Override
            protected void done() {
                try {
                    List<Product> products = get();
                    tableModel.setRowCount(0);
                    for (Product product : products) {
                        tableModel.addRow(new Object[]{
                                product.getId(),
                                product.getName(),
                                product.getPrice(),
                                product.getCategory()
                        });
                    }
                    clearForm();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi tải danh sách sản phẩm: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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
                if (response.isSuccess()) {
                    return (List<Product>) response.getData();
                } else {
                    throw new Exception(response.getMessage());
                }
            }

            @Override
            protected void done() {
                try {
                    List<Product> products = get();
                    tableModel.setRowCount(0);
                    for (Product product : products) {
                        tableModel.addRow(new Object[]{
                                product.getId(),
                                product.getName(),
                                product.getPrice(),
                                product.getCategory()
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi tìm kiếm: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void addProduct() {
        if (!validateInput()) return;
        try {
            Product product = new Product();
            product.setName(txtName.getText().trim());
            product.setPrice(new java.math.BigDecimal(txtPrice.getText().trim()));
            product.setCategory(txtCategory.getText().trim());

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    Response response = clientConnection.sendRequest(new Request("PRODUCT_CREATE", product));
                    if (!response.isSuccess()) {
                        throw new Exception(response.getMessage());
                    }
                    return true;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(ProductPanel.this, "Thêm sản phẩm thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        loadProducts();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi thêm sản phẩm: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Giá phải là số!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateProduct() {
        if (txtId.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm cần sửa!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!validateInput()) return;
        try {
            Product product = new Product();
            product.setId(Integer.parseInt(txtId.getText().trim()));
            product.setName(txtName.getText().trim());
            product.setPrice(new java.math.BigDecimal(txtPrice.getText().trim()));
            product.setCategory(txtCategory.getText().trim());

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    Response response = clientConnection.sendRequest(new Request("PRODUCT_UPDATE", product));
                    if (!response.isSuccess()) {
                        throw new Exception(response.getMessage());
                    }
                    return true;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(ProductPanel.this, "Cập nhật sản phẩm thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        loadProducts();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi cập nhật: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Giá phải là số!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteProduct() {
        if (txtId.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm cần xóa!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn chắc chắn muốn xóa sản phẩm này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    Response response = clientConnection.sendRequest(new Request("PRODUCT_DELETE", Integer.parseInt(txtId.getText().trim())));
                    if (!response.isSuccess()) {
                        throw new Exception(response.getMessage());
                    }
                    return true;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(ProductPanel.this, "Xóa sản phẩm thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        loadProducts();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi xóa: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private boolean validateInput() {
        if (txtName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên sản phẩm không được trống!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            txtName.requestFocus();
            return false;
        }
        if (txtPrice.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Giá không được trống!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            txtPrice.requestFocus();
            return false;
        }
        try {
            double price = Double.parseDouble(txtPrice.getText().trim());
            if (price <= 0) {
                JOptionPane.showMessageDialog(this, "Giá phải lớn hơn 0!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                txtPrice.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Giá phải là số!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            txtPrice.requestFocus();
            return false;
        }
        if (txtCategory.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Danh mục không được trống!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            txtCategory.requestFocus();
            return false;
        }
        return true;
    }

    private void clearForm() {
        txtId.setText("");
        txtName.setText("");
        txtPrice.setText("");
        txtCategory.setText("");
        productTable.clearSelection();
    }

    private void exportProducts() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn đường dẫn lưu file CSV");
        fileChooser.setSelectedFile(new File("products.csv"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".csv")) {
                filePath += ".csv";
            }
            final String finalFilePath = filePath;

            new SwingWorker<String, Void>() {
                private List<Product> products;

                @Override
                protected String doInBackground() throws Exception {
                    // Lấy danh sách sản phẩm từ server
                    Response response = clientConnection.sendRequest(new Request("PRODUCT_FIND_ALL", null));
                    if (!response.isSuccess()) {
                        throw new Exception(response.getMessage());
                    }
                    products = (List<Product>) response.getData();

                    // Ghi file CSV
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(finalFilePath))) {
                        // Ghi header
                        writer.write("ID,Tên sản phẩm,Giá,Danh mục");
                        writer.newLine();
                        for (Product product : products) {
                            writer.write(product.getId() + "," +
                                    product.getName() + "," +
                                    product.getPrice().toString() + "," +
                                    product.getCategory());
                            writer.newLine();
                        }
                    }
                    return finalFilePath;
                }

                @Override
                protected void done() {
                    try {
                        String savedPath = get();
                        JOptionPane.showMessageDialog(ProductPanel.this, "Xuất file CSV thành công tại: " + savedPath, "Thành công", JOptionPane.INFORMATION_MESSAGE);
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
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToImport = fileChooser.getSelectedFile();
            String filePath = fileToImport.getAbsolutePath();

            new SwingWorker<Integer, Void>() {
                private int importedCount = 0;
                private List<Product> productsToImport = new ArrayList<>();

                @Override
                protected Integer doInBackground() throws Exception {
                    // Đọc file CSV
                    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                        String line;
                        boolean isFirstLine = true;
                        while ((line = reader.readLine()) != null) {
                            if (isFirstLine) {
                                isFirstLine = false;
                                continue; // Bỏ qua header
                            }
                            String[] parts = line.split(",");
                            if (parts.length >= 4) {
                                // Lấy thông tin (bỏ qua ID cũ, vì DB tự tạo mới)
                                String name = parts[1].trim();
                                String priceStr = parts[2].trim();
                                String category = parts[3].trim();
                                Product product = new Product();
                                product.setName(name);
                                product.setPrice(new BigDecimal(priceStr));
                                product.setCategory(category);
                                productsToImport.add(product);
                            }
                        }
                    }

                    // Gửi từng sản phẩm lên server để lưu
                    for (Product product : productsToImport) {
                        Response response = clientConnection.sendRequest(new Request("PRODUCT_CREATE", product));
                        if (response.isSuccess()) {
                            importedCount++;
                        }
                    }
                    return importedCount;
                }

                @Override
                protected void done() {
                    try {
                        int count = get();
                        JOptionPane.showMessageDialog(ProductPanel.this, "Nhập file CSV thành công! Đã thêm " + count + "/" + productsToImport.size() + " sản phẩm.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        loadProducts();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi nhập file CSV: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }
}
