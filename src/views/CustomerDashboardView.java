package views;

import controllers.CustomerController;
import models.Customer;
import utils.PasswordUtil; // Imported to check password strength

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CustomerDashboardView extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private Customer loggedInCustomer;
    private CustomerController customerController = new CustomerController(); 
    private CardLayout cardLayout = new CardLayout();
    private JPanel cardPanel = new JPanel(cardLayout);

    private JPanel createHomePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
     
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;
     
        JLabel titleLabel = new JLabel("Client Portal: Construction Material Supply");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(41, 128, 185));
        panel.add(titleLabel, gbc);
     
        JLabel subLabel = new JLabel("Welcome back! Select an option below to manage your supply orders.");
        subLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        subLabel.setForeground(Color.DARK_GRAY);
        panel.add(subLabel, gbc);
     
        JLabel userLabel = new JLabel("Company: " + loggedInCustomer.getCompanyName());
        userLabel.setFont(new Font("Arial", Font.BOLD, 16));
        userLabel.setForeground(new Color(46, 204, 113));
        panel.add(userLabel, gbc);
     
        return panel;
    }

    // ==========================================
    // COMPANY PROFILE EDITOR
    // ==========================================
    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(250, 250, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Edit Company Profile");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(new Color(41, 128, 185));

        JTextField txtCompany = new JTextField(loggedInCustomer.getCompanyName(), 20);
        JTextField txtContact = new JTextField(loggedInCustomer.getFullName(), 20);
        JTextField txtPhone   = new JTextField(loggedInCustomer.getPhone(), 20);
        JTextField txtEmail   = new JTextField(loggedInCustomer.getEmail(), 20);
        JTextArea txtAddress  = new JTextArea(loggedInCustomer.getShippingAddress(), 4, 20);
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);

        // Buttons
        JButton btnSave = new JButton("Save Changes");
        btnSave.setBackground(new Color(39, 174, 96));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("Arial", Font.BOLD, 14));
        btnSave.setFocusPainted(false);

        JButton btnChangePassword = new JButton("Change Password");
        btnChangePassword.setBackground(new Color(41, 128, 185));
        btnChangePassword.setForeground(Color.WHITE);
        btnChangePassword.setFont(new Font("Arial", Font.BOLD, 14));
        btnChangePassword.setFocusPainted(false);

        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        actionButtonPanel.setBackground(new Color(250, 250, 250));
        actionButtonPanel.add(btnSave);
        actionButtonPanel.add(btnChangePassword);

        // Layout Assembly
        int row = 0;
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        addFormRow(panel, gbc, row++, "Company Name:", txtCompany);
        addFormRow(panel, gbc, row++, "Contact Person:", txtContact);
        addFormRow(panel, gbc, row++, "Phone Number:", txtPhone);
        addFormRow(panel, gbc, row++, "Email Address:", txtEmail);

        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.NORTHEAST;
        panel.add(new JLabel("Shipping Address:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JScrollPane(txtAddress), gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 10, 10, 10);
        panel.add(actionButtonPanel, gbc);

        // ACTIONS
        btnSave.addActionListener(e -> {
            String company = txtCompany.getText().trim();
            String contact = txtContact.getText().trim();
            String phone   = txtPhone.getText().trim();
            String email   = txtEmail.getText().trim();
            String address = txtAddress.getText().trim();

            if (company.isEmpty() || contact.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Company Name and Contact Person cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean success = customerController.updateCustomerProfile(
                loggedInCustomer.getCustomerId(), company, contact, phone, email, address
            );

            if (success) {
                loggedInCustomer.setCompanyName(company);
                loggedInCustomer.setFullName(contact);
                loggedInCustomer.setPhone(phone);
                loggedInCustomer.setEmail(email);
                loggedInCustomer.setShippingAddress(address);
                JOptionPane.showMessageDialog(this, "Profile updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Database Error: Could not update profile.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnChangePassword.addActionListener(e -> showChangePasswordDialog());

        return panel;
    }
    

    // ==========================================
    // SECURE PASSWORD CHANGE DIALOG
    // ==========================================
    private void showChangePasswordDialog() {
        JPasswordField currentPasswordField = new JPasswordField(15);
        JPasswordField newPasswordField = new JPasswordField(15);
        JPasswordField confirmPasswordField = new JPasswordField(15);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.add(new JLabel("Current Password:"));
        panel.add(currentPasswordField);
        panel.add(new JLabel("New Password:"));
        panel.add(newPasswordField);
        panel.add(new JLabel("Confirm New Password:"));
        panel.add(confirmPasswordField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Change Account Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String currentPw = new String(currentPasswordField.getPassword());
            String newPw = new String(newPasswordField.getPassword());
            String confirmPw = new String(confirmPasswordField.getPassword());

            // 1. Validation Checks
            if (currentPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!newPw.equals(confirmPw)) {
                JOptionPane.showMessageDialog(this, "New passwords do not match. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String strengthIssue = PasswordUtil.checkStrength(newPw);
            if (strengthIssue != null) {
                JOptionPane.showMessageDialog(this, strengthIssue, "Weak Password", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 2. Verify current password by attempting a "background login"
            Customer authCheck = customerController.authenticateLogin(loggedInCustomer.getUsername(), currentPw);
            if (authCheck == null) {
                JOptionPane.showMessageDialog(this, "The current password you entered is incorrect.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 3. Save new password to database
            boolean success = customerController.updateCustomerPassword(loggedInCustomer.getCustomerId(), newPw);
            if (success) {
                JOptionPane.showMessageDialog(this, "Your password has been successfully updated!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Database Error: Could not update password.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Helper method to keep layout code clean
    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(field, gbc);
    }

    public CustomerDashboardView(Customer loggedInCustomer) {
        this.loggedInCustomer = loggedInCustomer;
        
        setTitle("Customer Portal - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        setResizable(true);

        // TOP PANEL: Welcome + Logout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(240, 240, 240));
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel welcomeLabel = new JLabel("Welcome, " + loggedInCustomer.getFullName());
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener((ActionEvent e) -> {
            dispose();
            new LoginView().setVisible(true);
        });

        topPanel.add(welcomeLabel, BorderLayout.WEST);
        topPanel.add(logoutButton, BorderLayout.EAST);

        // NAVIGATION PANEL
        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        buttonPanel.setBackground(new Color(245, 245, 245));

        JButton homeBtn = new JButton("Home");
        JButton orderMatBtn = new JButton("Order Materials");
        JButton myOrdersBtn = new JButton("My Orders");
        JButton profileBtn = new JButton("Company Profile");

        Font btnFont = new Font("Arial", Font.BOLD, 14);
        for (JButton btn : new JButton[]{homeBtn, orderMatBtn, myOrdersBtn, profileBtn}) {
            btn.setFont(btnFont);
            btn.setFocusPainted(false);
            btn.setBackground(Color.WHITE);
            buttonPanel.add(btn);
        }

        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(topPanel, BorderLayout.NORTH);
        northContainer.add(buttonPanel, BorderLayout.CENTER);

        // CENTER PANEL (Card Layout)
        cardPanel.add(createHomePanel(), "Home");
        cardPanel.add(new OrderMaterialsView(loggedInCustomer.getCustomerId()), "OrderMaterials");
        cardPanel.add(new MyOrdersView(loggedInCustomer.getCustomerId()),       "MyOrders");
        cardPanel.add(createProfilePanel(), "Profile");

        cardLayout.show(cardPanel, "Home");

        // BOTTOM PANEL
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JLabel statusBar = new JLabel(" System Status: Active | Logged in as: " + loggedInCustomer.getUsername());
        statusBar.setHorizontalAlignment(SwingConstants.CENTER);
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusBar.setBackground(new Color(230, 230, 230));
        statusBar.setOpaque(true);
        bottomPanel.add(statusBar, BorderLayout.CENTER);

        // BUTTON ACTIONS
        homeBtn.addActionListener(e -> cardLayout.show(cardPanel, "Home"));
        orderMatBtn.addActionListener(e -> cardLayout.show(cardPanel, "OrderMaterials"));
        myOrdersBtn.addActionListener(e -> cardLayout.show(cardPanel, "MyOrders"));
        profileBtn.addActionListener(e -> cardLayout.show(cardPanel, "Profile"));

        setLayout(new BorderLayout());
        add(northContainer, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
}