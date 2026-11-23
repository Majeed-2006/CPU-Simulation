package scheduler;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import model.ExecutionSnapshot;
import model.ExecutionSnapshot.CpuState;
import model.Process;

/**
 * Preemptive Shortest Job First (SRTF) Scheduler.
 * CONTRACT WARNING: Do NOT change method signatures (arrive, step, getCompletedCount),
 * as both Main.java and SchedulerGUI.java depend on them.
 */
public class SJFScheduler {
    private int contextSwitchTime;
    private int completedCount;

    private List<Process> readyQueue = new ArrayList<>(); // waiting processes (NOT the running one)
    private Process current = null;       // process running on the CPU now
    private Process lastProcess = null;   // last process that used the CPU
    private Process switchTarget = null;  // process being loaded during a context switch
    private int csLeft = 0;               // seconds of context switch still left

    public SJFScheduler(int contextSwitchTime) {
        this.contextSwitchTime = contextSwitchTime;
    }

    /** Called when a process arrives: just add it to the ready queue. */
    public void arrive(Process p) {
        readyQueue.add(p);
    }

    /** One call = one second of the CPU. */
    public ExecutionSnapshot step(int currentTime, LocalTime startTime) {
        // 1. If no context switch is in progress, decide who should run
        if (csLeft == 0) {
            chooseProcess();
        }

        // 2. Context switch second
        if (csLeft > 0) {
            ExecutionSnapshot snap = makeSnapshot(currentTime, startTime, CpuState.CONTEXT_SWITCH,
                    null, "Context switch: loading " + switchTarget.id);
            csLeft--;
            if (csLeft == 0) {          // switch finished, the process can run next second
                current = switchTarget;
                switchTarget = null;
            }
            return snap;
        }

        // 3. Nobody to run
        if (current == null) {
            return makeSnapshot(currentTime, startTime, CpuState.IDLE, null, "CPU idle");
        }

        // 4. Run the current process for one second
        return runOneSecond(currentTime, startTime);
    }

    public int getCompletedCount() {
        return completedCount;
    }

    // ---------------- helper methods ----------------

    /** Picks the process with the shortest remaining time and starts a context switch if needed. */
    private void chooseProcess() {
        Process best = findShortest();
        if (best == null || best == current) {
            return;                      // nothing to change
        }

        // best is a different process -> put the running one back in the queue (preemption)
        if (current != null) {
            readyQueue.add(current);
            current = null;
        }
        readyQueue.remove(best);

        // context switch only if the CPU was used by another process before
        if (contextSwitchTime > 0 && lastProcess != null && lastProcess != best) {
            switchTarget = best;
            csLeft = contextSwitchTime;
        } else {
            current = best;
        }
    }

    /** Looks at the running process + the ready queue and returns the shortest remaining one. */
    private Process findShortest() {
        Process best = current;          // start with the running one so ties don't cause preemption
        for (Process p : readyQueue) {
            if (best == null
                    || p.remainingTime < best.remainingTime
                    || (p.remainingTime == best.remainingTime && best != current
                        && p.arrivalTime < best.arrivalTime)) {
                best = p;
            }
        }
        return best;
    }

    private ExecutionSnapshot runOneSecond(int currentTime, LocalTime startTime) {
        // first time this process gets the CPU -> save its start time (for response time)
        if (!current.isStarted) {
            current.isStarted = true;
            current.startTime = currentTime;
        }

        String note = "";
        if (current.remainingTime == 1) {
            note = current.id + " finishes";
        }
        // snapshot is taken BEFORE decreasing the remaining time
        ExecutionSnapshot snap = makeSnapshot(currentTime, startTime, CpuState.RUNNING, current.id, note);

        current.remainingTime--;
        lastProcess = current;

        if (current.remainingTime == 0) {
            current.calculateFinalMetrics(currentTime + 1);
            completedCount++;
            current = null;
        }
        return snap;
    }

    /** Builds the snapshot with the ready queue sorted by remaining time. */
    private ExecutionSnapshot makeSnapshot(int time, LocalTime startTime, CpuState state,
                                           String runningId, String note) {
        List<Process> sorted = new ArrayList<>(readyQueue);
        sorted.sort((a, b) -> a.remainingTime - b.remainingTime);
        return new ExecutionSnapshot(time, startTime, state, runningId, sorted, note);
    }
}