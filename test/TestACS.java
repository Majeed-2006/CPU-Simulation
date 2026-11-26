package test;

import java.util.*;
import model.ExecutionSnapshot;
import model.Process;
import scheduler.ACSScheduler;

/**
 * Standalone sandbox for testing ACSScheduler against the hand-solved
 * examples in the assignment PDF.
 */
public class TestACS {
    public static void main(String[] args) {
        System.out.println("=== Test Case 1 (Q=3, CS=0) ===");
        run(List.of(
                new Process("P1", 0, 4, 2),
                new Process("P2", 1, 7, 3),
                new Process("P3", 3, 2, 1)), 0, 3);

        System.out.println("\n=== Test Case 2 (Q=3, CS=1) ===");
        run(List.of(
                new Process("P1", 0, 5, 4),
                new Process("P2", 1, 6, 2),
                new Process("P3", 4, 4, 3)), 1, 3);
    }

    static void run(List<Process> input, int cs, int q) {
        // Work on copies so each run starts from a clean state
        List<Process> procs = new ArrayList<>();
        for (Process p : input) procs.add(p.copy());

        ACSScheduler s = new ACSScheduler(cs, q);   // Constructor order: (cs, quantum)
        int t = 0;
        int maxTime = 60; // Safety guard against an infinite loop until Tier 2 is implemented

        while (s.getCompletedCount() < procs.size() && t < maxTime) {
            // Deliver arrivals BEFORE calling step()
            for (Process p : procs) if (p.arrivalTime == t) s.arrive(p);

            ExecutionSnapshot e = s.step(t, null);
            System.out.printf("t=%-3d %-14s %-4s | T1=%s | T2=%s | %s%n",
                    e.time, e.state, e.runningProcessId == null ? "-" : e.runningProcessId,
                    e.tier1State, e.tier2State, e.eventNote);
            t++;
        }

        System.out.println("\nProcess | Finish | TAT | WT | RT");
        for (Process p : procs)
            System.out.printf("%-7s | %-6d | %-3d | %-2d | %d%n",
                    p.id, p.finishTime, p.turnaroundTime, p.waitingTime, p.responseTime);
    }
}