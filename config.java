
/**
 * Config – all simulation settings in one place.
 */

public class config {

    static int DIFFICULTY            = 4;   // Number of leading zeros the hash must start with
    static int WINDOW_SECONDS        = 10;  // Duration of each time window (seconds)
    static int MAX_TOKENS_PER_WINDOW = 5;   // Max accepted tokens per time window
    static int NUM_VEHICLES          = 8;   // Total number of vehicles in the simulation
    static int NUM_SYBIL             = 6;   // Number of Sybil attackers
    static int SIM_DURATION          = 30;  // Total simulation time in seconds
    static int TOKEN_INTERVAL        = 2;   // Seconds between each vehicle's token attempt
}