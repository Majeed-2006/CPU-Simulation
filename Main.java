import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import model.ExecutionSnapshot;
import model.Process;
import scheduler.ACSScheduler;
import scheduler.SJFScheduler;
import view.SchedulerGUI;


public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Choose Mode: (1) Console  |  (2) GUI");
        int choice = sc.nextInt();

        if (choice == 2) {
            SwingUtilities.invokeLater(() -> {
                new SchedulerGUI().setVisible(true);
            });
        } else {
            runConsoleMode(sc);
        }
    }

/**
 * Console runner for testing SJF and ACS algorithms interactively.
 * Note for Algorithm Developers: You can call your scheduler here to test with full user input,
 * or use TestSJF.java / TestACS.java in the 'test' package for quick hardcoded tests.
 */
    private static void runConsoleMode(Scanner sc) {
      
    }
}