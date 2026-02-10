import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ParkingServiceImpl extends UnicastRemoteObject implements ParkingInterface {

    // Data Store: Map<ZoneID, Map<SlotID, Status>>
    private final Map<String, Map<String, String>> zones;
    private static final String AUTH_TOKEN = "DIST-PARK-SECURE-2024";

    // Logger to send messages to the GUI
    private final Consumer<String> logger;

    public ParkingServiceImpl(Consumer<String> logger) throws RemoteException {
        super();
        this.logger = logger;
        zones = new ConcurrentHashMap<>();

        // Initialize Zone A
        Map<String, String> zoneA = new ConcurrentHashMap<>();
        zoneA.put("A1", "Available");
        zoneA.put("A2", "Available");
        zoneA.put("A3", "Available");
        zones.put("ZoneA", zoneA);

        // Initialize Zone B
        Map<String, String> zoneB = new ConcurrentHashMap<>();
        zoneB.put("B1", "Available");
        zoneB.put("B2", "Available");
        zoneB.put("B3", "Available");
        zones.put("ZoneB", zoneB);

        logger.accept(">>> Backend Initialized: Zones A & B created.");
    }

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
        if (zone == null)
            return "Invalid Zone";

        String currentStatus = zone.get(slotId);
        if ("Available".equals(currentStatus)) {
            zone.put(slotId, "Occupied: " + vehicleNumber);
            logger.accept("BOOKING: [" + zoneId + "] Slot " + slotId + " reserved for " + vehicleNumber);
            return "Success";
        }
        return "Conflict: Already Booked";
    }

    @Override
    public synchronized String releaseSlot(String zoneId, String slotId, String authToken) throws RemoteException {
        if (!AUTH_TOKEN.equals(authToken))
            return "Auth Failed";

        Map<String, String> zone = zones.get(zoneId);
        if (zone != null && zone.containsKey(slotId)) {
            zone.put(slotId, "Available");
            logger.accept("RELEASE: [" + zoneId + "] Slot " + slotId + " is now free.");
            return "Success";
        }
        return "Error";
    }

    @Override
    public String findAnyFreeSlot() throws RemoteException {
        logger.accept("QUERY: Searching all zones for free space...");
        for (String zKey : zones.keySet()) {
            for (Map.Entry<String, String> entry : zones.get(zKey).entrySet()) {
                if ("Available".equals(entry.getValue())) {
                    return "Suggestion: " + entry.getKey() + " in " + zKey;
                }
            }
        }
        return "System Full";
    }
}