package main.kernel;

import main.elements.ProcessMemoryImage;
import main.elements.ProcessState;
import main.exceptions.InvalidInstructionException;
import java.io.*;
import java.util.*;

public class Scheduler {
    private final ArrayList<ProcessMemoryImage> processList;
    private final ArrayList<Integer> burstTimesList;
    private final ArrayList<ProcessMemoryImage> inMemoryProcessMemoryImages;
    private static Queue<ProcessMemoryImage> readyQueue;
    private static Queue<ProcessMemoryImage> blockedQueue;
    private ProcessMemoryImage currentRunningProcessMemoryImage;
    private final int instructionsPerTimeSlice;
    private int maximumUsedPID;
    private static int instructionCycle;
    private final int[] scheduledArrivalTimes;
    private final String[] scheduledArrivalFileLocations;
    private boolean firstArrivalsHandled;

    public Scheduler(int instructionsPerTimeSlice, int[] scheduledArrivalTimes, String[] scheduledArrivalFileLocations){
        this.processList = new ArrayList<>();
        this.burstTimesList = new ArrayList<>();
        this.inMemoryProcessMemoryImages = new ArrayList<>();
        readyQueue = new ArrayDeque<>();
        blockedQueue = new ArrayDeque<>();
        this.instructionsPerTimeSlice = instructionsPerTimeSlice;
        this.maximumUsedPID = -1; // First arrived process will be given ID -1 + 1 = 0
        this.scheduledArrivalTimes = scheduledArrivalTimes;
        this.scheduledArrivalFileLocations = scheduledArrivalFileLocations;
    }

    public synchronized Queue<ProcessMemoryImage> getReadyQueue() {
        return readyQueue;
    }

    public synchronized Queue<ProcessMemoryImage> getBlockedQueue() {
        return blockedQueue;
    }

    public synchronized ArrayList<ProcessMemoryImage> getInMemoryProcesses() {
        return inMemoryProcessMemoryImages;
    }

    /*
     * Generates new PID for a new Process.
     * Resets that PID to 0 if it crosses Max Possible PID,
     * and finds the first unique PID number that
     * isn't acquired by any other process.
     */
    public synchronized int getNextProcessID() {
        final int MAX_POSSIBLE_PID = 100;
        maximumUsedPID++;
        if(maximumUsedPID > MAX_POSSIBLE_PID){
            maximumUsedPID = 0;
            for(int currentPID=0; currentPID<101; currentPID++) {
                for (ProcessMemoryImage p : processList) {
                    if (currentPID != p.getPCB().getProcessID())
                        return currentPID;
                    currentPID++;
                }
            }
        }
        return maximumUsedPID;
    }

    public synchronized void addArrivedProcess(ProcessMemoryImage p) {
        this.processList.add(p);
    }

    public synchronized void addBurstTime(int linesOfCode) {
        this.burstTimesList.add(linesOfCode);
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
        System.out.println("Current Running Process(ID): " + currentRunningProcessMemoryImage.toString());
    }

    public static void incrementInstructionCycleAndPrintMemory(){
        System.out.println("\n**********************************************");
        System.out.println("Memory at inst. cycle = " + instructionCycle + " instructions executed:\n");
        System.out.println(Kernel.getMemory());
        instructionCycle++;
    }

    public synchronized void executeRoundRobin(){
        if(!firstArrivalsHandled)
            checkAndHandleFirstProcessArrivals();

        int remInstructions = instructionsPerTimeSlice;
        if(readyQueue.isEmpty()){
            finalizeOutput();
            return;
        }
        assignNewRunningProcess();
        ProcessMemoryImage currProcess = currentRunningProcessMemoryImage;

        while(remInstructions > 0) {
            if (!inMemoryProcessMemoryImages.contains(currProcess))
                moveCurrentRunningProcessToMemory();
            if (currProcess.hasNextInstruction()) {
                String instruction = Kernel.getInterpreter().getNextProcessInstruction(currProcess);
                if (hasNestedInstruction(instruction)){
                    String innerInstruction = getNestedInstruction(instruction);
                    decodeAndExecute(innerInstruction);
                    remInstructions--;
                    checkAndHandleProcessArrivals();
                }
                currProcess.incrementPC();

                decodeAndExecute(instruction);
                remInstructions--;
                checkAndHandleProcessArrivals();

                if(currProcess.getPCB().getProcessState() == ProcessState.BLOCKED)
                    return;
            } else {
                finishCurrentRunningProcess();
                return;
            }
        }

        // After running for a time slice, check again if the process has any instructions left.
        if(currProcess.hasNextInstruction())
            preemptCurrentRunningProcess();
        else
            finishCurrentRunningProcess();
    }

    private synchronized void assignNewRunningProcess(){
        currentRunningProcessMemoryImage = readyQueue.remove();
        currentRunningProcessMemoryImage.getPCB().setProcessState(ProcessState.RUNNING);
    }

