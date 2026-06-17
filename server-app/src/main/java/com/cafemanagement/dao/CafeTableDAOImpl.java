package com.cafemanagement.dao;

import com.cafemanagement.database.DatabaseConnection;
import com.cafemanagement.model.CafeTable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CafeTableDAOImpl implements CafeTableDAO {

    @Override
    public boolean add(CafeTable cafeTable) {
        String sql = "INSERT INTO cafe_tables (table_number, status) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, cafeTable.getTableNumber());
            pstmt.setString(2, cafeTable.getStatus());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(CafeTable cafeTable) {
        String sql = "UPDATE cafe_tables SET table_number = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, cafeTable.getTableNumber());
            pstmt.setString(2, cafeTable.getStatus());
            pstmt.setInt(3, cafeTable.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM cafe_tables WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public CafeTable findById(int id) {
        String sql = "SELECT * FROM cafe_tables WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return extractCafeTable(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public CafeTable findByTableNumber(int tableNumber) {
        String sql = "SELECT * FROM cafe_tables WHERE table_number = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tableNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return extractCafeTable(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<CafeTable> findAll() {
        List<CafeTable> tables = new ArrayList<>();
        String sql = "SELECT * FROM cafe_tables";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tables.add(extractCafeTable(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tables;
    }

    @Override
    public List<CafeTable> findByStatus(String status) {
        List<CafeTable> tables = new ArrayList<>();
        String sql = "SELECT * FROM cafe_tables WHERE status = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tables.add(extractCafeTable(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tables;
    }

    private CafeTable extractCafeTable(ResultSet rs) throws SQLException {
        CafeTable cafeTable = new CafeTable();
        cafeTable.setId(rs.getInt("id"));
        cafeTable.setTableNumber(rs.getInt("table_number"));
        cafeTable.setStatus(rs.getString("status"));
        return cafeTable;
    }
}
