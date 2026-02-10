import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

// UNIT 3: Distributed Objects - This is the actual object instance
public class ParkingServiceImpl extends UnicastRemoteObject implements ParkingService {

    // Thread-safe map for slot management (Consistency)
    private Map<String, String> parkingSlots;
    private static final String SECURITY_TOKEN = "DIST-PARK-SECURE-2024";

    // Constructor must throw RemoteException because of UnicastRemoteObject
    public ParkingServiceImpl() throws RemoteException {
        super();
        parkingSlots = new ConcurrentHashMap<>();

        // Initializing slots
        parkingSlots.put("A1", "Available");
        parkingSlots.put("A2", "Available");
        parkingSlots.put("B1", "Available");
        parkingSlots.put("B2", "Available");
        parkingSlots.put("C1", "Available");
    }

    @Override
    public Map<String, String> getAllSlots() throws RemoteException {
        return parkingSlots;
    }

    // UNIT 2: Mutual Exclusion (synchronized keyword)
    @Override
    public synchronized String bookSlot(String slotId, String authToken) throws RemoteException {
        // Authentication Check
        if (!SECURITY_TOKEN.equals(authToken)) {
            System.out.println("[ALERT] Unauthorized access attempt blocked.");
            return "ERROR: Invalid Authentication Token";
        }

        if (!parkingSlots.containsKey(slotId)) {
            return "ERROR: Slot ID not found";
        }

        String currentStatus = parkingSlots.get(slotId);

        // Check availability
        if ("Available".equals(currentStatus)) {
            parkingSlots.put(slotId, "Occupied");
            System.out.println("[LOG] Slot " + slotId + " successfully booked.");
            return "SUCCESS: Slot " + slotId + " reserved.";
        } else {
            System.out.println("[LOG] Conflict detected for slot " + slotId);
            return "CONFLICT: Slot " + slotId + " is already occupied.";
        }
    }

    @Override
    public boolean isAlive() throws RemoteException {
        return true;
    }
}