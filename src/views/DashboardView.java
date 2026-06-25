package views;

import models.Staff;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class DashboardView extends JFrame {
    private static final long serialVersionUID = 1L;
    
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
     
        JLabel titleLabel = new JLabel("Construction Material Supply System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(41, 128, 185));
        panel.add(titleLabel, gbc);
     
        JLabel subLabel = new JLabel("Select a module from the navigation bar below to get started.");
        subLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        subLabel.setForeground(Color.DARK_GRAY);
        panel.add(subLabel, gbc);
     
        JLabel userLabel = new JLabel("Logged in as: " + loggedInStaff.getFullName() + " (" + loggedInStaff.getRole() + ")");
        userLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        userLabel.setForeground(new Color(46, 204, 113));
        panel.add(userLabel, gbc);
     
        JLabel footerLabel = new JLabel("2026 | WeShowSpeed Group Project | BAXU 3113");
        footerLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        footerLabel.setForeground(Color.GRAY);
        panel.add(footerLabel, gbc);
     
        return panel;
    }

    public DashboardView(Staff loggedInStaff) {
        this.loggedInStaff = loggedInStaff;
        
        setTitle("Construction Material Supply System - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        setResizable(true);

        // ============================
        // TOP PANEL: Welcome + Logout
        // ============================
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(240, 240, 240));
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel welcomeLabel = new JLabel(
            "Welcome, " + loggedInStaff.getFullName() + " (" + loggedInStaff.getRole() + ")"
        );
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener((ActionEvent e) -> {
            dispose();
            new LoginView().setVisible(true);
        });

        topPanel.add(welcomeLabel, BorderLayout.WEST);
        topPanel.add(logoutButton, BorderLayout.EAST);

        // ============================
        // NAVIGATION PANEL (Buttons)
        // ============================
        JPanel buttonPanel = new JPanel(new GridLayout(1, 7, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        buttonPanel.setBackground(new Color(245, 245, 245)); // Light gray to distinguish from content

        JButton homeBtn = new JButton("Home");
        JButton staffBtn = new JButton("Staff");
        JButton materialBtn = new JButton("Materials");
        JButton customerBtn = new JButton("Customers");
        JButton supplierBtn = new JButton("Suppliers");
        JButton orderBtn = new JButton("Orders");
        JButton reportBtn = new JButton("Reports");

        // Style all buttons
        Font btnFont = new Font("Arial", Font.BOLD, 14);
        for (JButton btn : new JButton[]{homeBtn, staffBtn, materialBtn, customerBtn, supplierBtn, orderBtn, reportBtn}) {
            btn.setFont(btnFont);
            btn.setFocusPainted(false);
            btn.setBackground(Color.WHITE);
        }

        // Add buttons to the panel
        buttonPanel.add(homeBtn);
        buttonPanel.add(staffBtn);
        buttonPanel.add(materialBtn);
        buttonPanel.add(customerBtn);
        buttonPanel.add(supplierBtn);
        buttonPanel.add(orderBtn);
        buttonPanel.add(reportBtn);

        // Staff button restricted to Admin only
        if (!loggedInStaff.getRole().equals("Admin")) {
            staffBtn.setEnabled(false);
            staffBtn.setToolTipText("Access restricted to Admins only.");
        }

        // ============================
        // TOP CONTAINER (Welcome + Nav Bar)
        // ============================
        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(topPanel, BorderLayout.NORTH);
        northContainer.add(buttonPanel, BorderLayout.CENTER);

        // ============================
        // CENTER PANEL (Card Layout)
        // ============================
        cardPanel.add(createHomePanel(), "Home");
        
        // Pass loggedInStaff so safety guardrails work
        cardPanel.add(new StaffManagementView(loggedInStaff), "Staff"); 
        
        // Added the real Customer Management View here!
        cardPanel.add(new CustomerManagementView(), "Customers"); 
        
        // Placeholders for remaining modules
        cardPanel.add(new JPanel() {{ add(new JLabel("Materials Module Coming Soon")); }}, "Materials");
        cardPanel.add(new SupplierManagementView(), "Suppliers");
        cardPanel.add(new JPanel() {{ add(new JLabel("Orders Module Coming Soon")); }}, "Orders");
        cardPanel.add(new JPanel() {{ add(new JLabel("Reports Module Coming Soon")); }}, "Reports");

        // Show Home by default
        cardLayout.show(cardPanel, "Home");

        // ============================
        // BOTTOM PANEL (Status Bar only)
        // ============================
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JLabel statusBar = new JLabel(" WeShowSpeed | Logged in as: " + loggedInStaff.getUsername());
        statusBar.setHorizontalAlignment(SwingConstants.CENTER);
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusBar.setBackground(new Color(230, 230, 230));
        statusBar.setOpaque(true);
        bottomPanel.add(statusBar, BorderLayout.CENTER);

        // ============================
        // BUTTON ACTIONS (Swap Cards)
        // ============================
        homeBtn.addActionListener(e -> cardLayout.show(cardPanel, "Home"));
        staffBtn.addActionListener(e -> cardLayout.show(cardPanel, "Staff"));
        materialBtn.addActionListener(e -> cardLayout.show(cardPanel, "Materials"));
        customerBtn.addActionListener(e -> cardLayout.show(cardPanel, "Customers"));
        supplierBtn.addActionListener(e -> cardLayout.show(cardPanel, "Suppliers"));
        orderBtn.addActionListener(e -> cardLayout.show(cardPanel, "Orders"));
        reportBtn.addActionListener(e -> cardLayout.show(cardPanel, "Reports"));

        // ============================
        // FINAL ASSEMBLY
        // ============================
        setLayout(new BorderLayout());
        add(northContainer, BorderLayout.NORTH); // Top + Nav bar at the top
        add(cardPanel, BorderLayout.CENTER);     // Content in the middle
        add(bottomPanel, BorderLayout.SOUTH);    // Status bar at the bottom
    }
}