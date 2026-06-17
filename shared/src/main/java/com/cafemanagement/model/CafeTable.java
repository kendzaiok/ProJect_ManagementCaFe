package com.cafemanagement.model;

import java.io.Serializable;

public class CafeTable implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int tableNumber; // Bổ sung biến này cho chuẩn với Database
    private String name;     // Biến này để hiển thị trên Giao diện
    private String status;

    public CafeTable() {
    }

    public CafeTable(int id, int tableNumber, String status) {
        this.id = id;
        this.setTableNumber(tableNumber); // Gọi hàm set để tự gen luôn tên bàn
        this.status = status;
    }

    // --- Các hàm Getter/Setter ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Lấy số bàn (Dành cho Database DAO)
    public int getTableNumber() {
        return tableNumber;
    }

    // Set số bàn và TỰ ĐỘNG tạo tên bàn (Dành cho UI)
    public void setTableNumber(int tableNumber) {
        this.tableNumber = tableNumber;
        this.name = "Bàn " + tableNumber; // Ví dụ: số 1 -> "Bàn 1"
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return this.name + " (" + this.status + ")";
    }
}