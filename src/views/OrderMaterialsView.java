package views;

import controllers.MaterialController;
import controllers.OrderController;
import models.Order;
import models.OrderItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * OrderMaterialsView
 *
 * Tabs:
 *   1. Browse & Add to Cart  – customer browses company inventory, picks qty, adds to cart
 *   2. Shopping Cart         – review items, see live cost calculation, place order
 */
public class OrderMaterialsView extends JPanel {

    // ── dependencies ──────────────────────────────────────────────────────────
    private final int customerId;
    private final MaterialController materialCtrl = new MaterialController();
    private final OrderController    orderCtrl    = new OrderController();

    // ── Browse tab ────────────────────────────────────────────────────────────
    private JTextField      searchField;
    private JTable          catalogTable;
    private DefaultTableModel catalogModel;

    // ── Cart tab ──────────────────────────────────────────────────────────────
    private JTable          cartTable;
    private DefaultTableModel cartModel;

    private JLabel lblSubtotal   = new JLabel("Subtotal:  RM 0.00");
    private JLabel lblTax        = new JLabel("Tax (6%): RM 0.00");
    private JLabel lblTotal      = new JLabel("Grand Total:  RM 0.00");

    // Cart is a list of { materialId, name, qty, unitPrice, lineTotal }
    private final List<Object[]> cartItems = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────

    public OrderMaterialsView(int customerId) {
        this.customerId = customerId;
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Browse & Add to Cart", createBrowsePanel());
        tabs.addTab("Shopping Cart",        createCartPanel());

        add(tabs, BorderLayout.CENTER);
    }

    // =========================================================================
    // TAB 1 – BROWSE & ADD TO CART
    // =========================================================================
    private JPanel createBrowsePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ── title ─────────────────────────────────────────────────────────────
        JLabel title = new JLabel("Available Materials");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(new Color(41, 128, 185));

        // ── search bar ────────────────────────────────────────────────────────
        searchField = new JTextField(20);
        JButton btnSearch  = new JButton("Search");
        JButton btnRefresh = new JButton("Show All");
        styleBtn(btnSearch,  new Color(41, 128, 185));
        styleBtn(btnRefresh, new Color(127, 140, 141));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBar.add(new JLabel("Search:"));
        searchBar.add(searchField);
        searchBar.add(btnSearch);
        searchBar.add(btnRefresh);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.add(title,     BorderLayout.WEST);
        topRow.add(searchBar, BorderLayout.EAST);

        // ── catalog table ─────────────────────────────────────────────────────
        String[] cols = {"ID", "Material Name", "Category", "Unit Price (RM)", "In Stock"};
        catalogModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        catalogTable = new JTable(catalogModel);
        catalogTable.setRowHeight(26);
        catalogTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        catalogTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        catalogTable.setFont(new Font("Arial", Font.PLAIN, 13));
        // hide the ID column from user but keep it for lookups
        catalogTable.getColumnModel().getColumn(0).setMinWidth(0);
        catalogTable.getColumnModel().getColumn(0).setMaxWidth(0);
        catalogTable.getColumnModel().getColumn(0).setWidth(0);

        // ── add-to-cart controls ──────────────────────────────────────────────
        JLabel lblQty = new JLabel("Quantity:");
        JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        qtySpinner.setPreferredSize(new Dimension(70, 28));

        JButton btnAddCart = new JButton("Add to Cart");
        styleBtn(btnAddCart, new Color(39, 174, 96));

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        bottomBar.add(lblQty);
        bottomBar.add(qtySpinner);
        bottomBar.add(btnAddCart);

        panel.add(topRow,                        BorderLayout.NORTH);
        panel.add(new JScrollPane(catalogTable), BorderLayout.CENTER);
        panel.add(bottomBar,                     BorderLayout.SOUTH);

        // ── actions ───────────────────────────────────────────────────────────
        btnSearch.addActionListener(e  -> loadCatalog(searchField.getText().trim()));
        btnRefresh.addActionListener(e -> { searchField.setText(""); loadCatalog(""); });
        searchField.addActionListener(e -> loadCatalog(searchField.getText().trim()));

