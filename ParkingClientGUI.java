import javax.swing.*;
import java.awt.*;
import java.rmi.Naming;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ParkingClientGUI {

    private static ParkingInterface service;
    private static final String AUTH_TOKEN = "DIST-PARK-SECURE-2024";

    // Failover Configuration
    private static String primaryIP = "10.85.228.240";
    private static String backupIP = ""; // Can be empty
    private static String currentIP;

    private static JLabel connectionStatus;
    private static JPanel slotPanel;
    private static JFrame frame;
    private static String currentZone = "ZoneA";

    public static void main(String[] args) {
        // UI for IP Configuration
        JTextField pIp = new JTextField(primaryIP);
        JTextField bIp = new JTextField("");
        Object[] msg = {
                "Primary Server IP:", pIp,
                "Backup Server IP (Optional):", bIp
        };

        int result = JOptionPane.showConfirmDialog(null, msg, "Client Config", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION)
            System.exit(0);

        primaryIP = pIp.getText();
        backupIP = bIp.getText();
        currentIP = primaryIP;

        createGUI();
        connectToServer();
        startHeartbeat();
    }

    private static void connectToServer() {
        try {
            String url = "rmi://" + currentIP + ":1099/ParkingService";
            service = (ParkingInterface) Naming.lookup(url);
            updateStatus("Connected to: " + currentIP, new Color(0, 150, 0)); // Dark Green
            refreshSlots();
        } catch (Exception e) {
            handleFailure();
        }
    }

    private static void handleFailure() {
        updateStatus("Connection Lost! Retrying...", Color.RED);

        // Only switch IP if a valid Backup IP exists
        if (backupIP != null && !backupIP.isEmpty()) {
            if (currentIP.equals(primaryIP))
                currentIP = backupIP;
            else
                currentIP = primaryIP;
            System.out.println(">>> FAILOVER: Switching to " + currentIP);
        } else {
            System.out.println(">>> RETRY: staying on " + currentIP);
        }

        try {
            Thread.sleep(1000);
            String url = "rmi://" + currentIP + ":1099/ParkingService";
            service = (ParkingInterface) Naming.lookup(url);
            updateStatus("Reconnected: " + currentIP, Color.ORANGE);
        } catch (Exception e) {
            updateStatus("SERVER DOWN / UNREACHABLE", Color.RED);
        }
    }

    private static void startHeartbeat() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (service != null)
                        service.isAlive(); // Heartbeat
                    refreshSlots();
                } catch (Exception e) {
                    handleFailure();
                }
            }
        }, 0, 2000);
    }

    // --- STANDARD GUI SETUP ---

    private static void createGUI() {
        frame = new JFrame("Distributed Client");
        frame.setSize(600, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        connectionStatus = new JLabel("Connecting...", SwingConstants.CENTER);
        connectionStatus.setOpaque(true);
        connectionStatus.setBackground(Color.BLACK);
        connectionStatus.setForeground(Color.WHITE);
        top.add(connectionStatus, BorderLayout.NORTH);

        String[] zones = { "ZoneA", "ZoneB" };
        JComboBox<String> box = new JComboBox<>(zones);
        box.addActionListener(e -> {
            currentZone = (String) box.getSelectedItem();
            refreshSlots();
        });

        JPanel zonePanel = new JPanel();
        zonePanel.add(new JLabel("Zone: "));
        zonePanel.add(box);
        top.add(zonePanel, BorderLayout.SOUTH);

        frame.add(top, BorderLayout.NORTH);

        slotPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        frame.add(slotPanel, BorderLayout.CENTER);

        JButton bookBtn = new JButton("Book Slot");
        bookBtn.setFont(new Font("Arial", Font.BOLD, 14));
        bookBtn.addActionListener(e -> performBooking());
        frame.add(bookBtn, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private static void performBooking() {
        String slot = JOptionPane.showInputDialog("Slot ID (e.g. A1):");
        if (slot == null)
            return;
        String veh = JOptionPane.showInputDialog("Vehicle No:");
        if (veh == null)
            return;

        try {
            if (service != null) {
                String res = service.bookSlot(currentZone, slot, veh, AUTH_TOKEN);
                JOptionPane.showMessageDialog(frame, res);
                refreshSlots();
            }
        } catch (Exception e) {
            handleFailure();
        }
    }

    private static void updateStatus(String text, Color bg) {
        SwingUtilities.invokeLater(() -> {
            connectionStatus.setText(text);
            connectionStatus.setBackground(bg);
        });
    }

    private static void refreshSlots() {
        try {
            if (service == null)
                return;
            Map<String, String> data = service.getZoneStatus(currentZone);
            slotPanel.removeAll();
            for (var entry : data.entrySet()) {
                JButton b = new JButton(
                        "<html><center>" + entry.getKey() + "<br>" + entry.getValue() + "</center></html>");
                b.setFont(new Font("Arial", Font.BOLD, 12));
                b.setBackground("Available".equals(entry.getValue()) ? Color.GREEN : Color.RED);
                if (!"Available".equals(entry.getValue()))
                    b.setForeground(Color.WHITE);
                slotPanel.add(b);
            }
            slotPanel.revalidate();
            slotPanel.repaint();
        } catch (Exception e) {
            /* Heartbeat handles logic */ }
    }
}