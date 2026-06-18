package com.cafemanagement.ui;

import com.cafemanagement.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainForm extends JFrame {
    private User currentUser;

    // Khung chứa nội dung chính
    private JPanel mainContentPanel;
    private CardLayout cardLayout;

    // Các trang chức năng
    private ProductPanel productPanel;
    private OrderPanel orderPanel;
    private DashboardPanel dashboardPanel;
    private UserPanel userPanel;

    // Màu sắc chủ đạo giống Sapo
    private final Color SIDEBAR_BG = Color.decode("#2C3E50");
    private final Color BTN_BG_NORMAL = Color.decode("#34495E");
    private final Color BTN_BG_HOVER = Color.decode("#2980B9");
    private final Color TEXT_COLOR = Color.WHITE;

    public MainForm(User user) {
        this.currentUser = user;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("SHOP MANAGER - Xin chào: " + currentUser.getUsername() + " (" + currentUser.getRole().toUpperCase() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 1. TẠO SIDEBAR (MENU BÊN TRÁI)
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BorderLayout());
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(250, 0));

        // -- Logo / Tiêu đề Sidebar --
        JPanel logoPanel = new JPanel(new GridLayout(3, 1));
        logoPanel.setBackground(SIDEBAR_BG);
        logoPanel.setBorder(new EmptyBorder(20, 10, 20, 10));

        JLabel lblTitle = new JLabel("SHOP MANAGER", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        lblTitle.setForeground(TEXT_COLOR);

        JLabel lblUser = new JLabel("Chào: " + currentUser.getUsername(), SwingConstants.CENTER);
        lblUser.setFont(new Font("Arial", Font.ITALIC, 16));
        lblUser.setForeground(Color.LIGHT_GRAY);

        JLabel lblRole = new JLabel("(" + currentUser.getRole().toUpperCase() + ")", SwingConstants.CENTER);
        lblRole.setFont(new Font("Arial", Font.BOLD, 14));
        lblRole.setForeground(Color.decode("#2ECC71")); // Màu xanh lá cho chức vụ

        logoPanel.add(lblTitle);
        logoPanel.add(lblUser);
        logoPanel.add(lblRole);
        sidebar.add(logoPanel, BorderLayout.NORTH);

        // -- Các nút Menu --
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(SIDEBAR_BG);
        menuPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        // 2. KHỞI TẠO KHUNG CHỨA NỘI DUNG (CENTER)
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);

        // 3. THÊM TÍNH NĂNG VÀ PHÂN QUYỀN
        // Tính năng Bán Hàng (POS) - Ai cũng được dùng
        orderPanel = new OrderPanel();
        mainContentPanel.add(orderPanel, "POS");
        JButton btnPos = createMenuButton("Bán Hàng (POS)", "POS");
        menuPanel.add(btnPos);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Các tính năng dành riêng cho Admin
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            // Quản lý sản phẩm
            productPanel = new ProductPanel();
            mainContentPanel.add(productPanel, "PRODUCT");
            JButton btnProduct = createMenuButton("Quản Lý Sản Phẩm", "PRODUCT");
            menuPanel.add(btnProduct);
            menuPanel.add(Box.createRigidArea(new Dimension(0, 15)));

            // Thống kê doanh thu
            dashboardPanel = new DashboardPanel();
            mainContentPanel.add(dashboardPanel, "DASHBOARD");
            JButton btnDash = createMenuButton("Thống Kê Doanh Thu", "DASHBOARD");
            menuPanel.add(btnDash);
            menuPanel.add(Box.createRigidArea(new Dimension(0, 15)));

            // Quản lý tài khoản
            userPanel = new UserPanel();
            mainContentPanel.add(userPanel, "USER");
            JButton btnUser = createMenuButton("Quản Lý Tài Khoản", "USER");
            menuPanel.add(btnUser);
        }

        sidebar.add(menuPanel, BorderLayout.CENTER);

        // -- Nút Đăng xuất --
        JButton btnLogout = new JButton("Đăng Xuất");
        btnLogout.setFont(new Font("Arial", Font.BOLD, 16));
        btnLogout.setBackground(Color.decode("#E74C3C"));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusPainted(false);
        btnLogout.setBorder(new EmptyBorder(15, 0, 15, 0));
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> logout());
        sidebar.add(btnLogout, BorderLayout.SOUTH);

        // Gắn Sidebar và Nội dung vào Frame chính
        add(sidebar, BorderLayout.WEST);
        add(mainContentPanel, BorderLayout.CENTER);

        // Mặc định hiển thị trang đầu tiên (POS) khi đăng nhập
        cardLayout.show(mainContentPanel, "POS");
        orderPanel.loadTablesAndProducts();
    }

    // Hàm tạo nút Menu chuẩn giao diện
    private JButton createMenuButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setForeground(TEXT_COLOR);
        btn.setBackground(BTN_BG_NORMAL);
        btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hiệu ứng Hover chuột
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(BTN_BG_HOVER);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(BTN_BG_NORMAL);
            }
        });

        // Sự kiện chuyển trang
        btn.addActionListener(e -> {
            cardLayout.show(mainContentPanel, cardName);
            // Auto-load data khi chuyển trang
            if (cardName.equals("PRODUCT") && productPanel != null) productPanel.loadProducts();
            if (cardName.equals("POS")) orderPanel.loadTablesAndProducts();
            if (cardName.equals("DASHBOARD") && dashboardPanel != null) dashboardPanel.loadStats();
            if (cardName.equals("USER") && userPanel != null) userPanel.loadUsers();
        });

        return btn;
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn đăng xuất?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            this.dispose(); // Tắt form hiện tại
            SwingUtilities.invokeLater(() -> {
                new LoginForm().setVisible(true); // Mở lại form đăng nhập
            });
        }
    }
}