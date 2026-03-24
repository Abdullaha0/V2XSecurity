/**
 * Token – represents a solved PoW puzzle.
 * Stores information about which vehicle solved it, when, and the result.
 */
public class token {

    String vehicleId;   // Which vehicle created the token
    long   timestamp;   // Start time of the time window (Unix time in seconds)
    int    nonce;       // The number we searched for to get the correct hash
    String hashValue;   // The SHA-256 hash we found

    token(String vehicleId, long timestamp, int nonce, String hashValue) {
        this.vehicleId = vehicleId;
        this.timestamp = timestamp;
        this.nonce     = nonce;
        this.hashValue = hashValue;
    }
}