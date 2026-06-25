package views;

import controllers.MaterialController;
import models.Staff;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ManagePricingView extends JPanel {
    private final MaterialController materialController = new MaterialController();
    private final Staff loggedInStaff;

    private JTable inventoryTable;
    private DefaultTableModel tableModel;
    private JTextField searchField, selectedMaterialField, newPriceField;
    private int selectedMaterialId = -1;

    public ManagePricingView(Staff loggedInStaff) {
        this.loggedInStaff = loggedInStaff;

        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Restock Inventory", new InventoryManagementView(loggedInStaff));
        tabbedPane.addTab("Catalog & Pricing", createCatalogPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // Kept so any old code calling the no-arg constructor still compiles.
    public ManagePricingView() {
        this(null);
    }

    // ==========================================
    // TAB: SEARCH / VIEW CATALOG, SET PRICE, REMOVE FROM SALE
    // ==========================================
    private JPanel createCatalogPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ==========================================
        // TOP PANEL: Search + Remove from Sale
        // ==========================================
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        JButton searchBtn = new JButton("Search Our Stock");
        JButton refreshBtn = new JButton("Refresh All");
        JButton removeFromSaleBtn = new JButton("Remove from Sale");
        removeFromSaleBtn.setBackground(new Color(192, 57, 43));
        removeFromSaleBtn.setForeground(Color.WHITE);
        removeFromSaleBtn.setFocusPainted(false);

        topPanel.add(new JLabel("Search Material: "));
        topPanel.add(searchField);
        topPanel.add(searchBtn);
        topPanel.add(refreshBtn);
        topPanel.add(removeFromSaleBtn);

        // ==========================================
        // CENTER PANEL: Company Inventory Table
        // ==========================================
        String[] columns = {"Mat ID", "Material Name", "Category", "Supplier Cost (RM)", "Customer Price (RM)", "Available to Sell"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        inventoryTable = new JTable(tableModel);
        inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inventoryTable.setRowHeight(26);
        inventoryTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        inventoryTable.setFont(new Font("Arial", Font.PLAIN, 13));

        // Highlight items that haven't been priced yet (Price = 0.00)
        inventoryTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    double sellingPrice = Double.parseDouble(table.getModel().getValueAt(row, 4).toString());
                    if (sellingPrice == 0.0) c.setBackground(new Color(255, 228, 225)); // Misty Rose (Needs Pricing)
                    else c.setBackground(Color.WHITE);
                }
                return c;
            }
        });

        refreshTable("");

        // ==========================================
        // BOTTOM PANEL: Update Pricing Form
        // ==========================================
        JPanel pricingPanel = new JPanel(new GridBagLayout());
        pricingPanel.setBorder(BorderFactory.createTitledBorder("Set Customer Selling Price"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        selectedMaterialField = new JTextField(20);
        selectedMaterialField.setEditable(false);
        newPriceField = new JTextField(10);
        JButton updatePriceBtn = new JButton("Update Price");
        updatePriceBtn.setBackground(new Color(39, 174, 96));
        updatePriceBtn.setForeground(Color.WHITE);

        gbc.gridx = 0; gbc.gridy = 0; pricingPanel.add(new JLabel("Selected Material:"), gbc);
        gbc.gridx = 1; pricingPanel.add(selectedMaterialField, gbc);

        gbc.gridx = 2; pricingPanel.add(new JLabel("New Selling Price (RM):"), gbc);
        gbc.gridx = 3; pricingPanel.add(newPriceField, gbc);

        gbc.gridx = 4; pricingPanel.add(updatePriceBtn, gbc);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        panel.add(pricingPanel, BorderLayout.SOUTH);

        // ==========================================
        // ACTIONS
        // ==========================================
        inventoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && inventoryTable.getSelectedRow() != -1) {
                int row = inventoryTable.getSelectedRow();
                selectedMaterialId = (int) tableModel.getValueAt(row, 0);
                selectedMaterialField.setText(tableModel.getValueAt(row, 1).toString());
                newPriceField.setText(tableModel.getValueAt(row, 4).toString());
            }
        });

        searchBtn.addActionListener(e -> refreshTable(searchField.getText().trim()));
        refreshBtn.addActionListener(e -> { searchField.setText(""); refreshTable(""); });

        updatePriceBtn.addActionListener(e -> {
            if (selectedMaterialId == -1) {
                JOptionPane.showMessageDialog(this, "Please select a material from the table first.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                double newPrice = Double.parseDouble(newPriceField.getText().trim());
                if (newPrice < 0) {
                    JOptionPane.showMessageDialog(this, "Price cannot be negative.", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int row = inventoryTable.getSelectedRow();
                double costPrice = (double) tableModel.getValueAt(row, 3);
                if (newPrice < costPrice && newPrice > 0) {
                    int confirm = JOptionPane.showConfirmDialog(this, "Warning: This selling price is LOWER than the supplier cost.\nYou will lose money. Continue?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (confirm != JOptionPane.YES_OPTION) return;
                }

                if (materialController.updateSellingPrice(selectedMaterialId, newPrice)) {
                    JOptionPane.showMessageDialog(this, "Price updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    newPriceField.setText("");
                    selectedMaterialField.setText("");
                    selectedMaterialId = -1;
                    refreshTable(searchField.getText().trim());
                } else {
                    JOptionPane.showMessageDialog(this, "Database Error: Could not update price.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number for the price.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        removeFromSaleBtn.addActionListener(e -> {
            int row = inventoryTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a material from the table first.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int    matId   = (int)    tableModel.getValueAt(row, 0);
            String matName = (String) tableModel.getValueAt(row, 1);
            int    stock   = (int)    tableModel.getValueAt(row, 5);

            if (stock == 0) {
                JOptionPane.showMessageDialog(this, "'" + matName + "' is already removed from sale.", "Already Removed", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                "Remove '" + matName + "' from the customer storefront?\n" +
                "All " + stock + " units currently in company stock will be set to 0.\n" +
                "This does NOT affect the supplier's own listing or stock.",
                "Confirm Remove from Sale", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                if (materialController.removeFromSale(matId)) {
                    JOptionPane.showMessageDialog(this, "'" + matName + "' has been removed from sale.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    selectedMaterialField.setText("");
                    newPriceField.setText("");
                    selectedMaterialId = -1;
                    refreshTable(searchField.getText().trim());
                } else {
                    JOptionPane.showMessageDialog(this, "Database Error: Could not remove material from sale.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        return panel;
    }

    private void refreshTable(String keyword) {
        tableModel.setRowCount(0);
        List<Object[]> inventory = materialController.getCompanyInventory(keyword);
        for (Object[] row : inventory) {
            tableModel.addRow(row);
        }
    }
}