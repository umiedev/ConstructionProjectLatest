package views;

import controllers.SupplierController;
import models.Supplier;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class SupplierLoginView extends JFrame {
    private SupplierController supplierController = new SupplierController();
    private JTextField usernameField;
    private JPasswordField passwordField;

    public SupplierLoginView() {
        setTitle("Supplier Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 200);
        setLocationRelativeTo(null);
        setResizable(true);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel userLabel = new JLabel("Username:");
        JLabel passLabel = new JLabel("Password:");
        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        panel.add(userLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        panel.add(passLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(passwordField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(buttonPanel, gbc);

        add(panel);

        loginButton.addActionListener((ActionEvent e) -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            Supplier supplier = supplierController.authenticateSupplier(username, password);
            if (supplier != null) {
                JOptionPane.showMessageDialog(this, "Welcome, " + supplier.getSupplierName() + "!");
                new SupplierDashboardView(supplier).setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials or inactive account.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
            }
        });

        registerButton.addActionListener((ActionEvent e) -> {
            new SupplierRegistrationView().setVisible(true);
            dispose();
        });

        getRootPane().setDefaultButton(loginButton);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SupplierLoginView().setVisible(true));
    }
}