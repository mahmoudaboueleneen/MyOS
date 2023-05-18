package main.kernel;

import main.MyOS;
import main.elements.ProcessMemoryImage;
import main.elements.ProcessState;
import main.exceptions.InvalidInstructionException;

import java.io.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

public class Scheduler {
    private final ArrayList<ProcessMemoryImage> arrivedProcessMemoryImages;
    private final ArrayList<Integer> burstTimes;
    private final ArrayList<ProcessMemoryImage> inMemoryProcessMemoryImages;
    private final Queue<ProcessMemoryImage> readyQueue;
    private final Queue<ProcessMemoryImage> blockedQueue;
    private int nextProcessID;
    private ProcessMemoryImage currentRunningProcessMemoryImage;
    private final int roundRobinTimeSlice;
    private int timer;

    public Scheduler(int roundRobinTimeSlice){
        this.arrivedProcessMemoryImages = new ArrayList<>();
        this.burstTimes = new ArrayList<>();
        this.inMemoryProcessMemoryImages = new ArrayList<>();
        this.readyQueue = new ArrayDeque<>();
        this.blockedQueue = new ArrayDeque<>();
        this.nextProcessID = 0;
        this.roundRobinTimeSlice = roundRobinTimeSlice;
        this.timer = 0;
    }

    public ArrayList<ProcessMemoryImage> getArrivedProcesses() {
        return arrivedProcessMemoryImages;
    }
    public ProcessMemoryImage getCurrentRunningProcess() {
        return currentRunningProcessMemoryImage;
    }
    public ArrayList<ProcessMemoryImage> getInMemoryProcesses() {
        return inMemoryProcessMemoryImages;
    }
    public Queue<ProcessMemoryImage> getReadyQueue() {
        return readyQueue;
    }
    public Queue<ProcessMemoryImage> getBlockedQueue() {
        return blockedQueue;
    }
    public int getNextProcessID() {
        int temp = nextProcessID;
        nextProcessID++;
        return temp;
    }

    public void addArrivedProcess(ProcessMemoryImage p){
        // Add process to arrived & change its state from NEW to READY
        this.arrivedProcessMemoryImages.add(p);
        p.getPCB().setProcessState(ProcessState.READY);
    }

    public void addBurstTime(int linesOfCode){
        // Add burst time corresponding to arrived process
        this.burstTimes.add(linesOfCode);
    }

//    public void printQueues(){
//        System.out.println("Ready Queue: {");
//        Iterator<Process> iterator = readyQueue.iterator();
//        while (iterator.hasNext()) {
//            int processID = iterator.next().getPCB().getProcessID();
//            System.out.println(processID + " ");
//        }
//    }

    // Serialize Process Memory Image
    public void swapOutToDisk(ProcessMemoryImage p){
        try {
            FileOutputStream fileOut = new FileOutputStream("src/temp/" + p.getPCB().getProcessID() + ".txt");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(p);
            out.close();
            fileOut.close();
            System.out.printf("Serialized data is saved in src/temp/" + p.getPCB().getProcessID() + ".txt");
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    // Deserialize Process Memory Image
    public ProcessMemoryImage swapInFromDisk(String location){
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

    public void executeRoundRobin() {

        boolean found = false;

        // Check if process is found in memory
        for(ProcessMemoryImage p : inMemoryProcessMemoryImages){
            if(p.equals(currentRunningProcessMemoryImage)){
                found = true;
                break;
            }
        }

        // If found, execute
        if(found){
            // while(current process execution time < scheduler's time slice variable)

            // Fetch
            String instruction = MyOS.getInterpreter().getNextProcessInstruction(currentRunningProcessMemoryImage);

            // Decode & execute
            // SHOULD HANDLE THIS DIFFERENTLY!!!!!
            try {
                MyOS.getInterpreter().interpret( instruction );
            } catch (InvalidInstructionException e) {
                throw new RuntimeException(e);
            }

            // Increment PC (We increment after executing to make sure that the instruction was executed successfully, so we can move on to the next)
            currentRunningProcessMemoryImage.getPCB().setProgramCounter( currentRunningProcessMemoryImage.getPCB().getProgramCounter()+1 );

        }
        // Else, move to memory
        else {

        }
    }

}
