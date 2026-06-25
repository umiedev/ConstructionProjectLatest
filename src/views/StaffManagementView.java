package views;

import controllers.StaffController;
import models.Staff;
import utils.PasswordUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class StaffManagementView extends JPanel {

    private final StaffController staffController = new StaffController();

    // Shared table 
    private JTable staffTable;
    private DefaultTableModel tableModel;

    // View / Search Tab
    private JTextField searchField;

    // Add Tab
    private JTextField     addUsernameField;
    private JTextField     addFullNameField;
    private JPasswordField addPasswordField;
    private JPasswordField addConfirmPasswordField;
    private JComboBox<String> addRoleComboBox;

    // Update Tab
    private JTextField        updateFindField;  
    private JTextField        updateIdField;
    private JTextField        updateFullNameField;
    private JComboBox<String> updateRoleComboBox;

    // Change Password Tab
    private JTextField     pwFindField;         
    private JTextField     pwIdField;
    private JTextField     pwUsernameDisplay;
    private JPasswordField pwNewPasswordField;
    private JPasswordField pwConfirmPasswordField;

    private final Staff loggedInStaff;

    public StaffManagementView(Staff loggedInStaff) {
        this.loggedInStaff = loggedInStaff;

        initSharedTable();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Add Staff",        createAddPanel());
        tabbedPane.addTab("View / Search",    createViewPanel());
        tabbedPane.addTab("Update Profile",   createUpdatePanel());
        tabbedPane.addTab("Change Password",  createPasswordPanel());

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
    }

    public StaffManagementView() {
        this(null);
    }

    // ==========================================
    // SHARED TABLE INIT (before any panel builds)
    // ==========================================

    private void initSharedTable() {
        String[] columns = {"ID", "Username", "Full Name", "Role", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        staffTable = new JTable(tableModel);
        staffTable.setRowHeight(26);
        staffTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        staffTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        staffTable.setFont(new Font("Arial", Font.PLAIN, 13));

        // Gray out Inactive rows so deactivated staff are visually distinct from Active ones
        javax.swing.table.DefaultTableCellRenderer statusAwareRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) table.getModel().getValueAt(row, 4);
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
        };
        staffTable.setDefaultRenderer(Object.class, statusAwareRenderer);

        refreshTable();
    }

    // ==========================================
    // TAB 1 — ADD STAFF
    // ==========================================

    private JPanel createAddPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addUsernameField = new JTextField(22);
        addPasswordField = new JPasswordField(22);
        addConfirmPasswordField = new JPasswordField(22);
        addFullNameField = new JTextField(22);
        addRoleComboBox  = new JComboBox<>(new String[]{"Admin", "Staff"});

        JLabel passwordHintLabel = new JLabel("<html><i>Min 8 chars, with upper/lowercase, a number &amp; a symbol.</i></html>");
        passwordHintLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        passwordHintLabel.setForeground(Color.GRAY);

        JButton addButton = new JButton("Add Staff Member");
        styleButton(addButton, new Color(39, 174, 96));

        int row = 0;
        addFormRow(panel, gbc, row++, "Username:",  addUsernameField);
        addFormRow(panel, gbc, row++, "Password:",  addPasswordField);
        addFormRow(panel, gbc, row++, "Confirm Password:", addConfirmPasswordField);

        gbc.gridx = 1; gbc.gridy = row++; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(passwordHintLabel, gbc);

        addFormRow(panel, gbc, row++, "Full Name:", addFullNameField);
        addFormRow(panel, gbc, row++, "Role:",      addRoleComboBox);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(addButton, gbc);

        addButton.addActionListener((ActionEvent e) -> {
            String username = addUsernameField.getText().trim();
            String password = new String(addPasswordField.getPassword()).trim();
            String confirmPassword = new String(addConfirmPasswordField.getPassword()).trim();
            String fullName = addFullNameField.getText().trim();
            String role     = (String) addRoleComboBox.getSelectedItem();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || fullName.isEmpty()) {
                showError("All fields are required.");
                return;
            }

            if (!password.equals(confirmPassword)) {
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

            Staff newStaff = new Staff(username, password, fullName, role, "Active");
            boolean success = staffController.addStaff(newStaff);

            if (success) {
                showInfo("Staff member '" + username + "' added successfully.");
                clearAddFields();
                refreshTable();
            } else {
                showError("Failed to add staff. Username '" + username + "' may already exist.");
            }
        });

        return panel;
    }

    // ==========================================
    // TAB 2 — VIEW / SEARCH / DEACTIVATE
    // ==========================================

    private JPanel createViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchField = new JTextField(22);
        JButton searchButton     = new JButton("Search");
        JButton refreshButton    = new JButton("Refresh");
        JButton deactivateButton = new JButton("Deactivate Selected");
        JButton deleteButton     = new JButton("Delete Permanently");
        styleButton(deactivateButton, new Color(192, 57, 43));
        styleButton(deleteButton, new Color(120, 20, 20));

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(refreshButton);
        searchPanel.add(deactivateButton);
        searchPanel.add(deleteButton);

        JScrollPane scrollPane = new JScrollPane(staffTable);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane,  BorderLayout.CENTER);

        searchButton.addActionListener((ActionEvent e) -> {
            String keyword = searchField.getText().trim();
            if (keyword.isEmpty()) refreshTable();
            else updateTable(staffController.searchAllStaff(keyword));
        });

        searchField.addActionListener(e -> searchButton.doClick());

        refreshButton.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });

        deactivateButton.addActionListener((ActionEvent e) -> {
            int selectedRow = staffTable.getSelectedRow();
            if (selectedRow == -1) {
                showError("Please select a staff member from the table first.");
                return;
            }

            int    staffId  = (int)    tableModel.getValueAt(selectedRow, 0);
            String fullName = (String) tableModel.getValueAt(selectedRow, 2);
            String status   = (String) tableModel.getValueAt(selectedRow, 4);

            // Self-deactivation guard
            if (loggedInStaff != null && staffId == loggedInStaff.getStaffId()) {
                showError("You cannot deactivate your own account while logged in.");
                return;
            }

            if ("Inactive".equalsIgnoreCase(status)) {
                showError("'" + fullName + "' is already inactive.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                "Deactivate '" + fullName + "'?\nThis will prevent them from logging in.",
                "Confirm Deactivation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                boolean success = staffController.deactivateStaff(staffId);
                if (success) {
                    showInfo("'" + fullName + "' has been deactivated.");
                    refreshTable();
                } else {
                    showError("Deactivation failed. Please try again.");
                }
            }
        });

        deleteButton.addActionListener((ActionEvent e) -> {
            int selectedRow = staffTable.getSelectedRow();
            if (selectedRow == -1) {
                showError("Please select a staff member from the table first.");
                return;
            }

            int    staffId  = (int)    tableModel.getValueAt(selectedRow, 0);
            String fullName = (String) tableModel.getValueAt(selectedRow, 2);

            // Self-deletion guard
            if (loggedInStaff != null && staffId == loggedInStaff.getStaffId()) {
                showError("You cannot delete your own account while logged in.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                "Permanently delete '" + fullName,
                "Confirm Permanent Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                boolean deleted = staffController.deleteStaff(staffId);
                if (deleted) {
                    // IDs are intentionally NOT renumbered. Each staff member keeps
                    // their original ID, and AUTO_INCREMENT continues to the next
                    // value even after deletions (gaps are expected and fine).
                    showInfo("'" + fullName + "' has been permanently deleted.");
                    searchField.setText("");
                    refreshTable();
                } else {
                    showError("Deletion failed. Please try again.");
                }
            }
        });

        return panel;
    }

    // ==========================================
    // TAB 3 — UPDATE PROFILE
    // ==========================================

    private JPanel createUpdatePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // "Find Staff" box
        updateFindField = new JTextField(16);
        JButton findButton = new JButton("Find");
        JPanel findPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        findPanel.add(updateFindField);
        findPanel.add(findButton);

        updateIdField = new JTextField(22);
        updateIdField.setEditable(false);
        updateIdField.setBackground(new Color(230, 230, 230));

        updateFullNameField = new JTextField(22);
        updateRoleComboBox  = new JComboBox<>(new String[]{"Admin", "Staff"});

        JButton updateButton = new JButton("Save Profile Changes");
        styleButton(updateButton, new Color(39, 174, 96));

        int row = 0;
        addFormRow(panel, gbc, row++, "Find Staff (ID or Name):", findPanel);
        addFormRow(panel, gbc, row++, "Staff ID (read-only):", updateIdField);
        addFormRow(panel, gbc, row++, "New Full Name:",         updateFullNameField);
        addFormRow(panel, gbc, row++, "New Role:",              updateRoleComboBox);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(updateButton, gbc);

        // Load directly by typing a Staff ID or a name
        findButton.addActionListener((ActionEvent e) -> {
            Staff found = findStaffByIdOrName(updateFindField.getText());
            if (found != null) {
                updateIdField.setText(String.valueOf(found.getStaffId()));
                updateFullNameField.setText(found.getFullName());
                updateRoleComboBox.setSelectedItem(found.getRole());
            }
        });
        updateFindField.addActionListener(e -> findButton.doClick());

        updateButton.addActionListener((ActionEvent e) -> {
            String idText = updateIdField.getText().trim();
            if (idText.isEmpty()) {
                showError("Use the 'Find Staff' box above to load a staff member first.");
                return;
            }
            String newName = updateFullNameField.getText().trim();
            String newRole = (String) updateRoleComboBox.getSelectedItem();

            if (newName.isEmpty()) {
                showError("Full Name cannot be empty.");
                return;
            }

            int staffId = Integer.parseInt(idText);
            boolean success = staffController.updateStaffProfile(staffId, newName, newRole);

            if (success) {
                showInfo("Profile updated successfully.");
                refreshTable();
                updateIdField.setText("");
                updateFullNameField.setText("");
            } else {
                showError("Update failed. Please try again.");
            }
        });

        return panel;
    }

    // ==========================================
    // TAB 4 — CHANGE PASSWORD
    // ==========================================

    private JPanel createPasswordPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // "Find Staff" box
        pwFindField = new JTextField(16);
        JButton pwFindButton = new JButton("Find");
        JPanel pwFindPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pwFindPanel.add(pwFindField);
        pwFindPanel.add(pwFindButton);

        pwIdField = new JTextField(22);
        pwIdField.setEditable(false);
        pwIdField.setBackground(new Color(230, 230, 230));

        pwUsernameDisplay = new JTextField(22);
        pwUsernameDisplay.setEditable(false);
        pwUsernameDisplay.setBackground(new Color(230, 230, 230));

        pwNewPasswordField     = new JPasswordField(22);
        pwConfirmPasswordField = new JPasswordField(22);

        JLabel pwHintLabel = new JLabel("<html><i>Min 8 chars, with upper/lowercase, a number &amp; a symbol.</i></html>");
        pwHintLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        pwHintLabel.setForeground(Color.GRAY);

        JButton savePwButton = new JButton("Update Password");
        styleButton(savePwButton, new Color(39, 174, 96));

        int row = 0;
        addFormRow(panel, gbc, row++, "Find Staff (ID or Name):", pwFindPanel);
        addFormRow(panel, gbc, row++, "Staff ID (read-only):",  pwIdField);
        addFormRow(panel, gbc, row++, "Username (read-only):",  pwUsernameDisplay);
        addFormRow(panel, gbc, row++, "New Password:",           pwNewPasswordField);

        gbc.gridx = 1; gbc.gridy = row++; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(pwHintLabel, gbc);

        addFormRow(panel, gbc, row++, "Confirm New Password:",   pwConfirmPasswordField);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(savePwButton, gbc);

        // Load directly by typing a Staff ID or a name 
        pwFindButton.addActionListener((ActionEvent e) -> {
            Staff found = findStaffByIdOrName(pwFindField.getText());
            if (found != null) {
                pwIdField.setText(String.valueOf(found.getStaffId()));
                pwUsernameDisplay.setText(found.getUsername());
                pwNewPasswordField.setText("");
                pwConfirmPasswordField.setText("");
            }
        });
        pwFindField.addActionListener(e -> pwFindButton.doClick());

        savePwButton.addActionListener((ActionEvent e) -> {
            String idText = pwIdField.getText().trim();
            if (idText.isEmpty()) {
                showError("Use the 'Find Staff' box above to load a staff member first.");
                return;
            }

            String newPassword     = new String(pwNewPasswordField.getPassword()).trim();
            String confirmPassword = new String(pwConfirmPasswordField.getPassword()).trim();

            if (newPassword.isEmpty()) {
                showError("New password cannot be empty.");
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                showError("Passwords do not match. Please re-enter.");
                pwNewPasswordField.setText("");
                pwConfirmPasswordField.setText("");
                return;
            }

            String strengthIssue = PasswordUtil.checkStrength(newPassword);
            if (strengthIssue != null) {
                showError(strengthIssue);
                return;
            }

            int staffId = Integer.parseInt(idText);
            boolean success = staffController.updateStaffPassword(staffId, newPassword);

            if (success) {
                showInfo("Password for '" + pwUsernameDisplay.getText() + "' updated successfully.");
                pwIdField.setText("");
                pwUsernameDisplay.setText("");
                pwNewPasswordField.setText("");
                pwConfirmPasswordField.setText("");
            } else {
                showError("Password update failed. Please try again.");
            }
        });

        return panel;
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    /**
     * Resolves whatever the user typed into a "Find Staff" box into one Staff record.
     * - If the input is purely numeric, it's treated as a Staff ID (exact lookup).
     * - Otherwise it's treated as a name/username keyword search.
     *   - 0 matches  -> show error, return null
     *   - 1 match    -> return it directly
     *   - 2+ matches -> ask the user to pick the right one from a dropdown
     */
    private Staff findStaffByIdOrName(String rawInput) {
        String input = rawInput == null ? "" : rawInput.trim();
        if (input.isEmpty()) {
            showError("Type a Staff ID or a name to search for.");
            return null;
        }

        // Numeric input is treated as an exact Staff ID lookup
        if (input.matches("\\d+")) {
            Staff byId = staffController.getStaffById(Integer.parseInt(input));
            if (byId != null) {
                return byId;
            }
            showError("No active staff found with ID " + input + ".");
            return null;
        }

        // Otherwise, search by name/username
        List<Staff> matches = staffController.searchStaff(input);

        if (matches.isEmpty()) {
            showError("No active staff found matching \"" + input + "\".");
            return null;
        }

        if (matches.size() == 1) {
            return matches.get(0);
        }

        // Multiple matches found 
        String[] options = new String[matches.size()];
        for (int i = 0; i < matches.size(); i++) {
            Staff s = matches.get(i);
            options[i] = s.getStaffId() + " - " + s.getFullName() + " (" + s.getUsername() + ")";
        }

        String choice = (String) JOptionPane.showInputDialog(
            this,
            "Multiple staff matched \"" + input + "\". Please select one:",
            "Select Staff Member",
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );

        if (choice == null) {
            return null; // user cancelled the dialog
        }

        int chosenIndex = java.util.Arrays.asList(options).indexOf(choice);
        return matches.get(chosenIndex);
    }

    private void refreshTable() {
        updateTable(staffController.getAllStaff());
    }

    private void updateTable(List<Staff> staffList) {
        tableModel.setRowCount(0);
        for (Staff s : staffList) {
            tableModel.addRow(new Object[]{
                s.getStaffId(),
                s.getUsername(),
                s.getFullName(),
                s.getRole(),
                s.getStatus()
            });
        }
    }

    private void clearAddFields() {
        addUsernameField.setText("");
        addPasswordField.setText("");
        addConfirmPasswordField.setText("");
        addFullNameField.setText("");
        addRoleComboBox.setSelectedIndex(0);
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(field, gbc);
    }

    private void styleButton(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 13));
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}