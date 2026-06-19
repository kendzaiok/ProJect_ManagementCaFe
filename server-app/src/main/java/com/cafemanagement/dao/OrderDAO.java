package com.cafemanagement.dao;

import com.cafemanagement.model.Order;
import com.cafemanagement.model.OrderItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface OrderDAO {
    boolean add(Order order);
    boolean update(Order order);
    boolean delete(int id);
    Order findById(int id);
    List<Order> findAll();
    List<Order> findByTableId(int tableId);
    List<Order> findByStatus(String status);
    List<Order> search(String keyword);
    boolean createOrder(Order order, List<OrderItem> items);
    BigDecimal getTotalRevenue();
    int getTotalOrders();

    // ========================================================
    // CÁC HÀM PHỤC VỤ CHO DASHBOARD THỐNG KÊ (ĐÃ NÂNG CẤP)
    // ========================================================

    // 1. Lấy chi tiết đơn hàng (Khi click đúp vào dòng hóa đơn)
    List<Map<String, Object>> getOrderDetails(int orderId);

    // 2. Lấy danh sách hóa đơn theo ngày (Cho Tab 1)
    List<Order> getOrdersByDate(String dateString);

    // 3. Lấy dữ liệu Biểu đồ và Top 5 (Có gắn bộ lọc thời gian period cho Tab 2)
    List<Map<String, Object>> getRevenueByPeriod(String period);
    List<Map<String, Object>> getTopSellingProductsByPeriod(String period);
}