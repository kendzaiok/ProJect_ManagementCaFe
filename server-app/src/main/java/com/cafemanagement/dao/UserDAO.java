package com.cafemanagement.dao;

import com.cafemanagement.model.User;
import java.util.List;

public interface UserDAO {
    boolean add(User user);
    boolean update(User user);
    boolean delete(int id);
    User findById(int id);
    User findByUsername(String username);
    List<User> findAll();
    List<User> search(String keyword);
}
