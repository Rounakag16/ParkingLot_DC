import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

// Unit 3: Service Description (Interface Definition)
public interface ParkingService extends Remote {

    // Unit 2: Request-Reply Protocol (Get Status)
    Map<String, String> getAllSlots() throws RemoteException;

    // Unit 2: RMI with Mutual Exclusion logic
    // Returns a message: "Success", "Already Booked", or "Auth Failed"
    String bookSlot(String slotId, String authToken) throws RemoteException;

    // Unit 3: Distributed Object Health Check
    boolean isAlive() throws RemoteException;
}