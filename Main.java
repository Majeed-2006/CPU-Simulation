import java.util.Scanner;
import javax.swing.SwingUtilities;

import test.TestSJF;
import test.TestACS;
import view.SchedulerGUI;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Choose Mode: (1) Console | (2) GUI");
        int choice = sc.nextInt();

        if (choice == 2) {
            SwingUtilities.invokeLater(() -> {
                new SchedulerGUI().setVisible(true);
            });
        } else {
            runConsoleMode();
        }
    }

    private static void runConsoleMode() {
        System.out.println("\n========== RUNNING SJF TESTS ==========");
        TestSJF.runSJFTests();

        System.out.println("\n========== RUNNING ACS TESTS ==========");
        TestACS.runACSTests();
        
        System.out.println("\n========== ALL TESTS COMPLETED ==========");
    }
}