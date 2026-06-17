package com.cafemanagement.ui;

import com.cafemanagement.client.ClientConnection;
import com.cafemanagement.dto.Request;
import com.cafemanagement.dto.Response;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardPanel extends JPanel {
    private ClientConnection clientConnection;
    private JLabel totalRevenueLabel;
    private JLabel totalOrdersLabel;
    private JTable topProductsTable;
    private DefaultTableModel topProductsTableModel;

    public DashboardPanel() {
        clientConnection = ClientConnection.getInstance();
        initializeUI();
        loadStats();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Stats panel
        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 20, 20));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Thống kê tổng quan"));

        // Total revenue
        JPanel revenuePanel = new JPanel(new BorderLayout());
        revenuePanel.setBorder(BorderFactory.createLineBorder(new Color(52, 152, 219), 2));
        revenuePanel.setBackground(new Color(236, 240, 241));
        JLabel revenueTitleLabel = new JLabel("Tổng doanh thu", SwingConstants.CENTER);
        revenueTitleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        revenuePanel.add(revenueTitleLabel, BorderLayout.NORTH);
        totalRevenueLabel = new JLabel("0 VNĐ", SwingConstants.CENTER);
        totalRevenueLabel.setFont(new Font("Arial", Font.BOLD, 32));
        totalRevenueLabel.setForeground(new Color(46, 204, 113));
        revenuePanel.add(totalRevenueLabel, BorderLayout.CENTER);
        statsPanel.add(revenuePanel);

        // Total orders
        JPanel ordersPanel = new JPanel(new BorderLayout());
        ordersPanel.setBorder(BorderFactory.createLineBorder(new Color(46, 204, 113), 2));
        ordersPanel.setBackground(new Color(236, 240, 241));
        JLabel ordersTitleLabel = new JLabel("Tổng số đơn hàng", SwingConstants.CENTER);
        ordersTitleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        ordersPanel.add(ordersTitleLabel, BorderLayout.NORTH);
        totalOrdersLabel = new JLabel("0", SwingConstants.CENTER);
        totalOrdersLabel.setFont(new Font("Arial", Font.BOLD, 32));
        totalOrdersLabel.setForeground(new Color(52, 152, 219));
        ordersPanel.add(totalOrdersLabel, BorderLayout.CENTER);
        statsPanel.add(ordersPanel);

        add(statsPanel, BorderLayout.NORTH);

        // Top products panel
        JPanel topProductsPanel = new JPanel(new BorderLayout());
        topProductsPanel.setBorder(BorderFactory.createTitledBorder("Top 10 sản phẩm bán chạy nhất"));
        String[] columnNames = {"Tên sản phẩm", "Số lượng bán", "Doanh thu"};
        topProductsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        topProductsTable = new JTable(topProductsTableModel);
        topProductsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        topProductsTable.setAutoCreateRowSorter(true);
        topProductsTable.getTableHeader().setReorderingAllowed(false);
        topProductsPanel.add(new JScrollPane(topProductsTable), BorderLayout.CENTER);

        add(topProductsPanel, BorderLayout.CENTER);
    }

    public void loadStats() {
        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                Response response = clientConnection.sendRequest(new Request("DASHBOARD_STATS", null));
                if (!response.isSuccess()) {
                    throw new Exception(response.getMessage());
                }
                return (Map<String, Object>) response.getData();
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> stats = get();
                    BigDecimal totalRevenue = (BigDecimal) stats.get("total_revenue");
                    Integer totalOrders = (Integer) stats.get("total_orders");
                    List<Map<String, Object>> topProducts = (List<Map<String, Object>>) stats.get("top_products");

                    totalRevenueLabel.setText(totalRevenue != null ? totalRevenue.toString() + " VNĐ" : "0 VNĐ");
                    totalOrdersLabel.setText(totalOrders != null ? totalOrders.toString() : "0");

                    topProductsTableModel.setRowCount(0);
                    if (topProducts != null) {
                        for (Map<String, Object> product : topProducts) {
                            topProductsTableModel.addRow(new Object[]{
                                    product.get("name"),
                                    product.get("total_quantity"),
                                    product.get("total_revenue") + " VNĐ"
                            });
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DashboardPanel.this, "Lỗi tải thống kê: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
