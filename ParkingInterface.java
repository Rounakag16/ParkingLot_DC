import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface ParkingInterface extends Remote {
    // Client Methods
    Map<String, String> getZoneStatus(String zoneId) throws RemoteException;

    String bookSlot(String zoneId, String slotId, String vehicleNumber, String authToken) throws RemoteException;

    String releaseSlot(String zoneId, String slotId, String authToken) throws RemoteException;

    String findAnyFreeSlot() throws RemoteException;

    // Heartbeat / Health Check
    boolean isAlive() throws RemoteException;

    // Replication Method (Primary calls this on Backup)
    void syncState(Map<String, Map<String, String>> masterData) throws RemoteException;
}