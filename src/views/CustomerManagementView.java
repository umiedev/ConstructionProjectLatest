package views;

import controllers.CustomerController;
import models.Customer;
import utils.PasswordUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class CustomerManagementView extends JPanel {

    private final CustomerController customerController = new CustomerController();

    private JTable customerTable;
    private DefaultTableModel tableModel;

    // View / Search Tab
    private JTextField searchField;

    // Add Tab
    private JTextField     addUsernameField;
    private JPasswordField addPasswordField;
    private JPasswordField addConfirmPasswordField;
    private JTextField     addCompanyField;
    private JTextField     addFullNameField;
    private JTextField     addPhoneField;
    private JTextField     addEmailField;
    private JTextField     addAddressField;   // Changed to JTextField

    // Update Tab
    private JTextField updateFindField;
    private JTextField updateIdField;
    private JTextField updateCompanyField;
    private JTextField updateFullNameField;
    private JTextField updatePhoneField;
    private JTextField updateEmailField;
    private JTextField updateAddressField;   // Changed to JTextField

    // Change Password Tab
    private JTextField     pwFindField;
    private JTextField     pwIdField;
    private JTextField     pwUsernameDisplay;
    private JPasswordField pwNewPasswordField;
    private JPasswordField pwConfirmPasswordField;

    public CustomerManagementView() {
        initSharedTable();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Add Customer",   createAddPanel());
        tabbedPane.addTab("View / Search",  createViewPanel());
        tabbedPane.addTab("Update Profile", createUpdatePanel());
        tabbedPane.addTab("Change Password", createPasswordPanel());

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==========================================
    // SHARED TABLE
    // ==========================================

    private void initSharedTable() {
        String[] columns = {"ID", "Username", "Company", "Full Name", "Phone", "Email", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        customerTable = new JTable(tableModel);
        customerTable.setRowHeight(26);
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        customerTable.setFont(new Font("Arial", Font.PLAIN, 13));

        customerTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
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
    // TAB 1 – ADD CUSTOMER (Matches Staff style)
    // ==========================================

    private JPanel createAddPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;   // important: don't let fields stretch extra

        addUsernameField = new JTextField(22);
        addPasswordField = new JPasswordField(22);
        addConfirmPasswordField = new JPasswordField(22);
        addCompanyField = new JTextField(22);
        addFullNameField = new JTextField(22);
        addPhoneField = new JTextField(22);
        addEmailField = new JTextField(22);
        addAddressField = new JTextField(22);   // single line

        JLabel passwordHintLabel = new JLabel("<html><i>Min 8 chars, with upper/lowercase, a number &amp; a symbol.</i></html>");
        passwordHintLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        passwordHintLabel.setForeground(Color.GRAY);

        JButton addButton = new JButton("Add Customer");
        styleButton(addButton, new Color(39, 174, 96));

        int row = 0;
        addFormRow(panel, gbc, row++, "Username:", addUsernameField);
        addFormRow(panel, gbc, row++, "Password:", addPasswordField);
        addFormRow(panel, gbc, row++, "Confirm Password:", addConfirmPasswordField);

        // Hint – left-aligned
        gbc.gridx = 1;
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(passwordHintLabel, gbc);

        addFormRow(panel, gbc, row++, "Company:", addCompanyField);
        addFormRow(panel, gbc, row++, "Full Name:", addFullNameField);
        addFormRow(panel, gbc, row++, "Phone:", addPhoneField);
        addFormRow(panel, gbc, row++, "Email:", addEmailField);
        addFormRow(panel, gbc, row++, "Address:", addAddressField);   // now a JTextField

        // Button – centered
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(addButton, gbc);

        addButton.addActionListener((ActionEvent e) -> {
            String username = addUsernameField.getText().trim();
            String password = new String(addPasswordField.getPassword()).trim();
            String confirm  = new String(addConfirmPasswordField.getPassword()).trim();

            if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                showError("Username, password, and confirmation are required.");
                return;
            }
            if (!password.equals(confirm)) {
                showError("Passwords do not match. Please re-enter.");
                addPasswordField.setText("");
                addConfirmPasswordField.setText("");
                return;
            }
            String strengthIssue = PasswordUtil.checkStrength(password);
            if (strengthIssue != null) {
                showError(strengthIssue);
                return;
            }

            Customer c = new Customer(username, password,
                    addCompanyField.getText().trim(),
                    addFullNameField.getText().trim(),
                    addPhoneField.getText().trim(),
                    addEmailField.getText().trim(),
                    addAddressField.getText().trim(),
                    "Active");

            if (customerController.addCustomer(c)) {
                showInfo("Customer added successfully.");
                refreshTable();
                clearAddFields();
            } else {
                String err = customerController.getLastError();
                showError(err != null ? err : "Failed to add customer.");
            }
        });

        return panel;
    }

    private void clearAddFields() {
        addUsernameField.setText("");
        addPasswordField.setText("");
        addConfirmPasswordField.setText("");
        addCompanyField.setText("");
        addFullNameField.setText("");
        addPhoneField.setText("");
        addEmailField.setText("");
        addAddressField.setText("");
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

        JScrollPane scroll = new JScrollPane(customerTable);
        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        searchBtn.addActionListener(e -> {
            String keyword = searchField.getText().trim();
            if (keyword.isEmpty()) refreshTable();
            else updateTable(customerController.searchAllCustomers(keyword));
        });
        searchField.addActionListener(e -> searchBtn.doClick());

        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });

        deactivateBtn.addActionListener((ActionEvent e) -> {
            int row = customerTable.getSelectedRow();
            if (row == -1) {
                showError("Please select a customer from the table first.");
                return;
            }
            int id = (int) tableModel.getValueAt(row, 0);
            String name = (String) tableModel.getValueAt(row, 2);
            String status = (String) tableModel.getValueAt(row, 6);
            if ("Inactive".equalsIgnoreCase(status)) {
                showError("'" + name + "' is already inactive.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Deactivate '" + name + "'?\nThis will prevent them from logging in.",
                    "Confirm Deactivation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean success = customerController.deactivateCustomer(id);
                if (success) {
                    showInfo("'" + name + "' has been deactivated.");
                    refreshTable();
                } else {
                    showError("Deactivation failed.");
                }
            }
        });

        deleteBtn.addActionListener((ActionEvent e) -> {
            int row = customerTable.getSelectedRow();
            if (row == -1) {
                showError("Please select a customer from the table first.");
                return;
            }
            int id = (int) tableModel.getValueAt(row, 0);
            String name = (String) tableModel.getValueAt(row, 2);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Permanently delete '" + name + "'?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean deleted = customerController.deleteCustomer(id);
                if (deleted) {
                    customerController.renumberCustomerIds();
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
    // TAB 3 – UPDATE PROFILE (Matches Staff style)
    // ==========================================

    private JPanel createUpdatePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;

        updateFindField = new JTextField(16);
        JButton findBtn = new JButton("Find");
        JPanel findPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        findPanel.add(updateFindField);
        findPanel.add(findBtn);

        updateIdField = new JTextField(22);
        updateIdField.setEditable(false);
        updateIdField.setBackground(new Color(230, 230, 230));

        updateCompanyField = new JTextField(22);
        updateFullNameField = new JTextField(22);
        updatePhoneField = new JTextField(22);
        updateEmailField = new JTextField(22);
        updateAddressField = new JTextField(22);   // single line

        JButton saveBtn = new JButton("Save Changes");
        styleButton(saveBtn, new Color(39, 174, 96));

        int row = 0;
        addFormRow(panel, gbc, row++, "Find Customer (ID or Name):", findPanel);
        addFormRow(panel, gbc, row++, "Customer ID (read-only):", updateIdField);
        addFormRow(panel, gbc, row++, "Company:", updateCompanyField);
        addFormRow(panel, gbc, row++, "Full Name:", updateFullNameField);
        addFormRow(panel, gbc, row++, "Phone:", updatePhoneField);
        addFormRow(panel, gbc, row++, "Email:", updateEmailField);
        addFormRow(panel, gbc, row++, "Address:", updateAddressField);

        // Save button
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(saveBtn, gbc);

        findBtn.addActionListener(e -> loadCustomerForUpdate(updateFindField.getText()));
        updateFindField.addActionListener(e -> loadCustomerForUpdate(updateFindField.getText()));

        saveBtn.addActionListener((ActionEvent e) -> {
            String idText = updateIdField.getText().trim();
            if (idText.isEmpty()) {
                showError("Use 'Find Customer' to load a customer first.");
                return;
            }
            String company = updateCompanyField.getText().trim();
            String fullName = updateFullNameField.getText().trim();
            String phone = updatePhoneField.getText().trim();
            String email = updateEmailField.getText().trim();
            String address = updateAddressField.getText().trim();
            if (company.isEmpty() || fullName.isEmpty()) {
                showError("Company and Full Name must be filled.");
                return;
            }
            int id = Integer.parseInt(idText);
            boolean success = customerController.updateCustomerProfile(id, company, fullName, phone, email, address);
            if (success) {
                showInfo("Customer profile updated successfully.");
                refreshTable();
                clearUpdateFields();
            } else {
                showError("Update failed.");
            }
        });

        return panel;
    }

    private void loadCustomerForUpdate(String input) {
        Customer c = findCustomerByIdOrName(input);
        if (c != null) {
            updateIdField.setText(String.valueOf(c.getCustomerId()));
            updateCompanyField.setText(c.getCompanyName());
            updateFullNameField.setText(c.getFullName());
            updatePhoneField.setText(c.getPhone());
            updateEmailField.setText(c.getEmail());
            updateAddressField.setText(c.getShippingAddress());
        }
    }

    private void clearUpdateFields() {
        updateIdField.setText("");
        updateCompanyField.setText("");
        updateFullNameField.setText("");
        updatePhoneField.setText("");
        updateEmailField.setText("");
        updateAddressField.setText("");
    }

    // ==========================================
    // TAB 4 – CHANGE PASSWORD (Matches Staff style)
    // ==========================================

    private JPanel createPasswordPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;

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
        pwConfirmPasswordField = new JPasswordField(22);

        JLabel pwHintLabel = new JLabel("<html><i>Min 8 chars, with upper/lowercase, a number &amp; a symbol.</i></html>");
        pwHintLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        pwHintLabel.setForeground(Color.GRAY);

        JButton savePwBtn = new JButton("Update Password");
        styleButton(savePwBtn, new Color(39, 174, 96));

        int row = 0;
        addFormRow(panel, gbc, row++, "Find Customer (ID or Name):", pwFindPanel);
        addFormRow(panel, gbc, row++, "Customer ID (read-only):", pwIdField);
        addFormRow(panel, gbc, row++, "Username (read-only):", pwUsernameDisplay);
        addFormRow(panel, gbc, row++, "New Password:", pwNewPasswordField);

        // Hint – left-aligned
        gbc.gridx = 1;
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(pwHintLabel, gbc);

        addFormRow(panel, gbc, row++, "Confirm New Password:", pwConfirmPasswordField);

        // Save button
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(savePwBtn, gbc);

        pwFindBtn.addActionListener(e -> loadCustomerForPassword(pwFindField.getText()));
        pwFindField.addActionListener(e -> loadCustomerForPassword(pwFindField.getText()));

        savePwBtn.addActionListener((ActionEvent e) -> {
            String idText = pwIdField.getText().trim();
            if (idText.isEmpty()) {
                showError("Use 'Find Customer' to load a customer first.");
                return;
            }
            String newPw = new String(pwNewPasswordField.getPassword()).trim();
            String confirm = new String(pwConfirmPasswordField.getPassword()).trim();
            if (newPw.isEmpty() || confirm.isEmpty()) {
                showError("Please enter and confirm the new password.");
                return;
            }
            if (!newPw.equals(confirm)) {
                showError("Passwords do not match.");
                pwNewPasswordField.setText("");
                pwConfirmPasswordField.setText("");
                return;
            }
            String issue = PasswordUtil.checkStrength(newPw);
            if (issue != null) {
                showError(issue);
                return;
            }
            int id = Integer.parseInt(idText);
            boolean success = customerController.updateCustomerPassword(id, newPw);
            if (success) {
                showInfo("Password updated successfully.");
                pwIdField.setText("");
                pwUsernameDisplay.setText("");
                pwNewPasswordField.setText("");
                pwConfirmPasswordField.setText("");
            } else {
                showError("Password update failed.");
            }
        });

        return panel;
    }

    private void loadCustomerForPassword(String input) {
        Customer c = findCustomerByIdOrName(input);
        if (c != null) {
            pwIdField.setText(String.valueOf(c.getCustomerId()));
            pwUsernameDisplay.setText(c.getUsername());
            pwNewPasswordField.setText("");
            pwConfirmPasswordField.setText("");
        }
    }

    // ==========================================
    // HELPER – Find Customer by ID or Name
    // ==========================================

    private Customer findCustomerByIdOrName(String rawInput) {
        String input = rawInput == null ? "" : rawInput.trim();
        if (input.isEmpty()) {
            showError("Type a Customer ID or a name to search.");
            return null;
        }

        if (input.matches("\\d+")) {
            Customer byId = customerController.getCustomerById(Integer.parseInt(input));
            if (byId != null) return byId;
            showError("No customer found with ID " + input + ".");
            return null;
        }

        List<Customer> matches = customerController.searchAllCustomers(input);
        if (matches.isEmpty()) {
            showError("No customer found matching \"" + input + "\".");
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }

        String[] options = new String[matches.size()];
        for (int i = 0; i < matches.size(); i++) {
            Customer c = matches.get(i);
            options[i] = c.getCustomerId() + " - " + c.getFullName() + " (" + c.getUsername() + ")";
        }
        String choice = (String) JOptionPane.showInputDialog(this,
                "Multiple customers matched \"" + input + "\". Please select one:",
                "Select Customer", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == null) return null;
        int idx = java.util.Arrays.asList(options).indexOf(choice);
        return matches.get(idx);
    }

    // ==========================================
    // TABLE HELPERS
    // ==========================================

    private void refreshTable() {
        updateTable(customerController.getAllCustomers());
    }

    private void updateTable(List<Customer> list) {
        tableModel.setRowCount(0);
        for (Customer c : list) {
            tableModel.addRow(new Object[]{
                    c.getCustomerId(),
                    c.getUsername(),
                    c.getCompanyName(),
                    c.getFullName(),
                    c.getPhone(),
                    c.getEmail(),
                    c.getStatus()
            });
        }
    }

    // ==========================================
    // UI HELPERS (matching Staff style)
    // ==========================================

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
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