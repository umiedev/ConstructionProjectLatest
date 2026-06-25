package models;
import java.sql.Date;

public class Payment {
    private int paymentId;
    private int orderId;
    private Date paymentDate;
    private double amountPaid;
    private String paymentMethod;
    private String paymentStatus;

    // CONSTRUCTOR 1: Used when making a NEW payment (paymentId is unknown)
    public Payment(int orderId, Date paymentDate, double amountPaid, String paymentMethod, String paymentStatus) {
        this.orderId = orderId;
        this.paymentDate = paymentDate;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
    }

    // CONSTRUCTOR 2: Used when retrieving an EXISTING payment from the database (paymentId is known)
    public Payment(int paymentId, int orderId, Date paymentDate, double amountPaid, String paymentMethod, String paymentStatus) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.paymentDate = paymentDate;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
    }

    // Getters
    public int getPaymentId() { return paymentId; }
    public int getOrderId() { return orderId; }
    public Date getPaymentDate() { return paymentDate; }
    public double getAmountPaid() { return amountPaid; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    
    // Setters (if you ever need to update a payment record later)
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
}