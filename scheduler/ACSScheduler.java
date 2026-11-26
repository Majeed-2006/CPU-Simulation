package scheduler;

import java.time.LocalTime;
import java.util.*;
import model.ExecutionSnapshot;
import model.ExecutionSnapshot.CpuState;
import model.Process;

/**
 * Adaptive Container Scheduler (ACS).
 * Tier 1: Round Robin with a static quantum (all new processes start here).
 * Tier 2: Non-preemptive priority queue for demoted CPU-bound processes.
 * Tier 3: Aging, applied to processes waiting in Tier 2.
 *
 * CONTRACT: keep the signatures of arrive(), step() and getCompletedCount()
 * intact for Main and GUI integration.
 */
public class ACSScheduler {
    private final int contextSwitchTime;
    private final int quantum;
    private int completedCount;

    // Ready queues
    private final Queue<Process> tier1 = new LinkedList<>();
    private final List<Process> tier2 = new ArrayList<>();

    // CPU state
    private Process running = null;   // Process currently on the CPU
    private Process lastRun = null;   // Last process that ran (used to decide if a CS is needed)
    private Process pending = null;   // Process being loaded during a context switch
    private int quantumUsed = 0;      // Time units consumed in the current Tier 1 slice
    private int csRemaining = 0;      // Context-switch time units still left

    public ACSScheduler(int contextSwitchTime, int quantum) {
        this.contextSwitchTime = contextSwitchTime;
        this.quantum = quantum;
    }

    // ================= TIER 1 =================

    /** Called by Main/GUI when a process arrives. All new processes enter Tier 1. */
    public void arrive(Process p) {
        tier1.add(p);
    }

    /**
     * Advances the simulation by one time unit [currentTime, currentTime + 1).
     * Main/GUI must call arrive() for every process arriving at currentTime
     * BEFORE calling step(currentTime).
     */
    public ExecutionSnapshot step(int currentTime, LocalTime startTime) {
        // 1) If the CPU is free and no context switch is in progress, pick the next process
        if (running == null && pending == null) {
            Process next = pickNext(currentTime);
            if (next != null) {
                if (lastRun != null && lastRun != next && contextSwitchTime > 0) {
                    // A different process than the last one: a context switch is required
                    pending = next;
                    csRemaining = contextSwitchTime;
                } else {
                    running = next;
                    quantumUsed = 0;
                }
            }
        }

        // 2) Build the snapshot BEFORE executing this time unit
        ExecutionSnapshot snap = buildSnapshot(currentTime, startTime);

        // 3) Context switch in progress: consume one unit of CS time
        if (pending != null) {
            snap.eventNote = "Context switch loads " + pending.id;
            csRemaining--;
            if (csRemaining == 0) {
                running = pending;
                pending = null;
                quantumUsed = 0;
            }
            return snap;
        }

        // 4) Execute the running process for one time unit
        if (running != null) {
            Process current = running;

            // Record the first time the process actually gets the CPU (for Response Time)
            if (!current.isStarted) {
                current.isStarted = true;
                current.startTime = currentTime;
            }

            current.remainingTime--;
            quantumUsed++;

            if (current.remainingTime == 0) {
                // Process finished
                current.calculateFinalMetrics(currentTime + 1);
                completedCount++;
                lastRun = current;
                running = null;
                snap.eventNote = current.id + " finished";
            } else if (isInTier1(current) && quantumUsed == quantum) {
                // Used its entire quantum without finishing: demote to Tier 2
                current.tier2ArrivalTime = currentTime + 1;
                tier2.add(current);
                lastRun = current;
                running = null;
                snap.eventNote = current.id + " demoted to Tier 2";
            }
        }
        return snap;
    }

    /** Tier 1 always has priority over Tier 2. */
    private Process pickNext(int now) {
        if (!tier1.isEmpty()) return tier1.poll();
        return pickFromTier2(now);
    }

    /** A process is still in Tier 1 until it has been demoted. */
    private boolean isInTier1(Process p) {
        return p.tier2ArrivalTime == -1;
    }

    // ================= TIER 2 + AGING =================

    /**
     * Applies aging, selects the highest-priority process (lowest number),
     * removes it from tier2 and returns it. Returns null if tier2 is empty.
     * A process picked from here runs non-preemptively until it finishes.
     */
    private Process pickFromTier2(int now) {
        return null; // TODO: Tier 2 owner
    }

    // ================= SNAPSHOT =================

    private ExecutionSnapshot buildSnapshot(int t, LocalTime startTime) {
        CpuState state;
        String id;
        if (pending != null)      { state = CpuState.CONTEXT_SWITCH; id = null; }
        else if (running != null) { state = CpuState.RUNNING;        id = running.id; }
        else                      { state = CpuState.IDLE;           id = null; }

        // Pass copies of the queues; the snapshot converts them to strings immediately
        return new ExecutionSnapshot(t, startTime, state, id,
                new ArrayList<>(tier1), new ArrayList<>(tier2), "");
    }

    public int getCompletedCount() { return completedCount; }
}