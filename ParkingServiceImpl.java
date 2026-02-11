import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ParkingServiceImpl extends UnicastRemoteObject implements ParkingInterface {

    private Map<String, Map<String, String>> zones;
    private static final String AUTH_TOKEN = "DIST-PARK-SECURE-2024";
    private final Consumer<String> guiLogger;
    private boolean isPrimary;
    private String backupServiceUrl;

    public ParkingServiceImpl(Consumer<String> guiLogger, boolean isPrimary, String backupIp) throws RemoteException {
        super();
        this.guiLogger = guiLogger;
        this.isPrimary = isPrimary;
        this.zones = new ConcurrentHashMap<>();

        // Initialize 4 Slots per Zone
        initZone("ZoneA", "A", 4);
        initZone("ZoneB", "B", 4);

        if (isPrimary && backupIp != null && !backupIp.isEmpty()) {
            this.backupServiceUrl = "rmi://" + backupIp + ":1099/ParkingService";
        }
        SystemLogger.log("INFO", "Service Started. Role: " + (isPrimary ? "PRIMARY" : "BACKUP"));
    }

    private void initZone(String zoneName, String prefix, int count) {
        Map<String, String> z = new ConcurrentHashMap<>();
        for (int i = 1; i <= count; i++)
            z.put(prefix + i, "Available");
        zones.put(zoneName, z);
    }

    // --- HEARTBEAT IMPLEMENTATION ---
    @Override
    public boolean isAlive() throws RemoteException {
        // Simple ping response to prove server is running
        return true;
    }

    // --- ADMIN LOGIC ---
    @Override
    public synchronized String addZone(String zoneName) throws RemoteException {
        if (zones.containsKey(zoneName))
            return "Error: Zone already exists.";
        zones.put(zoneName, new ConcurrentHashMap<>());
        guiLogger.accept("ADMIN: Added " + zoneName);
        SystemLogger.log("ADMIN", "Zone Created: " + zoneName);
        triggerReplication();
        return "Success: " + zoneName + " created.";
    }

    @Override
    public synchronized String addSlot(String zoneName, String slotId) throws RemoteException {
        if (!zones.containsKey(zoneName))
            return "Error: Zone '" + zoneName + "' does not exist!";
        if (zones.get(zoneName).containsKey(slotId))
            return "Error: Slot " + slotId + " already exists.";

        zones.get(zoneName).put(slotId, "Available");
        guiLogger.accept("ADMIN: Added Slot " + slotId + " to " + zoneName);
        SystemLogger.log("ADMIN", "Slot Added: " + slotId);
        triggerReplication();
        return "Success: Slot " + slotId + " added.";
    }

    // --- CLIENT LOGIC ---
    @Override
    public String findAnyFreeSlot(String zoneId) throws RemoteException {
        Map<String, String> z = zones.get(zoneId);
        if (z == null)
            return "Invalid Zone";
        List<String> sortedSlots = new ArrayList<>(z.keySet());
        sortedSlots.sort(Comparator.comparingInt(String::length).thenComparing(String::compareTo));
        for (String slotId : sortedSlots) {
            if ("Available".equals(z.get(slotId)))
                return slotId;
        }
        return "None";
    }

    @Override
    public synchronized String bookSlot(String zoneId, String slotId, String vehicleNumber, String authToken)
            throws RemoteException {
        if (!AUTH_TOKEN.equals(authToken))
            return "Auth Failed";
        Map<String, String> zone = zones.get(zoneId);
        if (zone != null && "Available".equals(zone.get(slotId))) {
            zone.put(slotId, "Occupied: " + vehicleNumber);
            guiLogger.accept("BOOKING: [" + zoneId + "] " + slotId + " -> " + vehicleNumber);
            SystemLogger.log("TRANSACTION", "Booked " + slotId + " (" + zoneId + ") for " + vehicleNumber);
            triggerReplication();
            return "Success";
        }
        return "Conflict: Slot occupied or invalid.";
    }

    @Override
    public synchronized String releaseSlot(String zoneId, String slotId, String authToken) throws RemoteException {
        Map<String, String> zone = zones.get(zoneId);
        if (zone != null && zone.containsKey(slotId)) {
            zone.put(slotId, "Available");
            guiLogger.accept("RELEASE: [" + zoneId + "] " + slotId);
            SystemLogger.log("TRANSACTION", "Released " + slotId + " (" + zoneId + ")");
            triggerReplication();
            return "Success";
        }
        return "Error";
    }

    // --- SYSTEM METHODS ---
    @Override
    public Set<String> getZoneList() throws RemoteException {
        return zones.keySet();
    }

    @Override
    public Map<String, String> getZoneStatus(String zoneId) throws RemoteException {
        return zones.getOrDefault(zoneId, new HashMap<>());
    }

    private void triggerReplication() {
        if (!isPrimary || backupServiceUrl == null)
            return;
        new Thread(() -> {
            try {
                ParkingInterface backup = (ParkingInterface) Naming.lookup(backupServiceUrl);
                backup.syncState(this.zones);
            } catch (Exception e) {
                /* Ignore */ }
        }).start();
    }

    @Override
    public void syncState(Map<String, Map<String, String>> masterData) throws RemoteException {
        this.zones = new ConcurrentHashMap<>(masterData);
        guiLogger.accept("REPLICA: Synced with Primary.");
    }
}