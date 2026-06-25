package controllers;

import db.DatabaseConnection;
import models.Order;
import models.OrderItem;
import models.Delivery;
import models.Payment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderController {

    // ==========================================
    // 1. THE CALCULATION COMPONENT 
    // ==========================================
    private static final double TAX_RATE = 0.06; // 6% Standard Tax

    public double calculateLineTotal(int quantity, double unitPrice) {
        return quantity * unitPrice;
    }

    public double calculateTax(double subtotal) {
        return subtotal * TAX_RATE;
    }

    public double calculateGrandTotal(double subtotal, double tax, double deliveryFee) {
        return subtotal + tax + deliveryFee;
    }

    // ==========================================
    // 2. SALES ORDER PROCESSING (Customer Buys from Company)
    // ==========================================
    
    public boolean placeOrder(Order order, List<OrderItem> items) {
        String insertOrderQuery = "INSERT INTO orders (customer_id, order_date, subtotal, tax_amount, grand_total, order_status) VALUES (?, CURDATE(), ?, ?, ?, ?)";
        String insertItemQuery = "INSERT INTO order_items (order_id, material_id, quantity, line_total) VALUES (?, ?, ?, ?)";
        
        // FIXED: Customer orders now deduct strictly from COMPANY stock
        String updateStockQuery = "UPDATE materials SET company_stock = company_stock - ? WHERE material_id = ?";

        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); 

            int generatedOrderId = -1;
            try (PreparedStatement orderStmt = conn.prepareStatement(insertOrderQuery, Statement.RETURN_GENERATED_KEYS)) {
                orderStmt.setInt(1, order.getCustomerId());
                orderStmt.setDouble(2, order.getSubtotal());
                orderStmt.setDouble(3, order.getTaxAmount());
                orderStmt.setDouble(4, order.getGrandTotal());
                orderStmt.setString(5, "Pending");
                orderStmt.executeUpdate();

                ResultSet rs = orderStmt.getGeneratedKeys();
                if (rs.next()) {
                    generatedOrderId = rs.getInt(1);
                }
            }

            if (generatedOrderId == -1) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement itemStmt = conn.prepareStatement(insertItemQuery);
                 PreparedStatement stockStmt = conn.prepareStatement(updateStockQuery)) {
                
                for (OrderItem item : items) {
                    itemStmt.setInt(1, generatedOrderId);
                    itemStmt.setInt(2, item.getMaterialId());
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.setDouble(4, item.getLineTotal());
                    itemStmt.addBatch();

                    // Deduct stock from company_stock
                    stockStmt.setInt(1, item.getQuantity());
                    stockStmt.setInt(2, item.getMaterialId());
                    stockStmt.addBatch();
                }
                itemStmt.executeBatch();
                stockStmt.executeBatch();
            }

            conn.commit(); 
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
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

    // ==========================================
    // 3. PURCHASE ORDER PROCESSING (Staff Buys from Supplier)
    // ==========================================
    
    /**
     * Creates a purchase order.
     * ONLY deducts from the Supplier's stock to reserve it.
     */
    public boolean createPurchaseOrder(int supplierId, int materialId, int quantity, double unitPrice) {
        String insertPOQuery = "INSERT INTO purchase_orders (supplier_id, order_date, total_amount, status) VALUES (?, CURDATE(), ?, 'Pending')";
        String insertPOItemQuery = "INSERT INTO purchase_order_items (po_id, material_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
        
        // FIXED: Only reduce supplier stock. Do NOT touch company stock yet.
        String updateSupplierStockQuery = "UPDATE materials SET stock_quantity = stock_quantity - ? WHERE material_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); 

            double totalAmount = calculateLineTotal(quantity, unitPrice);

            int generatedPoId = -1;
            try (PreparedStatement poStmt = conn.prepareStatement(insertPOQuery, Statement.RETURN_GENERATED_KEYS)) {
                poStmt.setInt(1, supplierId);
                poStmt.setDouble(2, totalAmount);
                poStmt.executeUpdate();

                ResultSet rs = poStmt.getGeneratedKeys();
                if (rs.next()) generatedPoId = rs.getInt(1);
            }

            if (generatedPoId == -1) { conn.rollback(); return false; }

            try (PreparedStatement itemStmt = conn.prepareStatement(insertPOItemQuery);
                 PreparedStatement stockStmt = conn.prepareStatement(updateSupplierStockQuery)) {
                
                itemStmt.setInt(1, generatedPoId);
                itemStmt.setInt(2, materialId);
                itemStmt.setInt(3, quantity);
                itemStmt.setDouble(4, unitPrice);
                itemStmt.executeUpdate();

                stockStmt.setInt(1, quantity); 
                stockStmt.setInt(2, materialId);
                stockStmt.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /**
     * Called by Supplier. Marks PO as Delivered and adds goods to Company warehouse.
     */
    public boolean completePurchaseOrderDelivery(int poId, int materialId, int quantity) {
        String updatePOStatusQuery = "UPDATE purchase_orders SET status = 'Delivered' WHERE po_id = ?";
        
        // FIXED: Now we officially add the stock to our Company inventory
        String addCompanyStockQuery = "UPDATE materials SET company_stock = company_stock + ? WHERE material_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); 

            try (PreparedStatement statusStmt = conn.prepareStatement(updatePOStatusQuery)) {
                statusStmt.setInt(1, poId);
                statusStmt.executeUpdate();
            }

            try (PreparedStatement stockStmt = conn.prepareStatement(addCompanyStockQuery)) {
                stockStmt.setInt(1, quantity);
                stockStmt.setInt(2, materialId);
                stockStmt.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /**
     * Fetches all purchase orders for a specific supplier dashboard.
     */
    public List<Object[]> getPurchaseOrdersForSupplier(int supplierId) {
        List<Object[]> orders = new ArrayList<>();
        
        String query = "SELECT p.po_id, p.order_date, m.material_id, m.name, i.quantity, p.total_amount, p.status " +
                       "FROM purchase_orders p " +
                       "JOIN purchase_order_items i ON p.po_id = i.po_id " +
                       "JOIN materials m ON i.material_id = m.material_id " +
                       "WHERE p.supplier_id = ? " +
                       "ORDER BY p.order_date DESC, p.po_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, supplierId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                orders.add(new Object[]{
                    rs.getInt("po_id"),
                    rs.getDate("order_date"),
                    rs.getInt("material_id"),
                    rs.getString("name"),
                    rs.getInt("quantity"),
                    rs.getDouble("total_amount"),
                    rs.getString("status")
                });
            }
        } catch (SQLException e) {
            System.err.println("Database Error fetching POs: " + e.getMessage());
        }
        return orders;
    }

    // ==========================================
    // 4. DELIVERY & PAYMENT MANAGEMENT
    // ==========================================

    public boolean scheduleDelivery(Delivery delivery) {
        String query = "INSERT INTO deliveries (order_id, delivery_date, delivery_address, delivery_fee, driver_name, delivery_status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, delivery.getOrderId());
            stmt.setDate(2, delivery.getDeliveryDate());
            stmt.setString(3, delivery.getDeliveryAddress());
            stmt.setDouble(4, delivery.getDeliveryFee());
            stmt.setString(5, delivery.getDriverName());
            stmt.setString(6, delivery.getDeliveryStatus());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean recordPayment(Payment payment) {
        String query = "INSERT INTO payments (order_id, payment_date, amount_paid, payment_method, payment_status) VALUES (?, CURDATE(), ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, payment.getOrderId());
            stmt.setDouble(2, payment.getAmountPaid());
            stmt.setString(3, payment.getPaymentMethod());
            stmt.setString(4, payment.getPaymentStatus());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateOrderStatus(int orderId, String newStatus) {
        String query = "UPDATE orders SET order_status = ? WHERE order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, newStatus);
            stmt.setInt(2, orderId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
 // ============================================================
 // ADD THESE TWO METHODS to OrderController.java
 // Paste them just BEFORE the closing brace  }  of the class
 // ============================================================

     /**
      * Returns all orders placed by a specific customer.
      * Each row: { orderId, orderDate, subtotal, taxAmount, grandTotal, orderStatus }
      */
     public List<Object[]> getOrdersForCustomer(int customerId) {
         List<Object[]> orders = new ArrayList<>();
         String query = "SELECT order_id, order_date, subtotal, tax_amount, grand_total, order_status "
                      + "FROM orders WHERE customer_id = ? ORDER BY order_date DESC, order_id DESC";
         try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement stmt = conn.prepareStatement(query)) {
             stmt.setInt(1, customerId);
             ResultSet rs = stmt.executeQuery();
             while (rs.next()) {
                 orders.add(new Object[]{
                     rs.getInt("order_id"),
                     rs.getDate("order_date"),
                     rs.getDouble("subtotal"),
                     rs.getDouble("tax_amount"),
                     rs.getDouble("grand_total"),
                     rs.getString("order_status")
                 });
             }
         } catch (SQLException e) {
             System.err.println("Error fetching orders for customer: " + e.getMessage());
         }
         return orders;
     }

     /**
      * Returns every line item inside one order.
      * Each row: { materialName, quantity, unitPrice, lineTotal }
      */
     /**
      * Returns every order in the system, joined with the customer's name.
      * Used by the Admin "Orders" dashboard panel.
      * Each row: { orderId, orderDate, customerName, subtotal, taxAmount, grandTotal, orderStatus }
      */
     public List<Object[]> getAllOrdersForAdmin() {
         List<Object[]> orders = new ArrayList<>();
         String query = "SELECT o.order_id, o.order_date, "
                      + "       COALESCE(c.company_name, c.full_name) AS customer_name, "
                      + "       o.subtotal, o.tax_amount, o.grand_total, o.order_status "
                      + "FROM orders o "
                      + "JOIN customers c ON o.customer_id = c.customer_id "
                      + "ORDER BY o.order_date DESC, o.order_id DESC";
         try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement stmt = conn.prepareStatement(query);
              ResultSet rs = stmt.executeQuery()) {
             while (rs.next()) {
                 orders.add(new Object[]{
                     rs.getInt("order_id"),
                     rs.getDate("order_date"),
                     rs.getString("customer_name"),
                     rs.getDouble("subtotal"),
                     rs.getDouble("tax_amount"),
                     rs.getDouble("grand_total"),
                     rs.getString("order_status")
                 });
             }
         } catch (SQLException e) {
             System.err.println("Error fetching all orders for admin: " + e.getMessage());
         }
         return orders;
     }

     /**
      * Same as getAllOrdersForAdmin() but filtered by customer name, order id, or status keyword.
      */
     public List<Object[]> searchOrdersForAdmin(String keyword) {
         List<Object[]> orders = new ArrayList<>();
         String query = "SELECT o.order_id, o.order_date, "
                      + "       COALESCE(c.company_name, c.full_name) AS customer_name, "
                      + "       o.subtotal, o.tax_amount, o.grand_total, o.order_status "
                      + "FROM orders o "
                      + "JOIN customers c ON o.customer_id = c.customer_id "
                      + "WHERE CAST(o.order_id AS CHAR) LIKE ? "
                      + "   OR c.company_name LIKE ? "
                      + "   OR c.full_name LIKE ? "
                      + "   OR o.order_status LIKE ? "
                      + "ORDER BY o.order_date DESC, o.order_id DESC";
         try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement stmt = conn.prepareStatement(query)) {
             String like = "%" + keyword + "%";
             stmt.setString(1, like);
             stmt.setString(2, like);
             stmt.setString(3, like);
             stmt.setString(4, like);
             ResultSet rs = stmt.executeQuery();
             while (rs.next()) {
                 orders.add(new Object[]{
                     rs.getInt("order_id"),
                     rs.getDate("order_date"),
                     rs.getString("customer_name"),
                     rs.getDouble("subtotal"),
                     rs.getDouble("tax_amount"),
                     rs.getDouble("grand_total"),
                     rs.getString("order_status")
                 });
             }
         } catch (SQLException e) {
             System.err.println("Error searching orders for admin: " + e.getMessage());
         }
         return orders;
     }

     public List<Object[]> getOrderItems(int orderId) {
         List<Object[]> items = new ArrayList<>();
         String query = "SELECT m.name, oi.quantity, "
                      + "       (oi.line_total / oi.quantity) AS unit_price, oi.line_total "
                      + "FROM order_items oi "
                      + "JOIN materials m ON oi.material_id = m.material_id "
                      + "WHERE oi.order_id = ?";
         try (Connection conn = DatabaseConnection.getConnection();
              PreparedStatement stmt = conn.prepareStatement(query)) {
             stmt.setInt(1, orderId);
             ResultSet rs = stmt.executeQuery();
             while (rs.next()) {
                 items.add(new Object[]{
                     rs.getString("name"),
                     rs.getInt("quantity"),
                     rs.getDouble("unit_price"),
                     rs.getDouble("line_total")
                 });
             }
         } catch (SQLException e) {
             System.err.println("Error fetching order items: " + e.getMessage());
         }
         return items;
     }
}