package scheduler;

import java.time.LocalTime;
import model.ExecutionSnapshot;
import model.Process;

/**
 * Adaptive Multi-Tier Scheduler (ACS / MLFQ).
 * CONTRACT WARNING: Keep (arrive, step, getCompletedCount) signatures intact for Main & GUI integration.
 */
public class ACSScheduler {  
    private int contextSwitchTime;
    private int quantum;
    private int completedCount;
    public ACSScheduler(int contextSwitchTime, int quantum) {
        this.contextSwitchTime = contextSwitchTime;
        this.quantum = quantum;
    }
/**
 * Called by Main/GUI when a process arrives at currentTime.
 * All new processes enter Tier 1 (Highest Priority) first.
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