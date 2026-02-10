import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Scanner;

public class ParkingClient {

    // Authentication Token must match the server
    private static final String AUTH_TOKEN = "DIST-PARK-SECURE-2024";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Replace with Server's ZeroTier IP
        String serverIP = "10.85.228.240";
        String rmiURL = "rmi://" + serverIP + ":1099/SmartParkingService";

        ParkingService service = null;

        // Objective: Fault Tolerance (Connection Retry Logic)
        try {
            System.out.println(">>> Attempting to connect to Server at " + serverIP + "...");
            service = (ParkingService) Naming.lookup(rmiURL);

            // Health Check
            if (service.isAlive()) {
                System.out.println(">>> Connected to Distributed Server successfully!");
            }
        } catch (Exception e) {
            System.err.println(">>> CRITICAL FAILURE: Cannot reach server.");
            System.err.println(">>> System entering Offline Mode (Fault Tolerance Triggered).");
            return;
        }

        while (true) {
            try {
                System.out.println("\n--- DISTRIBUTED PARKING ZONE ---");
                System.out.println("1. View Slot Status (Real-time)");
                System.out.println("2. Book a Slot");
                System.out.println("3. Exit");
                System.out.print("Enter choice: ");

                int choice = scanner.nextInt();

                if (choice == 1) {
                    // Unit 2: Request-Reply Protocol
                    Map<String, String> slots = service.getAllSlots();
                    System.out.println("\n--- Current Slot Availability ---");
                    for (Map.Entry<String, String> entry : slots.entrySet()) {
                        System.out.println("Slot " + entry.getKey() + ": " + entry.getValue());
                    }
                } else if (choice == 2) {
                    System.out.print("Enter Slot ID to book (e.g., A1): ");
                    String slotId = scanner.next();

                    // Unit 3: Remote Invocation with Auth
                    String result = service.bookSlot(slotId, AUTH_TOKEN);
                    System.out.println(">>> Server Response: " + result);
                } else if (choice == 3) {
                    break;
                }

            } catch (RemoteException re) {
                // Handling Node Failure during operation
                System.err.println(">>> Connection Lost! The Main Server might be down.");
                System.err.println(">>> Retrying connection...");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        scanner.close();
    }
}