        btnAddCart.addActionListener(e -> {
            int row = catalogTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this,
                    "Please select a material from the list first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int    matId    = (int)    catalogModel.getValueAt(row, 0);
            String matName  = (String) catalogModel.getValueAt(row, 1);
            double unitPrice = parseDouble(catalogModel.getValueAt(row, 3));
            int    inStock  = parseInt(catalogModel.getValueAt(row, 4));
            int    qty      = (int) qtySpinner.getValue();

            if (qty > inStock) {
                JOptionPane.showMessageDialog(this,
                    "Only " + inStock + " unit(s) available in stock.",
                    "Insufficient Stock", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // check if already in cart – if yes, update qty
            for (Object[] ci : cartItems) {
                if ((int) ci[0] == matId) {
                    int newQty = (int) ci[2] + qty;
                    if (newQty > inStock) {
                        JOptionPane.showMessageDialog(this,
                            "Total quantity would exceed available stock (" + inStock + ").",
                            "Stock Limit", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    ci[2] = newQty;
                    ci[4] = orderCtrl.calculateLineTotal(newQty, unitPrice);
                    refreshCartTable();
                    JOptionPane.showMessageDialog(this,
                        matName + " quantity updated in cart.", "Cart Updated",
                        JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }

            double lineTotal = orderCtrl.calculateLineTotal(qty, unitPrice);
            cartItems.add(new Object[]{ matId, matName, qty, unitPrice, lineTotal });
            refreshCartTable();
            JOptionPane.showMessageDialog(this,
                matName + " added to cart!", "Added", JOptionPane.INFORMATION_MESSAGE);
        });

        loadCatalog("");
        return panel;
    }

    // =========================================================================
    // TAB 2 – SHOPPING CART
    // =========================================================================
    private JPanel createCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Your Shopping Cart");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(new Color(41, 128, 185));

        // ── cart table ────────────────────────────────────────────────────────
        String[] cols = {"Material Name", "Qty", "Unit Price (RM)", "Line Total (RM)"};
        cartModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        cartTable = new JTable(cartModel);
        cartTable.setRowHeight(26);
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cartTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        cartTable.setFont(new Font("Arial", Font.PLAIN, 13));

        // ── totals panel ──────────────────────────────────────────────────────
        Font totalFont = new Font("Arial", Font.BOLD, 14);
        lblSubtotal.setFont(totalFont);
        lblTax.setFont(totalFont);
        lblTotal.setFont(new Font("Arial", Font.BOLD, 16));
        lblTotal.setForeground(new Color(39, 174, 96));

        JPanel totalsPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        totalsPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        totalsPanel.add(lblSubtotal);
        totalsPanel.add(lblTax);
        totalsPanel.add(lblTotal);

        // ── buttons ───────────────────────────────────────────────────────────
        JButton btnRemove      = new JButton("Remove Selected");
        JButton btnClearCart   = new JButton("Clear Cart");
        JButton btnPlaceOrder  = new JButton("Place Order");

        styleBtn(btnRemove,     new Color(231, 76,  60));
        styleBtn(btnClearCart,  new Color(127, 140, 141));
        styleBtn(btnPlaceOrder, new Color(39,  174,  96));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        btnPanel.add(btnRemove);
        btnPanel.add(btnClearCart);
        btnPanel.add(btnPlaceOrder);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(totalsPanel, BorderLayout.CENTER);
        southPanel.add(btnPanel,    BorderLayout.SOUTH);

        panel.add(title,                       BorderLayout.NORTH);
        panel.add(new JScrollPane(cartTable),  BorderLayout.CENTER);
        panel.add(southPanel,                  BorderLayout.SOUTH);

        // ── actions ───────────────────────────────────────────────────────────
        btnRemove.addActionListener(e -> {
            int row = cartTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a cart item to remove.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            cartItems.remove(row);
            refreshCartTable();
        });

        btnClearCart.addActionListener(e -> {
            if (cartItems.isEmpty()) return;
            int confirm = JOptionPane.showConfirmDialog(this,
                "Clear all items from cart?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                cartItems.clear();
                refreshCartTable();
            }
        });

        btnPlaceOrder.addActionListener(e -> placeOrder());

        return panel;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void loadCatalog(String keyword) {
        catalogModel.setRowCount(0);
        // getCompanyInventory returns: materialId, name, category, unitPrice, sellingPrice, companyStock
        List<Object[]> rows = materialCtrl.getCompanyInventory(keyword);
        if (rows.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No materials found" + (keyword.isEmpty() ? "." : " for: " + keyword),
                "No Results", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        for (Object[] r : rows) {
            // columns shown: ID, name, category, sellingPrice (index 4), companyStock (index 5)
            catalogModel.addRow(new Object[]{
                r[0],                              // hidden ID
                r[1],                              // name
                r[2],                              // category
                String.format("%.2f", r[4]),       // selling price
                r[5]                               // company stock
            });
        }
    }

    private void refreshCartTable() {
        cartModel.setRowCount(0);
        double subtotal = 0;
        for (Object[] ci : cartItems) {
            double lineTotal = (double) ci[4];
            subtotal += lineTotal;
            cartModel.addRow(new Object[]{
                ci[1],
                ci[2],
                String.format("%.2f", ci[3]),
                String.format("%.2f", lineTotal)
            });
        }
        double tax   = orderCtrl.calculateTax(subtotal);
        double grand = orderCtrl.calculateGrandTotal(subtotal, tax, 0);

        lblSubtotal.setText(String.format("Subtotal:    RM %.2f", subtotal));
        lblTax.setText(String.format("Tax (6%):   RM %.2f", tax));
        lblTotal.setText(String.format("Grand Total: RM %.2f", grand));
    }

    private void placeOrder() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Your cart is empty. Please add materials before placing an order.",
                "Empty Cart", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Confirm order?\n" + lblTotal.getText(),
            "Place Order", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        // Build Order & OrderItems from cart
        double subtotal = 0;
        List<OrderItem> items = new ArrayList<>();
        for (Object[] ci : cartItems) {
            int    matId     = (int)    ci[0];
            int    qty       = (int)    ci[2];
            double lineTotal = (double) ci[4];
            subtotal += lineTotal;
            items.add(new OrderItem(matId, qty, lineTotal));
        }

        double tax   = orderCtrl.calculateTax(subtotal);
        double grand = orderCtrl.calculateGrandTotal(subtotal, tax, 0);

        Order order = new Order(customerId, subtotal, tax, grand, "Pending");
        boolean ok  = orderCtrl.placeOrder(order, items);

        if (ok) {
            cartItems.clear();
            refreshCartTable();
            JOptionPane.showMessageDialog(this,
                "Order placed successfully!\nYou can track it in 'My Orders'.",
                "Order Placed", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to place order. Please try again.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void styleBtn(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setFocusPainted(false);
    }

    private double parseDouble(Object val) {
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0; }
    }
    private int parseInt(Object val) {
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return 0; }
    }
}