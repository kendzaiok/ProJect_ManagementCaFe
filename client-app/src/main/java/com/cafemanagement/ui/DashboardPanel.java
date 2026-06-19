package com.cafemanagement.ui;

import com.cafemanagement.client.ClientConnection;
import com.cafemanagement.dto.Request;
import com.cafemanagement.dto.Response;
import com.cafemanagement.model.Order;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DashboardPanel extends JPanel {
    private ClientConnection clientConnection;
    private JTabbedPane tabbedPane;

    // UI Tab 1
    private JComboBox<String> yearCombo, monthCombo, dayCombo;
    private JTable invoiceTable;
    private DefaultTableModel invoiceTableModel;
    private JLabel lblTotalRevenueDay, lblTotalOrdersDay;

    // UI Tab 2
    private JComboBox<String> periodChartCombo; // Thêm khai báo JComboBox cho bộ lọc
    private JPanel chartPanelContainer;
    private JTable topProductsTable;
    private DefaultTableModel topProductsTableModel;

    public DashboardPanel() {
        clientConnection = ClientConnection.getInstance();
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));

        tabbedPane.addTab("Chi tiết Hóa Đơn", createInvoiceTab());
        tabbedPane.addTab("Biểu đồ & Top 5", createChartTab());

        add(tabbedPane, BorderLayout.CENTER);

        // Tải dữ liệu mặc định khi mở lên
        loadInvoiceData();
        loadChartData();
    }

    // ================= TAB 1: QUẢN LÝ HÓA ĐƠN =================
    private JPanel createInvoiceTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Bộ lọc thời gian
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Bộ lọc thời gian báo cáo"));

        yearCombo = new JComboBox<>(new String[]{"2024", "2025", "2026"});
        yearCombo.setSelectedItem("2026"); // Mặc định
        monthCombo = new JComboBox<>(new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"});
        dayCombo = new JComboBox<>();
        for (int i = 1; i <= 31; i++) dayCombo.addItem(String.format("%02d", i));

        JButton btnView = new JButton("Xem Báo Cáo");
        btnView.setBackground(new Color(41, 128, 185));
        btnView.setForeground(Color.WHITE);
        btnView.addActionListener(e -> loadInvoiceData());

        filterPanel.add(new JLabel("Năm:")); filterPanel.add(yearCombo);
        filterPanel.add(new JLabel("Tháng:")); filterPanel.add(monthCombo);
        filterPanel.add(new JLabel("Ngày:")); filterPanel.add(dayCombo);
        filterPanel.add(btnView);

        // 2. Tóm tắt nhanh
        JPanel summaryPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        JPanel pnlRev = new JPanel(new BorderLayout());
        pnlRev.setBackground(new Color(46, 204, 113));
        pnlRev.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel lbl1 = new JLabel("TỔNG DOANH THU TRONG NGÀY");
        lbl1.setForeground(Color.WHITE);
        lblTotalRevenueDay = new JLabel("0 đ");
        lblTotalRevenueDay.setForeground(Color.WHITE);
        lblTotalRevenueDay.setFont(new Font("Arial", Font.BOLD, 24));
        pnlRev.add(lbl1, BorderLayout.NORTH); pnlRev.add(lblTotalRevenueDay, BorderLayout.CENTER);

        JPanel pnlOrd = new JPanel(new BorderLayout());
        pnlOrd.setBackground(new Color(52, 152, 219));
        pnlOrd.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel lbl2 = new JLabel("TỔNG SỐ HÓA ĐƠN VỪA BÁN");
        lbl2.setForeground(Color.WHITE);
        lblTotalOrdersDay = new JLabel("0 Đơn");
        lblTotalOrdersDay.setForeground(Color.WHITE);
        lblTotalOrdersDay.setFont(new Font("Arial", Font.BOLD, 24));
        pnlOrd.add(lbl2, BorderLayout.NORTH); pnlOrd.add(lblTotalOrdersDay, BorderLayout.CENTER);

        summaryPanel.add(pnlRev); summaryPanel.add(pnlOrd);

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(filterPanel, BorderLayout.NORTH);
        topContainer.add(summaryPanel, BorderLayout.CENTER);

        // 3. Bảng dữ liệu hóa đơn
        String[] cols = {"Mã Hóa Đơn", "Thời gian thanh toán", "Tổng Tiền", "Trạng Thái"};
        invoiceTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        invoiceTable = new JTable(invoiceTableModel);
        invoiceTable.setRowHeight(25);

        // Sự kiện CLICK ĐÚP XEM CHI TIẾT
        invoiceTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && invoiceTable.getSelectedRow() != -1) {
                    int orderId = Integer.parseInt(invoiceTableModel.getValueAt(invoiceTable.getSelectedRow(), 0).toString().replace("HD", ""));
                    showInvoiceDetails(orderId);
                }
            }
        });

        panel.add(topContainer, BorderLayout.NORTH);
        panel.add(new JScrollPane(invoiceTable), BorderLayout.CENTER);
        return panel;
    }

    // ================= TAB 2: BIỂU ĐỒ & TOP 5 (CẬP NHẬT BỘ LỌC) =================
    private JPanel createChartTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. Tạo thanh chọn bộ lọc thời gian
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Chọn kỳ báo cáo biểu đồ: "));
        periodChartCombo = new JComboBox<>(new String[]{
                "7 ngày gần nhất", "Tuần này", "Tuần trước", "Tháng này", "Tháng trước"
        });
        periodChartCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        // Cứ mỗi lần chọn menu khác, tự động vẽ lại biểu đồ
        periodChartCombo.addActionListener(e -> loadChartData());
        filterPanel.add(periodChartCombo);

        chartPanelContainer = new JPanel(new BorderLayout());
        chartPanelContainer.setBorder(BorderFactory.createTitledBorder("Biểu đồ doanh thu"));
        chartPanelContainer.setPreferredSize(new Dimension(800, 300));

        // Gom bộ lọc và biểu đồ vào 1 khu
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(filterPanel, BorderLayout.NORTH);
        topContainer.add(chartPanelContainer, BorderLayout.CENTER);

        JPanel top5Panel = new JPanel(new BorderLayout());
        top5Panel.setBorder(BorderFactory.createTitledBorder("Top 5 món bán chạy nhất"));
        String[] cols = {"Tên Món", "Đã Bán", "Doanh Thu Mang Lại"};
        topProductsTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable topTable = new JTable(topProductsTableModel);
        topTable.setRowHeight(25);
        top5Panel.add(new JScrollPane(topTable), BorderLayout.CENTER);
        top5Panel.setPreferredSize(new Dimension(800, 200));

        panel.add(topContainer, BorderLayout.CENTER);
        panel.add(top5Panel, BorderLayout.SOUTH);
        return panel;
    }

    // ================= CÁC HÀM XỬ LÝ LOGIC (SWING WORKER) =================

    private void loadInvoiceData() {
        String dateStr = yearCombo.getSelectedItem() + "-" + monthCombo.getSelectedItem() + "-" + dayCombo.getSelectedItem();

        new SwingWorker<List<Order>, Void>() {
            @Override
            protected List<Order> doInBackground() throws Exception {
                Response res = clientConnection.sendRequest(new Request("DASHBOARD_GET_INVOICES", dateStr));
                if (!res.isSuccess()) throw new Exception(res.getMessage());
                return (List<Order>) res.getData();
            }
            @Override
            protected void done() {
                try {
                    List<Order> orders = get();
                    invoiceTableModel.setRowCount(0);
                    BigDecimal totalRev = BigDecimal.ZERO;
                    for (Order o : orders) {
                        invoiceTableModel.addRow(new Object[]{
                                "HD" + o.getId(), o.getCreatedAt(), o.getTotalPrice() + " đ", o.getStatus()
                        });
                        totalRev = totalRev.add(o.getTotalPrice());
                    }
                    lblTotalRevenueDay.setText(totalRev.toString() + " đ");
                    lblTotalOrdersDay.setText(orders.size() + " Đơn");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DashboardPanel.this, "Lỗi tải hóa đơn: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void loadChartData() {
        // Lấy chữ đang hiển thị trên ComboBox (nếu chưa có thì mặc định 7 ngày)
        String selectedPeriod = periodChartCombo != null ? (String) periodChartCombo.getSelectedItem() : "7 ngày gần nhất";

        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                // Ném cái selectedPeriod đó lên Server qua Socket
                Response res = clientConnection.sendRequest(new Request("DASHBOARD_CHART_STATS", selectedPeriod));
                if (!res.isSuccess()) throw new Exception(res.getMessage());
                return (Map<String, Object>) res.getData();
            }
            @Override
            protected void done() {
                try {
                    Map<String, Object> data = get();
                    List<Map<String, Object>> chartData = (List<Map<String, Object>>) data.get("chart_data");
                    List<Map<String, Object>> top5 = (List<Map<String, Object>>) data.get("top_products");

                    // Đổ dữ liệu Top 5
                    topProductsTableModel.setRowCount(0);
                    for (Map<String, Object> p : top5) {
                        topProductsTableModel.addRow(new Object[]{p.get("name"), p.get("total_quantity"), p.get("total_revenue") + " đ"});
                    }

                    // Vẽ Biểu đồ
                    chartPanelContainer.removeAll();
                    chartPanelContainer.add(new CustomBarChart(chartData), BorderLayout.CENTER);
                    chartPanelContainer.revalidate();
                    chartPanelContainer.repaint();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void showInvoiceDetails(int orderId) {
        new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                Response res = clientConnection.sendRequest(new Request("DASHBOARD_INVOICE_DETAILS", orderId));
                return (List<Map<String, Object>>) res.getData();
            }
            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> details = get();
                    JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(DashboardPanel.this), "Chi tiết Hóa Đơn HD" + orderId, true);
                    String[] cols = {"Tên món", "Số lượng", "Đơn giá", "Thành tiền"};
                    DefaultTableModel model = new DefaultTableModel(cols, 0);
                    for (Map<String, Object> row : details) {
                        model.addRow(new Object[]{row.get("name"), row.get("quantity"), row.get("price") + " đ", row.get("total") + " đ"});
                    }
                    JTable table = new JTable(model);
                    dialog.add(new JScrollPane(table));
                    dialog.setSize(500, 300);
                    dialog.setLocationRelativeTo(DashboardPanel.this);
                    dialog.setVisible(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    // Thêm hàm này vào để MainForm gọi mỗi khi chuyển trang
    public void loadStats() {
        loadInvoiceData();
        loadChartData();
    }

    // ================= LỚP VẼ BIỂU ĐỒ NỘI BỘ (KHÔNG DÙNG THƯ VIỆN) =================
    class CustomBarChart extends JPanel {
        private List<Map<String, Object>> data;
        public CustomBarChart(List<Map<String, Object>> data) {
            this.data = data;
            Collections.reverse(this.data); // Đảo ngược để ngày cũ hiện bên trái
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.isEmpty()) return;

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int padding = 40;
            int numBars = data.size();

            // Xử lý chia cột linh hoạt (tránh lỗi chia cho 0 nếu data rỗng, và chia đều khoảng cách)
            int barWidth = 40; // Độ rộng mặc định của cột
            if(numBars > 0) {
                barWidth = Math.min(80, (width - 2 * padding) / numBars - 20);
            }
            if (barWidth < 5) barWidth = 5; // Độ rộng tối thiểu

            // Tìm giá trị doanh thu lớn nhất để chia tỷ lệ chiều cao cột
            double maxRev = 0;
            for (Map<String, Object> row : data) {
                double rev = ((BigDecimal) row.get("revenue")).doubleValue();
                if (rev > maxRev) maxRev = rev;
            }
            if (maxRev == 0) maxRev = 1; // Chống chia cho 0

            // Vẽ trục X và Y
            g2d.drawLine(padding, height - padding, width - padding, height - padding);
            g2d.drawLine(padding, height - padding, padding, padding);

            // Vẽ các cột
            for (int i = 0; i < numBars; i++) {
                Map<String, Object> row = data.get(i);
                String date = (String) row.get("date");
                double rev = ((BigDecimal) row.get("revenue")).doubleValue();

                int barHeight = (int) ((rev / maxRev) * (height - 2 * padding - 20));

                // Căn giữa các cột nếu số lượng ít
                int totalContentWidth = numBars * (barWidth + 20);
                int startX = padding + Math.max(10, (width - 2 * padding - totalContentWidth) / 2);

                int x = startX + i * (barWidth + 20);
                int y = height - padding - barHeight;

                // Màu cột
                g2d.setColor(new Color(52, 152, 219));
                g2d.fillRect(x, y, barWidth, barHeight);

                // Chữ doanh thu trên đỉnh cột
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.PLAIN, 11));

                // Format số doanh thu gọn lại nếu nó quá dài (VD: 1.500.000 -> 1.5M)
                String revText;
                if(rev >= 1000000) {
                    revText = String.format("%.1fM", rev / 1000000.0);
                } else if (rev >= 1000) {
                    revText = String.format("%.1fK", rev / 1000.0);
                } else {
                    revText = String.format("%,.0f", rev);
                }

                // Tính toán để căn giữa chữ trên đỉnh cột
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(revText);
                g2d.drawString(revText, x + (barWidth - textWidth) / 2, y - 5);

                // Chữ ngày tháng dưới đáy cột (Cắt bỏ năm cho gọn)
                String shortDate = date.substring(5); // Lấy "MM-DD"

                // Nếu quá nhiều cột (như Tháng này = 30 cột), chỉ hiển thị ngày của một vài cột để không bị rối
                if (numBars <= 10 || i % 3 == 0) {
                    int dateWidth = fm.stringWidth(shortDate);
                    g2d.drawString(shortDate, x + (barWidth - dateWidth) / 2, height - padding + 15);
                }
            }
        }
    }
}