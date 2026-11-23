import csv
import os
import matplotlib
matplotlib.use("Agg")          # save to files, no window needed
import matplotlib.pyplot as plt

# Paths are relative to this script, so it works from any folder
here = os.path.dirname(os.path.abspath(__file__))
csv_path = os.path.join(here, "..", "results", "results.csv")
graphs_dir = os.path.join(here, "graphs")
os.makedirs(graphs_dir, exist_ok=True)

# Read results.csv (created by TestSJF): test,cs,avg_waiting,avg_turnaround
data = {}   # {"Test 1": [[cs, waiting, turnaround], ...]}
with open(csv_path) as f:
    for row in csv.DictReader(f):
        data.setdefault(row["test"], []).append(
            [int(row["cs"]), float(row["avg_waiting"]), float(row["avg_turnaround"])]
        )

# One graph per test case
for name, rows in data.items():
    cs = [r[0] for r in rows]
    waiting = [r[1] for r in rows]
    turnaround = [r[2] for r in rows]

    plt.figure(figsize=(6, 4))
    plt.plot(cs, waiting, marker="o", label="Avg Waiting Time")
    plt.plot(cs, turnaround, marker="s", label="Avg Turnaround Time")
    plt.title("SJF - " + name + ": effect of Context Switch time")
    plt.xlabel("Context Switch time (CS)")
    plt.ylabel("Average time (time units)")
    plt.xticks(cs)
    plt.grid(True)
    plt.legend()
    plt.tight_layout()
    plt.savefig(os.path.join(graphs_dir, name.replace(" ", "_") + ".png"), dpi=150)
    plt.close()

# One summary graph: average waiting time of all tests together
plt.figure(figsize=(6, 4))
for name, rows in data.items():
    plt.plot([r[0] for r in rows], [r[1] for r in rows], marker="o", label=name)
plt.title("SJF - Avg Waiting Time vs CS (all tests)")
plt.xlabel("Context Switch time (CS)")
plt.ylabel("Average Waiting Time")
plt.xticks([0, 1, 2, 3, 4])
plt.grid(True)
plt.legend()
plt.tight_layout()
plt.savefig(os.path.join(graphs_dir, "all_tests_waiting.png"), dpi=150)
plt.close()

print("Done. Graphs saved in:", graphs_dir)