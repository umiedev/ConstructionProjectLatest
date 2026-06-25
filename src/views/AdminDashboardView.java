package views;

import models.Staff;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class AdminDashboardView extends JFrame {
    private Staff loggedInAdmin;
    private CardLayout cardLayout = new CardLayout();
    private JPanel cardPanel = new JPanel(cardLayout);

    private JPanel createHomePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel titleLabel = new JLabel("Admin Dashboard");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(192, 57, 43));
        panel.add(titleLabel, gbc);

        JLabel userLabel = new JLabel("Logged in as: " + loggedInAdmin.getFullName() + " (Administrator)");
        userLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(userLabel, gbc);

        JLabel footerLabel = new JLabel("2026 | WeShowSpeed Group Project | BAXU 3113");
        footerLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        footerLabel.setForeground(Color.GRAY);
        panel.add(footerLabel, gbc);

        return panel;
    }

    private JPanel placeholder(String moduleName) {
        JPanel p = new JPanel(new GridBagLayout());
        JLabel l = new JLabel(moduleName + " — Coming Soon");
        l.setFont(new Font("Arial", Font.ITALIC, 18));
        l.setForeground(Color.GRAY);
        p.add(l);
        return p;
    }

    public AdminDashboardView(Staff admin) {
        this.loggedInAdmin = admin;

        setTitle("Admin Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setResizable(true);

        // TOP PANEL
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(240, 240, 240));
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel welcomeLabel = new JLabel("Welcome Admin, " + admin.getFullName());
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> { dispose(); new LoginView().setVisible(true); });
        topPanel.add(welcomeLabel, BorderLayout.WEST);
        topPanel.add(logoutButton, BorderLayout.EAST);

        // NAV BAR — now 7 buttons to match full admin access
        JPanel buttonPanel = new JPanel(new GridLayout(1, 7, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        buttonPanel.setBackground(new Color(245, 245, 245));

        JButton homeBtn     = new JButton("Home");
        JButton staffBtn    = new JButton("Staff");
        JButton customerBtn = new JButton("Customers");
        JButton supplierBtn = new JButton("Suppliers");
        JButton materialBtn = new JButton("Materials");
        JButton orderBtn    = new JButton("Orders");
        JButton reportBtn   = new JButton("Reports");

        Font btnFont = new Font("Arial", Font.BOLD, 13);
        for (JButton btn : new JButton[]{homeBtn, staffBtn, customerBtn, supplierBtn, materialBtn, orderBtn, reportBtn}) {
            btn.setFont(btnFont);
            btn.setBackground(Color.WHITE);
            btn.setFocusPainted(false);
            buttonPanel.add(btn);
        }

        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(topPanel, BorderLayout.NORTH);
        northContainer.add(buttonPanel, BorderLayout.CENTER);

        // CARD PANEL — all 7 modules wired up
        cardPanel.add(createHomePanel(),                  "Home");
        cardPanel.add(new StaffManagementView(admin),     "Staff");
        cardPanel.add(new CustomerManagementView(),       "Customers");
        cardPanel.add(new SupplierManagementView(),       "Suppliers");
        cardPanel.add(new ManagePricingView(),            "Materials");
        cardPanel.add(new OrderManagementView(),          "Orders");
        cardPanel.add(new ReportsView(),                  "Reports");

        cardLayout.show(cardPanel, "Home");

        // BUTTON ACTIONS
        homeBtn.addActionListener(e     -> cardLayout.show(cardPanel, "Home"));
        staffBtn.addActionListener(e    -> cardLayout.show(cardPanel, "Staff"));
        customerBtn.addActionListener(e -> cardLayout.show(cardPanel, "Customers"));
        supplierBtn.addActionListener(e -> cardLayout.show(cardPanel, "Suppliers"));
        materialBtn.addActionListener(e -> cardLayout.show(cardPanel, "Materials"));
        orderBtn.addActionListener(e    -> cardLayout.show(cardPanel, "Orders"));
        reportBtn.addActionListener(e   -> cardLayout.show(cardPanel, "Reports"));

        // STATUS BAR
        JLabel statusBar = new JLabel(" WeShowSpeed  |  Logged in as: " + admin.getUsername() + "  (Admin)");
        statusBar.setHorizontalAlignment(SwingConstants.CENTER);
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusBar.setBackground(new Color(230, 230, 230));
        statusBar.setOpaque(true);

        setLayout(new BorderLayout());
        add(northContainer, BorderLayout.NORTH);
        add(cardPanel,      BorderLayout.CENTER);
        add(statusBar,      BorderLayout.SOUTH);
    }
}