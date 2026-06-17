package com.cafemanagement.ui;

import com.cafemanagement.client.ClientConnection;
import com.cafemanagement.dto.Request;
import com.cafemanagement.dto.Response;
import com.cafemanagement.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class LoginForm extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton exitButton;

    public LoginForm() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("ĐĂNG NHẬP HỆ THỐNG");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main panel with GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel titleLabel = new JLabel("ĐĂNG NHẬP HỆ THỐNG", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        // Username label
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        mainPanel.add(usernameLabel, gbc);

        // Username field
        gbc.gridx = 1;
        usernameField = new JTextField(15);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        mainPanel.add(usernameField, gbc);

        // Password label
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        mainPanel.add(passwordLabel, gbc);

        // Password field
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        mainPanel.add(passwordField, gbc);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        loginButton = new JButton("Đăng nhập");
        loginButton.setFont(new Font("Arial", Font.PLAIN, 14));
        loginButton.setPreferredSize(new Dimension(120, 35));

        exitButton = new JButton("Thoát");
        exitButton.setFont(new Font("Arial", Font.PLAIN, 14));
        exitButton.setPreferredSize(new Dimension(120, 35));

        buttonPanel.add(loginButton);
        buttonPanel.add(exitButton);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.CENTER;
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel);

        // Action listeners
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });

        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ Username và Password!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            ClientConnection connection = ClientConnection.getInstance();

            Map<String, String> loginData = new HashMap<>();
            loginData.put("username", username);
            loginData.put("password", password);

            Request request = new Request("LOGIN", loginData);
            Response response = connection.sendRequest(request);

            if (response.isSuccess()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userInfo = (Map<String, Object>) response.getData();
                int id = (int) userInfo.get("id");
                String dbUsername = (String) userInfo.get("username");
                String role = (String) userInfo.get("role");
                User user = new User(id, dbUsername, "", role);

                JOptionPane.showMessageDialog(this, "Đăng nhập thành công! Vai trò của bạn: " + role, "Thành công", JOptionPane.INFORMATION_MESSAGE);

                // Open main form
                this.dispose();
                MainForm mainForm = new MainForm(user);
                mainForm.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, response.getMessage(), "Lỗi đăng nhập", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối đến server: " + ex.getMessage(), "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LoginForm loginForm = new LoginForm();
                loginForm.setVisible(true);
            }
        });
    }
}
