package models;

public class Customer {
    private int customerId;
    private String username;
    private String passwordHash;
    private String companyName;
    private String fullName; 
    private String phone;
    private String email;
    private String shippingAddress;
    private String status;

    public Customer(String username, String passwordHash, String companyName, String fullName, String phone, String email, String shippingAddress, String status) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.companyName = companyName;
        this.fullName = fullName; // Changed
        this.phone = phone;
        this.email = email;
        this.shippingAddress = shippingAddress;
        this.status = status;
    }

    public Customer(int customerId, String username, String passwordHash, String companyName, String fullName, String phone, String email, String shippingAddress, String status) {
        this.customerId = customerId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.companyName = companyName;
        this.fullName = fullName; // Changed
        this.phone = phone;
        this.email = email;
        this.shippingAddress = shippingAddress;
        this.status = status;
    }

    // Getters
    public int getCustomerId() { return customerId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getCompanyName() { return companyName; }
    public String getFullName() { return fullName; } // Changed
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getShippingAddress() { return shippingAddress; }
    public String getStatus() { return status; }

    // Setters
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public void setFullName(String fullName) { this.fullName = fullName; } // Changed
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public void setStatus(String status) { this.status = status; }
}