package controllers;

import db.DatabaseConnection;
import models.Material;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MaterialController {

    public boolean addMaterial(Material m) {
        String query = "INSERT INTO materials (supplier_id, name, category, unit_price, stock_quantity, status) VALUES (?, ?, ?, ?, ?, 'Available')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, m.getSupplierId());
            stmt.setString(2, m.getName());
            stmt.setString(3, m.getCategory());
            stmt.setDouble(4, m.getUnitPrice());
            stmt.setInt(5, m.getStockQuantity());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateMaterial(int materialId, int supplierId, String category, double newPrice, int newStock) {
        // Added 'category = ?' to the SQL query
        String query = "UPDATE materials SET category = ?, unit_price = ?, stock_quantity = ? WHERE material_id = ? AND supplier_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, category);
            stmt.setDouble(2, newPrice);
            stmt.setInt(3, newStock);
            stmt.setInt(4, materialId);
            stmt.setInt(5, supplierId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean discontinueMaterial(int materialId, int supplierId) {
        String query = "UPDATE materials SET status = 'Discontinued' WHERE material_id = ? AND supplier_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, materialId);
            stmt.setInt(2, supplierId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean restoreMaterial(int materialId, int supplierId) {
        String query = "UPDATE materials SET status = 'Available' WHERE material_id = ? AND supplier_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, materialId);
            stmt.setInt(2, supplierId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
 // 1. Checks if a material with the same name already exists for this supplier
    public boolean checkMaterialExists(int supplierId, String materialName) {
        String query = "SELECT COUNT(*) FROM materials WHERE supplier_id = ? AND LOWER(name) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, supplierId);
            stmt.setString(2, materialName.trim());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // Returns true if count is 1 or more
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 2. Permanently deletes a material from the database
    public boolean deleteMaterial(int materialId, int supplierId) {
        String query = "DELETE FROM materials WHERE material_id = ? AND supplier_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, materialId);
            stmt.setInt(2, supplierId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Material> getSupplierMaterials(int supplierId) {
        List<Material> list = new ArrayList<>();
        String query = "SELECT * FROM materials WHERE supplier_id = ? ORDER BY status ASC, name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, supplierId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new Material(
                    rs.getInt("material_id"), rs.getInt("supplier_id"), rs.getString("name"),
                    rs.getString("category"), rs.getDouble("unit_price"), rs.getInt("stock_quantity"), rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
 // Fetches materials and joins them with the supplier's name, including both stock levels
    public List<Object[]> getCatalogWithSupplierInfo(String keyword) {
        List<Object[]> catalog = new ArrayList<>();
        
        // Ensure m.company_stock is selected so we get 7 columns of data
        String query = "SELECT m.material_id, s.supplier_name, m.name, m.category, m.unit_price, m.company_stock, m.stock_quantity " +
                       "FROM materials m " +
                       "INNER JOIN suppliers s ON m.supplier_id = s.supplier_id " +
                       "WHERE m.status = 'Available' ";
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            query += "AND (m.name LIKE ? OR s.supplier_name LIKE ?) ";
        }
        query += "ORDER BY s.supplier_name ASC, m.name ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchPattern = "%" + keyword.trim() + "%";
                stmt.setString(1, searchPattern);
                stmt.setString(2, searchPattern);
            }
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                catalog.add(new Object[]{
                    rs.getInt("material_id"),
                    rs.getString("supplier_name"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("unit_price"),
                    rs.getInt("company_stock"), // Column 5: Our Stock (Will show 0 now!)
                    rs.getInt("stock_quantity") // Column 6: Supplier Stock
                });
            }
        } catch (SQLException e) {
            System.err.println("Database Error fetching catalog: " + e.getMessage());
        }
        return catalog;
    }
 // Fetches the hidden Supplier ID based on the Material selected
    public int getSupplierIdFromMaterial(int materialId) {
        String query = "SELECT supplier_id FROM materials WHERE material_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, materialId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("supplier_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
 // 1. Fetches ONLY materials that the Company currently has in stock
    public List<Object[]> getCompanyInventory(String keyword) {
        List<Object[]> inventory = new ArrayList<>();
        
        // Notice we only select items where company_stock > 0
        String query = "SELECT material_id, name, category, unit_price, selling_price, company_stock " +
                       "FROM materials WHERE company_stock > 0 AND status = 'Available' ";
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            query += "AND name LIKE ? ";
        }
        query += "ORDER BY name ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                stmt.setString(1, "%" + keyword.trim() + "%");
            }
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                inventory.add(new Object[]{
                    rs.getInt("material_id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("unit_price"),     // The cost from supplier
                    rs.getDouble("selling_price"),  // The price we charge the customer
                    rs.getInt("company_stock")      // Amount available to sell
                });
            }
        } catch (SQLException e) {
            System.err.println("Database Error fetching company inventory: " + e.getMessage());
        }
        return inventory;
    }

    // 2. Updates the Selling Price of a material
    public boolean updateSellingPrice(int materialId, double newSellingPrice) {
        String query = "UPDATE materials SET selling_price = ? WHERE material_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setDouble(1, newSellingPrice);
            stmt.setInt(2, materialId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating selling price: " + e.getMessage());
            return false;
        }
    }

     public boolean removeFromSale(int materialId) {
         String query = "UPDATE materials SET company_stock = 0 WHERE material_id = ?";
         try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement stmt = conn.prepareStatement(query)) {
             stmt.setInt(1, materialId);
             return stmt.executeUpdate() > 0;
         } catch (SQLException e) {
             System.err.println("Error removing material from sale: " + e.getMessage());
             return false;
         }
     }
}