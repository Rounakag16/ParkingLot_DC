import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

class ProductState {
    String status; // "NEW", "SCANNED", "FLAGGED"
    String lastLocation;
    long lastScanTime;

    public ProductState() {
        this.status = "NEW";
        this.lastLocation = "Factory (Chennai)"; // Default factory location
        this.lastScanTime = 0;
    }
}

public class CounterfeitServiceImpl extends UnicastRemoteObject implements CounterfeitInterface {

    private Map<String, ProductState> productDB;
    // Stats: [0]=Total, [1]=Authentic, [2]=Warnings, [3]=Fraud
    private int[] stats = { 0, 0, 0, 0 };
    private final Consumer<String[]> guiLogger;

    public CounterfeitServiceImpl(Consumer<String[]> guiLogger) throws RemoteException {
        super();
        this.guiLogger = guiLogger;
        this.productDB = new ConcurrentHashMap<>();

        // FIXED: Initial Seed is now PROD1, PROD2, PROD3, PROD4, PROD5
        registerProductBatch("PROD", 5);
    }

    @Override
    public synchronized String verifyProduct(String productID, String location) throws RemoteException {
        stats[0]++; // Increment Total Scans

        // 1. Invalid ID Check (If ID is not PROD1-5, it's Fraud)
        if (!productDB.containsKey(productID)) {
            stats[3]++; // Fraud
            logToTable(productID, location, "INVALID ID", "CRITICAL");
            return "FRAUD: INVALID ID";
        }

        ProductState state = productDB.get(productID);

        // 2. First Scan (Authentic)
        if ("NEW".equals(state.status)) {
            state.status = "SCANNED";
            state.lastLocation = location;
            state.lastScanTime = System.currentTimeMillis();
            stats[1]++; // Authentic
            logToTable(productID, location, "AUTHENTIC", "SUCCESS");
            return "AUTHENTIC";
        }

        // 3. Subsequent Scans (Double Scan or Clone)
        if ("SCANNED".equals(state.status) || "FLAGGED".equals(state.status)) {

            // Geolocation Fraud Check (Impossible Travel)
            if (!state.lastLocation.equals(location)) {
                state.status = "FLAGGED";
                stats[3]++; // Fraud
                String msg = "CLONED TAG (" + state.lastLocation + " -> " + location + ")";
                logToTable(productID, location, msg, "CRITICAL");
                return "FRAUD: " + msg;
            } else {
                stats[2]++; // Warning
                logToTable(productID, location, "DOUBLE SCAN", "WARNING");
                return "WARNING: ALREADY SCANNED";
            }
        }
        return "ERROR";
    }

    @Override
    public synchronized String registerProductBatch(String prefix, int quantity) throws RemoteException {
        int start = 1;
        // Find the next available number if prefix exists (simple logic)
        while (productDB.containsKey(prefix + start)) {
            start++;
        }

        for (int i = 0; i < quantity; i++) {
            productDB.put(prefix + (start + i), new ProductState());
        }
        return "Registered " + quantity + " codes: " + prefix + start + " to " + prefix + (start + quantity - 1);
    }

    @Override
    public int[] getDashboardStats() throws RemoteException {
        return stats;
    }

    private void logToTable(String id, String loc, String status, String type) {
        String time = java.time.LocalTime.now().toString().substring(0, 8);
        guiLogger.accept(new String[] { time, id, loc, status, type });
    }
}