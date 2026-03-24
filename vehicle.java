import java.util.*;

/**
 * Vehicle – represents a vehicle in the simulation.
 *
 * Each vehicle runs in its own thread and tries to create new tokens
 * during the simulation time. Sybil vehicles attempt much more frequently.
 */
public class vehicle extends Thread {

    String           vehicleId;
    rateControlLayer controller;
    boolean          isSybil;   // true = attacker, false = legitimate vehicle
    long             simEnd;

    // Statistics – saved for the summary report at the end
    List<String> reasons   = new ArrayList<>();  // Result of each attempt
    List<Double> mineTimes = new ArrayList<>();  // Time taken to mine each token
    int          accepted  = 0;
    int          rejected  = 0;

    vehicle(String vehicleId, rateControlLayer controller, boolean isSybil, long simEnd) {
        this.vehicleId  = vehicleId;
        this.controller = controller;
        this.isSybil    = isSybil;
        this.simEnd     = simEnd;
        this.setDaemon(true); // Thread exits automatically when the main thread ends
    }

    // run – called when the thread starts.
    // Loops and attempts to create tokens until the simulation is over.
    public void run() {
        // Sybil vehicles attempt 4x more often than legitimate vehicles
        int interval = isSybil ? config.TOKEN_INTERVAL / 4 : config.TOKEN_INTERVAL;
        if (interval < 1) interval = 1;

        while (System.currentTimeMillis() < simEnd) {

            // Solve the PoW puzzle and measure how long it takes
            Object[] result   = cryptoLayer.mine(vehicleId);
            token    token    = (token)  result[0];
            double   mineTime = (double) result[1];
            mineTimes.add(mineTime);

            // Submit the token to the rate controller
            String reason = controller.submit(token);
            reasons.add(reason);

            boolean ok = reason.equals("ACCEPTED");
            if (ok) accepted++;
            else    rejected++;

            // Print what happened
            String label = ok ? "[OK] ACCEPTED" : "[FAIL] " + reason;
            String vtype = isSybil ? "[SYBIL]" : "[legit]";
            System.out.printf("  %s %-12s  hash=%s…  nonce=%6d  mine=%.3fs  -> %s%n",
                    vtype, vehicleId,
                    token.hashValue.substring(0, 16),
                    token.nonce,
                    mineTime,
                    label);

            // Wait before the next attempt
            try {
                Thread.sleep(interval * 1000L);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}