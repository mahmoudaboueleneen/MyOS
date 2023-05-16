package main.elements;

import main.elements.Process;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

public class Scheduler {
    private ArrayList<Process> processes;
    private Process currentRunningProcess;
    private Queue<Process> readyQueue;
    private Queue<Process> blockedQueue;
    private int nextProcessID;
    private int roundRobinTimeSlice;

    public Scheduler(){
        this.processes = new ArrayList<Process>();
        this.readyQueue = new ArrayDeque<Process>();
        this.blockedQueue = new ArrayDeque<Process>();
        this.nextProcessID = 0;
        // roundRobinTimeSlice
    }

    public ArrayList<Process> getProcesses() {return processes;}
    public Process getCurrentRunningProcess() {return currentRunningProcess;}
    public Queue<Process> getReadyQueue() {return readyQueue;}
    public Queue<Process> getBlockedQueue() {return blockedQueue;}
    public int getNextProcessID() {
        int temp = nextProcessID;
        nextProcessID++;
        return temp;
    }



}
