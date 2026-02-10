import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ParkingServer {

    public static void main(String[] args) {
        try {
            // *** IMPORTANT: Replace with your ACTUAL Server ZeroTier IP ***
            String zeroTierIP = "10.85.228.240";

            // Set the hostname property so RMI uses the ZeroTier network
            System.setProperty("java.rmi.server.hostname", zeroTierIP);

            // 1. Create the implementation object
            ParkingServiceImpl parkingService = new ParkingServiceImpl();

            // 2. Start the RMI Registry on port 1099
            Registry registry = LocateRegistry.createRegistry(1099);

            // 3. Bind the object to the registry with a unique name
            registry.rebind("SmartParkingService", parkingService);

            System.out.println(">>> Distributed Parking Server is Ready.");
            System.out.println(">>> Listening on: " + zeroTierIP + ":1099");
            System.out.println(">>> Press Ctrl+C to stop.");

        } catch (Exception e) {
            System.err.println("Server Exception: " + e.toString());
            e.printStackTrace();
        }
    }
}