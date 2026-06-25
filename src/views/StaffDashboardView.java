package views;

import models.Staff;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class StaffDashboardView extends JFrame {
    private Staff loggedInStaff;
    private CardLayout cardLayout = new CardLayout();
    private JPanel cardPanel = new JPanel(cardLayout);

    private JPanel createHomePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;
     
        JLabel titleLabel = new JLabel("Staff Dashboard");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(41, 128, 185)); // Blue theme for Staff
        panel.add(titleLabel, gbc);
     
        JLabel userLabel = new JLabel("Logged in as: " + loggedInStaff.getFullName() + " (Staff)");
        userLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(userLabel, gbc);
     
        return panel;
    }

    public StaffDashboardView(Staff staff) {
        this.loggedInStaff = staff;
        
        setTitle("Staff Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        // TOP PANEL
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel welcomeLabel = new JLabel("Welcome, " + staff.getFullName());
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> { dispose(); new LoginView().setVisible(true); });
        topPanel.add(welcomeLabel, BorderLayout.WEST);
        topPanel.add(logoutButton, BorderLayout.EAST);

        // NAV BAR
        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JButton homeBtn = new JButton("Home");
        JButton salesBtn = new JButton("Process Sales Order");
        JButton pricingBtn = new JButton("Company Stock & Pricing");
        JButton inventoryBtn = new JButton("Supplier Inventory & Orders");
        JButton trackBtn = new JButton("View Orders & Tracking");

        Font btnFont = new Font("Arial", Font.BOLD, 14);
        for (JButton btn : new JButton[]{homeBtn, salesBtn, pricingBtn, inventoryBtn, trackBtn}) { // ADDED pricingBtn
            btn.setFont(btnFont); btn.setBackground(Color.WHITE); btn.setFocusPainted(false);
            buttonPanel.add(btn);
        }

        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(topPanel, BorderLayout.NORTH);
        northContainer.add(buttonPanel, BorderLayout.CENTER);

        // CARD PANEL
        cardPanel.add(createHomePanel(), "Home");
        cardPanel.add(new JPanel() {{ add(new JLabel("Process Customer Sales Orders Here")); }}, "Sales");
        cardPanel.add(new ManagePricingView(), "Pricing");
        cardPanel.add(new InventoryManagementView(loggedInStaff), "Inventory"); // CHANGED
        cardPanel.add(new JPanel() {{ add(new JLabel("Track Deliveries and View Payment History Here")); }}, "Track");

        cardLayout.show(cardPanel, "Home");

        // BUTTON ACTIONS
        homeBtn.addActionListener(e -> cardLayout.show(cardPanel, "Home"));
        salesBtn.addActionListener(e -> cardLayout.show(cardPanel, "Sales"));
        pricingBtn.addActionListener(e -> cardLayout.show(cardPanel, "Pricing"));
        inventoryBtn.addActionListener(e -> cardLayout.show(cardPanel, "Inventory")); // CHANGED
        trackBtn.addActionListener(e -> cardLayout.show(cardPanel, "Track"));

        setLayout(new BorderLayout());
        add(northContainer, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
    }
}