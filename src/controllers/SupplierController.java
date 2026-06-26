package controllers;

import db.DatabaseConnection;
import models.Supplier;
import models.Material;
import utils.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplierController {

    
    private String lastError = null;

    public String getLastError() {
        return lastError;
    }

    /** Turns a raw SQLException into a message that actually explains what went wrong. */
    private String describeSqlError(SQLException e) {
        int code = e.getErrorCode();
        if (code == 1062) { // MySQL: duplicate entry for a UNIQUE key (e.g. username)
            return "That username is already taken. Please choose a different username.";
        }
        if (code == 1406) { // MySQL: data too long for column
            return "A value is too long for its database column.\n"
                 + "This almost always means the 'password_hash' column is too small to\n"
                 + "hold the secure hash. Fix it once by running this SQL:\n\n"
                 + "    ALTER TABLE suppliers MODIFY password_hash VARCHAR(255);";
        }
        return "Database error (code " + code + "): " + e.getMessage();
    }

    // ==========================================
    // ORIGINAL CRUD (for all users)
    // ==========================================

    public boolean addSupplier(Supplier s) {
        lastError = null;
        String query = "INSERT INTO suppliers (supplier_name, contact_number, email, address, username, password_hash, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, s.getSupplierName());
            stmt.setString(2, s.getContactNumber());
            stmt.setString(3, s.getEmail());
            stmt.setString(4, s.getAddress());
            stmt.setString(5, s.getUsername());
            stmt.setString(6, s.getPasswordHash());
            stmt.setString(7, s.getStatus());
            boolean ok = stmt.executeUpdate() > 0;
            if (!ok) lastError = "Could not add supplier. Please try again.";
            return ok;
        } catch (SQLException e) {
            lastError = describeSqlError(e);
            System.err.println("addSupplier error: " + e.getMessage());
            return false;
        }
    }

    public List<Supplier> getAllActiveSuppliers() {
        List<Supplier> suppliers = new ArrayList<>();
        String query = "SELECT * FROM suppliers WHERE status = 'Active'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                suppliers.add(new Supplier(
                    rs.getInt("supplier_id"),
                    rs.getString("supplier_name"),
                    rs.getString("contact_number"),
                    rs.getString("email"),
                    rs.getString("address"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return suppliers;
    }

    public boolean updateSupplier(int supplierId, String newContact, String newEmail) {
        String query = "UPDATE suppliers SET contact_number = ?, email = ? WHERE supplier_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newContact);
            stmt.setString(2, newEmail);
            stmt.setInt(3, supplierId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean softDeleteSupplier(int supplierId) {
        String query = "UPDATE suppliers SET status = 'Inactive' WHERE supplier_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, supplierId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Supplier> searchSupplierByName(String keyword) {
        List<Supplier> suppliers = new ArrayList<>();
        String query = "SELECT * FROM suppliers WHERE supplier_name LIKE ? AND status = 'Active'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                suppliers.add(new Supplier(
                    rs.getInt("supplier_id"),
                    rs.getString("supplier_name"),
                    rs.getString("contact_number"),
                    rs.getString("email"),
                    rs.getString("address"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return suppliers;
    }

    // ==========================================
    // AUTHENTICATION & REGISTRATION (for suppliers)
    // ==========================================

    public Supplier authenticateSupplier(String username, String password) {
        String query = "SELECT * FROM suppliers WHERE username = ? AND status = 'Active'";
        try (Connection conn = DatabaseConnection.getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                
                // CRITICAL: This is where the secure check happens
                if (!PasswordUtil.verifyPassword(password, storedHash)) {
                    System.out.println("DEBUG: Supplier password verification failed.");
                    return null; 
                }
                
                return new Supplier(
                    rs.getInt("supplier_id"), rs.getString("supplier_name"), rs.getString("contact_number"),
                    rs.getString("email"), rs.getString("address"), rs.getString("username"),
                    storedHash, rs.getString("status")
                );
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        return null;
    }

    public boolean registerSupplier(Supplier supplier, String plainPassword) {
        lastError = null;
        String strengthIssue = PasswordUtil.checkStrength(plainPassword);
        if (strengthIssue != null) {
            lastError = strengthIssue;
            System.err.println("Registration failed: " + strengthIssue);
            return false;
        }
        String hashed = PasswordUtil.hashPassword(plainPassword);
        String query = "INSERT INTO suppliers (supplier_name, contact_number, email, address, username, password_hash, status) " +
                       "VALUES (?, ?, ?, ?, ?, ?, 'Active')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, supplier.getSupplierName());
            stmt.setString(2, supplier.getContactNumber());
            stmt.setString(3, supplier.getEmail());
            stmt.setString(4, supplier.getAddress());
            stmt.setString(5, supplier.getUsername());
            stmt.setString(6, hashed);
            boolean ok = stmt.executeUpdate() > 0;
            if (!ok) lastError = "Could not register supplier. Please try again.";
            return ok;
        } catch (SQLException e) {
            lastError = describeSqlError(e);
            System.err.println("Registration error: " + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // SUPPLIER-SPECIFIC MATERIALS (for dashboard)
    // ==========================================

    public List<Material> getMaterialsBySupplier(int supplierId) {
        List<Material> materials = new ArrayList<>();
        String query = "SELECT * FROM materials WHERE supplier_id = ? AND status = 'Available'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, supplierId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                materials.add(new Material(
                    rs.getInt("material_id"),
                    rs.getInt("supplier_id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("unit_price"),
                    rs.getInt("stock_quantity"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching supplier materials: " + e.getMessage());
        }
        return materials;
    }

    // ==========================================
    // NEW ADMIN CRUD METHODS
    // ==========================================

    public Supplier getSupplierById(int supplierId) {
        String query = "SELECT * FROM suppliers WHERE supplier_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, supplierId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Supplier(
                    rs.getInt("supplier_id"),
                    rs.getString("supplier_name"),
                    rs.getString("contact_number"),
                    rs.getString("email"),
                    rs.getString("address"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("status")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching supplier by ID: " + e.getMessage());
        }
        return null;
    }

    public List<Supplier> getAllSuppliers() {
        List<Supplier> suppliers = new ArrayList<>();
        String query = "SELECT * FROM suppliers ORDER BY supplier_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                suppliers.add(new Supplier(
                    rs.getInt("supplier_id"),
                    rs.getString("supplier_name"),
                    rs.getString("contact_number"),
                    rs.getString("email"),
                    rs.getString("address"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving all suppliers: " + e.getMessage());
        }
        return suppliers;
    }

    public List<Supplier> searchAllSuppliers(String keyword) {
        List<Supplier> suppliers = new ArrayList<>();
        String query = "SELECT * FROM suppliers WHERE supplier_name LIKE ? OR contact_number LIKE ? OR email LIKE ? OR username LIKE ? ORDER BY supplier_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            String pattern = "%" + keyword + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            stmt.setString(4, pattern);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                suppliers.add(new Supplier(
                    rs.getInt("supplier_id"),
                    rs.getString("supplier_name"),
                    rs.getString("contact_number"),
                    rs.getString("email"),
                    rs.getString("address"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error searching suppliers: " + e.getMessage());
        }
        return suppliers;
    }

    public boolean updateSupplierFull(int supplierId, String companyName, String contact, String email, String address) {
        String query = "UPDATE suppliers SET supplier_name = ?, contact_number = ?, email = ?, address = ? WHERE supplier_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, companyName);
            stmt.setString(2, contact);
            stmt.setString(3, email);
            stmt.setString(4, address);
            stmt.setInt(5, supplierId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating supplier: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteSupplier(int supplierId) {
        String query = "DELETE FROM suppliers WHERE supplier_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, supplierId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting supplier: " + e.getMessage());
            return false;
        }
    }

    public boolean renumberSupplierIds() {
        String selectQuery = "SELECT supplier_id FROM suppliers ORDER BY supplier_id ASC";
        String updateQuery = "UPDATE suppliers SET supplier_id = ? WHERE supplier_id = ?";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            List<Integer> oldIds = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectQuery)) {
                while (rs.next()) {
                    oldIds.add(rs.getInt("supplier_id"));
                }
            }
            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                int newId = 1;
                for (int oldId : oldIds) {
                    if (oldId != newId) {
                        updateStmt.setInt(1, newId);
                        updateStmt.setInt(2, oldId);
                        updateStmt.executeUpdate();
                    }
                    newId++;
                }
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE suppliers AUTO_INCREMENT = " + (oldIds.size() + 1));
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error renumbering supplier IDs: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    

    public boolean updateSupplierProfile(int supplierId, String name, String phone, String email, String address) {
        String query = "UPDATE suppliers SET supplier_name=?, contact_number=?, email=?, address=? WHERE supplier_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, phone);
            stmt.setString(3, email);
            stmt.setString(4, address);
            stmt.setInt(5, supplierId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateSupplierPassword(int supplierId, String newPassword) {
        String query = "UPDATE suppliers SET password_hash = ? WHERE supplier_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, PasswordUtil.hashPassword(newPassword));
            stmt.setInt(2, supplierId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}