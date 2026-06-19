package com.cafemanagement.ui;

import com.cafemanagement.client.ClientConnection;
import com.cafemanagement.dto.Request;
import com.cafemanagement.dto.Response;
import com.cafemanagement.model.User;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class LoginForm extends JFrame {
    private JPanel rightPanel; // Chứa CardLayout để chuyển đổi form
    private CardLayout cardLayout;

    // --- Components Form Đăng Nhập ---
    private JTextField txtLoginUser;
    private JPasswordField txtLoginPass;

    // --- Components Form Đăng Ký ---
    private JTextField txtRegUser;
    private JPasswordField txtRegPass;
    private JPasswordField txtRegConfirm;

    public LoginForm() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("HỆ THỐNG QUẢN LÝ QUÁN CAFE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 450); // Mở rộng size để chia đôi màn hình
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        // ================= TRÁI: LOGO / BACKGROUND =================
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(350, 450));
        // 1. Giữ lại màu nền xanh dự phòng (lỡ ảnh bị lỗi thì nó hiện màu xanh)
        leftPanel.setBackground(new Color(41, 128, 185));

        // 2. Dùng ImageIcon để load file logo.jpg
        // 2. Dùng ImageIcon để load file logo.png và THU NHỎ nó
        // 2. Dùng ImageIcon để load file logo và THU NHỎ GIỮ NGUYÊN TỶ LỆ GỐC (Aspect Ratio)
        try {
            ImageIcon originalIcon = new ImageIcon(getClass().getResource("/logo.png"));
            Image img = originalIcon.getImage();

            // Lấy kích thước thật của ảnh gốc
            int originalWidth = img.getWidth(null);
            int originalHeight = img.getHeight(null);

            // Kích thước tối đa của khung chứa bên trái
            int boundWidth = 350;
            int boundHeight = 450;

            // Thuật toán tìm tỷ lệ thu nhỏ tối ưu nhất để không bị méo ảnh
            double ratio = Math.min((double) boundWidth / originalWidth, (double) boundHeight / originalHeight);
            int newWidth = (int) (originalWidth * ratio);
            int newHeight = (int) (originalHeight * ratio);

            // Ép kích thước theo chuẩn mới tính được với chất lượng cao nhất
            Image scaledImage = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);

            // Gói lại thành Icon và căn giữa
            JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imageLabel.setVerticalAlignment(SwingConstants.CENTER);

            leftPanel.add(imageLabel, BorderLayout.CENTER);

        } catch (Exception e) {
            System.err.println("Không tìm thấy file ảnh logo.png");
        }
        // CÁCH CHÈN ẢNH: Bạn copy 1 file tên là "logo.png" vào thư mục resources của project,
        // Sau đó bỏ comment 2 dòng dưới đây để hiện ảnh thay vì màu trơn nhé:
        // ImageIcon icon = new ImageIcon(getClass().getResource("/logo.png"));
        // leftPanel.add(new JLabel(icon), BorderLayout.CENTER);

