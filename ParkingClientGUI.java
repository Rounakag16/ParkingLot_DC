import javax.swing.*;
import java.awt.*;
import java.rmi.Naming;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ParkingClientGUI {

    private static ParkingInterface service;
    private static final String AUTH_TOKEN = "DIST-PARK-SECURE-2024";
    private static String currentZone = "ZoneA";
    private static JFrame frame;
    private static JPanel slotPanel;

    public static void main(String[] args) {
        // *** IMPORTANT: SERVER ZERO TIER IP HERE ***
        String serverIP = "10.85.228.240";

        try {
            String url = "rmi://" + serverIP + ":1099/ParkingService";
            service = (ParkingInterface) Naming.lookup(url);
            createGUI();
            startLiveUpdates();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Cannot connect to Server!\nCheck IP or Network.");
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static void createGUI() {
        frame = new JFrame("Smart Parking Client");
        frame.setSize(600, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Top Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(50, 50, 150));
        JLabel title = new JLabel("Smart Parking System");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 18));

        String[] zones = { "ZoneA", "ZoneB" };
        JComboBox<String> zoneSelector = new JComboBox<>(zones);
        zoneSelector.addActionListener(e -> {
            currentZone = (String) zoneSelector.getSelectedItem();
            refreshSlots();
        });

        headerPanel.add(title);
        headerPanel.add(new JLabel("   |   Zone: "));
        headerPanel.add(zoneSelector);
        frame.add(headerPanel, BorderLayout.NORTH);

        // Center Panel (Slots)
        slotPanel = new JPanel();
        slotPanel.setLayout(new GridLayout(2, 3, 10, 10));
        slotPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        frame.add(slotPanel, BorderLayout.CENTER);

        // Bottom Panel (Controls)
        JPanel controlPanel = new JPanel();
        JTextField vehicleField = new JTextField(10);
        JButton bookBtn = new JButton("Book");
        JButton releaseBtn = new JButton("Release");
        JButton coordBtn = new JButton("Find Free Slot");

        controlPanel.add(new JLabel("Vehicle No:"));
        controlPanel.add(vehicleField);
        controlPanel.add(bookBtn);
        controlPanel.add(releaseBtn);
        controlPanel.add(coordBtn);
        frame.add(controlPanel, BorderLayout.SOUTH);

        // Actions
        bookBtn.addActionListener(e -> {
            String slot = JOptionPane.showInputDialog("Enter Slot ID (e.g., A1):");
            String vehicle = vehicleField.getText();
            if (slot != null && !vehicle.isEmpty()) {
                try {
                    String res = service.bookSlot(currentZone, slot, vehicle, AUTH_TOKEN);
                    JOptionPane.showMessageDialog(frame, res);
                    refreshSlots();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Enter Vehicle Number!");
            }
        });

        releaseBtn.addActionListener(e -> {
            String slot = JOptionPane.showInputDialog("Enter Slot ID to Release:");
            if (slot != null) {
                try {
                    String res = service.releaseSlot(currentZone, slot, AUTH_TOKEN);
                    JOptionPane.showMessageDialog(frame, res);
                    refreshSlots();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        coordBtn.addActionListener(e -> {
            try {
                String res = service.findAnyFreeSlot();
                JOptionPane.showMessageDialog(frame, res);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        frame.setVisible(true);
        refreshSlots();
    }

    private static void refreshSlots() {
        try {
            Map<String, String> data = service.getZoneStatus(currentZone);
            slotPanel.removeAll();

            for (Map.Entry<String, String> entry : data.entrySet()) {
                String slotId = entry.getKey();
                String status = entry.getValue();

                JButton slotBtn = new JButton("<html><center>" + slotId + "<br/>" + status + "</center></html>");
                slotBtn.setFont(new Font("Arial", Font.BOLD, 12));

                if ("Available".equals(status)) {
                    slotBtn.setBackground(Color.GREEN);
                } else {
                    slotBtn.setBackground(Color.RED);
                    slotBtn.setForeground(Color.WHITE);
                }
                slotPanel.add(slotBtn);
            }
            slotPanel.revalidate();
            slotPanel.repaint();
        } catch (Exception e) {
            // Server down or network issue
        }
    }

    private static void startLiveUpdates() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshSlots();
            }
        }, 0, 3000);
    }
}