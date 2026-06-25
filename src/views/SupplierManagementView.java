package views;

import controllers.SupplierController;
import models.Supplier;
import utils.PasswordUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class SupplierManagementView extends JPanel {

    private final SupplierController supplierController = new SupplierController();

    // Shared table
    private JTable supplierTable;
    private DefaultTableModel tableModel;

    // View / Search Tab
    private JTextField searchField;

    // Add Tab
    private JTextField addCompanyField, addContactField, addEmailField, addAddressField, addUsernameField;
    private JPasswordField addPasswordField, addConfirmField;

    // Update Tab
    private JTextField updateFindField;
    private JTextField updateIdField;
    private JTextField updateCompanyField, updateContactField, updateEmailField, updateAddressField;

    // Change Password Tab
    private JTextField pwFindField;
    private JTextField pwIdField;
    private JTextField pwUsernameDisplay;
    private JPasswordField pwNewPasswordField, pwConfirmField;

    public SupplierManagementView() {
        initSharedTable();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Add Supplier", createAddPanel());
        tabbedPane.addTab("View / Search", createViewPanel());
        tabbedPane.addTab("Update Supplier", createUpdatePanel());
        tabbedPane.addTab("Change Password", createPasswordPanel());

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==========================================
    // SHARED TABLE
    // ==========================================

    private void initSharedTable() {
        String[] columns = {"ID", "Company", "Contact", "Email", "Address", "Username", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        supplierTable = new JTable(tableModel);
        supplierTable.setRowHeight(26);
        supplierTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        supplierTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        supplierTable.setFont(new Font("Arial", Font.PLAIN, 13));

        // Gray out Inactive rows
        supplierTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) table.getModel().getValueAt(row, 6);
                if (!isSelected) {
                    if ("Inactive".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(235, 235, 235));
                        c.setForeground(Color.GRAY);
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        });

        refreshTable();
    }

    // ==========================================
    // TAB 1 – ADD SUPPLIER
    // ==========================================

    private JPanel createAddPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addCompanyField = new JTextField(22);
        addContactField = new JTextField(22);
        addEmailField = new JTextField(22);
        addAddressField = new JTextField(22);
        addUsernameField = new JTextField(22);
        addPasswordField = new JPasswordField(22);
        addConfirmField = new JPasswordField(22);

        JLabel pwHint = new JLabel("<html><i>Min 8 chars, with upper/lowercase, a number &amp; a symbol.</i></html>");
        pwHint.setFont(new Font("Arial", Font.PLAIN, 11));
        pwHint.setForeground(Color.GRAY);

        JButton addBtn = new JButton("Add Supplier");
        styleButton(addBtn, new Color(39, 174, 96));

        int row = 0;
        addRow(panel, gbc, row++, "Company Name:", addCompanyField);
        addRow(panel, gbc, row++, "Contact Number:", addContactField);
        addRow(panel, gbc, row++, "Email:", addEmailField);
        addRow(panel, gbc, row++, "Address:", addAddressField);
        addRow(panel, gbc, row++, "Username:", addUsernameField);
        addRow(panel, gbc, row++, "Password:", addPasswordField);
        addRow(panel, gbc, row++, "Confirm Password:", addConfirmField);

        gbc.gridx = 1; gbc.gridy = row++; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(pwHint, gbc);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(addBtn, gbc);

        addBtn.addActionListener((ActionEvent e) -> {
            String company = addCompanyField.getText().trim();
            String contact = addContactField.getText().trim();
            String email = addEmailField.getText().trim();
            String address = addAddressField.getText().trim();
            String username = addUsernameField.getText().trim();
            String password = new String(addPasswordField.getPassword());
            String confirm = new String(addConfirmField.getPassword());

            if (company.isEmpty() || contact.isEmpty() || email.isEmpty() || address.isEmpty() ||
                username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                showError("All fields are required.");
                return;
            }
            if (!password.equals(confirm)) {
                showError("Passwords do not match.");
                addPasswordField.setText("");
                addConfirmField.setText("");
                return;
            }
            String issue = PasswordUtil.checkStrength(password);
            if (issue != null) {
                showError(issue);
                return;
            }

            String hashed = PasswordUtil.hashPassword(password);
            Supplier s = new Supplier(company, contact, email, address, username, hashed, "Active");
            boolean success = supplierController.addSupplier(s);
            if (success) {
                showInfo("Supplier '" + company + "' added successfully.");
                clearAddFields();
                refreshTable();
            } else {
                String err = supplierController.getLastError();
                showError(err != null ? err : "Failed to add supplier.");
            }
        });

        return panel;
    }

    // ==========================================
    // TAB 2 – VIEW / SEARCH / DEACTIVATE / DELETE
    // ==========================================

    private JPanel createViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchField = new JTextField(22);
        JButton searchBtn = new JButton("Search");
        JButton refreshBtn = new JButton("Refresh");
        JButton deactivateBtn = new JButton("Deactivate Selected");
        JButton deleteBtn = new JButton("Delete Permanently");
        styleButton(deactivateBtn, new Color(192, 57, 43));
        styleButton(deleteBtn, new Color(120, 20, 20));

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(refreshBtn);
        searchPanel.add(deactivateBtn);
        searchPanel.add(deleteBtn);

        JScrollPane scroll = new JScrollPane(supplierTable);
        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        searchBtn.addActionListener(e -> {
            String keyword = searchField.getText().trim();
            if (keyword.isEmpty()) refreshTable();
            else updateTable(supplierController.searchAllSuppliers(keyword));
        });
        searchField.addActionListener(e -> searchBtn.doClick());

        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });

        deactivateBtn.addActionListener((ActionEvent e) -> {
            int row = supplierTable.getSelectedRow();
            if (row == -1) {
                showError("Please select a supplier from the table first.");
                return;
            }
            int id = (int) tableModel.getValueAt(row, 0);
            String name = (String) tableModel.getValueAt(row, 1);
            String status = (String) tableModel.getValueAt(row, 6);
            if ("Inactive".equalsIgnoreCase(status)) {
                showError("'" + name + "' is already inactive.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Deactivate '" + name + "'?\nThis will prevent them from logging in.",
                    "Confirm Deactivation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean success = supplierController.softDeleteSupplier(id);
                if (success) {
                    showInfo("'" + name + "' has been deactivated.");
                    refreshTable();
                } else {
                    showError("Deactivation failed.");
                }
            }
        });

        deleteBtn.addActionListener((ActionEvent e) -> {
            int row = supplierTable.getSelectedRow();
            if (row == -1) {
                showError("Please select a supplier from the table first.");
                return;
            }
            int id = (int) tableModel.getValueAt(row, 0);
            String name = (String) tableModel.getValueAt(row, 1);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Permanently delete '" + name + "'?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean deleted = supplierController.deleteSupplier(id);
                if (deleted) {
                    supplierController.renumberSupplierIds();
                    showInfo("'" + name + "' has been permanently deleted.");
                    searchField.setText("");
                    refreshTable();
                } else {
                    showError("Deletion failed.");
                }
            }
        });

        return panel;
    }

    // ==========================================
    // TAB 3 – UPDATE SUPPLIER
    // ==========================================

    private JPanel createUpdatePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        updateFindField = new JTextField(16);
        JButton findBtn = new JButton("Find");
        JPanel findPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        findPanel.add(updateFindField);
        findPanel.add(findBtn);

        updateIdField = new JTextField(22);
        updateIdField.setEditable(false);
        updateIdField.setBackground(new Color(230, 230, 230));

        updateCompanyField = new JTextField(22);
        updateContactField = new JTextField(22);
        updateEmailField = new JTextField(22);
        updateAddressField = new JTextField(22);

        JButton updateBtn = new JButton("Save Changes");
        styleButton(updateBtn, new Color(39, 174, 96));

        int row = 0;
        addRow(panel, gbc, row++, "Find Supplier (ID or Name):", findPanel);
        addRow(panel, gbc, row++, "Supplier ID (read-only):", updateIdField);
        addRow(panel, gbc, row++, "Company Name:", updateCompanyField);
        addRow(panel, gbc, row++, "Contact Number:", updateContactField);
        addRow(panel, gbc, row++, "Email:", updateEmailField);
        addRow(panel, gbc, row++, "Address:", updateAddressField);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(updateBtn, gbc);

        findBtn.addActionListener(e -> loadSupplierForUpdate(updateFindField.getText()));
        updateFindField.addActionListener(e -> loadSupplierForUpdate(updateFindField.getText()));

        updateBtn.addActionListener((ActionEvent e) -> {
            String idText = updateIdField.getText().trim();
            if (idText.isEmpty()) {
                showError("Use 'Find Supplier' to load a supplier first.");
                return;
            }
            String company = updateCompanyField.getText().trim();
            String contact = updateContactField.getText().trim();
            String email = updateEmailField.getText().trim();
            String address = updateAddressField.getText().trim();
            if (company.isEmpty() || contact.isEmpty() || email.isEmpty() || address.isEmpty()) {
                showError("All fields must be filled.");
                return;
            }
            int id = Integer.parseInt(idText);
            boolean success = supplierController.updateSupplierFull(id, company, contact, email, address);
            if (success) {
                showInfo("Supplier updated successfully.");
                refreshTable();
                clearUpdateFields();
            } else {
                showError("Update failed.");
            }
        });

        return panel;
    }

    private void loadSupplierForUpdate(String input) {
        Supplier s = findSupplierByIdOrName(input);
        if (s != null) {
            updateIdField.setText(String.valueOf(s.getSupplierId()));
            updateCompanyField.setText(s.getSupplierName());
            updateContactField.setText(s.getContactNumber());
            updateEmailField.setText(s.getEmail());
            updateAddressField.setText(s.getAddress());
        }
    }

    private void clearUpdateFields() {
        updateIdField.setText("");
        updateCompanyField.setText("");
        updateContactField.setText("");
        updateEmailField.setText("");
        updateAddressField.setText("");
    }

    // ==========================================
    // TAB 4 – CHANGE PASSWORD
    // ==========================================

    private JPanel createPasswordPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        pwFindField = new JTextField(16);
        JButton pwFindBtn = new JButton("Find");
        JPanel pwFindPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pwFindPanel.add(pwFindField);
        pwFindPanel.add(pwFindBtn);

        pwIdField = new JTextField(22);
        pwIdField.setEditable(false);
        pwIdField.setBackground(new Color(230, 230, 230));

        pwUsernameDisplay = new JTextField(22);
        pwUsernameDisplay.setEditable(false);
        pwUsernameDisplay.setBackground(new Color(230, 230, 230));

        pwNewPasswordField = new JPasswordField(22);
        pwConfirmField = new JPasswordField(22);

        JLabel pwHint = new JLabel("<html><i>Min 8 chars, with upper/lowercase, a number &amp; a symbol.</i></html>");
        pwHint.setFont(new Font("Arial", Font.PLAIN, 11));
        pwHint.setForeground(Color.GRAY);

        JButton savePwBtn = new JButton("Update Password");
        styleButton(savePwBtn, new Color(39, 174, 96));

        int row = 0;
        addRow(panel, gbc, row++, "Find Supplier (ID or Name):", pwFindPanel);
        addRow(panel, gbc, row++, "Supplier ID (read-only):", pwIdField);
        addRow(panel, gbc, row++, "Username (read-only):", pwUsernameDisplay);
        addRow(panel, gbc, row++, "New Password:", pwNewPasswordField);
        addRow(panel, gbc, row++, "Confirm Password:", pwConfirmField);

        gbc.gridx = 1; gbc.gridy = row++; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(pwHint, gbc);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(savePwBtn, gbc);

        pwFindBtn.addActionListener(e -> loadSupplierForPassword(pwFindField.getText()));
        pwFindField.addActionListener(e -> loadSupplierForPassword(pwFindField.getText()));

        savePwBtn.addActionListener((ActionEvent e) -> {
            String idText = pwIdField.getText().trim();
            if (idText.isEmpty()) {
                showError("Use 'Find Supplier' to load a supplier first.");
                return;
            }
            String newPw = new String(pwNewPasswordField.getPassword());
            String confirm = new String(pwConfirmField.getPassword());
            if (newPw.isEmpty() || confirm.isEmpty()) {
                showError("Please enter and confirm the new password.");
                return;
            }
            if (!newPw.equals(confirm)) {
                showError("Passwords do not match.");
                pwNewPasswordField.setText("");
                pwConfirmField.setText("");
                return;
            }
            String issue = PasswordUtil.checkStrength(newPw);
            if (issue != null) {
                showError(issue);
                return;
            }
            int id = Integer.parseInt(idText);
            boolean success = supplierController.updateSupplierPassword(id, newPw);
            if (success) {
                showInfo("Password updated successfully.");
                pwIdField.setText("");
                pwUsernameDisplay.setText("");
                pwNewPasswordField.setText("");
                pwConfirmField.setText("");
            } else {
                showError("Password update failed.");
            }
        });

        return panel;
    }

    private void loadSupplierForPassword(String input) {
        Supplier s = findSupplierByIdOrName(input);
        if (s != null) {
            pwIdField.setText(String.valueOf(s.getSupplierId()));
            pwUsernameDisplay.setText(s.getUsername());
            pwNewPasswordField.setText("");
            pwConfirmField.setText("");
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private Supplier findSupplierByIdOrName(String rawInput) {
        String input = rawInput == null ? "" : rawInput.trim();
        if (input.isEmpty()) {
            showError("Type a Supplier ID or a name to search.");
            return null;
        }

        if (input.matches("\\d+")) {
            Supplier byId = supplierController.getSupplierById(Integer.parseInt(input));
            if (byId != null) return byId;
            showError("No supplier found with ID " + input + ".");
            return null;
        }

        List<Supplier> matches = supplierController.searchAllSuppliers(input);
        if (matches.isEmpty()) {
            showError("No supplier found matching \"" + input + "\".");
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }

        String[] options = new String[matches.size()];
        for (int i = 0; i < matches.size(); i++) {
            Supplier s = matches.get(i);
            options[i] = s.getSupplierId() + " - " + s.getSupplierName() + " (" + s.getUsername() + ")";
        }
        String choice = (String) JOptionPane.showInputDialog(this,
                "Multiple suppliers matched \"" + input + "\". Please select one:",
                "Select Supplier", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == null) return null;
        int idx = java.util.Arrays.asList(options).indexOf(choice);
        return matches.get(idx);
    }

    private void refreshTable() {
        updateTable(supplierController.getAllSuppliers());
    }

    private void updateTable(List<Supplier> list) {
        tableModel.setRowCount(0);
        for (Supplier s : list) {
            tableModel.addRow(new Object[]{
                s.getSupplierId(),
                s.getSupplierName(),
                s.getContactNumber(),
                s.getEmail(),
                s.getAddress(),
                s.getUsername(),
                s.getStatus()
            });
        }
    }

    private void clearAddFields() {
        addCompanyField.setText("");
        addContactField.setText("");
        addEmailField.setText("");
        addAddressField.setText("");
        addUsernameField.setText("");
        addPasswordField.setText("");
        addConfirmField.setText("");
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(field, gbc);
    }

    private void styleButton(JButton button, Color bg) {
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 13));
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}