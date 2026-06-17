package com.cafemanagement.dao;

import com.cafemanagement.model.Product;
import java.util.List;

public interface ProductDAO {
    boolean add(Product product);
    boolean update(Product product);
    boolean delete(int id);
    Product findById(int id);
    List<Product> findAll();
    List<Product> search(String keyword);
    List<Product> findByCategory(String category);
}
