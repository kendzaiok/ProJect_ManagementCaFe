package com.cafemanagement.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static String URL;
    private static String USER;
    private static String PASSWORD;
    
    private static DatabaseConnection instance;
    private Connection connection;
    
    static {
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("server-config.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                System.out.println("Không tìm thấy file server-config.properties");
                URL = "jdbc:mysql://localhost:3306/cafe_management?useSSL=false&serverTimezone=UTC";
                USER = "root";
                PASSWORD = "";
            } else {
                prop.load(input);
                URL = prop.getProperty("db.url");
                USER = prop.getProperty("db.user");
                PASSWORD = prop.getProperty("db.password");
            }
        } catch (IOException e) {
            e.printStackTrace();
            URL = "jdbc:mysql://localhost:3306/cafe_management?useSSL=false&serverTimezone=UTC";
            USER = "root";
            PASSWORD = "";
        }
    }
    
    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connected successfully!");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Failed to connect to database! URL: " + URL + ", User: " + USER);
            e.printStackTrace();
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                System.out.println("Reconnecting to database...");
                this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            System.err.println("Error getting database connection!");
            e.printStackTrace();
        }
        return connection;
    }
    
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
