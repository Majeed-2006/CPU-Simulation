package scheduler;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import model.ExecutionSnapshot;
import model.ExecutionSnapshot.CpuState;
import model.Process;


public class SJFScheduler {
    private int contextSwitchTime;
    private int completedCount;

    private List<Process> readyQueue = new ArrayList<>(); 
    private Process current = null;       
    private Process lastProcess = null;   
    private Process switchTarget = null;  
    private int csLeft = 0;               

    public SJFScheduler(int contextSwitchTime) {
        this.contextSwitchTime = contextSwitchTime;
    }

    public void arrive(Process p) {
        readyQueue.add(p);
    }

    
    public ExecutionSnapshot step(int currentTime, LocalTime startTime) {
        if (csLeft == 0) {
            chooseProcess();
        }

        if (csLeft > 0) {
            ExecutionSnapshot snap = makeSnapshot(currentTime, startTime, CpuState.CONTEXT_SWITCH,
                    null, "Context switch: loading " + switchTarget.id);
            csLeft--;
            if (csLeft == 0) {          
                current = switchTarget;
                switchTarget = null;
            }
            return snap;
        }

        if (current == null) {
            return makeSnapshot(currentTime, startTime, CpuState.IDLE, null, "CPU idle");
        }

        return runOneSecond(currentTime, startTime);
    }

    public int getCompletedCount() {
        return completedCount;
    }


    private void chooseProcess() {
        Process best = findShortest();
        if (best == null || best == current) {
            return;                     
        }

        if (current != null) {
            readyQueue.add(current);
            current = null;
        }
        readyQueue.remove(best);

        if (contextSwitchTime > 0 && lastProcess != null && lastProcess != best) {
            switchTarget = best;
            csLeft = contextSwitchTime;
        } else {
            current = best;
        }
    }

    private Process findShortest() {
        Process best = current;         
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
        if (!current.isStarted) {
            current.isStarted = true;
            current.startTime = currentTime;
        }

        String note = "";
        if (current.remainingTime == 1) {
            note = current.id + " finishes";
        }
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

    private ExecutionSnapshot makeSnapshot(int time, LocalTime startTime, CpuState state,
                                           String runningId, String note) {
        List<Process> sorted = new ArrayList<>(readyQueue);
        sorted.sort((a, b) -> a.remainingTime - b.remainingTime);
        return new ExecutionSnapshot(time, startTime, state, runningId, sorted, note);
    }
}
