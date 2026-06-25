package models;
import java.sql.Date;

public class Delivery {
    private int deliveryId;
    private int orderId;
    private Date deliveryDate;
    private String deliveryAddress;
    private double deliveryFee;
    private String driverName;
    private String deliveryStatus;

    // CONSTRUCTOR 1: Used when creating a NEW delivery (deliveryId is unknown, DB will auto-increment)
    public Delivery(int orderId, Date deliveryDate, String deliveryAddress, double deliveryFee, String driverName, String deliveryStatus) {
        this.orderId = orderId;
        this.deliveryDate = deliveryDate;
        this.deliveryAddress = deliveryAddress;
        this.deliveryFee = deliveryFee;
        this.driverName = driverName;
        this.deliveryStatus = deliveryStatus;
    }

    // CONSTRUCTOR 2: Used when retrieving an EXISTING delivery from the DB (deliveryId is known)
    public Delivery(int deliveryId, int orderId, Date deliveryDate, String deliveryAddress, double deliveryFee, String driverName, String deliveryStatus) {
        this.deliveryId = deliveryId;
        this.orderId = orderId;
        this.deliveryDate = deliveryDate;
        this.deliveryAddress = deliveryAddress;
        this.deliveryFee = deliveryFee;
        this.driverName = driverName;
        this.deliveryStatus = deliveryStatus;
    }

    // Getters
    public int getDeliveryId() { return deliveryId; }
    public int getOrderId() { return orderId; }
    public Date getDeliveryDate() { return deliveryDate; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public double getDeliveryFee() { return deliveryFee; }
    public String getDriverName() { return driverName; }
    public String getDeliveryStatus() { return deliveryStatus; }
    
    // Setters (Useful if the delivery updates, e.g., from 'Pending' to 'Delivered')
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
}