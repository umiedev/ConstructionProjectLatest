package views;

import controllers.SupplierController;
import models.Supplier;
import utils.PasswordUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class SupplierRegistrationView extends JFrame {
    private SupplierController supplierController = new SupplierController();
    private JTextField nameField, contactField, emailField, addressField, usernameField;
    private JPasswordField passwordField, confirmField;

    public SupplierRegistrationView() {
        setTitle("Supplier Registration");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 450); // Slightly increased height for breathing room
        setLocationRelativeTo(null);
        setResizable(true);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        nameField = new JTextField(20);
        contactField = new JTextField(20);
        emailField = new JTextField(20);
        addressField = new JTextField(20);
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        confirmField = new JPasswordField(20);

        addRow(panel, gbc, row++, "Company Name:", nameField);
        addRow(panel, gbc, row++, "Contact Number:", contactField);
        addRow(panel, gbc, row++, "Email:", emailField);
        addRow(panel, gbc, row++, "Address:", addressField);
        addRow(panel, gbc, row++, "Username:", usernameField);
        addRow(panel, gbc, row++, "Password:", passwordField);
        addRow(panel, gbc, row++, "Confirm Password:", confirmField);

        // --- NEW: Button Panel for Register and Cancel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        
        JButton registerBtn = new JButton("Register");
        styleButton(registerBtn, new Color(39, 174, 96)); // Green
        
        JButton cancelBtn = new JButton("Cancel");
        styleButton(cancelBtn, new Color(192, 57, 43)); // Red

        buttonPanel.add(registerBtn);
        buttonPanel.add(cancelBtn);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(buttonPanel, gbc);

        add(panel);

        // --- ACTIONS ---

        // Cancel Action: Re-open LoginView and close this window
        cancelBtn.addActionListener(e -> {
            new LoginView().setVisible(true);
            dispose();
        });

        // Register Action
        registerBtn.addActionListener((ActionEvent e) -> {
            String name = nameField.getText().trim();
            String contact = contactField.getText().trim();
            String email = emailField.getText().trim();
            String address = addressField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirm = new String(confirmField.getPassword());

            if (name.isEmpty() || contact.isEmpty() || email.isEmpty() || address.isEmpty() ||
                username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!password.equals(confirm)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
                confirmField.setText("");
                return;
            }

            String strength = PasswordUtil.checkStrength(password);
            if (strength != null) {
                JOptionPane.showMessageDialog(this, strength, "Weak Password", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Supplier supplier = new Supplier(name, contact, email, address, username, null, "Active");
            boolean success = supplierController.registerSupplier(supplier, password);
            if (success) {
                JOptionPane.showMessageDialog(this, "Registration successful! Please log in.");
                new LoginView().setVisible(true);
                dispose();
            } else {
                String err = supplierController.getLastError();
                JOptionPane.showMessageDialog(this,
                        err != null ? err : "Registration failed. Please try again.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(field, gbc);
    }

    // Updated styleButton to accept colors so Cancel and Register can look different
    private void styleButton(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 14));
    }
}