package scheduler;

import java.time.LocalTime;
import model.ExecutionSnapshot;
import model.Process;

/**
 * Preemptive Shortest Job First (SRTF) Scheduler.
 * CONTRACT WARNING: Do NOT change method signatures (arrive, step, getCompletedCount),
 * as both Main.java and SchedulerGUI.java depend on them.
 */
public class SJFScheduler {
    private int contextSwitchTime;   
    private int completedCount;
    public SJFScheduler(int contextSwitchTime) {
        this.contextSwitchTime = contextSwitchTime;
    }
/**
 * Called by the simulation clock (Main/GUI) at the exact second a process arrives.
 * Add the newly arrived process to your ready queue here.
 */
    public void arrive(Process p) {

    }


    public ExecutionSnapshot step(int currentTime, LocalTime startTime) {

        ExecutionSnapshot e = null;
        return e;
    }
    public int getCompletedCount() {
        return completedCount;
    }
} 