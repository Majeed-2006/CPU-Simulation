package view;

import java.awt.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.ExecutionSnapshot;
import model.Process;
import scheduler.ACSScheduler;
import scheduler.SJFScheduler;

/**
 * Wizard GUI:
 *   algo -> (ACS only) tier -> source (built-in tests / custom) -> config -> result
 * Main.java only needs: new SchedulerGUI().setVisible(true);
 */
public class SchedulerGUI extends JFrame {

    // Safety guard: stops the loop if a scheduler never finishes (e.g. ACS Tier 2 not implemented yet)
    private static final int MAX_TIME = 500;

    // ---------- wizard state ----------
    private String algo = "SJF";       // "SJF" or "ACS"
    private boolean tier2Mode = false; // ACS: false = Tier 1 only, true = Tier 1 + Tier 2
    private boolean custom = false;    // false = built-in tests, true = user types data

    // ---------- UI parts ----------
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private final DefaultTableModel inputModel = new DefaultTableModel(
            new String[] { "ID", "Arrival", "Burst", "Priority" }, 0) {
        @Override public boolean isCellEditable(int r, int c) { return custom; }
    };
    private final JTable inputTable = new JTable(inputModel);
    private final JSpinner csSpin = new JSpinner(new SpinnerNumberModel(1, 0, 20, 1));
    private final JSpinner qSpin = new JSpinner(new SpinnerNumberModel(3, 1, 50, 1));
    private final JLabel qLabel = new JLabel("Quantum (Q):");
    private final JLabel configTitle = new JLabel();
    private final JComboBox<Preset> presetBox = new JComboBox<>();
    private final JLabel presetLabel = new JLabel("Test case:");
    private final JButton addRowBtn = new JButton("+ Add process");
    private final JButton delRowBtn = new JButton("- Remove selected");

