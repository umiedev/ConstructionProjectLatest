package views;

import controllers.CustomerController;
import models.Customer;
import utils.PasswordUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CustomerRegisterView extends JFrame {

    private CustomerController customerController = new CustomerController();

    private JTextField companyField;
    private JTextField contactField;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextField addressField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmField;

    public CustomerRegisterView() {
        setTitle("New Customer Registration");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 450);
        setLocationRelativeTo(null);
        setResizable(true);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        // Do NOT set fill or weightx here – use default (NONE, 0)

        companyField = new JTextField(20);
        contactField = new JTextField(20);
        phoneField = new JTextField(20);
        emailField = new JTextField(20);
        addressField = new JTextField(20);   // single line, like Supplier
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        confirmField = new JPasswordField(20);

        JLabel passwordHintLabel = new JLabel("<html><i>Min 8 chars, with upper/lowercase, a number &amp; a symbol.</i></html>");
        passwordHintLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        passwordHintLabel.setForeground(Color.GRAY);

        JButton registerBtn = new JButton("Register");
        styleButton(registerBtn, new Color(39, 174, 96));
        JButton cancelBtn = new JButton("Cancel");
        styleButton(cancelBtn, new Color(192, 57, 43));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.add(registerBtn);
        buttonPanel.add(cancelBtn);

        int row = 0;
        addRow(panel, gbc, row++, "Company Name:", companyField);
        addRow(panel, gbc, row++, "Contact Number:", contactField);
        addRow(panel, gbc, row++, "Email:", emailField);
        addRow(panel, gbc, row++, "Address:", addressField);
        addRow(panel, gbc, row++, "Username:", usernameField);
        addRow(panel, gbc, row++, "Password:", passwordField);
        addRow(panel, gbc, row++, "Confirm Password:", confirmField);

        // Hint – placed under Confirm field
        gbc.gridx = 1;
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(passwordHintLabel, gbc);

        // Buttons
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(buttonPanel, gbc);

        add(panel);

        // ---- Actions ----
        cancelBtn.addActionListener(e -> {
            new LoginView().setVisible(true);
            dispose();
        });

        registerBtn.addActionListener((ActionEvent e) -> {
            String company  = companyField.getText().trim();
            String contact  = contactField.getText().trim();
            String phone    = phoneField.getText().trim();
            String email    = emailField.getText().trim();
            String address  = addressField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String confirm  = new String(confirmField.getPassword()).trim();

            if (company.isEmpty() || contact.isEmpty() || username.isEmpty() ||
                password.isEmpty() || confirm.isEmpty()) {
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

            Customer customer = new Customer(username, password, company, contact, phone, email, address, "Active");
            boolean success = customerController.addCustomer(customer);
            if (success) {
                JOptionPane.showMessageDialog(this, "Registration successful! Please log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
                new LoginView().setVisible(true);
                dispose();
            } else {
                String err = customerController.getLastError();
                JOptionPane.showMessageDialog(this, err != null ? err : "Registration failed.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
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
        button.setFont(new Font("Arial", Font.BOLD, 14));
    }
}