package main.kernel;

import main.elements.ProcessMemoryImage;
import main.elements.ProcessState;
import main.exceptions.InvalidInstructionException;

import java.io.*;
import java.util.*;

public class Scheduler {
    private final ArrayList<ProcessMemoryImage> arrivedProcessMemoryImages;
    private final ArrayList<Integer> burstTimes;
    private final ArrayList<ProcessMemoryImage> inMemoryProcessMemoryImages;
    private final Queue<ProcessMemoryImage> readyQueue;
    private final Queue<ProcessMemoryImage> blockedQueue;
    private int nextProcessID;
    private ProcessMemoryImage currentRunningProcessMemoryImage;
    private final int roundRobinTimeSlice;
    //private int timer;

    public Scheduler(int roundRobinTimeSlice){
        this.arrivedProcessMemoryImages = new ArrayList<>();
        this.burstTimes = new ArrayList<>();
        this.inMemoryProcessMemoryImages = new ArrayList<>();
        this.readyQueue = new ArrayDeque<>();
        this.blockedQueue = new ArrayDeque<>();
        this.nextProcessID = 0;
        this.roundRobinTimeSlice = roundRobinTimeSlice;
        //this.timer = 0;
    }

    public synchronized ArrayList<ProcessMemoryImage> getArrivedProcessMemoryImages() {return arrivedProcessMemoryImages;}
    public synchronized ProcessMemoryImage getCurrentRunningProcessMemoryImage() {return currentRunningProcessMemoryImage;}
    public synchronized ArrayList<ProcessMemoryImage> getInMemoryProcesses() {
        return inMemoryProcessMemoryImages;
    }
    public synchronized Queue<ProcessMemoryImage> getReadyQueue() {
        return readyQueue;
    }
    public synchronized Queue<ProcessMemoryImage> getBlockedQueue() {
        return blockedQueue;
    }
    public synchronized int getNextProcessID() {
        int temp = nextProcessID;
        nextProcessID++;
        return temp;
    }

//  Add process to arrived & change its state from NEW to READY
    public synchronized void addArrivedProcess(ProcessMemoryImage p){
        this.arrivedProcessMemoryImages.add(p);
    }

//  Add burst time corresponding to arrived process
    public synchronized void addBurstTime(int linesOfCode){
        this.burstTimes.add(linesOfCode);
    }

    public synchronized void addToReadyQueue(ProcessMemoryImage p) {
        this.readyQueue.add(p);
        p.getPCB().setProcessState(ProcessState.READY);
    }


//    public synchronized void printQueues(){
//        System.out.println("Ready Queue: {");
//        Iterator<ProcessMemoryImage> iterator = readyQueue.iterator();
//        while (iterator.hasNext()) {
//            int processID = iterator.next().getPCB().getProcessID();
//            System.out.println(processID + " ");
//        }
//    }

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
            delay = roundRobinTimeSlice * 1000;
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
        }, delay, roundRobinTimeSlice * 1000);




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
