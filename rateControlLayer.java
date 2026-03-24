import java.util.*;

/**
 * RateControlLayer – the rate-control layer.
 *
 * Responsible for:
 *   - Receiving tokens from vehicles
 *   - Rejecting tokens that are invalid, already used, or exceed the rate limit
 *   - Keeping statistics per time window
 */
public class rateControlLayer {

    // Tracks how many tokens have been accepted per time window
    // Key = window timestamp, Value = number of accepted tokens
    Map<Long, Integer> acceptedPerWindow = new HashMap<>();

    // Tracks how many tokens have been rejected per time window
    Map<Long, Integer> rejectedPerWindow = new HashMap<>();

    // Stores all vehicle IDs that attempted per window (used for the summary report)
    Map<Long, List<String>> vehiclesPerWindow = new HashMap<>();

    // Stores tokens we have already seen – prevents the same token from being used again (replay attack)
    Set<String> seenTokens = new HashSet<>();



    // submit – receives a token and decides whether to accept it.
    // Returns a string describing the result:
    //   "ACCEPTED", "INVALID_HASH_OR_STALE", "REPLAY_DETECTED", "RATE_LIMIT_EXCEEDED"

    public String submit(token token) {

        // Step 1: Check that the hash is valid and the token is not stale
        if (!cryptoLayer.verify(token)) {
            return "INVALID_HASH_OR_STALE";
        }

        // Step 2: Check that we have not seen this token before (replay attack)
        String tokenKey = token.vehicleId + ":" + token.timestamp + ":" + token.nonce;
        if (seenTokens.contains(tokenKey)) {
            return "REPLAY_DETECTED";
        }
        seenTokens.add(tokenKey);

        // Save which vehicle attempted (used in the statistics)
        long window = token.timestamp;
        vehiclesPerWindow.putIfAbsent(window, new ArrayList<>());
        vehiclesPerWindow.get(window).add(token.vehicleId);

        // Step 3: Check rate limit – max X tokens accepted per time window
        int accepted = acceptedPerWindow.getOrDefault(window, 0);
        if (accepted >= config.MAX_TOKENS_PER_WINDOW) {
            rejectedPerWindow.put(window, rejectedPerWindow.getOrDefault(window, 0) + 1);
            return "RATE_LIMIT_EXCEEDED";
        }

        // All checks passed – accept the token
        acceptedPerWindow.put(window, accepted + 1);
        return "ACCEPTED";
    }
}