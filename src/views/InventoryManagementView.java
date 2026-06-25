package views;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import controllers.MaterialController;
import controllers.OrderController;
import models.Staff;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class InventoryManagementView extends JPanel {
    private final Staff loggedInStaff;
    private final MaterialController materialController = new MaterialController();
    private final OrderController orderController = new OrderController(); // NEW: Added OrderController
    
    private JTable inventoryTable;
    private DefaultTableModel tableModel;
    private JTextField searchField, orderQtyField, selectedMaterialField, selectedSupplierField;
    private int selectedMaterialId = -1;
    private int selectedSupplierId = -1; // We will extract this from the DB later, keeping it simple for UI now
    private double selectedUnitPrice = 0.0;

    public InventoryManagementView(Staff staff) {
        this.loggedInStaff = staff;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ==========================================
        // TOP PANEL: Search
        // ==========================================
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        JButton searchBtn = new JButton("Search Inventory");
        JButton refreshBtn = new JButton("Refresh All");
        
        topPanel.add(new JLabel("Search Material/Supplier: "));
        topPanel.add(searchField);
        topPanel.add(searchBtn);
        topPanel.add(refreshBtn);

        // ==========================================
        // CENTER PANEL: Dual-Stock Inventory Table
        // ==========================================
        // NEW COLUMNS: Clearly separating Our Stock from Supplier Stock
        String[] columns = {"Mat ID", "Supplier Name", "Material Name", "Category", "Price (RM)", "Our Stock (Company)", "Supplier Stock (To Buy)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        inventoryTable = new JTable(tableModel);
        inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inventoryTable.setRowHeight(26);
        inventoryTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        inventoryTable.setFont(new Font("Arial", Font.PLAIN, 13));
        
        // Highlight "Our Stock" column to make it stand out
        inventoryTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    int stock = Integer.parseInt(value.toString());
                    if (stock <= 10) c.setBackground(new Color(255, 200, 200)); // Light Red for low stock
                    else c.setBackground(new Color(220, 255, 220)); // Light Green for good stock
                }
                return c;
            }
        });
        
        refreshTable(""); 

        // ==========================================
        // BOTTOM PANEL: Restock Purchase Order Form
        // ==========================================
        JPanel orderPanel = new JPanel(new GridBagLayout());
        orderPanel.setBorder(BorderFactory.createTitledBorder("Restock Company Inventory (Create Purchase Order to Supplier)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        selectedSupplierField = new JTextField(15); selectedSupplierField.setEditable(false);
        selectedMaterialField = new JTextField(15); selectedMaterialField.setEditable(false);
        orderQtyField = new JTextField(10);
        JButton placeOrderBtn = new JButton("Submit Purchase Order");
        placeOrderBtn.setBackground(new Color(41, 128, 185));
        placeOrderBtn.setForeground(Color.WHITE);

        gbc.gridx = 0; gbc.gridy = 0; orderPanel.add(new JLabel("Supplier:"), gbc);
        gbc.gridx = 1; orderPanel.add(selectedSupplierField, gbc);
        
        gbc.gridx = 2; orderPanel.add(new JLabel("Material:"), gbc);
        gbc.gridx = 3; orderPanel.add(selectedMaterialField, gbc);
        
        gbc.gridx = 4; orderPanel.add(new JLabel("Order Quantity:"), gbc);
        gbc.gridx = 5; orderPanel.add(orderQtyField, gbc);
        
        gbc.gridx = 6; orderPanel.add(placeOrderBtn, gbc);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        add(orderPanel, BorderLayout.SOUTH);

        // ==========================================
        // ACTIONS
        // ==========================================
        
        inventoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && inventoryTable.getSelectedRow() != -1) {
                int row = inventoryTable.getSelectedRow();
                selectedMaterialId = (int) tableModel.getValueAt(row, 0);
                selectedSupplierField.setText(tableModel.getValueAt(row, 1).toString());
                selectedMaterialField.setText(tableModel.getValueAt(row, 2).toString());
                selectedUnitPrice = (double) tableModel.getValueAt(row, 4);
            }
        });

        searchBtn.addActionListener(e -> refreshTable(searchField.getText().trim()));
        refreshBtn.addActionListener(e -> { searchField.setText(""); refreshTable(""); });

        placeOrderBtn.addActionListener(e -> {
            int row = inventoryTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a material from the inventory list to restock.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String qtyStr = orderQtyField.getText().trim();
            if (qtyStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter an order quantity.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int orderQty = Integer.parseInt(qtyStr);
                int supplierStock = Integer.parseInt(tableModel.getValueAt(row, 6).toString()); 
                
                if (orderQty <= 0) {
                    JOptionPane.showMessageDialog(this, "Quantity must be greater than 0.", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                if (orderQty > supplierStock) {
                    JOptionPane.showMessageDialog(this, "Supplier does not have enough stock to fulfill this order. Max available: " + supplierStock, "Insufficient Supplier Stock", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // --- NEW: ACTUAL DATABASE INTEGRATION ---
                // 1. Get the hidden supplier ID
                int supplierId = materialController.getSupplierIdFromMaterial(selectedMaterialId);
                
                if (supplierId != -1) {
                    // 2. Execute the Order Transaction!
                    boolean success = orderController.createPurchaseOrder(supplierId, selectedMaterialId, orderQty, selectedUnitPrice);
                    
                    if (success) {
                        String matName = selectedMaterialField.getText();
                        String supName = selectedSupplierField.getText();
                        double totalCost = orderQty * selectedUnitPrice;
                        
                        JOptionPane.showMessageDialog(this, 
                            "Purchase Order Generated Successfully!\n\n" +
                            "Ordered: " + orderQty + " units of " + matName + "\n" +
                            "From: " + supName + "\n" +
                            "Total Cost: RM " + String.format("%.2f", totalCost) + "\n\n" +
                            "Status: Pending Delivery. The supplier has been notified.", 
                            "Order Placed", JOptionPane.INFORMATION_MESSAGE);
                        
                        // Clear fields and refresh the table to show the updated stock!
                        orderQtyField.setText("");
                        inventoryTable.clearSelection();
                        selectedSupplierField.setText("");
                        selectedMaterialField.setText("");
                        refreshTable(searchField.getText().trim());
                        
                    } else {
                        JOptionPane.showMessageDialog(this, "Database Error: Could not place the order.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Error: Could not identify the supplier for this material.", "Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Quantity must be a valid number.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void refreshTable(String keyword) {
        tableModel.setRowCount(0);
        List<Object[]> catalog = materialController.getCatalogWithSupplierInfo(keyword);
        for (Object[] row : catalog) {
            tableModel.addRow(row);
        }
    }
}