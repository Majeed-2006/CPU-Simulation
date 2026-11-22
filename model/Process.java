package model;
public class Process {
    // 1. Basic input attributes
    public String id;
    public int arrivalTime;
    public int burstTime;
    public int initialPriority;

    // 2. Dynamic runtime attributes
    public int remainingTime;
    public int currentPriority;
    public int tier2ArrivalTime;
    public boolean isStarted;

    // 3. Final calculated metrics
    public int startTime;
    public int finishTime;
    public int waitingTime;
    public int turnaroundTime;
    public int responseTime;

    public Process(String id, int arrivalTime, int burstTime, int initialPriority) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.initialPriority = initialPriority;
        
        // Initialize default runtime values
        this.remainingTime = burstTime;
        this.currentPriority = initialPriority;
        this.tier2ArrivalTime = -1;
        this.isStarted = false;
    }

    // Returns a deep copy to isolate data across different schedulers
    public Process copy() {
        return new Process(this.id, this.arrivalTime, this.burstTime, this.initialPriority);
    }

    // Computes final scheduling metrics upon process completion
    public void calculateFinalMetrics(int finishTime) {
        this.finishTime = finishTime;
        this.turnaroundTime = this.finishTime - this.arrivalTime;
        this.waitingTime = this.turnaroundTime - this.burstTime;
        this.responseTime = this.startTime - this.arrivalTime;
    }

    // Formats process state for queue trace output
    @Override
    public String toString() {
        return id + " (remaining: " + remainingTime + ", Priority: " + currentPriority + ")";
    }
}