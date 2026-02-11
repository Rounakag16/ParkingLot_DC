import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SystemLogger {
    private static final String LOG_FILE = "verification_logs.txt";

    public static void log(String type, String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String entry = String.format("[%s] [%s] %s", timestamp, type, message);

        // Print to Console
        System.out.println(entry);

        // Write to File
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
                PrintWriter pw = new PrintWriter(fw)) {
            pw.println(entry);
        } catch (IOException e) {
            System.err.println("Logger Error: " + e.getMessage());
        }
    }
}