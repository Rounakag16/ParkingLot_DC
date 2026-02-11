import javax.swing.*;
import java.awt.*;
import java.rmi.Naming;

public class ClientGUI {

    private static CounterfeitInterface service;
    private static String serverIP = "10.85.228.240";

    private static JFrame frame;
    private static JTextField idField;
    private static JComboBox<String> locationBox;
    private static JLabel statusIcon, statusText, statusSubText;
    private static JPanel resultPanel;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        String ip = JOptionPane.showInputDialog("Enter Server IP:", serverIP);
        if (ip != null)
            serverIP = ip;

        createGUI();
    }

    private static void createGUI() {
        frame = new JFrame("SecureScan India - Product Verifier");
        frame.setSize(450, 650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel();
        header.setBackground(new Color(0, 51, 102));
        JLabel title = new JLabel("SECURE SCAN INDIA");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        header.add(title);
        frame.add(header, BorderLayout.NORTH);

        // Content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        content.add(new JLabel("Select Location:"));
        String[] indLocs = {
                "Mumbai (Main Warehouse)", "Delhi (Retail Store A)",
                "Bangalore (Tech Park)", "Chennai (Distribution Hub)",
                "Kolkata (Dealer)", "Hyderabad (Mall)"
        };
        locationBox = new JComboBox<>(indLocs);
        locationBox.setMaximumSize(new Dimension(400, 30));
        content.add(locationBox);
        content.add(Box.createVerticalStrut(20));

        content.add(new JLabel("Scan Product ID (e.g. PROD1):"));
        idField = new JTextField("PROD1"); // FIXED: Default ID
        idField.setFont(new Font("Consolas", Font.BOLD, 24));
        idField.setHorizontalAlignment(JTextField.CENTER);
        idField.setMaximumSize(new Dimension(400, 50));
        content.add(idField);
        content.add(Box.createVerticalStrut(30));

        JButton scanBtn = new JButton("VERIFY AUTHENTICITY");
        scanBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        scanBtn.setFont(new Font("Arial", Font.BOLD, 16));
        scanBtn.setBackground(new Color(0, 102, 204));
        scanBtn.setForeground(Color.WHITE);
        scanBtn.setFocusPainted(false);
        scanBtn.setOpaque(true);
        scanBtn.setBorderPainted(false);
        scanBtn.addActionListener(e -> performScan());
        content.add(scanBtn);

        frame.add(content, BorderLayout.CENTER);

        // Result Panel
        resultPanel = new JPanel();
        resultPanel.setPreferredSize(new Dimension(450, 200));
        resultPanel.setLayout(new GridLayout(3, 1));
        resultPanel.setBackground(Color.LIGHT_GRAY);

        statusIcon = new JLabel("●", SwingConstants.CENTER);
        statusIcon.setFont(new Font("Arial", Font.BOLD, 60));
        statusIcon.setForeground(Color.GRAY);

        statusText = new JLabel("READY TO SCAN", SwingConstants.CENTER);
        statusText.setFont(new Font("Arial", Font.BOLD, 22));

        statusSubText = new JLabel("Enter ID to verify", SwingConstants.CENTER);
        statusSubText.setFont(new Font("Arial", Font.PLAIN, 14));

        resultPanel.add(statusIcon);
        resultPanel.add(statusText);
        resultPanel.add(statusSubText);
        frame.add(resultPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private static void performScan() {
        String pid = idField.getText().trim().toUpperCase();
        String loc = (String) locationBox.getSelectedItem();

        if (pid.isEmpty()) {
            updateUI("EMPTY", "Enter an ID", Color.GRAY);
            return;
        }

        try {
            if (service == null)
                service = (CounterfeitInterface) Naming.lookup("rmi://" + serverIP + ":1099/CounterfeitService");

            String result = service.verifyProduct(pid, loc);

            if (result.contains("AUTHENTIC")) {
                updateUI("✔", "AUTHENTIC", new Color(46, 204, 113));
            } else if (result.contains("WARNING")) {
                updateUI("⚠", "ALREADY SCANNED", new Color(241, 196, 15));
            } else {
                updateUI("✘", "FRAUD DETECTED", new Color(231, 76, 60));
            }
            statusSubText.setText(result.replace("FRAUD: ", "").replace("WARNING: ", ""));

        } catch (Exception e) {
            updateUI("!", "SERVER OFFLINE", Color.BLACK);
            service = null;
        }
    }

    private static void updateUI(String icon, String text, Color bg) {
        statusIcon.setText(icon);
        statusText.setText(text);
        resultPanel.setBackground(bg);
        Color txtColor = (bg == Color.BLACK || bg.getRed() > 200) ? Color.WHITE : Color.BLACK;
        statusIcon.setForeground(txtColor);
        statusText.setForeground(txtColor);
        statusSubText.setForeground(txtColor);
    }
}