import java.util.*;
import java.text.SimpleDateFormat;

public class sim {

    public static void main(String[] args) throws InterruptedException {

        // Print header
        System.out.println("=".repeat(70));
        System.out.println("  V2X SYBIL RESISTANCE SIMULATION");
        System.out.printf("  Difficulty: %d leading zeros  |  Window: %ds  |  Max tokens/window: %d%n",
                config.DIFFICULTY, config.WINDOW_SECONDS, config.MAX_TOKENS_PER_WINDOW);
        System.out.printf("  Vehicles: %d total  (%d Sybil attackers)  |  Duration: %ds%n",
                config.NUM_VEHICLES, config.NUM_SYBIL, config.SIM_DURATION);
        System.out.println("=".repeat(70));

        // Create the rate controller (shared by all vehicles)
        rateControlLayer controller = new rateControlLayer();

        // Calculate when the simulation ends
        long simEnd = System.currentTimeMillis() + (config.SIM_DURATION * 1000L);

        // Create all vehicles
        List<vehicle> vehicles = new ArrayList<>();
        for (int i = 0; i < config.NUM_VEHICLES; i++) {
            boolean isSybil = i < config.NUM_SYBIL;
            String vid      = isSybil
                    ? String.format("SYBIL-%02d", i + 1)
                    : String.format("V-%02d",     i + 1);
            vehicles.add(new vehicle(vid, controller, isSybil, simEnd));
        }

        // Start the simulation
        System.out.printf("%n[SIM START]  %s%n%n", new SimpleDateFormat("HH:mm:ss").format(new Date()));
        for (vehicle v : vehicles) v.start();

        // Wait until all vehicles are done
        for (vehicle v : vehicles) v.join(config.SIM_DURATION * 1000L + 10000L);

        System.out.printf("%n[SIM END]  %s%n%n", new SimpleDateFormat("HH:mm:ss").format(new Date()));

        // Print the summary report
        printSummary(vehicles, controller);
    }


    // printSummary – prints the statistics tables

    static void printSummary(List<vehicle> vehicles, rateControlLayer controller) {

        // ── Table 1: Per vehicle ──────────────────────────────────────────────
        System.out.println("=".repeat(70));
        System.out.println("  PER-VEHICLE SUMMARY");
        System.out.println("=".repeat(70));
        System.out.printf("  %-12s  %-6s  %8s  %8s  %8s  %11s%n",
                "Vehicle", "Type", "Attempts", "Accepted", "Rejected", "Avg Mine(s)");
        System.out.println("  " + "-".repeat(64));

        int totalAccepted = 0;
        int totalRejected = 0;
        int sybilAccepted = 0;
        int legitAccepted = 0;
        List<Double> allMineTimes = new ArrayList<>();

        for (vehicle v : vehicles) {
            int    attempts = v.accepted + v.rejected;
            double avgMine  = 0;

            if (!v.mineTimes.isEmpty()) {
                double sum = 0;
                for (double t : v.mineTimes) sum += t;
                avgMine = sum / v.mineTimes.size();
            }

            String vtype = v.isSybil ? "SYBIL" : "legit";
            System.out.printf("  %-12s  %-6s  %8d  %8d  %8d  %11.3f%n",
                    v.vehicleId, vtype, attempts, v.accepted, v.rejected, avgMine);

            totalAccepted += v.accepted;
            totalRejected += v.rejected;
            allMineTimes.addAll(v.mineTimes);
            if (v.isSybil) sybilAccepted += v.accepted;
            else           legitAccepted += v.accepted;
        }

        System.out.println("  " + "-".repeat(64));
        System.out.printf("  %-12s  %-6s  %8d  %8d  %8d%n",
                "TOTAL", "", totalAccepted + totalRejected, totalAccepted, totalRejected);

        // ── Table 2: Per time window ──────────────────────────────────────────
        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("  RATE-CONTROL WINDOW SUMMARY");
        System.out.println("=".repeat(70));

        Set<Long> allWindows = new TreeSet<>(controller.acceptedPerWindow.keySet());
        allWindows.addAll(controller.rejectedPerWindow.keySet());

        for (long w : allWindows) {
            int acc = controller.acceptedPerWindow.getOrDefault(w, 0);
            int rej = controller.rejectedPerWindow.getOrDefault(w, 0);
            List<String> vehs = controller.vehiclesPerWindow.getOrDefault(w, new ArrayList<>());
            Set<String> uniqueVehs = new LinkedHashSet<>(vehs); // remove duplicates
            String timeStr = new SimpleDateFormat("HH:mm:ss").format(new Date(w * 1000L));
            System.out.printf("  Window [%s]  accepted=%3d  rejected=%3d  total=%3d  vehicles=%s%n",
                    timeStr, acc, rej, acc + rej, uniqueVehs);
        }

        // ── Overall statistics ────────────────────────────────────────────────
        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("  OVERALL STATISTICS");
        System.out.println("=".repeat(70));

        if (!allMineTimes.isEmpty()) {
            double sum = 0, max = 0;
            for (double t : allMineTimes) {
                sum += t;
                if (t > max) max = t;
            }
            System.out.printf("  Avg PoW mining time : %.4fs%n", sum / allMineTimes.size());
            System.out.printf("  Max PoW mining time : %.4fs%n", max);
        }

        System.out.printf("  Sybil tokens accepted : %d%n", sybilAccepted);
        System.out.printf("  Legit tokens accepted : %d%n", legitAccepted);
        System.out.printf("  Total rejected        : %d%n", totalRejected);

        int windowsUsed    = allWindows.size();
        int theoreticalMax = windowsUsed * config.MAX_TOKENS_PER_WINDOW;
        System.out.printf("  Rate-limit efficiency : %d/%d slots used across %d window(s)%n",
                totalAccepted, theoreticalMax, windowsUsed);
        System.out.println("=".repeat(70));
    }
}