package views;

import controllers.OrderController;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Admin "Orders" panel — view every sales order in the system,
 * drill into line items, and update an order's status.
 */
public class OrderManagementView extends JPanel {
    private final OrderController orderController = new OrderController();

    private JTable ordersTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> statusCombo;

    private static final String[] STATUS_OPTIONS = {
        "Pending", "Processing", "Completed", "Cancelled"
    };

    public OrderManagementView() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ==========================================
        // TOP: Search bar
        // ==========================================
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        JButton searchBtn = new JButton("Search");
        JButton refreshBtn = new JButton("Refresh All");

        topPanel.add(new JLabel("Search (Order ID / Customer / Status): "));
        topPanel.add(searchField);
        topPanel.add(searchBtn);
        topPanel.add(refreshBtn);

        // ==========================================
        // CENTER: Orders table
        // ==========================================
        String[] columns = {"Order ID", "Date", "Customer", "Subtotal (RM)", "Tax (RM)", "Grand Total (RM)", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        ordersTable = new JTable(tableModel);
        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ordersTable.setRowHeight(26);
        ordersTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        ordersTable.setFont(new Font("Arial", Font.PLAIN, 13));

        refreshTable("");

        // ==========================================
        // BOTTOM: Actions (view items / update status)
        // ==========================================
        JPanel actionPanel = new JPanel(new GridBagLayout());
        actionPanel.setBorder(BorderFactory.createTitledBorder("Order Actions"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JButton viewItemsBtn = new JButton("View Order Items");

        statusCombo = new JComboBox<>(STATUS_OPTIONS);
        JButton updateStatusBtn = new JButton("Update Status");
        updateStatusBtn.setBackground(new Color(39, 174, 96));
        updateStatusBtn.setForeground(Color.WHITE);

        gbc.gridx = 0; gbc.gridy = 0; actionPanel.add(viewItemsBtn, gbc);
        gbc.gridx = 1; actionPanel.add(new JLabel("Set Status:"), gbc);
        gbc.gridx = 2; actionPanel.add(statusCombo, gbc);
        gbc.gridx = 3; actionPanel.add(updateStatusBtn, gbc);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(ordersTable), BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);

        // ==========================================
        // ACTIONS
        // ==========================================
        ordersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && ordersTable.getSelectedRow() != -1) {
                int row = ordersTable.getSelectedRow();
                String currentStatus = tableModel.getValueAt(row, 6).toString();
                statusCombo.setSelectedItem(currentStatus);
            }
        });

        searchBtn.addActionListener(e -> refreshTable(searchField.getText().trim()));
        refreshBtn.addActionListener(e -> { searchField.setText(""); refreshTable(""); });

        viewItemsBtn.addActionListener(e -> showOrderItemsDialog());

        updateStatusBtn.addActionListener(e -> {
            int row = ordersTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select an order from the table first.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int orderId = (int) tableModel.getValueAt(row, 0);
            String newStatus = (String) statusCombo.getSelectedItem();

            if (orderController.updateOrderStatus(orderId, newStatus)) {
                JOptionPane.showMessageDialog(this, "Order #" + orderId + " status updated to '" + newStatus + "'.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable(searchField.getText().trim());
            } else {
                JOptionPane.showMessageDialog(this, "Database Error: Could not update order status.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void showOrderItemsDialog() {
        int row = ordersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order from the table first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int orderId = (int) tableModel.getValueAt(row, 0);
        String customer = tableModel.getValueAt(row, 2).toString();

        List<Object[]> items = orderController.getOrderItems(orderId);

        String[] itemColumns = {"Material", "Quantity", "Unit Price (RM)", "Line Total (RM)"};
        DefaultTableModel itemModel = new DefaultTableModel(itemColumns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Object[] item : items) {
            itemModel.addRow(item);
        }

        JTable itemTable = new JTable(itemModel);
        itemTable.setRowHeight(24);
        itemTable.setFont(new Font("Arial", Font.PLAIN, 13));
        itemTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));

        JScrollPane scrollPane = new JScrollPane(itemTable);
        scrollPane.setPreferredSize(new Dimension(500, 200));

        JOptionPane.showMessageDialog(this, scrollPane,
            "Items in Order #" + orderId + " — " + customer,
            JOptionPane.PLAIN_MESSAGE);
    }

    private void refreshTable(String keyword) {
        tableModel.setRowCount(0);
        List<Object[]> orders = keyword.isEmpty()
            ? orderController.getAllOrdersForAdmin()
            : orderController.searchOrdersForAdmin(keyword);
        for (Object[] row : orders) {
            tableModel.addRow(row);
        }
    }
}