    private final DefaultTableModel resultModel = new DefaultTableModel(
            new String[] { "Process", "Arrival", "Burst", "Finish", "Turnaround", "Waiting", "Response" }, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTextArea traceArea = new JTextArea();
    private final JLabel avgLabel = new JLabel(" ");
    private final JLabel warnLabel = new JLabel(" ");
    private final GanttPanel gantt = new GanttPanel();

    public SchedulerGUI() {
        super("CPU Scheduler Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 620);
        setLocationRelativeTo(null);

        root.add(buildAlgoPage(), "algo");
        root.add(buildTierPage(), "tier");
        root.add(buildSourcePage(), "source");
        root.add(buildConfigPage(), "config");
        root.add(buildResultPage(), "result");
        add(root);
        cards.show(root, "algo");
    }

    // =====================================================================
    // Pages
    // =====================================================================

    private JPanel menuPage(String title, String subtitle, JButton[] buttons, JButton back) {
        JPanel p = new JPanel(new GridBagLayout());
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 22f));
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel s = new JLabel(subtitle);
        s.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(t);
        box.add(Box.createVerticalStrut(6));
        box.add(s);
        box.add(Box.createVerticalStrut(24));
        for (JButton b : buttons) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(320, 44));
            b.setPreferredSize(new Dimension(320, 44));
            box.add(b);
            box.add(Box.createVerticalStrut(12));
        }
        if (back != null) {
            back.setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(Box.createVerticalStrut(10));
            box.add(back);
        }
        p.add(box);
        return p;
    }

    private JPanel buildAlgoPage() {
        JButton sjf = new JButton("SJF (Preemptive / SRTF)");
        JButton acs = new JButton("ACS (Adaptive Container Scheduler)");
        sjf.addActionListener(e -> { algo = "SJF"; cards.show(root, "source"); });
        acs.addActionListener(e -> { algo = "ACS"; cards.show(root, "tier"); });
        return menuPage("Choose the algorithm", "Which scheduler do you want to run?",
                new JButton[] { sjf, acs }, null);
    }

    private JPanel buildTierPage() {
        JButton t1 = new JButton("Tier 1 only (Round Robin)");
        JButton t2 = new JButton("Tier 1 + Tier 2 (full ACS with aging)");
        JButton back = new JButton("< Back");
        t1.addActionListener(e -> { tier2Mode = false; cards.show(root, "source"); });
        t2.addActionListener(e -> { tier2Mode = true; cards.show(root, "source"); });
        back.addActionListener(e -> cards.show(root, "algo"));
        return menuPage("ACS: which part to test?", "Tier 2 = demoted processes + priority + aging",
                new JButton[] { t1, t2 }, back);
    }

    private JPanel buildSourcePage() {
        JButton built = new JButton("Run the built-in test cases");
        JButton own = new JButton("I will enter my own numbers");
        JButton back = new JButton("< Back");
        built.addActionListener(e -> { custom = false; openConfig(); });
        own.addActionListener(e -> { custom = true; openConfig(); });
        back.addActionListener(e -> cards.show(root, algo.equals("ACS") ? "tier" : "algo"));
        return menuPage("Input data", "Where should the processes come from?",
                new JButton[] { built, own }, back);
    }

    private JPanel buildConfigPage() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        configTitle.setFont(configTitle.getFont().deriveFont(Font.BOLD, 18f));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        top.add(configTitle);
        top.add(presetLabel);
        top.add(presetBox);
        p.add(top, BorderLayout.NORTH);

        p.add(new JScrollPane(inputTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        JButton back = new JButton("< Back");
        JButton run = new JButton("Run simulation");
        bottom.add(back);
        bottom.add(addRowBtn);
        bottom.add(delRowBtn);
        bottom.add(new JLabel("Context switch (CS):"));
        bottom.add(csSpin);
        bottom.add(qLabel);
        bottom.add(qSpin);
        bottom.add(run);
        p.add(bottom, BorderLayout.SOUTH);

        back.addActionListener(e -> cards.show(root, "source"));
        run.addActionListener(e -> runSimulation());
        addRowBtn.addActionListener(e ->
                inputModel.addRow(new Object[] { "P" + (inputModel.getRowCount() + 1), 0, 1, 1 }));
        delRowBtn.addActionListener(e -> {
            int r = inputTable.getSelectedRow();
            if (r >= 0) {
                if (inputTable.isEditing()) inputTable.getCellEditor().cancelCellEditing();
                inputModel.removeRow(r);
            }
        });
        presetBox.addActionListener(e -> {
            Preset ps = (Preset) presetBox.getSelectedItem();
            if (!custom && ps != null) loadPreset(ps);
        });
        return p;
    }

    private JPanel buildResultPage() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane ganttScroll = new JScrollPane(gantt);
        ganttScroll.setPreferredSize(new Dimension(800, 120));
        ganttScroll.setBorder(BorderFactory.createTitledBorder("Gantt chart (1 box = 1 time unit)"));
        p.add(ganttScroll, BorderLayout.NORTH);

        traceArea.setEditable(false);
        traceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Metrics", new JScrollPane(new JTable(resultModel)));
        tabs.addTab("Step-by-step trace", new JScrollPane(traceArea));
        p.add(tabs, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        JPanel info = new JPanel(new GridLayout(2, 1));
        avgLabel.setFont(avgLabel.getFont().deriveFont(Font.BOLD));
        warnLabel.setForeground(new Color(180, 0, 0));
        info.add(avgLabel);
        info.add(warnLabel);
        south.add(info, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton edit = new JButton("< Edit data");
        JButton again = new JButton("New run (main menu)");
        edit.addActionListener(e -> cards.show(root, "config"));
        again.addActionListener(e -> cards.show(root, "algo"));
        btns.add(edit);
        btns.add(again);
        south.add(btns, BorderLayout.EAST);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    // =====================================================================
    // Config page logic
    // =====================================================================

    private void openConfig() {
        boolean acs = algo.equals("ACS");
        configTitle.setText(acs ? (tier2Mode ? "ACS - Tier 1 + Tier 2" : "ACS - Tier 1 only") : "SJF");
        qLabel.setVisible(acs);
        qSpin.setVisible(acs);
        presetLabel.setVisible(!custom);
        presetBox.setVisible(!custom);
        addRowBtn.setVisible(custom);
        delRowBtn.setVisible(custom);

        // SJF does not use priority -> hide that column (the model keeps a dummy 1 for the Process constructor)
        inputTable.createDefaultColumnsFromModel();
        if (!acs) inputTable.removeColumn(inputTable.getColumnModel().getColumn(3));

        inputModel.setRowCount(0);
        if (custom) {
            inputModel.addRow(new Object[] { "P1", 0, 5, 1 });   // starter row to edit
        } else {
            presetBox.removeAllItems();
            for (Preset ps : Preset.forAlgo(algo, tier2Mode)) presetBox.addItem(ps);
            // addItem already fires the listener for the first item
        }
        cards.show(root, "config");
    }

    private void loadPreset(Preset ps) {
        inputModel.setRowCount(0);
        for (int i = 0; i < ps.data.length; i++) {
            inputModel.addRow(new Object[] { "P" + (i + 1), ps.data[i][0], ps.data[i][1], ps.data[i][2] });
        }
        csSpin.setValue(ps.cs);
        qSpin.setValue(ps.q);
    }

    /** Reads the table into Process objects. Shows a message and returns null if the input is bad. */
    private List<Process> readProcesses() {
        if (inputTable.isEditing()) inputTable.getCellEditor().stopCellEditing();
        if (inputModel.getRowCount() == 0) {
            error("Add at least one process.");
            return null;
        }
        List<Process> list = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        try {
            for (int r = 0; r < inputModel.getRowCount(); r++) {
                String id = String.valueOf(inputModel.getValueAt(r, 0)).trim();
                int arrival = Integer.parseInt(String.valueOf(inputModel.getValueAt(r, 1)).trim());
                int burst = Integer.parseInt(String.valueOf(inputModel.getValueAt(r, 2)).trim());
                int prio = Integer.parseInt(String.valueOf(inputModel.getValueAt(r, 3)).trim());
                if (id.isEmpty() || !ids.add(id)) { error("Row " + (r + 1) + ": ID is empty or duplicated."); return null; }
                if (arrival < 0) { error("Row " + (r + 1) + ": arrival must be >= 0."); return null; }
                if (burst < 1) { error("Row " + (r + 1) + ": burst must be >= 1."); return null; }
                list.add(new Process(id, arrival, burst, prio));
            }
        } catch (NumberFormatException ex) {
            error("Arrival, Burst and Priority must be whole numbers.");
            return null;
        }
        return list;
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Invalid input", JOptionPane.WARNING_MESSAGE);
    }

    // =====================================================================
    // Simulation
    // =====================================================================

    /** SJFScheduler and ACSScheduler share the same 3 methods but no common interface, so we wrap them. */
    private interface Sim {
        void arrive(Process p);
        ExecutionSnapshot step(int t, LocalTime start);
        int completed();
    }

    private void runSimulation() {
        List<Process> procs = readProcesses();
        if (procs == null) return;

        int cs = (Integer) csSpin.getValue();
        int q = (Integer) qSpin.getValue();

        // ACS Tier-1-only mode: warn if some process will not fit in one quantum
        if (algo.equals("ACS") && !tier2Mode) {
            for (Process p : procs) {
                if (p.burstTime > q) {
                    int ans = JOptionPane.showConfirmDialog(this,
                            p.id + " has burst " + p.burstTime + " > Q (" + q + ") so it will be demoted to Tier 2.\n"
                                    + "Continue anyway?", "Tier 1 only", JOptionPane.YES_NO_OPTION);
                    if (ans != JOptionPane.YES_OPTION) return;
                    break;
                }
            }
        }

        Sim sim;
        if (algo.equals("SJF")) {
            SJFScheduler s = new SJFScheduler(cs);
            sim = new Sim() {
                public void arrive(Process p) { s.arrive(p); }
                public ExecutionSnapshot step(int t, LocalTime st) { return s.step(t, st); }
                public int completed() { return s.getCompletedCount(); }
            };
        } else {
            ACSScheduler s = new ACSScheduler(cs, q);   // constructor order: (cs, quantum)
            sim = new Sim() {
                public void arrive(Process p) { s.arrive(p); }
                public ExecutionSnapshot step(int t, LocalTime st) { return s.step(t, st); }
                public int completed() { return s.getCompletedCount(); }
            };
        }

        // main loop: deliver arrivals BEFORE step()
        LocalTime start = LocalTime.of(0, 0, 0);
        List<ExecutionSnapshot> snaps = new ArrayList<>();
        int t = 0;
        while (sim.completed() < procs.size() && t < MAX_TIME) {
            for (Process p : procs) if (p.arrivalTime == t) sim.arrive(p);
            snaps.add(sim.step(t, start));
            t++;
        }
        boolean stuck = sim.completed() < procs.size();

        showResults(procs, snaps, stuck);
    }

    private void showResults(List<Process> procs, List<ExecutionSnapshot> snaps, boolean stuck) {
        // trace
        StringBuilder sb = new StringBuilder();
        for (ExecutionSnapshot e : snaps) {
            sb.append(String.format("t=%-3d %-15s %-4s | Q1=%s", e.time, e.state,
                    e.runningProcessId == null ? "-" : e.runningProcessId, e.tier1State));
            if (e.tier2State != null) sb.append(" | Q2=").append(e.tier2State);
            sb.append(" | ").append(e.eventNote).append('\n');
        }
        traceArea.setText(sb.toString());
        traceArea.setCaretPosition(0);

        // metrics table
        resultModel.setRowCount(0);
        double totalTat = 0, totalWt = 0;
        int finished = 0;
        for (Process p : procs) {
            if (p.remainingTime == 0) {
                resultModel.addRow(new Object[] { p.id, p.arrivalTime, p.burstTime, p.finishTime,
                        p.turnaroundTime, p.waitingTime, p.responseTime });
                totalTat += p.turnaroundTime;
                totalWt += p.waitingTime;
                finished++;
            } else {
                resultModel.addRow(new Object[] { p.id, p.arrivalTime, p.burstTime, "-", "-", "-", "-" });
            }
        }
        if (finished > 0) {
            avgLabel.setText(String.format("Average Waiting: %.2f    |    Average Turnaround: %.2f"
                    + (stuck ? "   (finished processes only)" : ""), totalWt / finished, totalTat / finished));
        } else {
            avgLabel.setText("No process finished.");
        }

        warnLabel.setText(stuck
                ? "Stopped at t=" + MAX_TIME + ": some processes never finished (Tier 2 not implemented yet?)"
                : " ");

        gantt.setSnapshots(snaps);
        cards.show(root, "result");
    }

    // =====================================================================
    // Built-in test cases (copied from TestSJF / TestACS). data row = {arrival, burst, priority}
    // =====================================================================

    private static class Preset {
        final String name;
        final int cs, q;
        final int[][] data;

        Preset(String name, int cs, int q, int[][] data) {
            this.name = name; this.cs = cs; this.q = q; this.data = data;
        }

        @Override public String toString() { return name; }

        static Preset[] forAlgo(String algo, boolean tier2) {
            if (algo.equals("SJF")) {
                return new Preset[] {
                    new Preset("Test 1 (CS=1, preemption)", 1, 3, new int[][] { {0,5,1}, {1,2,1} }),
                    new Preset("Test 2 (CS=0, assignment case 1)", 0, 3, new int[][] { {0,4,2}, {1,7,3}, {3,2,1} }),
                    new Preset("Test 3 (CS=1, idle gap)", 1, 3, new int[][] { {0,2,1}, {5,4,1}, {7,1,1} }),
                    new Preset("Test 4 (CS=1, assignment case 2)", 1, 3, new int[][] { {0,5,4}, {1,6,2}, {4,4,3} }),
                    new Preset("Test 5 (CS=0, ties)", 0, 3, new int[][] { {0,5,1}, {1,4,1}, {2,4,1} }),
                };
            }
            if (!tier2) {   // ACS Tier 1 only: every burst <= Q so nothing is demoted
                return new Preset[] {
                    new Preset("Tier 1 - all bursts <= Q (CS=0)", 0, 3, new int[][] { {0,2,1}, {1,3,1}, {2,1,1} }),
                    new Preset("Tier 1 - with context switch (CS=1)", 1, 3, new int[][] { {0,2,1}, {1,3,1}, {2,1,1} }),
                };
            }
            return new Preset[] {   // ACS full
                new Preset("ACS case 1 (Q=3, CS=0)", 0, 3, new int[][] { {0,4,2}, {1,7,3}, {3,2,1} }),
                new Preset("ACS case 2 (Q=3, CS=1)", 1, 3, new int[][] { {0,5,4}, {1,6,2}, {4,4,3} }),
            };
        }
    }

    // =====================================================================
    // Gantt chart
    // =====================================================================

    private static class GanttPanel extends JPanel {
        private static final int W = 30, H = 40, LEFT = 10, TOP = 12;
        private static final Color[] PALETTE = {
            new Color(0x6FA8DC), new Color(0xF6B26B), new Color(0x93C47D), new Color(0xE06666),
            new Color(0xB4A7D6), new Color(0xFFD966), new Color(0x76A5AF), new Color(0xC27BA0) };
        private List<ExecutionSnapshot> snaps = new ArrayList<>();
        private final Map<String, Color> colors = new HashMap<>();

        void setSnapshots(List<ExecutionSnapshot> s) {
            snaps = s;
            colors.clear();
            setPreferredSize(new Dimension(LEFT * 2 + s.size() * W, TOP + H + 30));
            revalidate();
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (int i = 0; i < snaps.size(); i++) {
                ExecutionSnapshot e = snaps.get(i);
                int x = LEFT + i * W;
                String label;
                Color c;
                switch (e.state) {
                    case RUNNING:
                        c = colors.computeIfAbsent(e.runningProcessId, k -> PALETTE[colors.size() % PALETTE.length]);
                        label = e.runningProcessId;
                        break;
                    case CONTEXT_SWITCH:
                        c = Color.GRAY;
                        label = "CS";
                        break;
                    default:
                        c = new Color(0xEEEEEE);
                        label = "idle";
                }
                g.setColor(c);
                g.fillRect(x, TOP, W, H);
                g.setColor(Color.BLACK);
                g.drawRect(x, TOP, W, H);
                g.setFont(g.getFont().deriveFont(10f));
                g.drawString(label, x + 4, TOP + H / 2 + 4);
                g.drawString(String.valueOf(e.time), x + 2, TOP + H + 14);
            }
        }
    }
}
