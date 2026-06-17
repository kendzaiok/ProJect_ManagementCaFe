package com.cafemanagement.dao;

import com.cafemanagement.model.OrderItem;
import java.util.List;

public interface OrderItemDAO {
    boolean add(OrderItem orderItem);
    boolean update(OrderItem orderItem);
    boolean delete(int id);
    OrderItem findById(int id);
    List<OrderItem> findAll();
    List<OrderItem> findByOrderId(int orderId);
}
