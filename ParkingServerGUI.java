import javax.swing.*;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ParkingServerGUI {

    private static JTextArea logArea;

    public static void main(String[] args) {
        // Configuration Dialog
        JTextField myIpField = new JTextField("10.85.228.240");
        JTextField partnerIpField = new JTextField(""); // Optional by default
        String[] roles = { "PRIMARY (Master)", "BACKUP (Replica)" };
        JComboBox<String> roleBox = new JComboBox<>(roles);

        Object[] message = {
                "My ZeroTier IP:", myIpField,
                "Partner/Backup IP (Optional):", partnerIpField,
                "Server Role:", roleBox
        };

        int option = JOptionPane.showConfirmDialog(null, message, "Server Config", JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION)
            System.exit(0);

        boolean isPrimary = roleBox.getSelectedIndex() == 0;
        String myIp = myIpField.getText();
        String backupIp = partnerIpField.getText();

        setupDashboard(isPrimary, myIp);
        startServer(myIp, backupIp, isPrimary);
    }

    private static void startServer(String myIp, String backupIp, boolean isPrimary) {
        try {
            System.setProperty("java.rmi.server.hostname", myIp);

            ParkingServiceImpl service = new ParkingServiceImpl(
                    msg -> SwingUtilities.invokeLater(() -> logArea.append(msg + "\n")),
                    isPrimary,
                    backupIp);

            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("ParkingService", service);

            log(">>> SERVER STARTED: " + (isPrimary ? "PRIMARY" : "BACKUP"));
            log(">>> IP: " + myIp);
            if (backupIp == null || backupIp.isEmpty()) {
                log(">>> Mode: STANDALONE (No backup configured)");
            } else {
                log(">>> Replication Target: " + backupIp);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Startup Error: " + e.getMessage());
        }
    }

    private static void setupDashboard(boolean isPrimary, String ip) {
        JFrame frame = new JFrame("Server [" + (isPrimary ? "PRIMARY" : "BACKUP") + "] - " + ip);
        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(20, 20, 20));
        logArea.setForeground(isPrimary ? Color.GREEN : Color.CYAN);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        frame.add(new JScrollPane(logArea));
        frame.setVisible(true);
    }

    private static void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }
}