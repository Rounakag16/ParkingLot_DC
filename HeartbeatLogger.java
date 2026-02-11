import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HeartbeatLogger {
    private static final String LOG_FILE = "heartbeat_logs.txt";

    public static void log(String status, String ip) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String entry = String.format("[%s] [HEARTBEAT] Status: %s | Server: %s", timestamp, status, ip);

        // Print to Console (Optional, can comment out if too noisy)
        System.out.println(entry);

        // Write to separate file
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
                PrintWriter pw = new PrintWriter(fw)) {
            pw.println(entry);
        } catch (IOException e) {
            System.err.println("Heartbeat Logger Error: " + e.getMessage());
        }
    }
}