package controllers;

import db.DatabaseConnection;
import models.Customer;
import utils.PasswordUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerController {

    /** Human-readable description of the most recent failure (null when the last op succeeded). */
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
                 + "    ALTER TABLE customers MODIFY password_hash VARCHAR(255);";
        }
        return "Database error (code " + code + "): " + e.getMessage();
    }

    public Customer authenticateLogin(String username, String password) {
        String query = "SELECT * FROM customers WHERE username = ? AND status = 'Active'";
        try (Connection conn = DatabaseConnection.getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                
                // NO BYPASS: Always verify using PasswordUtil
                if (!PasswordUtil.verifyPassword(password, storedHash)) {
                    System.out.println("DEBUG: Password verification failed for: " + username);
                    return null; 
                }
                return extractCustomer(rs);
            } else {
                System.out.println("DEBUG: User not found or inactive: " + username);
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        return null;
    }

    public boolean addCustomer(Customer c) {
        lastError = null;
        String query = "INSERT INTO customers (username, password_hash, company_name, full_name, phone, email, shipping_address, status) VALUES (?, ?, ?, ?, ?, ?, ?, 'Active')";
        try (Connection conn = DatabaseConnection.getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            // Ensure column in DB is VARCHAR(255) to hold long bcrypt hashes
            String hashed = PasswordUtil.hashPassword(c.getPasswordHash());
            
            stmt.setString(1, c.getUsername());
            stmt.setString(2, hashed);
            stmt.setString(3, c.getCompanyName());
            stmt.setString(4, c.getFullName());
            stmt.setString(5, c.getPhone());
            stmt.setString(6, c.getEmail());
            stmt.setString(7, c.getShippingAddress());
            
            boolean ok = stmt.executeUpdate() > 0;
            if (!ok) lastError = "Could not add customer. Please try again.";
            return ok;
        } catch (SQLException e) { 
            lastError = describeSqlError(e);
            System.err.println("addCustomer error: " + e.getMessage());
            return false; 
        }
    }

    public Customer getCustomerById(int customerId) {
        String query = "SELECT * FROM customers WHERE customer_id = ? AND status = 'Active'";
        try (Connection conn = DatabaseConnection.getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return extractCustomer(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public boolean updateCustomerProfile(int customerId, String company, String fullName, String phone, String email, String address) {
        String query = "UPDATE customers SET company_name=?, full_name=?, phone=?, email=?, shipping_address=? WHERE customer_id=?";
        try (Connection conn = DatabaseConnection.getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, company); stmt.setString(2, fullName); stmt.setString(3, phone); 
            stmt.setString(4, email); stmt.setString(5, address); stmt.setInt(6, customerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean updateCustomerPassword(int customerId, String newPassword) {
        String query = "UPDATE customers SET password_hash = ? WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, PasswordUtil.hashPassword(newPassword)); 
            stmt.setInt(2, customerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean deactivateCustomer(int customerId) {
        String query = "UPDATE customers SET status = 'Inactive' WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, customerId); 
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean deleteCustomer(int customerId) {
        try (Connection conn = DatabaseConnection.getConnection(); 
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM customers WHERE customer_id = ?")) {
            stmt.setInt(1, customerId); 
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public void renumberCustomerIds() {
        try (Connection conn = DatabaseConnection.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("SET @count = 0;");
            stmt.executeUpdate("UPDATE customers SET customer_id = (@count:= @count + 1);");
            stmt.executeUpdate("ALTER TABLE customers AUTO_INCREMENT = 1;");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Customer> searchCustomers(String keyword) {
    	String query = "SELECT * FROM customers WHERE (username LIKE ? OR full_name LIKE ? OR CAST(customer_id AS CHAR) = ?) AND status = 'Active'";
        return searchCustomerList(query, keyword);
    }

    public List<Customer> searchAllCustomers(String keyword) {
        String query = "SELECT * FROM customers WHERE (username LIKE ? OR full_name LIKE ? OR CAST(customer_id AS CHAR) = ?) ORDER BY customer_id ASC";
        return searchCustomerList(query, keyword);
    }

    public List<Customer> getAllCustomers() { return getCustomerList("SELECT * FROM customers ORDER BY customer_id ASC"); }
    public List<Customer> getAllActiveCustomers() { return getCustomerList("SELECT * FROM customers WHERE status = 'Active' ORDER BY customer_id ASC"); }

    private List<Customer> getCustomerList(String query) {
        List<Customer> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) list.add(extractCustomer(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private List<Customer> searchCustomerList(String query, String keyword) {
        List<Customer> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            String pattern = "%" + keyword + "%";
            stmt.setString(1, pattern); stmt.setString(2, pattern); stmt.setString(3, keyword);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(extractCustomer(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private Customer extractCustomer(ResultSet rs) throws SQLException {
        return new Customer(rs.getInt("customer_id"), rs.getString("username"), rs.getString("password_hash"),
                rs.getString("company_name"), rs.getString("full_name"), rs.getString("phone"),
                rs.getString("email"), rs.getString("shipping_address"), rs.getString("status"));
    }
}