package com.cafemanagement.ui;

import com.cafemanagement.client.ClientConnection;
import com.cafemanagement.dto.Request;
import com.cafemanagement.dto.Response;
import com.cafemanagement.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class UserPanel extends JPanel {
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JComboBox<String> cboRole;
    private JTextField txtId;
    private ClientConnection clientConnection;

    public UserPanel() {
        clientConnection = ClientConnection.getInstance();
        initializeUI();
        loadUsers();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Table panel
        String[] columnNames = {"ID", "Username", "Role"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setAutoCreateRowSorter(true);
        userTable.getTableHeader().setReorderingAllowed(false);
        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && userTable.getSelectedRow() != -1) {
                int selectedRow = userTable.convertRowIndexToModel(userTable.getSelectedRow());
                txtId.setText(tableModel.getValueAt(selectedRow, 0).toString());
                txtUsername.setText(tableModel.getValueAt(selectedRow, 1).toString());
                cboRole.setSelectedItem(tableModel.getValueAt(selectedRow, 2).toString());
            }
        });
        add(new JScrollPane(userTable), BorderLayout.CENTER);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Thông tin tài khoản"));
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
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        txtUsername = new JTextField(15);
        formPanel.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        txtPassword = new JPasswordField(15);
        formPanel.add(txtPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        String[] roles = {"ADMIN", "EMPLOYEE"};
        cboRole = new JComboBox<>(roles);
        formPanel.add(cboRole, gbc);

        // Button panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton btnAdd = new JButton("Thêm");
        JButton btnUpdate = new JButton("Sửa");
        JButton btnDelete = new JButton("Xóa");
        JButton btnRefresh = new JButton("Làm mới");
        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnRefresh);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(btnPanel, gbc);

        add(formPanel, BorderLayout.SOUTH);

        // Button listeners
        btnAdd.addActionListener(e -> addUser());
        btnUpdate.addActionListener(e -> updateUser());
        btnDelete.addActionListener(e -> deleteUser());
        btnRefresh.addActionListener(e -> loadUsers());
    }

    public void loadUsers() {
        new SwingWorker<List<User>, Void>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                Response response = clientConnection.sendRequest(new Request("USER_GET_ALL", null));
                if (!response.isSuccess()) {
                    throw new Exception(response.getMessage());
                }
                return (List<User>) response.getData();
            }

            @Override
            protected void done() {
                try {
                    List<User> users = get();
                    tableModel.setRowCount(0);
                    for (User user : users) {
                        tableModel.addRow(new Object[]{
                                user.getId(),
                                user.getUsername(),
                                user.getRole()
                        });
                    }
                    clearForm();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserPanel.this, "Lỗi tải danh sách tài khoản: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void addUser() {
        if (!validateInput()) return;
        try {
            User user = new User();
            user.setUsername(txtUsername.getText().trim());
            user.setPassword(new String(txtPassword.getPassword()));
            user.setRole(cboRole.getSelectedItem().toString());

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    Response response = clientConnection.sendRequest(new Request("USER_CREATE", user));
                    if (!response.isSuccess()) {
                        throw new Exception(response.getMessage());
                    }
                    return true;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(UserPanel.this, "Thêm tài khoản thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        loadUsers();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(UserPanel.this, "Lỗi thêm tài khoản: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateUser() {
        if (txtId.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn tài khoản cần sửa!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!validateInput()) return;
        try {
            User user = new User();
            user.setId(Integer.parseInt(txtId.getText().trim()));
            user.setUsername(txtUsername.getText().trim());
            user.setPassword(new String(txtPassword.getPassword()));
            user.setRole(cboRole.getSelectedItem().toString());

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    Response response = clientConnection.sendRequest(new Request("USER_UPDATE", user));
                    if (!response.isSuccess()) {
                        throw new Exception(response.getMessage());
                    }
                    return true;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(UserPanel.this, "Cập nhật tài khoản thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        loadUsers();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(UserPanel.this, "Lỗi cập nhật tài khoản: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void deleteUser() {
        if (txtId.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn tài khoản cần xóa!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn chắc chắn muốn xóa tài khoản này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    Response response = clientConnection.sendRequest(new Request("USER_DELETE", Integer.parseInt(txtId.getText().trim())));
                    if (!response.isSuccess()) {
                        throw new Exception(response.getMessage());
                    }
                    return true;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(UserPanel.this, "Xóa tài khoản thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        loadUsers();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(UserPanel.this, "Lỗi xóa tài khoản: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private boolean validateInput() {
        if (txtUsername.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username không được trống!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            txtUsername.requestFocus();
            return false;
        }
        String password = new String(txtPassword.getPassword());
        if (password.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password không được trống!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            txtPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void clearForm() {
        txtId.setText("");
        txtUsername.setText("");
        txtPassword.setText("");
        cboRole.setSelectedIndex(0);
        userTable.clearSelection();
    }
}
