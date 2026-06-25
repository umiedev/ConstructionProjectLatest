package models;

public class OrderItem {
    private int orderItemId;
    private int orderId;
    private int materialId;
    private int quantity;
    private double lineTotal;

    // CONSTRUCTOR 1: Used when making a NEW order (IDs are unknown)
    public OrderItem(int materialId, int quantity, double lineTotal) {
        this.materialId = materialId;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
    }

    // CONSTRUCTOR 2: Used when VIEWING an existing order from the database (IDs are known)
    public OrderItem(int orderItemId, int orderId, int materialId, int quantity, double lineTotal) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.materialId = materialId;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
    }

    // Getters
    public int getOrderItemId() { return orderItemId; }
    public int getOrderId() { return orderId; }
    public int getMaterialId() { return materialId; }
    public int getQuantity() { return quantity; }
    public double getLineTotal() { return lineTotal; }
    
    // Setters
    public void setOrderId(int orderId) { this.orderId = orderId; }
}