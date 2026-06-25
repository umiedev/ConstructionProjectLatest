package views;

import controllers.ReportController;
import controllers.ReportController.SummaryStats;
import utils.SimplePdfExporter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Admin "Reports" panel — sales summary cards plus breakdown tables
 * (top materials, revenue by status, top customers, low stock alert).
 */
public class ReportsView extends JPanel {
    private final ReportController reportController = new ReportController();

    private JLabel totalOrdersValue, totalRevenueValue, totalTaxValue, pendingValue, completedValue;

    public ReportsView() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Sales & Inventory Reports");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        JButton refreshBtn = new JButton("Refresh Reports");
        JButton printBtn = new JButton("Export Report as PDF");

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerButtons.add(printBtn);
        headerButtons.add(refreshBtn);

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(headerButtons, BorderLayout.EAST);

        JPanel summaryPanel = createSummaryPanel();

        JPanel tablesPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        tablesPanel.add(wrapInTitledPane("Top Selling Materials", buildTopMaterialsTable()));
        tablesPanel.add(wrapInTitledPane("Revenue by Order Status", buildRevenueByStatusTable()));
        tablesPanel.add(wrapInTitledPane("Top Customers", buildTopCustomersTable()));
        tablesPanel.add(wrapInTitledPane("Low Stock Alert (\u2264 10 units)", buildLowStockTable()));

        JPanel northContainer = new JPanel(new BorderLayout(0, 10));
        northContainer.add(headerPanel, BorderLayout.NORTH);
        northContainer.add(summaryPanel, BorderLayout.SOUTH);

        add(northContainer, BorderLayout.NORTH);
        add(tablesPanel, BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> refreshAll());
        printBtn.addActionListener(e -> exportReportAsPdf());
        refreshAll();
    }

    // ==========================================
    // SUMMARY CARDS
    // ==========================================
    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 10, 10));

        totalOrdersValue   = new JLabel("0");
        totalRevenueValue  = new JLabel("RM 0.00");
        totalTaxValue      = new JLabel("RM 0.00");
        pendingValue       = new JLabel("0");
        completedValue     = new JLabel("0");

        panel.add(createCard("Total Orders", totalOrdersValue, new Color(52, 73, 94)));
        panel.add(createCard("Total Revenue", totalRevenueValue, new Color(39, 174, 96)));
        panel.add(createCard("Total Tax Collected", totalTaxValue, new Color(41, 128, 185)));
        panel.add(createCard("Pending Orders", pendingValue, new Color(243, 156, 18)));
        panel.add(createCard("Completed Orders", completedValue, new Color(155, 89, 182)));

        return panel;
    }

    private JPanel createCard(String label, JLabel valueLabel, Color color) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(color);
        card.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel titleLabel = new JLabel(label);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        titleLabel.setForeground(Color.WHITE);

        valueLabel.setFont(new Font("Arial", Font.BOLD, 22));
        valueLabel.setForeground(Color.WHITE);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(valueLabel);
        return card;
    }

    // ==========================================
    // TABLE BUILDERS (each returns an empty JTable; data filled by refreshAll())
    // ==========================================
    private DefaultTableModel topMaterialsModel, revenueByStatusModel, topCustomersModel, lowStockModel;

    private JTable buildTopMaterialsTable() {
        topMaterialsModel = new DefaultTableModel(new String[]{"Material", "Qty Sold", "Revenue (RM)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        return styledTable(topMaterialsModel);
    }

    private JTable buildRevenueByStatusTable() {
        revenueByStatusModel = new DefaultTableModel(new String[]{"Status", "Orders", "Revenue (RM)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        return styledTable(revenueByStatusModel);
    }

    private JTable buildTopCustomersTable() {
        topCustomersModel = new DefaultTableModel(new String[]{"Customer", "Orders", "Total Spent (RM)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        return styledTable(topCustomersModel);
    }

    private JTable buildLowStockTable() {
        lowStockModel = new DefaultTableModel(new String[]{"Material", "Category", "Company Stock"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        return styledTable(lowStockModel);
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        return table;
    }

    private JPanel wrapInTitledPane(String title, JTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // ==========================================
    // REFRESH ALL DATA
    // ==========================================
    private void refreshAll() {
        SummaryStats stats = reportController.getSummaryStats();
        totalOrdersValue.setText(String.valueOf(stats.totalOrders));
        totalRevenueValue.setText(String.format("RM %.2f", stats.totalRevenue));
        totalTaxValue.setText(String.format("RM %.2f", stats.totalTax));
        pendingValue.setText(String.valueOf(stats.pendingOrders));
        completedValue.setText(String.valueOf(stats.completedOrders));

        topMaterialsModel.setRowCount(0);
        for (Object[] row : reportController.getTopSellingMaterials(10)) {
            topMaterialsModel.addRow(row);
        }

        revenueByStatusModel.setRowCount(0);
        for (Object[] row : reportController.getRevenueByStatus()) {
            revenueByStatusModel.addRow(row);
        }

        topCustomersModel.setRowCount(0);
        for (Object[] row : reportController.getTopCustomers(10)) {
            topCustomersModel.addRow(row);
        }

        lowStockModel.setRowCount(0);
        List<Object[]> lowStock = reportController.getLowStockMaterials(10);
        for (Object[] row : lowStock) {
            lowStockModel.addRow(row);
        }
    }

    // ==========================================
    // EXPORT REPORT AS PDF
    // ==========================================
    private void exportReportAsPdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Report as PDF");
        chooser.setSelectedFile(new File("Sales_Inventory_Report.pdf"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Document (*.pdf)", "pdf"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new File(file.getParentFile(), file.getName() + ".pdf");
        }

        try {
            SimplePdfExporter.exportComponentAsPdf(this, file);
            int openNow = JOptionPane.showConfirmDialog(this,
                    "Report saved to:\n" + file.getAbsolutePath() + "\n\nOpen it now?",
                    "Export Successful", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (openNow == JOptionPane.YES_OPTION) {
                try {
                    Desktop.getDesktop().open(file);
                } catch (Exception openEx) {
                    JOptionPane.showMessageDialog(this,
                            "Could not open the file automatically. You can find it at:\n" + file.getAbsolutePath(),
                            "Note", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to export report as PDF: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}