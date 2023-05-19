package main.kernel;

import main.elements.ProcessMemoryImage;
import main.elements.ProcessState;
import main.exceptions.InvalidInstructionException;

import java.io.*;
import java.util.*;

public class Scheduler {
    private final ArrayList<ProcessMemoryImage> primaryProcessTable;
    private final ArrayList<Integer> totalBurstTimes;
    private final ArrayList<ProcessMemoryImage> inMemoryProcessMemoryImages;
    private final Queue<ProcessMemoryImage> readyQueue;
    private final Queue<ProcessMemoryImage> blockedQueue;
    private ProcessMemoryImage currentRunningProcessMemoryImage;
    private final int instructionsPerTimeSlice;
    private int maximumUsedProcessID;

    public Scheduler(int instructionsPerTimeSlice){
        this.primaryProcessTable = new ArrayList<>();
        this.totalBurstTimes = new ArrayList<>();
        this.inMemoryProcessMemoryImages = new ArrayList<>();
        this.readyQueue = new ArrayDeque<>();
        this.blockedQueue = new ArrayDeque<>();
        this.instructionsPerTimeSlice = instructionsPerTimeSlice;
        this.maximumUsedProcessID = -1; // First arrived process will be given ID -1 + 1 = 0
    }

    public synchronized ArrayList<ProcessMemoryImage> getPrimaryProcessTable() {return primaryProcessTable;}
    public synchronized ProcessMemoryImage getCurrentRunningProcessMemoryImage() {return currentRunningProcessMemoryImage;}
    public synchronized ArrayList<ProcessMemoryImage> getInMemoryProcesses() {return inMemoryProcessMemoryImages;}
    public synchronized Queue<ProcessMemoryImage> getReadyQueue() {
        return readyQueue;
    }
    public synchronized Queue<ProcessMemoryImage> getBlockedQueue() {
        return blockedQueue;
    }

    public synchronized int getNextProcessID() {
        maximumUsedProcessID++;

        final int MAX_POSSIBLE_PID = 100;
        if(maximumUsedProcessID > MAX_POSSIBLE_PID){
//          Reset PID to 0 if it crosses Max Possible PID
            maximumUsedProcessID = 0;
//          After resetting to 0, we have to make sure that
//          there is no other existing process with ID 0.
//          Otherwise, find the first unique PID number that isn't
//          acquired by any other process.
            for(int i=0; i<101; i++) {
                for (ProcessMemoryImage p : primaryProcessTable) {
                    if (i != p.getPCB().getProcessID())
                        return i;
                    i++;
                }
            }

        }
        return maximumUsedProcessID;
    }

    public synchronized void addArrivedProcess(ProcessMemoryImage p) {
        this.primaryProcessTable.add(p);
    }

//  Add burst time corresponding to arrived process
    public synchronized void addBurstTime(int linesOfCode) {
        this.totalBurstTimes.add(linesOfCode);
    }

    public synchronized void addToReadyQueue(ProcessMemoryImage p) {
        this.readyQueue.add(p);
        p.getPCB().setProcessState(ProcessState.READY);
    }

    public synchronized void printReadyQueue(){
        System.out.println("Ready Queue (PIDs): FRONT -> "  + readyQueue);
    }

    public synchronized void printBlockedQueue(){
        System.out.println("Blocked Queue (PIDs): FRONT -> " + blockedQueue);
    }

    public synchronized void printCurrentRunningProcess(){
        System.out.println("Current Running Process(ID): " + currentRunningProcessMemoryImage);
    }

//  Serialize Process Memory Image object
    public synchronized void swapOutToDisk(ProcessMemoryImage p){
        try {
            FileOutputStream fileOut = new FileOutputStream("src/temp/" + p.getPCB().getProcessID() + ".ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(p);
            out.close();
            fileOut.close();
            System.out.printf("Serialized data is saved in src/temp/" + p.getPCB().getProcessID() + ".ser");
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

//  Deserialize Process Memory Image object
    public synchronized ProcessMemoryImage swapInFromDisk(String location){
        ProcessMemoryImage p = null;
        try {
            FileInputStream fileIn = new FileInputStream(location);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            p = (ProcessMemoryImage) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException e ) {
            e.printStackTrace();
        }
        return p;
    }

    private synchronized void assignNewRunningProcess(){
        currentRunningProcessMemoryImage = readyQueue.remove();
        currentRunningProcessMemoryImage.getPCB().setProcessState(ProcessState.RUNNING);
    }

    private synchronized void preemptCurrentProcess() {
        readyQueue.add(currentRunningProcessMemoryImage);
        currentRunningProcessMemoryImage.getPCB().setProcessState(ProcessState.READY);
    }

    public synchronized void executeRoundRobinWithInstructions() {
        while(! readyQueue.isEmpty()) {
            assignNewRunningProcess();
            while(currentRunningProcessMemoryImage.getInstructions().length < 2) {

            }
            preemptCurrentProcess();
        }
    }

    public synchronized void executeRoundRobin() {
        if(readyQueue.isEmpty()) {
            return;
        }
        int delay = 0;
        if(currentRunningProcessMemoryImage == null) {
            assignNewRunningProcess();
            delay = instructionsPerTimeSlice * 1000;
        }
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                preemptCurrentProcess();
                assignNewRunningProcess();
            }

            @Override
            public boolean cancel() {
                return super.cancel();
            }
        }, delay, instructionsPerTimeSlice * 1000);




//      Check if process is found in memory
        if(inMemoryProcessMemoryImages.contains(currentRunningProcessMemoryImage)){
//          Process found in memory, so execute
            // while(current process execution time < scheduler's time slice variable)

//          Fetch
            String instruction = Kernel.getInterpreter().getNextProcessInstruction(currentRunningProcessMemoryImage);

//          Decode & execute
            // SHOULD HANDLE THIS DIFFERENTLY!!!!!
            try {
                Kernel.getInterpreter().interpret( instruction, currentRunningProcessMemoryImage );
            } catch (InvalidInstructionException e) {
                // What to do if instruction has invalid syntax? Kill process?
                //
            }

//          Increment PC (We increment after executing to make sure that the instruction was executed successfully, so we can move on to the next)
            currentRunningProcessMemoryImage.getPCB().setProgramCounter( currentRunningProcessMemoryImage.getPCB().getProgramCounter()+1 );

        }

        else {
//      Process not found in memory, so move to memory THEN execute

        }
    }

}
