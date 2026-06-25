package views;

import controllers.OrderController;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * MyOrdersView
 *
 * Tabs:
 *   1. Order History  – shows all past orders for this customer with status colour coding
 *   2. Order Details  – customer enters an Order ID and sees every line item + totals
 *   3. Cancel Order   – customer can cancel a Pending order
 */
public class MyOrdersView extends JPanel {

    private final int customerId;
    private final OrderController orderCtrl = new OrderController();

    // ── Order History tab ────────────────────────────────────────────────────
    private JTable           historyTable;
    private DefaultTableModel historyModel;
    private JTextField        searchOrderId;

    // ── Order Details tab ────────────────────────────────────────────────────
    private JTextField        detailsOrderId;
    private JTable            detailsTable;
    private DefaultTableModel detailsModel;
    private JLabel            lblDetailsSub   = new JLabel();
    private JLabel            lblDetailsTax   = new JLabel();
    private JLabel            lblDetailsTotal = new JLabel();
    private JLabel            lblDetailsStatus = new JLabel();

    // ── Cancel tab ───────────────────────────────────────────────────────────
    private JTextField        cancelOrderId;

    // ─────────────────────────────────────────────────────────────────────────

    public MyOrdersView(int customerId) {
        this.customerId = customerId;
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Order History",  createHistoryPanel());
        tabs.addTab("Order Details",  createDetailsPanel());
        tabs.addTab("Cancel Order",   createCancelPanel());

        add(tabs, BorderLayout.CENTER);
    }

    // =========================================================================
    // TAB 1 – ORDER HISTORY
    // =========================================================================
    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("My Order History");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(new Color(41, 128, 185));

        // optional filter by Order ID
        searchOrderId = new JTextField(10);
        JButton btnFilter  = new JButton("Filter by ID");
        JButton btnShowAll = new JButton("Show All");
        styleBtn(btnFilter,  new Color(41, 128, 185));
        styleBtn(btnShowAll, new Color(127, 140, 141));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBar.add(new JLabel("Order ID:"));
        searchBar.add(searchOrderId);
        searchBar.add(btnFilter);
        searchBar.add(btnShowAll);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.add(title,     BorderLayout.WEST);
        topRow.add(searchBar, BorderLayout.EAST);

        // table
        String[] cols = {"Order ID", "Order Date", "Subtotal (RM)", "Tax (RM)", "Grand Total (RM)", "Status"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        historyTable.setRowHeight(26);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        historyTable.setFont(new Font("Arial", Font.PLAIN, 13));

        // colour-code by status
        historyTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    String status = (String) t.getModel().getValueAt(row, 5);
                    switch (status) {
                        case "Pending":   c.setBackground(new Color(255, 243, 205)); break; // yellow
                        case "Completed": c.setBackground(new Color(212, 237, 218)); break; // green
                        case "Cancelled": c.setBackground(new Color(235, 235, 235)); c.setForeground(Color.GRAY); break;
                        default:          c.setBackground(Color.WHITE); c.setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        });

        // hint bar
        JLabel hint = new JLabel(" Green = Completed   |   Yellow = Pending   |   Grey = Cancelled");
        hint.setFont(new Font("Arial", Font.ITALIC, 11));
        hint.setForeground(Color.GRAY);

        panel.add(topRow,                         BorderLayout.NORTH);
        panel.add(new JScrollPane(historyTable),  BorderLayout.CENTER);
        panel.add(hint,                           BorderLayout.SOUTH);

