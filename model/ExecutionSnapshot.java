package model;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExecutionSnapshot {

    // Enum for strict, typo-free CPU states
    public enum CpuState {
        RUNNING,
        CONTEXT_SWITCH,
        IDLE
    }

    public int time;
    public String formattedWallTime;
    public CpuState state;
    public String runningProcessId;
    public List<String> tier1State;
    public List<String> tier2State;
    public String eventNote;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm:ss a");

    
   // 1. Full Constructor (For ACS with 2 tiers + Wall Clock)
    public ExecutionSnapshot(Integer time, LocalTime simStartTime, CpuState state, String runningProcessId,
                             List<Process> tier1, List<Process> tier2, String eventNote) {
        this.time = time;
        this.formattedWallTime = (simStartTime != null && time != null)
                                 ? simStartTime.plusSeconds(time).format(TIME_FORMAT) : "";
        this.state = state;
        this.runningProcessId = runningProcessId;
        this.eventNote = eventNote;

        this.tier1State = new ArrayList<>();
        if (tier1 != null) {
            for (Process p : tier1) this.tier1State.add(p.toString());
        }

        // Remains null if not provided (used to distinguish SJF from ACS)
        if (tier2 != null) {
            this.tier2State = new ArrayList<>();
            for (Process p : tier2) this.tier2State.add(p.toString());
        }
    }

    // 2. Overloaded Constructor for SJF (Only 1 Ready Queue, no Tier 2)
    public ExecutionSnapshot(Integer time, LocalTime simStartTime, CpuState state, String runningProcessId,
                             List<Process> readyQueue, String eventNote) {
        this(time, simStartTime, state, runningProcessId, readyQueue, null, eventNote);
    }

    // 3. Minimal Constructor (No Wall Clock, no Tier 2 - quick testing)
    public ExecutionSnapshot(Integer time, CpuState state, String runningProcessId,
                             List<Process> readyQueue, String eventNote) {
        this(time, null, state, runningProcessId, readyQueue, null, eventNote);
    }
}