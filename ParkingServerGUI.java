import javax.swing.*;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ParkingServerGUI {

    private static JTextArea logArea;
    private static ParkingServiceImpl serviceInstance;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        JTextField myIpField = new JTextField("10.85.228.240");
        JTextField partnerIpField = new JTextField("");
        String[] roles = { "PRIMARY", "BACKUP" };
        JComboBox<String> roleBox = new JComboBox<>(roles);

        Object[] message = { "My IP:", myIpField, "Backup IP:", partnerIpField, "Role:", roleBox };
        if (JOptionPane.showConfirmDialog(null, message, "Server Init",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            System.exit(0);

        boolean isPrimary = roleBox.getSelectedIndex() == 0;
        setupGUI(isPrimary, myIpField.getText());
        startServer(myIpField.getText(), partnerIpField.getText(), isPrimary);
    }

    private static void setupGUI(boolean isPrimary, String ip) {
        JFrame frame = new JFrame("Server [" + (isPrimary ? "PRIMARY" : "BACKUP") + "] - " + ip);
        frame.setSize(600, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(0, 255, 100));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        frame.add(new JScrollPane(logArea), BorderLayout.CENTER);

        if (isPrimary) {
            JPanel adminPanel = new JPanel(new GridLayout(1, 2, 10, 10));
            adminPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JButton addZoneBtn = new JButton("Add New Zone");
            addZoneBtn.addActionListener(e -> {
                String name = JOptionPane.showInputDialog(frame, "Enter Zone Name (e.g., ZoneC):");
                if (name != null && !name.trim().isEmpty()) {
                    try {
                        String result = serviceInstance.addZone(name.trim());
                        JOptionPane.showMessageDialog(frame, result);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            JButton addSlotBtn = new JButton("Add Slot to Zone");
            addSlotBtn.addActionListener(e -> {
                String zName = JOptionPane.showInputDialog(frame, "Enter Target Zone Name (e.g., ZoneA):");
                if (zName == null)
                    return;

                String sName = JOptionPane.showInputDialog(frame, "Enter New Slot ID (e.g., A5):");
                if (sName == null)
                    return;

                try {
                    String result = serviceInstance.addSlot(zName.trim(), sName.trim());
                    if (result.startsWith("Error")) {
                        JOptionPane.showMessageDialog(frame, result, "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(frame, result, "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            adminPanel.add(addZoneBtn);
            adminPanel.add(addSlotBtn);
            frame.add(adminPanel, BorderLayout.SOUTH);
        }

        frame.setVisible(true);
    }

    private static void startServer(String myIp, String backupIp, boolean isPrimary) {
        try {
            System.setProperty("java.rmi.server.hostname", myIp);
            serviceInstance = new ParkingServiceImpl(
                    msg -> SwingUtilities.invokeLater(() -> logArea.append(msg + "\n")),
                    isPrimary, backupIp);
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("ParkingService", serviceInstance);
            logArea.append(">>> Server Running. Waiting for Clients...\n");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }
}