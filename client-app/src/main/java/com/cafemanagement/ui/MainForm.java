package com.cafemanagement.ui;

import com.cafemanagement.model.User;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class MainForm extends JFrame {
    private User currentUser;
    private JTabbedPane tabbedPane;
    private ProductPanel productPanel;
    private OrderPanel orderPanel;
    private DashboardPanel dashboardPanel;
    private UserPanel userPanel;

    public MainForm(User user) {
        this.currentUser = user;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("HỆ THỐNG QUẢN LÝ QUÁN CAFE - " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Menu sidebar (or use JTabbedPane directly)
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);

        // Add product tab (visible for both roles)
        productPanel = new ProductPanel();
        tabbedPane.addTab("Quản lý sản phẩm", productPanel);

        // Add order tab (visible for both roles)
        orderPanel = new OrderPanel();
        tabbedPane.addTab("Quản lý bàn/order", orderPanel);

        // Add other tabs (for admin only)
        if ("admin".equalsIgnoreCase(currentUser.getRole())) {
            userPanel = new UserPanel();
            tabbedPane.addTab("Quản lý tài khoản", userPanel);
            dashboardPanel = new DashboardPanel();
            tabbedPane.addTab("Thống kê", dashboardPanel);
        }

        // Add ChangeListener to auto-load data when tab is selected
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int selectedIndex = tabbedPane.getSelectedIndex();
                String title = tabbedPane.getTitleAt(selectedIndex);
                switch (title) {
                    case "Quản lý sản phẩm":
                        productPanel.loadProducts();
                        break;
                    case "Quản lý bàn/order":
                        orderPanel.loadTablesAndProducts();
                        break;
                    case "Thống kê":
                        if (dashboardPanel != null) {
                            dashboardPanel.loadStats();
                        }
                        break;
                    case "Quản lý tài khoản":
                        if (userPanel != null) {
                            userPanel.loadUsers();
                        }
                        break;
                }
            }
        });

        add(tabbedPane, BorderLayout.CENTER);

        // Auto-load initial data for first tab
        SwingUtilities.invokeLater(() -> {
            productPanel.loadProducts();
        });
    }

}

