import javax.swing.*;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ParkingServerGUI {

    private static JTextArea logArea;

    public static void main(String[] args) {
        // 1. Setup GUI
        JFrame frame = new JFrame("Distributed Parking Server (Coordinator)");
        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(Color.GREEN);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(logArea);
        frame.add(scrollPane);
        frame.setVisible(true);

        // 2. Start RMI Server
        try {
            // *** IMPORTANT: YOUR ZERO TIER IP HERE ***
            String myIp = "10.85.228.240";
            System.setProperty("java.rmi.server.hostname", myIp);

            // Create Implementation and pass a method to write to the GUI
            ParkingServiceImpl service = new ParkingServiceImpl(msg -> log(msg));

            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("ParkingService", service);

            log(">>> Server Running on " + myIp);
            log(">>> Registry created on port 1099.");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Server Error: " + e.getMessage());
        }
    }

    // Helper to safely update GUI from background threads
    private static void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }
}