        // actions
        btnShowAll.addActionListener(e -> { searchOrderId.setText(""); loadHistory(null); });
        btnFilter.addActionListener(e -> {
            String txt = searchOrderId.getText().trim();
            if (txt.isEmpty()) { loadHistory(null); return; }
            try { loadHistory(Integer.parseInt(txt)); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid Order ID number.",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });

        loadHistory(null);
        return panel;
    }

    // =========================================================================
    // TAB 2 – ORDER DETAILS
    // =========================================================================
    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("View Order Details");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(new Color(41, 128, 185));

        // lookup bar
        detailsOrderId = new JTextField(10);
        JButton btnLoad = new JButton("Load Details");
        styleBtn(btnLoad, new Color(41, 128, 185));

        JPanel lookupBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        lookupBar.add(new JLabel("Order ID:"));
        lookupBar.add(detailsOrderId);
        lookupBar.add(btnLoad);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.add(title,      BorderLayout.WEST);
        topRow.add(lookupBar,  BorderLayout.EAST);

        // items table
        String[] cols = {"Material Name", "Qty", "Unit Price (RM)", "Line Total (RM)"};
        detailsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        detailsTable = new JTable(detailsModel);
        detailsTable.setRowHeight(26);
        detailsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        detailsTable.setFont(new Font("Arial", Font.PLAIN, 13));

        // summary strip
        Font sf = new Font("Arial", Font.BOLD, 13);
        lblDetailsStatus.setFont(sf);
        lblDetailsSub.setFont(sf);
        lblDetailsTax.setFont(sf);
        lblDetailsTotal.setFont(new Font("Arial", Font.BOLD, 15));
        lblDetailsTotal.setForeground(new Color(39, 174, 96));

        JPanel summary = new JPanel(new GridLayout(4, 1, 4, 4));
        summary.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        summary.add(lblDetailsStatus);
        summary.add(lblDetailsSub);
        summary.add(lblDetailsTax);
        summary.add(lblDetailsTotal);

        panel.add(topRow,                        BorderLayout.NORTH);
        panel.add(new JScrollPane(detailsTable), BorderLayout.CENTER);
        panel.add(summary,                       BorderLayout.SOUTH);

        // action
        btnLoad.addActionListener(e -> {
            String txt = detailsOrderId.getText().trim();
            if (txt.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter an Order ID.", "Missing Input", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try { loadOrderDetails(Integer.parseInt(txt)); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid Order ID number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    // =========================================================================
    // TAB 3 – CANCEL ORDER
    // =========================================================================
    private JPanel createCancelPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Cancel an Order");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(new Color(231, 76, 60));

        cancelOrderId = new JTextField(15);

        JLabel note = new JLabel("<html><i>Only <b>Pending</b> orders can be cancelled.<br>"
                + "Once cancelled the action cannot be undone.</i></html>");
        note.setFont(new Font("Arial", Font.PLAIN, 12));
        note.setForeground(Color.GRAY);

        JButton btnCancel = new JButton("Cancel Order");
        styleBtn(btnCancel, new Color(231, 76, 60));

        int row = 0;

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(title, gbc);

        gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Order ID to Cancel:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(cancelOrderId, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(note, gbc);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        panel.add(btnCancel, gbc);

        btnCancel.addActionListener(e -> cancelOrder());

        return panel;
    }

    // =========================================================================
    // DATA LOADING
    // =========================================================================

    private void loadHistory(Integer filterOrderId) {
        historyModel.setRowCount(0);
        List<Object[]> orders = orderCtrl.getOrdersForCustomer(customerId);
        boolean any = false;
        for (Object[] o : orders) {
            int orderId = (int) o[0];
            if (filterOrderId != null && orderId != filterOrderId) continue;
            historyModel.addRow(new Object[]{
                o[0],                                       // order_id
                o[1],                                       // order_date
                String.format("%.2f", o[2]),                // subtotal
                String.format("%.2f", o[3]),                // tax_amount
                String.format("%.2f", o[4]),                // grand_total
                o[5]                                        // order_status
            });
            any = true;
        }
        if (!any) {
            JOptionPane.showMessageDialog(this,
                filterOrderId != null
                    ? "Order #" + filterOrderId + " not found in your history."
                    : "You have no orders yet.",
                "No Records", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void loadOrderDetails(int orderId) {
        // First, confirm this order belongs to this customer
        boolean belongs = false;
        double sub = 0, tax = 0, grand = 0;
        String status = "";

        for (Object[] o : orderCtrl.getOrdersForCustomer(customerId)) {
            if ((int) o[0] == orderId) {
                belongs = true;
                sub   = (double) o[2];
                tax   = (double) o[3];
                grand = (double) o[4];
                status = (String) o[5];
                break;
            }
        }

        if (!belongs) {
            JOptionPane.showMessageDialog(this,
                "Order #" + orderId + " was not found in your account.",
                "Not Found", JOptionPane.WARNING_MESSAGE);
            detailsModel.setRowCount(0);
            lblDetailsStatus.setText("");
            lblDetailsSub.setText("");
            lblDetailsTax.setText("");
            lblDetailsTotal.setText("");
            return;
        }

        detailsModel.setRowCount(0);
        List<Object[]> items = orderCtrl.getOrderItems(orderId);
        for (Object[] item : items) {
            detailsModel.addRow(new Object[]{
                item[0],                               // material name
                item[1],                               // quantity
                String.format("%.2f", item[2]),        // unit price
                String.format("%.2f", item[3])         // line total
            });
        }

        lblDetailsStatus.setText("Order #" + orderId + "  —  Status: " + status);
        lblDetailsSub.setText(String.format("Subtotal:    RM %.2f", sub));
        lblDetailsTax.setText(String.format("Tax (6%):   RM %.2f", tax));
        lblDetailsTotal.setText(String.format("Grand Total: RM %.2f", grand));
    }

    private void cancelOrder() {
        String txt = cancelOrderId.getText().trim();
        if (txt.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an Order ID.", "Missing Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int orderId;
        try { orderId = Integer.parseInt(txt); }
        catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid Order ID number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check it belongs to this customer and is Pending
        String status = null;
        for (Object[] o : orderCtrl.getOrdersForCustomer(customerId)) {
            if ((int) o[0] == orderId) {
                status = (String) o[5];
                break;
            }
        }

        if (status == null) {
            JOptionPane.showMessageDialog(this,
                "Order #" + orderId + " was not found in your account.",
                "Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!"Pending".equals(status)) {
            JOptionPane.showMessageDialog(this,
                "Only Pending orders can be cancelled.\nOrder #" + orderId + " is currently: " + status,
                "Cannot Cancel", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to cancel Order #" + orderId + "?\nThis cannot be undone.",
            "Confirm Cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        boolean ok = orderCtrl.updateOrderStatus(orderId, "Cancelled");
        if (ok) {
            cancelOrderId.setText("");
            JOptionPane.showMessageDialog(this,
                "Order #" + orderId + " has been cancelled.",
                "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            loadHistory(null); // refresh history tab too
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to cancel order. Please try again.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── style helper ─────────────────────────────────────────────────────────
    private void styleBtn(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setFocusPainted(false);
    }
}