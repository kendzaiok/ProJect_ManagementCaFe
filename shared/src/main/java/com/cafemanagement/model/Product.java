package com.cafemanagement.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Product implements Serializable {
    private int id;
    private String name;
    private BigDecimal price;
    private String category;

    // 2 thuộc tính mới
    private String imagePath;
    private boolean isPos;

    public Product() {
    }

    // ==========================================
    // CÁC GETTER VÀ SETTER GỐC (BỊ THIẾU KHIẾN BÁO LỖI)
    // ==========================================
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // ==========================================
    // CÁC GETTER VÀ SETTER MỚI THÊM
    // ==========================================
    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isPos() {
        return isPos;
    }

    public void setPos(boolean pos) {
        this.isPos = pos;
    }
}