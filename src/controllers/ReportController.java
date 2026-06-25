package controllers;

import db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only reporting / analytics queries for the Admin "Reports" dashboard panel.
 * Does not modify any data — purely aggregates existing orders / materials tables.
 */
public class ReportController {

    /** Simple holder for the top summary cards shown at the top of the Reports panel. */
    public static class SummaryStats {
        public int totalOrders;
        public double totalRevenue;
        public double totalTax;
        public int pendingOrders;
        public int completedOrders;
    }

    public SummaryStats getSummaryStats() {
        SummaryStats stats = new SummaryStats();
        String query = "SELECT "
                     + "  COUNT(*) AS total_orders, "
                     + "  COALESCE(SUM(grand_total), 0) AS total_revenue, "
                     + "  COALESCE(SUM(tax_amount), 0) AS total_tax, "
                     + "  COALESCE(SUM(CASE WHEN order_status = 'Pending' THEN 1 ELSE 0 END), 0) AS pending_orders, "
                     + "  COALESCE(SUM(CASE WHEN order_status = 'Completed' THEN 1 ELSE 0 END), 0) AS completed_orders "
                     + "FROM orders";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                stats.totalOrders = rs.getInt("total_orders");
                stats.totalRevenue = rs.getDouble("total_revenue");
                stats.totalTax = rs.getDouble("total_tax");
                stats.pendingOrders = rs.getInt("pending_orders");
                stats.completedOrders = rs.getInt("completed_orders");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching summary stats: " + e.getMessage());
        }
        return stats;
    }

    /**
     * Top-selling materials by total quantity ordered.
     * Each row: { materialName, totalQuantitySold, totalRevenue }
     */
    public List<Object[]> getTopSellingMaterials(int limit) {
        List<Object[]> rows = new ArrayList<>();
        String query = "SELECT m.name, SUM(oi.quantity) AS total_qty, SUM(oi.line_total) AS total_revenue "
                     + "FROM order_items oi "
                     + "JOIN materials m ON oi.material_id = m.material_id "
                     + "GROUP BY m.material_id, m.name "
                     + "ORDER BY total_qty DESC "
                     + "LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getString("name"),
                    rs.getInt("total_qty"),
                    rs.getDouble("total_revenue")
                });
            }
        } catch (SQLException e) {
            System.err.println("Error fetching top selling materials: " + e.getMessage());
        }
        return rows;
    }

    /**
     * Revenue grouped by order status (Pending, Completed, Cancelled, etc).
     * Each row: { status, orderCount, totalRevenue }
     */
    public List<Object[]> getRevenueByStatus() {
        List<Object[]> rows = new ArrayList<>();
        String query = "SELECT order_status, COUNT(*) AS order_count, COALESCE(SUM(grand_total), 0) AS total_revenue "
                     + "FROM orders "
                     + "GROUP BY order_status "
                     + "ORDER BY total_revenue DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getString("order_status"),
                    rs.getInt("order_count"),
                    rs.getDouble("total_revenue")
                });
            }
        } catch (SQLException e) {
            System.err.println("Error fetching revenue by status: " + e.getMessage());
        }
        return rows;
    }

    /**
     * Top customers by total amount spent.
     * Each row: { customerName, orderCount, totalSpent }
     */
    public List<Object[]> getTopCustomers(int limit) {
        List<Object[]> rows = new ArrayList<>();
        String query = "SELECT COALESCE(c.company_name, c.full_name) AS customer_name, "
                     + "       COUNT(o.order_id) AS order_count, SUM(o.grand_total) AS total_spent "
                     + "FROM orders o "
                     + "JOIN customers c ON o.customer_id = c.customer_id "
                     + "GROUP BY o.customer_id, customer_name "
                     + "ORDER BY total_spent DESC "
                     + "LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getString("customer_name"),
                    rs.getInt("order_count"),
                    rs.getDouble("total_spent")
                });
            }
        } catch (SQLException e) {
            System.err.println("Error fetching top customers: " + e.getMessage());
        }
        return rows;
    }

    /**
     * Materials that are low on company stock (<= threshold), useful for a restock report.
     * Each row: { materialName, category, companyStock }
     */
    public List<Object[]> getLowStockMaterials(int threshold) {
        List<Object[]> rows = new ArrayList<>();
        String query = "SELECT name, category, company_stock FROM materials "
                     + "WHERE company_stock <= ? AND status = 'Available' "
                     + "ORDER BY company_stock ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, threshold);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getInt("company_stock")
                });
            }
        } catch (SQLException e) {
            System.err.println("Error fetching low stock materials: " + e.getMessage());
        }
        return rows;
    }
}
