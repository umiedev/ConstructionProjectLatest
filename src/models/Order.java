package models;
import java.sql.Date;

public class Order {
    private int orderId;
    private int customerId;
    private Date orderDate;
    private double subtotal;
    private double taxAmount;
    private double grandTotal;
    private String orderStatus;

    public Order(int customerId, double subtotal, double taxAmount, double grandTotal, String orderStatus) {
        this.customerId = customerId;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.grandTotal = grandTotal;
        this.orderStatus = orderStatus;
    }

    public Order(int orderId, int customerId, Date orderDate, double subtotal, double taxAmount, double grandTotal, String orderStatus) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.grandTotal = grandTotal;
        this.orderStatus = orderStatus;
    }

    // Getters and Setters
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public int getCustomerId() { return customerId; }
    public Date getOrderDate() { return orderDate; }
    public double getSubtotal() { return subtotal; }
    public double getTaxAmount() { return taxAmount; }
    public double getGrandTotal() { return grandTotal; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
}