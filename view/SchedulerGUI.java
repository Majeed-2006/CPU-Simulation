package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Swing GUI for the CPU Scheduling Simulator.
 * Architecture Note for GUI Developer:
 * Do NOT write scheduling logic here. Use SJFScheduler / ACSScheduler via:
 *   scheduler.arrive(p) -> feed processes at their arrivalTime
 *   scheduler.step(time, startTime) -> get ExecutionSnapshot for each second
 */
public class SchedulerGUI extends JFrame {

public SchedulerGUI() {
        // Apply native OS Look and Feel (Windows/Mac) instead of default Java Metal theme
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Basic Window Configuration
        setTitle("CPU Scheduling Simulator (SJF & ACS)");
        setSize(1000, 700);
        setMinimumSize(new Dimension(800, 550));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center window on screen
        setLayout(new BorderLayout(10, 10));

        // TODO (GUI Developer): Add your UI panels here using BorderLayout regions:
        // - BorderLayout.NORTH  -> Simulation inputs (Context Switch, Quantum, Algorithm selector, Run button)
        // - BorderLayout.WEST   -> Process input form / queue table
        // - BorderLayout.CENTER -> Custom JPanel (paintComponent) for the Gantt Chart
        // - BorderLayout.SOUTH  -> JTable inside JScrollPane for Process Metrics & Averages
    }
    

}