import java.security.MessageDigest;

/**
 * CryptoLayer – the cryptographic layer (Proof-of-Work).
 *
 * Responsible for:
 *   - Computing SHA-256 hashes
 *   - Finding a nonce that produces a hash with the required number of leading zeros (mining)
 *   - Verifying that a token is valid
 */
public class cryptoLayer {

    // The required prefix the hash must start with, based on the configured difficulty
    private static final String PREFIX = "0".repeat(config.DIFFICULTY);

    // Returns the start time of the current time window
    public static long currentWindow() {
        long nowInSeconds = System.currentTimeMillis() / 1000;
        return (nowInSeconds / config.WINDOW_SECONDS) * config.WINDOW_SECONDS;
    }



    // Computes the SHA-256 hash of a message.
    // The message is built from: vehicleId + timestamp + nonce

    public static String computeHash(String vehicleId, long timestamp, int nonce) {
        String message = vehicleId + ":" + timestamp + ":" + nonce;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(message.getBytes());

            // Convert bytes to a hex string (e.g. "0000a3f7...")
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // Mining – searches for a nonce that produces the correct hash.
    // Tries nonce = 0, 1, 2, 3... until the hash starts with PREFIX.

    public static Object[] mine(String vehicleId) {
        long   timestamp = currentWindow();
        int    nonce     = 0;
        long   start     = System.currentTimeMillis();

        while (true) {
            String hash = computeHash(vehicleId, timestamp, nonce);

            if (hash.startsWith(PREFIX)) {
                double elapsed = (System.currentTimeMillis() - start) / 1000.0;
                token token = new token(vehicleId, timestamp, nonce, hash);
                return new Object[]{token, elapsed};
            }

            nonce++;
        }
    }


    // Verification – checks whether a token is valid.
    // Checks two things:
    //   - That the token belongs to the current time window (not stale)
    //   - That the hash is correct and starts with PREFIX

    public static boolean verify(token token) {
        // Check that the token belongs to the current time window
        if (token.timestamp != currentWindow()) {
            return false; // Token is stale
        }

        // Recompute the hash and compare it to the stored one
        String expected = computeHash(token.vehicleId, token.timestamp, token.nonce);
        return expected.equals(token.hashValue) && token.hashValue.startsWith(PREFIX);
    }
}