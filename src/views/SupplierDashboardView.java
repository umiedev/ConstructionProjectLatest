package views;
import controllers.OrderController;
import controllers.MaterialController;
import controllers.SupplierController;
import models.Material;
import models.Supplier;
import utils.PasswordUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SupplierDashboardView extends JFrame {

    private final Supplier loggedInSupplier;
    private final MaterialController materialController = new MaterialController();
    private final SupplierController supplierController = new SupplierController();
    private final OrderController orderController = new OrderController();
    // UI Elements for Materials
    private JTable materialTable;
    private DefaultTableModel materialTableModel;
    private JTextField nameField, priceField, stockField;
    private JComboBox<String> categoryComboBox; 
    private JComboBox<String> filterComboBox; 

    // Shared category list
    private final String[] CATEGORIES = {
        "Cement & Binders", 
        "Aggregates & Sand", 
        "Steel & Reinforcement", 
        "Bricks & Blocks", 
        "Wood & Timber", 
        "Plumbing & Piping", 
        "Roofing & Insulation", 
        "Other Tools & Hardware"
    };

    public SupplierDashboardView(Supplier supplier) {
        this.loggedInSupplier = supplier;

        setTitle("Supplier Dashboard - " + supplier.getSupplierName());
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Manage My Materials", createManageMaterialsPanel());
        tabbedPane.addTab("View Purchase Orders", createPurchaseOrdersPanel());
        tabbedPane.addTab("Update Profile", createProfilePanel());

        // Top bar for logout
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel welcomeLabel = new JLabel("Welcome, " + supplier.getSupplierName() + " | ");
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            new LoginView().setVisible(true);
            dispose();
        });
        topPanel.add(welcomeLabel);
        topPanel.add(logoutBtn);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

 // =========================================
    // 1. MANAGE MATERIALS TAB
    // =========================================
    private JPanel createManageMaterialsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Filter Panel ---
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Filter by Category: "));
        
        String[] filterOptions = new String[CATEGORIES.length + 1];
        filterOptions[0] = "All Categories";
        System.arraycopy(CATEGORIES, 0, filterOptions, 1, CATEGORIES.length);
        
        filterComboBox = new JComboBox<>(filterOptions);
        filterComboBox.setBackground(Color.WHITE);
        filterPanel.add(filterComboBox);

        filterComboBox.addActionListener(e -> refreshMaterialTable(filterComboBox.getSelectedItem().toString()));

        // --- Table Setup ---
        String[] columns = {"ID", "Name", "Category", "Unit Price (RM)", "Stock", "Status"};
        materialTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        materialTable = new JTable(materialTableModel);
        materialTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        refreshMaterialTable("All Categories"); 

        // --- Form Setup ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Add / Update Material"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); gbc.fill = GridBagConstraints.HORIZONTAL;

        nameField = new JTextField(15); 
        priceField = new JTextField(10); 
        stockField = new JTextField(10);
        categoryComboBox = new JComboBox<>(CATEGORIES); 
        categoryComboBox.setBackground(Color.WHITE);

        JButton addBtn = new JButton("Add New Material");
        JButton updateBtn = new JButton("Update Selected Material");
        JButton toggleStatusBtn = new JButton("Discontinue Selected");
        
        // NEW: Permanent Delete Button
        JButton deleteBtn = new JButton("Delete Permanently");
        deleteBtn.setForeground(Color.RED);
        deleteBtn.setEnabled(false); // Disabled by default until a discontinued item is selected

        addFormRow(formPanel, gbc, 0, "Material Name:", nameField, "Category:", categoryComboBox);
        addFormRow(formPanel, gbc, 1, "Unit Price:", priceField, "Stock Quantity:", stockField);

        JPanel btnPanel = new JPanel();
        btnPanel.add(addBtn); 
        btnPanel.add(updateBtn); 
        btnPanel.add(toggleStatusBtn); 
        btnPanel.add(deleteBtn); // Add the new delete button to the UI
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4; gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(btnPanel, gbc);

        // --- ACTIONS ---
        
        // 1. ADD ACTION (With Duplicate Check)
        addBtn.addActionListener(e -> {
            if (nameField.getText().isEmpty() || priceField.getText().isEmpty() || stockField.getText().isEmpty()) {
                showError("Name, Price, and Stock are required."); return;
            }
            
            // NEW: Check for duplicates before adding
            if (materialController.checkMaterialExists(loggedInSupplier.getSupplierId(), nameField.getText())) {
                showError("A material with this name already exists in your inventory.");
                return;
            }

            try {
                double price = Double.parseDouble(priceField.getText());
                int stock = Integer.parseInt(stockField.getText());
                String selectedCategory = categoryComboBox.getSelectedItem().toString();
                
                Material m = new Material(loggedInSupplier.getSupplierId(), nameField.getText(), selectedCategory, price, stock, "Available");
                if (materialController.addMaterial(m)) { 
                    showInfo("Material Added!"); 
                    refreshMaterialTable(filterComboBox.getSelectedItem().toString()); 
                    clearMaterialFields(); 
                } else showError("Failed to add material.");
            } catch (NumberFormatException ex) { showError("Price and Stock must be valid numbers."); }
        });

        // 2. UPDATE ACTION
        updateBtn.addActionListener(e -> {
            int row = materialTable.getSelectedRow();
            if (row == -1) { showError("Select a material to update."); return; }
            try {
                double price = Double.parseDouble(priceField.getText());
                int stock = Integer.parseInt(stockField.getText());
                String selectedCategory = categoryComboBox.getSelectedItem().toString();
                
                if (materialController.updateMaterial((int) materialTableModel.getValueAt(row, 0), loggedInSupplier.getSupplierId(), selectedCategory, price, stock)) {
                    showInfo("Material Updated."); 
                    refreshMaterialTable(filterComboBox.getSelectedItem().toString());
                }
            } catch (NumberFormatException ex) { showError("Price and Stock must be valid numbers."); }
        });

        // 3. TOGGLE STATUS ACTION
        toggleStatusBtn.addActionListener(e -> {
            int row = materialTable.getSelectedRow();
            if (row == -1) { showError("Select a material first."); return; }
            
            int matId = (int) materialTableModel.getValueAt(row, 0);
            String currentStatus = materialTableModel.getValueAt(row, 5).toString();
            
            if (currentStatus.equals("Available")) {
                if (JOptionPane.showConfirmDialog(this, "Discontinue this material?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    if (materialController.discontinueMaterial(matId, loggedInSupplier.getSupplierId())) {
                        showInfo("Material Discontinued."); 
                        refreshMaterialTable(filterComboBox.getSelectedItem().toString());
                        deleteBtn.setEnabled(true); // Enable delete after discontinuing
                    }
                }
            } else {
                if (JOptionPane.showConfirmDialog(this, "Make this material available again?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    if (materialController.restoreMaterial(matId, loggedInSupplier.getSupplierId())) {
                        showInfo("Material is now Available."); 
                        refreshMaterialTable(filterComboBox.getSelectedItem().toString());
                        deleteBtn.setEnabled(false); // Disable delete after restoring
                    }
                }
            }
        });

        // 4. NEW: PERMANENT DELETE ACTION
        deleteBtn.addActionListener(e -> {
            int row = materialTable.getSelectedRow();
            if (row == -1) return;
            
            int matId = (int) materialTableModel.getValueAt(row, 0);
            String name = materialTableModel.getValueAt(row, 1).toString();
            
            // Extra warning because delete is permanent
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to PERMANENTLY delete '" + name + "'?\nThis action cannot be undone.", "Delete Material", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                if (materialController.deleteMaterial(matId, loggedInSupplier.getSupplierId())) {
                    showInfo("Material deleted permanently.");
                    refreshMaterialTable(filterComboBox.getSelectedItem().toString());
                    clearMaterialFields();
                    deleteBtn.setEnabled(false); // Reset button state
                } else {
                    showError("Database Error: Could not delete material.");
                }
            }
        });

        // TABLE SELECTION LISTENER
        materialTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && materialTable.getSelectedRow() != -1) {
                int r = materialTable.getSelectedRow();
                nameField.setText(materialTableModel.getValueAt(r, 1).toString());
                categoryComboBox.setSelectedItem(materialTableModel.getValueAt(r, 2).toString());
                priceField.setText(materialTableModel.getValueAt(r, 3).toString());
                stockField.setText(materialTableModel.getValueAt(r, 4).toString());
                
                String status = materialTableModel.getValueAt(r, 5).toString();
                if (status.equals("Discontinued")) {
                    toggleStatusBtn.setText("Make Available Again");
                    toggleStatusBtn.setForeground(new Color(39, 174, 96));
                    deleteBtn.setEnabled(true); // NEW: Enable delete if discontinued
                } else {
                    toggleStatusBtn.setText("Discontinue Selected");
                    toggleStatusBtn.setForeground(Color.RED); 
                    deleteBtn.setEnabled(false); // NEW: Disable delete if available
                }
            }
        });

        panel.add(filterPanel, BorderLayout.NORTH); 
        panel.add(new JScrollPane(materialTable), BorderLayout.CENTER);
        panel.add(formPanel, BorderLayout.SOUTH);
        return panel;
    }
    
 // =========================================
    // 4. VIEW PURCHASE ORDERS TAB (NEW)
    // =========================================
    private JPanel createPurchaseOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- Table Setup ---
        String[] columns = {"PO ID", "Date", "Mat ID", "Material Name", "Qty Requested", "Total Amount (RM)", "Status"};
        DefaultTableModel poTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable poTable = new JTable(poTableModel);
        poTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        poTable.setRowHeight(25);
        
        // Highlight Pending orders in Yellow, Delivered in Green
        poTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    String status = table.getModel().getValueAt(row, 6).toString();
                    if (status.equals("Pending")) c.setBackground(new Color(255, 250, 205)); // Light Yellow
                    else if (status.equals("Delivered")) c.setBackground(new Color(220, 255, 220)); // Light Green
                    else c.setBackground(Color.WHITE);
                }
                return c;
            }
        });

        // Load data into table
        Runnable refreshPOTable = () -> {
            poTableModel.setRowCount(0);
            List<Object[]> orders = orderController.getPurchaseOrdersForSupplier(loggedInSupplier.getSupplierId());
            for (Object[] row : orders) {
                poTableModel.addRow(row);
            }
        };
        refreshPOTable.run();

        // --- Action Panel (Bottom) ---
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshBtn = new JButton("Refresh Orders");
        JButton deliverBtn = new JButton("Mark as Delivered");
        
        deliverBtn.setBackground(new Color(39, 174, 96));
        deliverBtn.setForeground(Color.WHITE);
        deliverBtn.setFont(new Font("Arial", Font.BOLD, 14));

        actionPanel.add(refreshBtn);
        actionPanel.add(deliverBtn);

        // --- Button Actions ---
        refreshBtn.addActionListener(e -> refreshPOTable.run());

        deliverBtn.addActionListener(e -> {
            int row = poTable.getSelectedRow();
            if (row == -1) {
                showError("Please select a Purchase Order from the list.");
                return;
            }

            String currentStatus = poTableModel.getValueAt(row, 6).toString();
            if (currentStatus.equals("Delivered")) {
                showInfo("This order has already been delivered.");
                return;
            }

            // Extract data needed for the transaction
            int poId = (int) poTableModel.getValueAt(row, 0);
            int materialId = (int) poTableModel.getValueAt(row, 2);
            int quantity = (int) poTableModel.getValueAt(row, 4);
            String materialName = poTableModel.getValueAt(row, 3).toString();

            int confirm = JOptionPane.showConfirmDialog(this, 
                "Confirm delivery of " + quantity + " units of '" + materialName + "'?\n\nThis will transfer the stock to the Company's warehouse.", 
                "Confirm Delivery", JOptionPane.YES_NO_OPTION);
                
            if (confirm == JOptionPane.YES_OPTION) {
                // Execute the Controller logic we built earlier
                if (orderController.completePurchaseOrderDelivery(poId, materialId, quantity)) {
                    showInfo("Order Marked as Delivered! Stock has been transferred to the buyer.");
                    refreshPOTable.run();
                } else {
                    showError("Database Error: Could not update delivery status.");
                }
            }
        });

        panel.add(new JScrollPane(poTable), BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    // =========================================
    // 2. SUPPLIER PROFILE EDITOR TAB
    // =========================================
    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtName = new JTextField(loggedInSupplier.getSupplierName(), 20);
        JTextField txtPhone = new JTextField(loggedInSupplier.getContactNumber(), 20);
        JTextField txtEmail = new JTextField(loggedInSupplier.getEmail(), 20);
        JTextArea txtAddress = new JTextArea(loggedInSupplier.getAddress(), 4, 20);
        txtAddress.setLineWrap(true);

        JButton btnSave = new JButton("Save Profile Changes");
        btnSave.setBackground(new Color(39, 174, 96)); btnSave.setForeground(Color.WHITE);
        
        JButton btnChangePassword = new JButton("Change Password");
        btnChangePassword.setBackground(new Color(41, 128, 185)); btnChangePassword.setForeground(Color.WHITE);

        int row = 0;
        addFormRowSingle(panel, gbc, row++, "Supplier Name:", txtName);
        addFormRowSingle(panel, gbc, row++, "Phone Number:", txtPhone);
        addFormRowSingle(panel, gbc, row++, "Email:", txtEmail);
        
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.NORTHEAST; panel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; panel.add(new JScrollPane(txtAddress), gbc); row++;

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        actionPanel.add(btnSave); actionPanel.add(btnChangePassword);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(actionPanel, gbc);

        btnSave.addActionListener(e -> {
            if (txtName.getText().isEmpty() || txtPhone.getText().isEmpty()) { showError("Name and Phone are required."); return; }
            if (supplierController.updateSupplierProfile(loggedInSupplier.getSupplierId(), txtName.getText(), txtPhone.getText(), txtEmail.getText(), txtAddress.getText())) {
                loggedInSupplier.setSupplierName(txtName.getText());
                loggedInSupplier.setContactNumber(txtPhone.getText());
                loggedInSupplier.setEmail(txtEmail.getText());
                loggedInSupplier.setAddress(txtAddress.getText());
                showInfo("Profile updated successfully.");
            } else showError("Failed to update profile.");
        });

        btnChangePassword.addActionListener(e -> showChangePasswordDialog());

        return panel;
    }

    // =========================================
    // 3. SECURE PASSWORD CHANGE DIALOG
    // =========================================
    private void showChangePasswordDialog() {
        JPasswordField currentPasswordField = new JPasswordField(15);
        JPasswordField newPasswordField = new JPasswordField(15);
        JPasswordField confirmPasswordField = new JPasswordField(15);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.add(new JLabel("Current Password:")); panel.add(currentPasswordField);
        panel.add(new JLabel("New Password:")); panel.add(newPasswordField);
        panel.add(new JLabel("Confirm New Password:")); panel.add(confirmPasswordField);

        if (JOptionPane.showConfirmDialog(this, panel, "Change Password", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            String currentPw = new String(currentPasswordField.getPassword());
            String newPw = new String(newPasswordField.getPassword());
            String confirmPw = new String(confirmPasswordField.getPassword());

            if (currentPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) { showError("All fields required."); return; }
            if (!newPw.equals(confirmPw)) { showError("New passwords do not match."); return; }
            if (PasswordUtil.checkStrength(newPw) != null) { showError(PasswordUtil.checkStrength(newPw)); return; }

            Supplier authCheck = supplierController.authenticateSupplier(loggedInSupplier.getUsername(), currentPw);
            if (authCheck == null) { showError("Current password is incorrect."); return; }

            if (supplierController.updateSupplierPassword(loggedInSupplier.getSupplierId(), newPw)) {
                showInfo("Password updated successfully.");
            } else {
                showError("Database error updating password.");
            }
        }
    }

    // --- Helpers ---
    private void refreshMaterialTable(String categoryFilter) {
        materialTableModel.setRowCount(0);
        List<Material> materials = materialController.getSupplierMaterials(loggedInSupplier.getSupplierId());
        
        for (Material m : materials) {
            if (categoryFilter.equals("All Categories") || m.getCategory().equals(categoryFilter)) {
                materialTableModel.addRow(new Object[]{
                    m.getMaterialId(), m.getName(), m.getCategory(), m.getUnitPrice(), m.getStockQuantity(), m.getStatus()
                });
            }
        }
    }

    private void addFormRow(JPanel p, GridBagConstraints gbc, int y, String l1, JComponent f1, String l2, JComponent f2) {
        gbc.gridy = y; gbc.gridwidth = 1;
        gbc.gridx = 0; p.add(new JLabel(l1), gbc); gbc.gridx = 1; p.add(f1, gbc);
        gbc.gridx = 2; p.add(new JLabel(l2), gbc); gbc.gridx = 3; p.add(f2, gbc);
    }
    
    private void addFormRowSingle(JPanel p, GridBagConstraints gbc, int y, String l1, JComponent f1) {
        gbc.gridy = y; gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST; p.add(new JLabel(l1), gbc); 
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; p.add(f1, gbc);
    }

    private void clearMaterialFields() { 
        nameField.setText(""); 
        categoryComboBox.setSelectedIndex(0); 
        priceField.setText(""); 
        stockField.setText(""); 
    }
    
    private void showInfo(String msg) { JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE); }
    private void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
}