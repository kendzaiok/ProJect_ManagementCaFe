package com.cafemanagement.dao;

import com.cafemanagement.model.CafeTable;
import java.util.List;

public interface CafeTableDAO {
    boolean add(CafeTable cafeTable);
    boolean update(CafeTable cafeTable);
    boolean delete(int id);
    CafeTable findById(int id);
    CafeTable findByTableNumber(int tableNumber);
    List<CafeTable> findAll();
    List<CafeTable> findByStatus(String status);
}
