package views;

import controllers.StaffController;
import controllers.SupplierController;
import models.Staff;
import models.Supplier;
import models.Customer;
import controllers.CustomerController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginView extends JFrame {
    private StaffController staffController = new StaffController();
    private SupplierController supplierController = new SupplierController();
    private CustomerController customerController = new CustomerController();
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JRadioButton staffRadio, supplierRadio, customerRadio;
    private ButtonGroup roleGroup;

    public LoginView() {
        setTitle("Construction Material Supply System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 250);
        setLocationRelativeTo(null);
        setResizable(true);

        // Create UI Components
        JLabel userLabel = new JLabel("Username:");
        JLabel passLabel = new JLabel("Password:");
        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        // Role selection
        staffRadio = new JRadioButton("Staff");
        supplierRadio = new JRadioButton("Supplier");
        customerRadio = new JRadioButton("Customer");
        roleGroup = new ButtonGroup();
        roleGroup.add(staffRadio);
        roleGroup.add(supplierRadio);
        roleGroup.add(customerRadio);
        staffRadio.setSelected(true); // default

        JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rolePanel.add(new JLabel("Login as:"));
        rolePanel.add(staffRadio);
        rolePanel.add(supplierRadio);
        rolePanel.add(customerRadio);

        // Layout using GridBagLayout
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        // Role row
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        panel.add(rolePanel, gbc);
        row++;

        // Username
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.EAST;
        panel.add(userLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(usernameField, gbc);
        row++;

        // Password
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.EAST;
        panel.add(passLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(passwordField, gbc);
        row++;

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(buttonPanel, gbc);

        add(panel);

        // ====== LOGIN ACTION ======
        loginButton.addActionListener((ActionEvent e) -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter both username and password.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (staffRadio.isSelected()) {
                // Staff / Admin login
                Staff staff = staffController.authenticateLogin(username, password);
                if (staff != null) {
                    JOptionPane.showMessageDialog(this, "Welcome, " + staff.getFullName() + "!");
                    
                    // ROUTING LOGIC: Check role and open the correct dashboard
                    if (staff.getRole().equalsIgnoreCase("Admin")) {
                        new AdminDashboardView(staff).setVisible(true);
                    } else {
                        new StaffDashboardView(staff).setVisible(true);
                    }
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid credentials or inactive account.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                    passwordField.setText("");
                }
            } else if (supplierRadio.isSelected()) {
                // Supplier login
                Supplier supplier = supplierController.authenticateSupplier(username, password);
                if (supplier != null) {
                    JOptionPane.showMessageDialog(this, "Welcome, " + supplier.getSupplierName() + "!");
                    new SupplierDashboardView(supplier).setVisible(true);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid credentials or inactive account.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                    passwordField.setText("");
                }
            } else if (customerRadio.isSelected()) {
                // Customer login implementation
                models.Customer customer = customerController.authenticateLogin(username, password);
                if (customer != null) {
                    JOptionPane.showMessageDialog(this, "Welcome, " + customer.getFullName() + "!");
                    new CustomerDashboardView(customer).setVisible(true);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid credentials or inactive account.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                    passwordField.setText("");
                }
            }
        });

     // ====== REGISTER ACTION ======
        registerButton.addActionListener((ActionEvent e) -> {
            if (supplierRadio.isSelected()) {
                new SupplierRegistrationView().setVisible(true);
                dispose();
            } 
            else if (customerRadio.isSelected()) {
                // NEW: Now handles Customer Registration
                new CustomerRegisterView().setVisible(true);
                
            } 
            else if (staffRadio.isSelected()) {
                // Keep Staff registration restricted or handle it here if you have a view for it
                JOptionPane.showMessageDialog(this, "Staff accounts must be created by an Administrator.", "Registration Restricted", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        getRootPane().setDefaultButton(loginButton);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginView().setVisible(true));
    }
}