    //TODO: Finish method.
    private synchronized void moveCurrentRunningProcessToMemory() {
        Object[] canFitWhereInMemory = Kernel.canFitWhereInMemory(currentRunningProcessMemoryImage.getInstructions().length);
        boolean canFitInMemory = (boolean) canFitWhereInMemory[0];

        if ( !canFitInMemory ) {
            while ( !canFitInMemory ) {
                // Kick process
                ProcessMemoryImage processToBeSwappedOut = processToSwapOutToDisk();
                swapOutToDisk(processToBeSwappedOut);

                // Compact memory
                Kernel.getMemory().compactMemory();

                // Prepare to check again if process can fit
                canFitWhereInMemory = Kernel.canFitWhereInMemory(currentRunningProcessMemoryImage.getInstructions().length);
                canFitInMemory = (boolean) canFitWhereInMemory[0];
            }
        }
        swapInFromDisk(currentRunningProcessMemoryImage.getPCB().getTempLocation(), (Integer) canFitWhereInMemory[1], (Integer) canFitWhereInMemory[2]);
        inMemoryProcessMemoryImages.add(currentRunningProcessMemoryImage);
    }

    public synchronized static ProcessMemoryImage processToSwapOutToDisk(){
        ProcessMemoryImage p = null;

        if(!blockedQueue.isEmpty())
            p = ((ArrayDeque<ProcessMemoryImage>)blockedQueue).getLast();
        else if(!readyQueue.isEmpty())
            p = ((ArrayDeque<ProcessMemoryImage>)readyQueue).getLast();

        return p;
    }

    public synchronized void swapOutToDisk(ProcessMemoryImage p){
        String location = "src/temp/PID_" + p.getPCB().getProcessID() + ".ser";
        try {
            FileOutputStream fileOut = new FileOutputStream(location);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(p);
            out.close();
            fileOut.close();
            System.out.printf("Serialized data is saved in src/temp/" + p.getPCB().getProcessID() + ".ser");
        } catch (IOException i) {
            i.printStackTrace();
        }
        p.getPCB().setTempLocation(location);
        int lowerBound = p.getPCB().getLowerMemoryBoundary();
        int upperBound = p.getPCB().getUpperMemoryBoundary();
        Kernel.getMemory().deallocateMemoryPartition(lowerBound, upperBound);
        Kernel.getMemory().clearMemoryPartition(lowerBound, upperBound);
        inMemoryProcessMemoryImages.remove(p);
    }

    public synchronized void swapInFromDisk(String location, int lowerBound, int upperBound){
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
        p.getPCB().setTempLocation("---");
        Kernel.getMemory().allocateMemoryPartition(lowerBound, upperBound);
        Kernel.getMemory().fillMemoryPartition(p, lowerBound, upperBound);
        inMemoryProcessMemoryImages.add(p);
        //return p;
    }

    private synchronized void finishCurrentRunningProcess(){
        currentRunningProcessMemoryImage.getPCB().setProcessState(ProcessState.FINISHED);
        processList.remove(currentRunningProcessMemoryImage);
    }

    private synchronized void preemptCurrentRunningProcess() {
        readyQueue.add(currentRunningProcessMemoryImage);
        currentRunningProcessMemoryImage.getPCB().setProcessState(ProcessState.READY);
    }

    private boolean hasNestedInstruction(String instruction){
        String[] words = instruction.split(" ");
        return words[0].equals("assign") && words[2].equals("readFile");
    }

    private String getNestedInstruction(String fullInstruction){
        String[] words = fullInstruction.split(" ");
        return words[2] + " " + words[3];
    }

    private void decodeAndExecute(String instruction){
        try {
            Kernel.getInterpreter().interpret(instruction, currentRunningProcessMemoryImage);
        }
        catch (InvalidInstructionException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void checkAndHandleProcessArrivals(){
        for(int time : scheduledArrivalTimes){
            if(time == instructionCycle){
                //Notify user and create process
                System.out.println("Process " + scheduledArrivalTimes[time] + " arrived at time = " + scheduledArrivalTimes[time] + " instruction(s) executed.");
                Kernel.createNewProcess(scheduledArrivalFileLocations[time]);
            }
        }
    }

    private void checkAndHandleFirstProcessArrivals(){
        for(int time : scheduledArrivalTimes){
            if(time == instructionCycle){
                //Notify user and create process
                System.out.println("Process " + scheduledArrivalTimes[time] + " arrived at time = " + scheduledArrivalTimes[time] + " instruction(s) executed.");
                Kernel.createNewProcess(scheduledArrivalFileLocations[time]);
            }
        }
        firstArrivalsHandled = true;
    }

    //TODO: Finish method.
    /*
     * Executed when the whole program is finished.
     */
    private void finalizeOutput(){
        System.out.println("All processes finished.");
    }


}
