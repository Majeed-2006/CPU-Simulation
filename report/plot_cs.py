import csv
import os
import sys
import matplotlib
matplotlib.use("Agg")          
import matplotlib.pyplot as plt

here = os.path.dirname(os.path.abspath(__file__))
results_dir = os.path.join(here, "..", "results")
graphs_dir = os.path.join(here, "graphs")
REQUIRED = ["test", "cs", "avg_waiting", "avg_turnaround"]


def read_results(path):
    data = {}
    with open(path, newline="") as f:
        reader = csv.DictReader(f)
        missing = [c for c in REQUIRED if c not in (reader.fieldnames or [])]
        if missing:
            print("  Problem: these columns are missing in the CSV:", ", ".join(missing))
            return None
        for row in reader:
            data.setdefault(row["test"].strip(), []).append(
                [int(row["cs"]), float(row["avg_waiting"]), float(row["avg_turnaround"])]
            )
    for rows in data.values():
        rows.sort()           
    return data


def find_csv(algo):
    for name in ("results_%s.csv" % algo, "result_%s.csv" % algo):
        path = os.path.join(results_dir, name)
        if os.path.exists(path):
            return path
    return None


def plot_algorithm(algo):
    path = find_csv(algo)
    if path is None:
        print("[%s] file not found: results/results_%s.csv  (skipped)" % (algo.upper(), algo))
        return
    data = read_results(path)
    if not data:
        print("[%s] no data to draw (skipped)" % algo.upper())
        return

    os.makedirs(graphs_dir, exist_ok=True)
    label = algo.upper()

    # One graph per test case
    for name, rows in data.items():
        cs = [r[0] for r in rows]
        plt.figure(figsize=(6, 4))
        plt.plot(cs, [r[1] for r in rows], marker="o", label="Avg Waiting Time")
        plt.plot(cs, [r[2] for r in rows], marker="s", label="Avg Turnaround Time")
        plt.title("%s - %s: effect of Context Switch time" % (label, name))
        plt.xlabel("Context Switch time (CS)")
        plt.ylabel("Average time (time units)")
        plt.xticks(cs)
        plt.grid(True)
        plt.legend()
        plt.tight_layout()
        plt.savefig(os.path.join(graphs_dir, "%s_%s.png" % (name.replace(" ", "_"), algo)), dpi=150)
        plt.close()

    all_cs = sorted({r[0] for rows in data.values() for r in rows})
    plt.figure(figsize=(6, 4))
    for name, rows in data.items():
        plt.plot([r[0] for r in rows], [r[1] for r in rows], marker="o", label=name)
    plt.title("%s - Avg Waiting Time vs CS (all tests)" % label)
    plt.xlabel("Context Switch time (CS)")
    plt.ylabel("Average Waiting Time")
    plt.xticks(all_cs)
    plt.grid(True)
    plt.legend()
    plt.tight_layout()
    plt.savefig(os.path.join(graphs_dir, "all_tests_waiting_%s.png" % algo), dpi=150)
    plt.close()

    print("[%s] done: %d test(s) drawn -> %s" % (label, len(data), os.path.normpath(graphs_dir)))


algos = [a.lower() for a in sys.argv[1:]] or ["sjf", "acs"]
for a in algos:
    plot_algorithm(a)
