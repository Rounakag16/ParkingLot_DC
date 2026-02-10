import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface ParkingInterface extends Remote {
    // Get status of a specific zone
    Map<String, String> getZoneStatus(String zoneId) throws RemoteException;

    // Book a slot
    String bookSlot(String zoneId, String slotId, String vehicleNumber, String authToken) throws RemoteException;

    // Release a slot
    String releaseSlot(String zoneId, String slotId, String authToken) throws RemoteException;

    // Coordination: Find a free slot anywhere
    String findAnyFreeSlot() throws RemoteException;
}