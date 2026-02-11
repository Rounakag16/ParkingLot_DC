import javax.swing.*;
import java.awt.*;
import java.rmi.Naming;
import java.util.*;
import java.util.Timer;

public class ParkingClientGUI {

    private static ParkingInterface service;
    private static final String AUTH_TOKEN = "DIST-PARK-SECURE-2024";
    private static String currentIP = "10.85.228.240";
    private static String backupIP = "";

    // UI Elements
    private static JFrame frame;
    private static JPanel slotPanel;
    private static JComboBox<String> zoneSelector;
    private static JLabel connectionStatusLabel;
    private static String currentZone = "ZoneA";

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        JTextField pIp = new JTextField(currentIP);
        JTextField bIp = new JTextField("");
        Object[] msg = { "Primary IP:", pIp, "Backup IP:", bIp };
        if (JOptionPane.showConfirmDialog(null, msg, "Client Setup",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            currentIP = pIp.getText();
            backupIP = bIp.getText();
            createGUI();
            startHeartbeatMechanism();
        } else {
            System.exit(0);
        }
    }

    private static void createGUI() {
        frame = new JFrame("Distributed Parking System");
        frame.setSize(850, 650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // --- HEADER ---
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        header.setBackground(new Color(45, 45, 45));

        JLabel title = new JLabel("PARKING CONTROL");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 18));

        zoneSelector = new JComboBox<>();
        zoneSelector.addItem("ZoneA");
        zoneSelector.setPreferredSize(new Dimension(150, 30));
        zoneSelector.addActionListener(e -> {
            if (zoneSelector.getSelectedItem() != null) {
                currentZone = (String) zoneSelector.getSelectedItem();
                refreshData();
            }
        });

        connectionStatusLabel = new JLabel(" ● Connecting... ");
        connectionStatusLabel.setOpaque(true);
        connectionStatusLabel.setBackground(Color.YELLOW);
        connectionStatusLabel.setForeground(Color.BLACK);
        connectionStatusLabel.setFont(new Font("Monospaced", Font.BOLD, 12));

        header.add(title);
        header.add(zoneSelector);
        header.add(Box.createHorizontalStrut(50));
        header.add(connectionStatusLabel);

        frame.add(header, BorderLayout.NORTH);

        // --- CENTER (THE GRID) ---
        slotPanel = new JPanel();
        slotPanel.setLayout(new GridLayout(0, 4, 15, 15));
        slotPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        slotPanel.setBackground(new Color(240, 240, 240));
        frame.add(new JScrollPane(slotPanel), BorderLayout.CENTER);

        // --- FOOTER ---
        JPanel footer = new JPanel();
        footer.setBackground(new Color(240, 240, 240));
        footer.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));

        JButton findBtn = new JButton("Find Best Free Slot");
        findBtn.setFont(new Font("Arial", Font.BOLD, 14));
        findBtn.setBackground(new Color(70, 130, 180));
        findBtn.setForeground(Color.WHITE);
        findBtn.setFocusPainted(false);
        findBtn.setBorderPainted(false);
        findBtn.setOpaque(true);
        findBtn.addActionListener(e -> findFreeSlot());

        footer.add(findBtn);
        frame.add(footer, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    // --- HEARTBEAT & CONNECTION LOGIC ---
    private static void startHeartbeatMechanism() {
        new Timer().schedule(new TimerTask() {
            public void run() {
                try {
                    if (service == null) {
                        service = (ParkingInterface) Naming.lookup("rmi://" + currentIP + ":1099/ParkingService");
                    }

                    boolean alive = service.isAlive();

                    if (alive) {
                        updateStatus(" ● ONLINE (" + currentIP + ") ", new Color(50, 205, 50), Color.WHITE);
                        // LOG SUCCESS
                        HeartbeatLogger.log("ALIVE", currentIP);
                        refreshData();
                    }
                } catch (Exception e) {
                    updateStatus(" ● OFFLINE - RETRYING... ", Color.RED, Color.WHITE);
                    // LOG FAILURE
                    HeartbeatLogger.log("DEAD/UNREACHABLE", currentIP);

                    service = null;
                    if (!backupIP.isEmpty()) {
                        currentIP = currentIP.equals(backupIP) ? backupIP : currentIP;
                        HeartbeatLogger.log("FAILOVER", "Switching to " + currentIP);
                    }
                }
            }
        }, 0, 2000); // Check every 2 seconds
    }

    private static void updateStatus(String text, Color bg, Color fg) {
        SwingUtilities.invokeLater(() -> {
            connectionStatusLabel.setText(text);
            connectionStatusLabel.setBackground(bg);
            connectionStatusLabel.setForeground(fg);
        });
    }

    private static void refreshData() {
        if (service == null)
            return;
        try {
            Set<String> zones = service.getZoneList();
            if (zones.size() != zoneSelector.getItemCount()) {
                String selected = (String) zoneSelector.getSelectedItem();
                zoneSelector.removeAllItems();
                new TreeSet<>(zones).forEach(z -> zoneSelector.addItem(z));
                zoneSelector.setSelectedItem(selected);
            }

            Map<String, String> data = service.getZoneStatus(currentZone);
            java.util.List<String> sortedKeys = new ArrayList<>(data.keySet());
            sortedKeys.sort(Comparator.comparingInt(String::length).thenComparing(String::compareTo));

            slotPanel.removeAll();

            for (String id : sortedKeys) {
                String status = data.get(id);
                boolean isFree = "Available".equals(status);

                JButton btn = new JButton();
                String html = "<html><center><h1 style='margin:0; padding:0;'>" + id + "</h1>" +
                        "<p style='font-size:9px;'>" + (isFree ? "FREE" : "BOOKED") + "</p></center></html>";
                btn.setText(html);
                btn.setOpaque(true);
                btn.setBorderPainted(false);
                btn.setFocusPainted(false);

                if (isFree) {
                    btn.setBackground(new Color(152, 251, 152));
                    btn.setForeground(new Color(0, 100, 0));
                    btn.addActionListener(e -> bookSlot(id));
                } else {
                    btn.setBackground(new Color(255, 105, 97));
                    btn.setForeground(Color.WHITE);
                    btn.setToolTipText(status);
                    btn.addActionListener(e -> releaseSlot(id));
                }
                slotPanel.add(btn);
            }
            slotPanel.revalidate();
            slotPanel.repaint();

        } catch (Exception e) {
            service = null;
        }
    }

    private static void bookSlot(String slotId) {
        String vehicle = JOptionPane.showInputDialog(frame, "Booking " + slotId + "\nEnter Vehicle Number:");
        if (vehicle != null && !vehicle.trim().isEmpty()) {
            try {
                String res = service.bookSlot(currentZone, slotId, vehicle, AUTH_TOKEN);
                JOptionPane.showMessageDialog(frame, res);
                refreshData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void releaseSlot(String slotId) {
        if (JOptionPane.showConfirmDialog(frame, "Release Slot " + slotId + "?", "Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                String res = service.releaseSlot(currentZone, slotId, AUTH_TOKEN);
                JOptionPane.showMessageDialog(frame, res);
                refreshData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void findFreeSlot() {
        try {
            String res = service.findAnyFreeSlot(currentZone);
            if ("None".equals(res))
                JOptionPane.showMessageDialog(frame, "No slots available in " + currentZone);
            else
                JOptionPane.showMessageDialog(frame, "Best Available Slot: " + res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}