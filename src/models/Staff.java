package models;

public class Staff {
    private int staffId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String role; // Admin, Manager, Staff
    private String status; // Active, Inactive

    // Constructor for creating a new staff member (without ID, as DB auto-increments)
    public Staff(String username, String passwordHash, String fullName, String role, String status) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
    }

    // Constructor for retrieving from the database (includes ID)
    public Staff(int staffId, String username, String passwordHash, String fullName, String role, String status) {
        this.staffId = staffId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
    }

    // Getters
    public int getStaffId() { return staffId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getStatus() { return status; }

    // Setters
    public void setStaffId(int staffId) { this.staffId = staffId; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setRole(String role) { this.role = role; }
    public void setStatus(String status) { this.status = status; }
}