//        JLabel welcomeLabel = new JLabel("<html><center>WELCOME TO<br>CAFE MANAGEMENT</center></html>", SwingConstants.CENTER);
//        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
//        welcomeLabel.setForeground(Color.WHITE);
//        leftPanel.add(welcomeLabel, BorderLayout.CENTER);
//

        // ================= PHẢI: FORM LOGIN / REGISTER =================
        cardLayout = new CardLayout();
        rightPanel = new JPanel(cardLayout);
        rightPanel.setBackground(Color.WHITE);

        // Tạo 2 form riêng biệt
        rightPanel.add(createLoginCard(), "LOGIN");
        rightPanel.add(createRegisterCard(), "REGISTER");

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    // ----------- FORM ĐĂNG NHẬP -----------
    private JPanel createLoginCard() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("ĐĂNG NHẬP", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 26));
        title.setForeground(new Color(44, 62, 80));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        txtLoginUser = new JTextField(15);
        panel.add(txtLoginUser, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        txtLoginPass = new JPasswordField(15);
        panel.add(txtLoginPass, gbc);

        JButton btnLogin = new JButton("Đăng Nhập");
        btnLogin.setBackground(new Color(41, 128, 185));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.addActionListener(e -> handleLogin());

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(btnLogin, gbc);

        JButton btnSwitchToReg = new JButton("Chưa có tài khoản? Đăng ký ngay");
        btnSwitchToReg.setContentAreaFilled(false);
        btnSwitchToReg.setBorderPainted(false);
        btnSwitchToReg.setForeground(new Color(41, 128, 185));
        btnSwitchToReg.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSwitchToReg.addActionListener(e -> cardLayout.show(rightPanel, "REGISTER"));

        gbc.gridy = 4;
        panel.add(btnSwitchToReg, gbc);

        return panel;
    }

    // ----------- FORM ĐĂNG KÝ -----------
    private JPanel createRegisterCard() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 15, 8, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("ĐĂNG KÝ (STAFF)", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(new Color(44, 62, 80));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        txtRegUser = new JTextField(15);
        panel.add(txtRegUser, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        txtRegPass = new JPasswordField(15);
        panel.add(txtRegPass, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Xác nhận Password:"), gbc);
        gbc.gridx = 1;
        txtRegConfirm = new JPasswordField(15);
        panel.add(txtRegConfirm, gbc);

        JButton btnRegister = new JButton("Xác nhận Đăng Ký");
        btnRegister.setBackground(new Color(46, 204, 113));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.addActionListener(e -> handleRegister());

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(btnRegister, gbc);

        JButton btnSwitchToLogin = new JButton("Đã có tài khoản? Quay lại Đăng nhập");
        btnSwitchToLogin.setContentAreaFilled(false);
        btnSwitchToLogin.setBorderPainted(false);
        btnSwitchToLogin.setForeground(new Color(41, 128, 185));
        btnSwitchToLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSwitchToLogin.addActionListener(e -> cardLayout.show(rightPanel, "LOGIN"));

        gbc.gridy = 5;
        panel.add(btnSwitchToLogin, gbc);

        return panel;
    }

    // ================= XỬ LÝ LOGIC =================

    private void handleLogin() {
        String username = txtLoginUser.getText().trim();
        String password = new String(txtLoginPass.getPassword()).trim();

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
                Map<String, Object> userInfo = (Map<String, Object>) response.getData();
                User user = new User((int) userInfo.get("id"), (String) userInfo.get("username"), "", (String) userInfo.get("role"));

                JOptionPane.showMessageDialog(this, "Đăng nhập thành công! Vai trò: " + user.getRole(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
                this.dispose();
                new MainForm(user).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, response.getMessage(), "Lỗi đăng nhập", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối server: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRegister() {
        String username = txtRegUser.getText().trim();
        String password = new String(txtRegPass.getPassword());
        String confirm = new String(txtRegConfirm.getPassword());

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ các ô!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // KIỂM TRA ĐỘ MẠNH MẬT KHẨU
        if (!isStrongPassword(password)) {
            String errorMsg = "Mật khẩu không đạt yêu cầu bảo mật!\n\n" +
                    "Mật khẩu phải có:\n" +
                    "- Ít nhất 8 ký tự\n" +
                    "- Có chứa ít nhất 1 chữ cái\n" +
                    "- Có chứa ít nhất 1 chữ số\n" +
                    "- Có chứa ít nhất 1 ký tự đặc biệt (VD: @, #, $, %,...)";
            JOptionPane.showMessageDialog(this, errorMsg, "Mật khẩu quá yếu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            ClientConnection connection = ClientConnection.getInstance();
            Map<String, String> regData = new HashMap<>();
            regData.put("username", username);
            regData.put("password", password);

            Request request = new Request("REGISTER", regData);
            Response response = connection.sendRequest(request);

            if (response.isSuccess()) {
                JOptionPane.showMessageDialog(this, "Đăng ký thành công! Bạn có thể đăng nhập ngay.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                // Đăng ký xong thì xóa trắng ô form Đăng ký và tự động lật sang mặt Đăng nhập
                txtRegUser.setText(""); txtRegPass.setText(""); txtRegConfirm.setText("");
                txtLoginUser.setText(username); // Điền sẵn username cho tiện
                cardLayout.show(rightPanel, "LOGIN");
            } else {
                JOptionPane.showMessageDialog(this, response.getMessage(), "Lỗi đăng ký", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối đến server: " + ex.getMessage(), "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
        }
    }

    // HÀM KIỂM TRA MẬT KHẨU (CODE JAVA THUẦN RẤT DỄ BẢO VỆ ĐỒ ÁN)
    private boolean isStrongPassword(String password) {
        if (password.length() < 8) return false;

        boolean hasLetter = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        return hasLetter && hasDigit && hasSpecial;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }
}