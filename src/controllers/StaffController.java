package controllers;

import db.DatabaseConnection;
import models.Staff;
import utils.PasswordUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StaffController {

    // ==========================================
    // 1. AUTHENTICATION (Login Gateway)
    // ==========================================

	public Staff authenticateLogin(String username, String password) {
        String query = "SELECT * FROM staff WHERE username = ? AND status = 'Active'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                // ==========================================
                // NEW PASSWORD VERIFICATION LOGIC HERE
                // ==========================================
                
                // Bypass hashing ONLY for the manually created "admin" testing account
                if (username.equals("admin")) {
                    // Plain-text comparison just for this specific user
                    if (!password.equals(storedHash)) {
                        return null; // Incorrect password
                    }
                } else {
                    // Secure hash verification for all other users created through the system
                    if (!PasswordUtil.verifyPassword(password, storedHash)) {
                        return null; // Incorrect password
                    }
                }
                
                // ==========================================

                return new Staff(
                    rs.getInt("staff_id"),
                    rs.getString("username"),
                    storedHash,
                    rs.getString("full_name"),
                    rs.getString("role"),
                    rs.getString("status")
                );
            }
        } catch (SQLException e) {
            System.err.println("Database error during login: " + e.getMessage());
        }
        return null;
    }

    // ==========================================
    // 2. STAFF MANAGEMENT (CRUDS)
    // ==========================================

    public boolean addStaff(Staff staff) {
        String query = "INSERT INTO staff (username, password_hash, full_name, role, status) VALUES (?, ?, ?, ?, 'Active')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            // staff.getPasswordHash() holds the plain-text password typed in
            // the Add Staff form at this point — hash it here, right before
            // it's written, so a plain-text password is never persisted.
            String hashed = PasswordUtil.hashPassword(staff.getPasswordHash());

            stmt.setString(1, staff.getUsername());
            stmt.setString(2, hashed);
            stmt.setString(3, staff.getFullName());
            stmt.setString(4, staff.getRole());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding staff: " + e.getMessage());
            return false;
        }
    }


    public Staff getStaffById(int staffId) {
        String query = "SELECT * FROM staff WHERE staff_id = ? AND status = 'Active'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, staffId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Staff(
                    rs.getInt("staff_id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("full_name"),
                    rs.getString("role"),
                    rs.getString("status")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving staff by ID: " + e.getMessage());
        }
        return null;
    }

    public List<Staff> getAllActiveStaff() {
        List<Staff> staffList = new ArrayList<>();
        String query = "SELECT * FROM staff WHERE status = 'Active' ORDER BY staff_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                staffList.add(new Staff(
                    rs.getInt("staff_id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("full_name"),
                    rs.getString("role"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving staff list: " + e.getMessage());
        }
        return staffList;
    }


    public List<Staff> getAllStaff() {
        List<Staff> staffList = new ArrayList<>();
        String query = "SELECT * FROM staff ORDER BY staff_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                staffList.add(new Staff(
                    rs.getInt("staff_id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("full_name"),
                    rs.getString("role"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving full staff list: " + e.getMessage());
        }
        return staffList;
    }

    public boolean updateStaffProfile(int staffId, String newFullName, String newRole) {
        String query = "UPDATE staff SET full_name = ?, role = ? WHERE staff_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, newFullName);
            stmt.setString(2, newRole);
            stmt.setInt(3, staffId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating staff profile: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStaffPassword(int staffId, String newPassword) {
        String query = "UPDATE staff SET password_hash = ? WHERE staff_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            String hashed = PasswordUtil.hashPassword(newPassword);

            stmt.setString(1, hashed);
            stmt.setInt(2, staffId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
            return false;
        }
    }

    public boolean deactivateStaff(int staffId) {
        String query = "UPDATE staff SET status = 'Inactive' WHERE staff_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, staffId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deactivating staff: " + e.getMessage());
            return false;
        }
    }


    public boolean deleteStaff(int staffId) {
        String query = "DELETE FROM staff WHERE staff_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, staffId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting staff: " + e.getMessage());
            return false;
        }
    }


    public boolean renumberStaffIds() {
        String selectQuery = "SELECT staff_id FROM staff ORDER BY staff_id ASC";
        String updateQuery = "UPDATE staff SET staff_id = ? WHERE staff_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            List<Integer> oldIds = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectQuery)) {
                while (rs.next()) {
                    oldIds.add(rs.getInt("staff_id"));
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

            // Reset AUTO_INCREMENT so the next new staff member continues from the new max ID
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE staff AUTO_INCREMENT = " + (oldIds.size() + 1));
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error renumbering staff IDs: " + e.getMessage());
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

    public List<Staff> searchStaff(String keyword) {
        List<Staff> staffList = new ArrayList<>();
        String query = "SELECT * FROM staff WHERE (username LIKE ? OR full_name LIKE ?) AND status = 'Active'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                staffList.add(new Staff(
                    rs.getInt("staff_id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("full_name"),
                    rs.getString("role"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error searching staff: " + e.getMessage());
        }
        return staffList;
    }


    public List<Staff> searchAllStaff(String keyword) {
        List<Staff> staffList = new ArrayList<>();
        String query = "SELECT * FROM staff WHERE username LIKE ? OR full_name LIKE ? ORDER BY staff_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                staffList.add(new Staff(
                    rs.getInt("staff_id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("full_name"),
                    rs.getString("role"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error searching all staff: " + e.getMessage());
        }
        return staffList;
    }
}