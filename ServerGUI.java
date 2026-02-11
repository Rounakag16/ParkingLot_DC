import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerGUI {

    private static DefaultTableModel tableModel;
    private static JLabel totalLbl, authLbl, fraudLbl;
    private static CounterfeitServiceImpl service;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        String myIp = JOptionPane.showInputDialog("Enter Server IP:", "10.85.228.240");
        if (myIp == null)
            System.exit(0);

        setupGUI(myIp);
        startServer(myIp);
    }

    private static void setupGUI(String ip) {
        JFrame frame = new JFrame("Central Verification Authority - India Node");
        frame.setSize(900, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // --- DASHBOARD HEADER ---
        JPanel dashPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        dashPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        dashPanel.setBackground(new Color(30, 30, 30));

        totalLbl = createStatCard("TOTAL SCANS", "0", Color.BLUE);
        authLbl = createStatCard("AUTHENTIC", "0", new Color(0, 153, 0));
        fraudLbl = createStatCard("FRAUD ALERTS", "0", Color.RED);

        dashPanel.add(totalLbl);
        dashPanel.add(authLbl);
        dashPanel.add(fraudLbl);
        frame.add(dashPanel, BorderLayout.NORTH);

        // --- LIVE LOG TABLE ---
        String[] columns = { "Time", "Product ID", "Location", "Status", "Risk Level" };
        tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        table.setRowHeight(25);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));

        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                String risk = (String) table.getModel().getValueAt(row, 4);
                if ("CRITICAL".equals(risk))
                    c.setForeground(Color.RED);
                else if ("WARNING".equals(risk))
                    c.setForeground(Color.ORANGE.darker());
                else
                    c.setForeground(new Color(0, 128, 0));
                return c;
            }
        });

        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        // --- CONTROL PANEL ---
        JPanel footer = new JPanel();
        JButton batchBtn = new JButton("Generate New Batch");
        batchBtn.setFont(new Font("Arial", Font.BOLD, 14));
        batchBtn.addActionListener(e -> {
            // FIXED: Default is now PROD
            String pre = JOptionPane.showInputDialog("Enter Batch Prefix (Default: PROD):", "PROD");
            if (pre != null && !pre.isEmpty()) {
                try {
                    String res = service.registerProductBatch(pre, 5);
                    JOptionPane.showMessageDialog(frame, res);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        footer.add(batchBtn);
        frame.add(footer, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private static JLabel createStatCard(String title, String val, Color c) {
        JLabel lbl = new JLabel("<html><center>" + title + "<br/><font size='6'>" + val + "</font></center></html>",
                SwingConstants.CENTER);
        lbl.setOpaque(true);
        lbl.setBackground(Color.WHITE);
        lbl.setForeground(c);
        lbl.setBorder(BorderFactory.createLineBorder(c, 2));
        lbl.setFont(new Font("Arial", Font.BOLD, 14));
        return lbl;
    }

    private static void updateDashboard(int[] stats) {
        totalLbl.setText("<html><center>TOTAL SCANS<br/><font size='6'>" + stats[0] + "</font></center></html>");
        authLbl.setText("<html><center>AUTHENTIC<br/><font size='6'>" + stats[1] + "</font></center></html>");
        fraudLbl.setText("<html><center>FRAUD ALERTS<br/><font size='6'>" + stats[3] + "</font></center></html>");
    }

    private static void startServer(String myIp) {
        try {
            System.setProperty("java.rmi.server.hostname", myIp);
            service = new CounterfeitServiceImpl(
                    row -> SwingUtilities.invokeLater(() -> {
                        tableModel.insertRow(0, row);
                        try {
                            updateDashboard(service.getDashboardStats());
                        } catch (Exception e) {
                        }
                    }));
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("CounterfeitService", service);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Server Error: " + e.getMessage());
        }
    }
}