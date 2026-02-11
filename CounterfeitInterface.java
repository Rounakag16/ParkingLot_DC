import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface CounterfeitInterface extends Remote {
    // Verifies product and returns status string
    String verifyProduct(String productID, String location) throws RemoteException;

    // Admin: Registers a new batch of product codes
    String registerProductBatch(String prefix, int quantity) throws RemoteException;

    // Admin: Fetches stats for dashboard [Total, Authentic, Suspicious, Fraud]
    int[] getDashboardStats() throws RemoteException;
}