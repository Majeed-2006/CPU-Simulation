package test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import model.ExecutionSnapshot;
import model.Process;
import scheduler.ACSScheduler;

public class TestACS {

    private static StringBuilder csv;

    // ==========================================
    // MAIN TEST RUNNER
    // ==========================================

    public static void main(String[] args) {
        runACSTests();
    }

    public static void runACSTests() {

        csv = new StringBuilder(
                "test,cs,quantum,avg_waiting,avg_turnaround\n"
        );

        Process[] test1 = {
                new Process("P1", 0, 4, 2),
                new Process("P2", 1, 7, 3),
                new Process("P3", 3, 2, 1)
        };

        Process[] test2 = {
                new Process("P1", 0, 5, 4),
                new Process("P2", 1, 6, 2),
                new Process("P3", 4, 4, 3)
        };

        // Original assignment test cases
        System.out.println("========== ACS ORIGINAL TEST CASES ==========");

        runTest("Test 1 (Q=3, CS=0)", test1, 0, 3);
        runTest("Test 2 (Q=3, CS=1)", test2, 1, 3);

        // Experiments for report
        System.out.println("\n========== ACS EXPERIMENTS ==========");

        runExperiment("Test_1", test1);
        runExperiment("Test_2", test2);

        // Save CSV
        saveCsv();
    }

    // ==========================================
    // NORMAL TEST - WITH PRINTING
    // ==========================================

    private static void runTest(
            String name,
            Process[] original,
            int cs,
            int quantum) {

        System.out.println("\n========== " + name + " ==========");

        Process[] processes = copyProcesses(original);

        ACSScheduler scheduler = new ACSScheduler(cs, quantum);

        int time = 0;
        int maxTime = 10000;

        LocalTime startTime = LocalTime.of(0, 0, 0);

        while (scheduler.getCompletedCount() < processes.length
                && time < maxTime) {

            for (Process p : processes) {
                if (p.arrivalTime == time) {
                    scheduler.arrive(p);
                }
            }

            ExecutionSnapshot e = scheduler.step(time, startTime);

            System.out.printf(
                    "t=%-3d %-15s %-4s | T1=%s | T2=%s | %s%n",
                    e.time,
                    e.state,
                    e.runningProcessId == null ? "-" : e.runningProcessId,
                    e.tier1State,
                    e.tier2State,
                    e.eventNote
            );

            time++;
        }

        if (scheduler.getCompletedCount() != processes.length) {
            throw new IllegalStateException(
                    name + " did not finish. Check scheduler implementation."
            );
        }

        printResults(processes);
    }

    // ==========================================
    // SIMULATION - WITHOUT PRINTING
    // ==========================================

    private static double[] simulate(
            Process[] original,
            int cs,
            int quantum) {

        Process[] processes = copyProcesses(original);

        ACSScheduler scheduler = new ACSScheduler(cs, quantum);

        int time = 0;
        int maxTime = 10000;

        while (scheduler.getCompletedCount() < processes.length
                && time < maxTime) {

            for (Process p : processes) {
                if (p.arrivalTime == time) {
                    scheduler.arrive(p);
                }
            }

            scheduler.step(time, null);

            time++;
        }

        if (scheduler.getCompletedCount() != processes.length) {
            throw new IllegalStateException(
                    "Simulation failed: CS=" + cs + ", Q=" + quantum
            );
        }

        double totalWaiting = 0;
        double totalTurnaround = 0;

        for (Process p : processes) {
            totalWaiting += p.waitingTime;
            totalTurnaround += p.turnaroundTime;
        }

        return new double[] {
                totalWaiting / processes.length,
                totalTurnaround / processes.length
        };
    }

    // ==========================================
    // EXPERIMENTS - CS AND QUANTUM
    // ==========================================

    private static void runExperiment(
            String name,
            Process[] processes) {

        System.out.println("\nExperiment: " + name);

        System.out.println(
                "CS | Quantum | Avg Waiting | Avg Turnaround"
        );

        for (int quantum = 1; quantum <= 5; quantum++) {

            for (int cs = 0; cs <= 4; cs++) {

                double[] result = simulate(processes, cs, quantum);

                System.out.printf(
                        Locale.US,
                        "%-2d | %-7d | %-11.2f | %.2f%n",
                        cs,
                        quantum,
                        result[0],
                        result[1]
                );

                csv.append(
                        String.format(
                                Locale.US,
                                "%s,%d,%d,%.2f,%.2f%n",
                                name,
                                cs,
                                quantum,
                                result[0],
                                result[1]
                        )
                );
            }
        }
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private static Process[] copyProcesses(Process[] original) {

        Process[] copies = new Process[original.length];

        for (int i = 0; i < original.length; i++) {
            copies[i] = original[i].copy();
        }

        return copies;
    }

    private static void printResults(Process[] processes) {

        System.out.println("\nProcess | Arrival | Finish | TAT | WT | RT");

        double totalWaiting = 0;
        double totalTurnaround = 0;

        for (Process p : processes) {

            System.out.printf(
                    "%-7s | %-7d | %-6d | %-3d | %-2d | %d%n",
                    p.id,
                    p.arrivalTime,
                    p.finishTime,
                    p.turnaroundTime,
                    p.waitingTime,
                    p.responseTime
            );

            totalWaiting += p.waitingTime;
            totalTurnaround += p.turnaroundTime;
        }

        System.out.printf(
                Locale.US,
                "\nAverage Waiting Time: %.2f%n",
                totalWaiting / processes.length
        );

        System.out.printf(
                Locale.US,
                "Average Turnaround Time: %.2f%n",
                totalTurnaround / processes.length
        );
    }

    // ==========================================
    // CSV
    // ==========================================

    private static void saveCsv() {

        try {

            Files.createDirectories(Path.of("results"));

            Files.writeString(
                    Path.of("results", "results_acs.csv"),
                    csv.toString()
            );

            System.out.println(
                    "\nSaved: results/results_acs.csv"
            );

        } catch (IOException e) {

            System.out.println(
                    "Could not save ACS CSV: " + e.getMessage()
            );
        }
    }
}