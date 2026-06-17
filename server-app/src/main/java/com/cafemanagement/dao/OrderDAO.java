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
    List<Map<String, Object>> getTopSellingProducts();
}
