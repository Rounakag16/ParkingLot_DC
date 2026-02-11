import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

public interface ParkingInterface extends Remote {
    // Client Methods
    Set<String> getZoneList() throws RemoteException;

    Map<String, String> getZoneStatus(String zoneId) throws RemoteException;

    String bookSlot(String zoneId, String slotId, String vehicleNumber, String authToken) throws RemoteException;

    String releaseSlot(String zoneId, String slotId, String authToken) throws RemoteException;

    String findAnyFreeSlot(String zoneId) throws RemoteException;

    // Admin Methods
    String addZone(String zoneName) throws RemoteException;

    String addSlot(String zoneName, String slotId) throws RemoteException;

    // --- HEARTBEAT METHOD ---
    boolean isAlive() throws RemoteException;

    // System Methods
    void syncState(Map<String, Map<String, String>> masterData) throws RemoteException;
}