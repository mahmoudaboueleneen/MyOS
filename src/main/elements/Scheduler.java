package main.elements;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

public class Scheduler {
    private ArrayList<Process> arrivedProcesses;
    private ArrayList<Integer> burstTimes;
    private ArrayList<Process> inMemoryProcesses;
    private Queue<Process> readyQueue;
    private Queue<Process> blockedQueue;
    private int nextProcessID;
    private Process currentRunningProcess;
    private int roundRobinTimeSlice;
    private int timer;

    public Scheduler(int roundRobinTimeSlice){
        this.arrivedProcesses = new ArrayList<Process>();
        this.burstTimes = new ArrayList<Integer>();
        this.inMemoryProcesses = new ArrayList<Process>();
        this.readyQueue = new ArrayDeque<Process>();
        this.blockedQueue = new ArrayDeque<Process>();
        this.nextProcessID = 0;
        this.roundRobinTimeSlice = roundRobinTimeSlice;
        this.timer = 0;
    }

    public ArrayList<Process> getArrivedProcesses() {
        return arrivedProcesses;
    }
    public Process getCurrentRunningProcess() {
        return currentRunningProcess;
    }
    public ArrayList<Process> getInMemoryProcesses() {
        return inMemoryProcesses;
    }
    public Queue<Process> getReadyQueue() {
        return readyQueue;
    }
    public Queue<Process> getBlockedQueue() {
        return blockedQueue;
    }
    public int getNextProcessID() {
        int temp = nextProcessID;
        nextProcessID++;
        return temp;
    }

    public void addArrivedProcess(Process p){
        // Add process to arrived & change to ready state from new state
        this.arrivedProcesses.add(p);
        p.getPCB().setProcessState(ProcessState.READY);
    }

    public void addBurstTime(int linesOfCode){
        // Add burst time corresponding to arrived process
        this.burstTimes.add(linesOfCode);
    }

    public void executeRoundRobin() {

        boolean found = false;

        // Check if process is found in memory
        for(Process p : inMemoryProcesses){
            if(p.equals(currentRunningProcess)){
                found = true;
                break;
            }
        }

        // If found, execute
        if(found){

        }
        // Else, move to memory
        else {

        }

    }



}
