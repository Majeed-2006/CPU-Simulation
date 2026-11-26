import csv
import os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

here = os.path.dirname(os.path.abspath(__file__))
results_dir = os.path.join(here, "..", "results")
graphs_dir = os.path.join(here, "graphs")
csv_path = os.path.join(results_dir, "results_acs.csv")

os.makedirs(graphs_dir, exist_ok=True)


def read_results():
    data = {}

    with open(csv_path, newline="") as file:
        reader = csv.DictReader(file)

        for row in reader:
            test = row["test"]
            cs = int(row["cs"])
            quantum = int(row["quantum"])
            waiting = float(row["avg_waiting"])
            turnaround = float(row["avg_turnaround"])

            data.setdefault(test, []).append({
                "cs": cs,
                "quantum": quantum,
                "waiting": waiting,
                "turnaround": turnaround
            })

    return data


def draw_line_graph(test, rows, metric, metric_name, filename):
    plt.figure(figsize=(10, 6))

    quantums = sorted(set(row["quantum"] for row in rows))

    for q in quantums:
        q_rows = sorted(
            [row for row in rows if row["quantum"] == q],
            key=lambda row: row["cs"]
        )

        cs_values = [row["cs"] for row in q_rows]
        metric_values = [row[metric] for row in q_rows]

        plt.plot(
            cs_values,
            metric_values,
            marker="o",
            label=f"Quantum = {q}"
        )

    plt.xlabel("Context Switch Time (CS)")
    plt.ylabel(metric_name)
    plt.title(f"ACS - {test}: {metric_name}")
    plt.xticks(range(0, 5))
    plt.grid(True)
    plt.legend()
    plt.tight_layout()

    plt.savefig(os.path.join(graphs_dir, filename), dpi=150)
    plt.close()


def main():
    if not os.path.exists(csv_path):
        print("ACS CSV file not found:", csv_path)
        return

    data = read_results()

    for test, rows in data.items():
        draw_line_graph(
            test, rows,
            "waiting",
            "Average Waiting Time",
            f"{test}_avg_waiting_acs.png"
        )

        draw_line_graph(
            test, rows,
            "turnaround",
            "Average Turnaround Time",
            f"{test}_avg_turnaround_acs.png"
        )

    print("ACS line graphs generated successfully!")
    print("Output folder:", graphs_dir)


if __name__ == "__main__":
    main()