package test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Locale;
import model.ExecutionSnapshot;
import model.Process;
import scheduler.SJFScheduler;

/**
 * Hardcoded tests for the SJF scheduler.
 * Main calls runSJFTests() to run all of them.
 */
public class TestSJF {

    // all experiment results are collected here, then saved to results/results_sjf.csv
    private static StringBuilder csv = new StringBuilder("test,cs,avg_waiting,avg_turnaround\n");

    public static void runSJFTests() {
        // Test 1: CS = 1
        // Expected: P1 finish 9, TAT 9, WT 4, RT 0 | P2 finish 4, TAT 3, WT 1, RT 1
        Process[] test1 = {
            new Process("P1", 0, 5, 1),
            new Process("P2", 1, 2, 1)
        };
        runTest("Test 1 (CS = 1)", test1, 1);

        // Test 2: CS = 0 (same data as Test Case 1 of the assignment)
        // Expected: P1 finish 4, TAT 4, WT 0 | P2 finish 13, TAT 12, WT 5 | P3 finish 6, TAT 3, WT 1
        // Avg TAT = 6.33, Avg WT = 2.00
        Process[] test2 = {
            new Process("P1", 0, 4, 2),
            new Process("P2", 1, 7, 3),
            new Process("P3", 3, 2, 1)
        };
        runTest("Test 2 (CS = 0)", test2, 0);

        // Test 3: CS = 1, with an IDLE gap
        // Expected: P1 finish 2, TAT 2, WT 0 | P2 finish 13, TAT 8, WT 4 | P3 finish 9, TAT 2, WT 1
        // Avg TAT = 4, Avg WT = 1.67
        Process[] test3 = {
            new Process("P1", 0, 2, 1),
            new Process("P2", 5, 4, 1),
            new Process("P3", 7, 1, 1)
        };
        runTest("Test 3 (CS = 1, IDLE gap)", test3, 1);

        // Test 4: Assignment Test Case 2 data, CS = 1
        // Expected: Avg TAT = 9.00, Avg WT = 4.00
        Process[] test4 = {
            new Process("P1", 0, 5, 4),
            new Process("P2", 1, 6, 2),
            new Process("P3", 4, 4, 3)
        };
        runTest("Test 4 (Assignment Test Case 2, CS = 1)", test4, 1);

        // Test 5: ties in remaining time, CS = 0
        // Expected: Avg TAT = 8.00, Avg WT = 3.67
        Process[] test5 = {
            new Process("P1", 0, 5, 1),
            new Process("P2", 1, 4, 1),
            new Process("P3", 2, 4, 1)
        };
        runTest("Test 5 (Ties, CS = 0)", test5, 0);

        // ---------- CS experiments (for the report graphs) ----------
        runExperiment("Test 1", test1);
        runExperiment("Test 2", test2);
        runExperiment("Test 3", test3);
        runExperiment("Test 4", test4);
        runExperiment("Test 5", test5);

        saveCsv();
    }

    private static void runTest(String name, Process[] processes, int cs) {
        System.out.println("========== " + name + " ==========");
        SJFScheduler scheduler = new SJFScheduler(cs);
        LocalTime startTime = LocalTime.of(0, 0, 0);

        int time = 0;
        while (scheduler.getCompletedCount() < processes.length) {
            // feed the processes that arrive at this second
            for (Process p : processes) {
                if (p.arrivalTime == time) {
                    scheduler.arrive(p);
                }
            }
            ExecutionSnapshot e = scheduler.step(time, startTime);
            System.out.println("t=" + e.time + " | " + e.state + " | running: " + e.runningProcessId
                    + " | queue: " + e.tier1State + " | " + e.eventNote);
            time++;
        }

        // results table
        System.out.println();
        System.out.println("Process  Arrival  Finish  Turnaround  Waiting  Response");
        double totalTurnaround = 0;
        double totalWaiting = 0;
        for (Process p : processes) {
            System.out.println(p.id + "       " + p.arrivalTime + "        " + p.finishTime
                    + "       " + p.turnaroundTime + "           " + p.waitingTime
                    + "        " + p.responseTime);
            totalTurnaround += p.turnaroundTime;
            totalWaiting += p.waitingTime;
        }
        System.out.printf("Average Turnaround Time: %.2f%n", totalTurnaround / processes.length);
        System.out.printf("Average Waiting Time: %.2f%n%n", totalWaiting / processes.length);
    }

    // Runs the scheduler silently (no printing) and returns {avgWaiting, avgTurnaround}
    private static double[] simulate(Process[] original, int cs) {
        // fresh copies, because Process objects change while running
        Process[] processes = new Process[original.length];
        for (int i = 0; i < original.length; i++) {
            processes[i] = original[i].copy();
        }

        SJFScheduler scheduler = new SJFScheduler(cs);
        LocalTime startTime = LocalTime.of(0, 0, 0);
        int time = 0;
        while (scheduler.getCompletedCount() < processes.length) {
            for (Process p : processes) {
                if (p.arrivalTime == time) {
                    scheduler.arrive(p);
                }
            }
            scheduler.step(time, startTime);
            time++;
        }

        double totalWaiting = 0;
        double totalTurnaround = 0;
        for (Process p : processes) {
            totalWaiting += p.waitingTime;
            totalTurnaround += p.turnaroundTime;
        }
        return new double[] { totalWaiting / processes.length, totalTurnaround / processes.length };
    }

    // Runs one test case with CS = 0..4, prints the averages and stores them for the CSV file
    private static void runExperiment(String name, Process[] processes) {
        System.out.println("Experiment: " + name);
        System.out.println("CS | Avg Waiting | Avg Turnaround");
        for (int cs = 0; cs <= 4; cs++) {
            double[] result = simulate(processes, cs);
            System.out.printf("%d  | %.2f        | %.2f%n", cs, result[0], result[1]);
            // Locale.US makes sure the decimal point is "." and not ","
            csv.append(String.format(Locale.US, "%s,%d,%.2f,%.2f\n", name, cs, result[0], result[1]));
        }
        System.out.println();
    }

    // Writes everything to results/results_sjf.csv (the folder is created if it does not exist)
    private static void saveCsv() {
        try {
            Files.createDirectories(Path.of("results"));
            Files.writeString(Path.of("results", "results_sjf.csv"), csv.toString());
            System.out.println("Saved: results/results_sjf.csv");
        } catch (IOException e) {
            System.out.println("Could not save the CSV file: " + e.getMessage());
        }
    }
}
