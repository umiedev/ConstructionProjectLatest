package models;

public class Material {
    private int materialId;
    private int supplierId; // Links to the supplier who owns it
    private String name;
    private String category;
    private double unitPrice;
    private int stockQuantity;
    private String status;

    // Constructor for adding a new material (no ID yet)
    public Material(int supplierId, String name, String category, double unitPrice, int stockQuantity, String status) {
        this.supplierId = supplierId;
        this.name = name;
        this.category = category;
        this.unitPrice = unitPrice;
        this.stockQuantity = stockQuantity;
        this.status = status;
    }

    // Constructor for retrieving from database
    public Material(int materialId, int supplierId, String name, String category, double unitPrice, int stockQuantity, String status) {
        this.materialId = materialId;
        this.supplierId = supplierId;
        this.name = name;
        this.category = category;
        this.unitPrice = unitPrice;
        this.stockQuantity = stockQuantity;
        this.status = status;
    }

    // Getters
    public int getMaterialId() { return materialId; }
    public int getSupplierId() { return supplierId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getUnitPrice() { return unitPrice; }
    public int getStockQuantity() { return stockQuantity; }
    public String getStatus() { return status; }
}