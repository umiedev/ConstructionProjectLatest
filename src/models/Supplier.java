package models;

public class Supplier {
    private int supplierId;
    private String supplierName;
    private String contactNumber;
    private String email;
    private String address;
    private String username;
    private String passwordHash;
    private String status;

    // Constructor for new registration (no ID)
    public Supplier(String supplierName, String contactNumber, String email, String address,
                    String username, String passwordHash, String status) {
        this.supplierName = supplierName;
        this.contactNumber = contactNumber;
        this.email = email;
        this.address = address;
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = status;
    }

    // Constructor for retrieval from DB (with ID)
    public Supplier(int supplierId, String supplierName, String contactNumber, String email,
                    String address, String username, String passwordHash, String status) {
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.contactNumber = contactNumber;
        this.email = email;
        this.address = address;
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = status;
    }

    // Getters and setters
    public int getSupplierId() { return supplierId; }
    public String getSupplierName() { return supplierName; }
    public String getContactNumber() { return contactNumber; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getStatus() { return status; }

    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public void setEmail(String email) { this.email = email; }
    public void setAddress(String address) { this.address = address; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setStatus(String status) { this.status = status; }
}