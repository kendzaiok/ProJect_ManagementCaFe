-- Tạo database nếu chưa có
CREATE DATABASE IF NOT EXISTS cafe_management;
USE cafe_management;

-- Xóa bảng nếu có (để tạo lại cho chắc)
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cafe_tables;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;

-- Bảng users
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'EMPLOYEE') NOT NULL
);

-- Bảng products
CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(50) NOT NULL
);

-- Bảng cafe_tables
CREATE TABLE cafe_tables (
    id INT AUTO_INCREMENT PRIMARY KEY,
    table_number INT NOT NULL UNIQUE,
    status ENUM('AVAILABLE', 'OCCUPIED', 'RESERVED') NOT NULL DEFAULT 'AVAILABLE'
);

-- Bảng orders
CREATE TABLE orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    table_id INT NOT NULL,
    user_id INT NOT NULL,
    total_price DECIMAL(10, 2) DEFAULT 0.00,
    status ENUM('PENDING', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (table_id) REFERENCES cafe_tables(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Bảng order_items
CREATE TABLE order_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Chèn dữ liệu mẫu cho users (mật khẩu: "123")
INSERT INTO users (username, password, role) VALUES
('admin', '123', 'ADMIN'),
('employee1', '123', 'EMPLOYEE'),
('employee2', '123', 'EMPLOYEE');

-- Chèn dữ liệu mẫu cho products
INSERT INTO products (name, price, category) VALUES
('Cà phê sữa', 25000.00, 'Cà phê'),
('Cà phê đen', 20000.00, 'Cà phê'),
('Trà sữa trân châu', 30000.00, 'Trà sữa'),
('Trà đào cam sả', 35000.00, 'Trà trái cây'),
('Sinh tố bưởi', 40000.00, 'Sinh tố'),
('Bánh tiramisu', 45000.00, 'Bánh ngọt'),
('Cà phê sữa đá', 28000.00, 'Cà phê'),
('Trà xanh đá', 32000.00, 'Trà sữa');

-- Chèn dữ liệu mẫu cho cafe_tables
INSERT INTO cafe_tables (table_number, status) VALUES
(1, 'AVAILABLE'),
(2, 'AVAILABLE'),
(3, 'AVAILABLE'),
(4, 'AVAILABLE'),
(5, 'AVAILABLE'),
(6, 'AVAILABLE'),
(7, 'AVAILABLE'),
(8, 'AVAILABLE');
