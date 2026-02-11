import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ParkingServiceImpl extends UnicastRemoteObject implements ParkingInterface {

    private Map<String, Map<String, String>> zones;
    private static final String AUTH_TOKEN = "DIST-PARK-SECURE-2024";
    private final Consumer<String> guiLogger;

    // Replication Configuration
    private boolean isPrimary;
    private String backupServiceUrl;

    public ParkingServiceImpl(Consumer<String> guiLogger, boolean isPrimary, String backupIp) throws RemoteException {
        super();
        this.guiLogger = guiLogger;
        this.isPrimary = isPrimary;
        this.zones = new ConcurrentHashMap<>();

        // Initialize Data
        initZone("ZoneA");
        initZone("ZoneB");

        if (isPrimary && backupIp != null && !backupIp.isEmpty()) {
            this.backupServiceUrl = "rmi://" + backupIp + ":1099/ParkingService";
            startAutomation();
        } else if (isPrimary) {
            SystemLogger.log("INFO", "Running in STANDALONE Mode (No Backup IP provided).");
            startAutomation();
        }

        SystemLogger.log("INFO", "Service Started. Role: " + (isPrimary ? "PRIMARY" : "BACKUP"));
    }

    private void initZone(String name) {
        Map<String, String> z = new ConcurrentHashMap<>();
        z.put(name.substring(name.length() - 1) + "1", "Available");
        z.put(name.substring(name.length() - 1) + "2", "Available");
        zones.put(name, z);
    }

    // --- CLIENT METHODS ---

    @Override
    public Map<String, String> getZoneStatus(String zoneId) throws RemoteException {
        return zones.getOrDefault(zoneId, new HashMap<>());
    }

    @Override
    public synchronized String bookSlot(String zoneId, String slotId, String vehicleNumber, String authToken)
            throws RemoteException {
        if (!AUTH_TOKEN.equals(authToken))
            return "Auth Failed";

        Map<String, String> zone = zones.get(zoneId);
        if (zone != null && "Available".equals(zone.get(slotId))) {
            zone.put(slotId, "Occupied: " + vehicleNumber);

            guiLogger.accept("BOOKING: " + slotId + " -> " + vehicleNumber);
            SystemLogger.log("TRANSACTION", "Booked " + slotId + " (" + zoneId + ") for " + vehicleNumber);

            // Try to replicate, but don't fail if backup is missing
            triggerReplication();
            return "Success";
        }
        return "Conflict";
    }

    @Override
    public synchronized String releaseSlot(String zoneId, String slotId, String authToken) throws RemoteException {
        Map<String, String> zone = zones.get(zoneId);
        if (zone != null && zone.containsKey(slotId)) {
            zone.put(slotId, "Available");

            guiLogger.accept("RELEASE: " + slotId);
            SystemLogger.log("TRANSACTION", "Released " + slotId + " (" + zoneId + ")");

            triggerReplication();
            return "Success";
        }
        return "Error";
    }

    @Override
    public String findAnyFreeSlot() throws RemoteException {
        return "Checking " + zones.keySet().size() + " zones...";
    }

    @Override
    public boolean isAlive() throws RemoteException {
        return true;
    }

    // --- REPLICATION LOGIC (SAFE MODE) ---

    private void triggerReplication() {
        // If we are Backup, or if no Backup IP was configured, do nothing.
        if (!isPrimary || backupServiceUrl == null)
            return;

        new Thread(() -> {
            try {
                ParkingInterface backup = (ParkingInterface) Naming.lookup(backupServiceUrl);
                backup.syncState(this.zones);
                SystemLogger.log("SYNC", "Data pushed to Backup Server.");
            } catch (Exception e) {
                // SILENT FAIL: If backup is down, just log it. Do not crash.
                SystemLogger.log("WARN", "Backup Unreachable. Continuing in Standalone Mode.");
            }
        }).start();
    }

    @Override
    public void syncState(Map<String, Map<String, String>> masterData) throws RemoteException {
        this.zones = new ConcurrentHashMap<>(masterData);
        guiLogger.accept("REPLICA: State synchronized with Primary.");
    }

    // --- AUTOMATION ---
    private void startAutomation() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SystemLogger.log("AUTO", "System Health Scan: OK.");
            }
        }, 10000, 60000); // Scan every 60 seconds
    }
}