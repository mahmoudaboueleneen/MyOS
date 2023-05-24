package main.kernel;

import main.elements.*;
import main.exceptions.InvalidInstructionException;
import main.exceptions.VariableAssignmentException;

import java.io.*;
import java.util.*;

//TODO: Use burstTimesList? (commented out)
public class Scheduler {
    // Lists
    private static ArrayList<ProcessMemoryImage> processList;
    private static ArrayList<ProcessMemoryImage> inMemoryProcessMemoryImages;
    // Queues
    private static Queue<ProcessMemoryImage> readyQueue;
    private static Queue<ProcessMemoryImage> blockedQueue;
    // Other fields
    private ProcessMemoryImage currentRunningProcessMemoryImage;
    private final int instructionsPerTimeSlice;
    private static int maximumUsedPID;
    private static int instructionCycle;
    private final List<Integer> scheduledArrivalTimes;
    private final List<String> scheduledArrivalFileLocations;
    private boolean firstArrivalsHandled;
    private static List<String[]> executedInnerInstructions;
    //private static ArrayList<Integer> burstTimesList;

    public Scheduler(int instructionsPerTimeSlice, List<Integer> scheduledArrivalTimes, List<String> scheduledArrivalFileLocations){
        processList = new ArrayList<>();
        //burstTimesList = new ArrayList<>();
        inMemoryProcessMemoryImages = new ArrayList<>();
        readyQueue = new ArrayDeque<>();
        blockedQueue = new ArrayDeque<>();
        this.instructionsPerTimeSlice = instructionsPerTimeSlice;
        maximumUsedPID = -1;
        this.scheduledArrivalTimes = scheduledArrivalTimes;
        this.scheduledArrivalFileLocations = scheduledArrivalFileLocations;
        executedInnerInstructions = new ArrayList<>();
    }

    public synchronized void executeRoundRobin(){
        int instrsLeftInTimeSlice = instructionsPerTimeSlice;

        if(!firstArrivalsHandled)
            checkAndHandleFirstProcessArrivals();

        if(readyQueue.isEmpty())
            finalizeProgram();

        ProcessMemoryImage nextInLine = readyQueue.element();

        if (!inMemoryProcessMemoryImages.contains(nextInLine))
            moveProcessToMemory(nextInLine);

        assignNewRunningProcess();
        ProcessMemoryImage runningPMI = currentRunningProcessMemoryImage;

        while(instrsLeftInTimeSlice > 0) {
            printCurrentRunningProcess();

            if (runningPMI.hasNextInstruction()) {
                String instruction = Kernel.getInterpreter().getNextProcessInstruction(runningPMI);

                if ( hasNestedInstruction(instruction) && !isInnerInstructionAlreadyExecuted(instruction) ){
                    executeInnerInstruction(instruction);
                } else {
                    runningPMI.incrementPC();
                    executeInstruction(instruction);
                }
                instrsLeftInTimeSlice--;
                checkAndHandleProcessArrivals();
                if (runningPMI.getPCB().getProcessState() == ProcessState.BLOCKED)
                    return;
            } else {
                finishCurrentRunningProcess();
                return;
            }

        }

        if(runningPMI.hasNextInstruction())
            preemptCurrentRunningProcess();
        else
            finishCurrentRunningProcess();
    }

    private void checkAndHandleFirstProcessArrivals(){
        checkAndHandleProcessArrivals();
        firstArrivalsHandled = true;
    }

    private void checkAndHandleProcessArrivals(){
        System.out.println("\n**************************************************************************************************************************");
        System.out.println("CURRENT INSTRUCTION CYCLE = " + instructionCycle + " instruction(s) executed:\n");
        //System.out.println(Kernel.getMemory());

        Iterator<Integer> iterator = scheduledArrivalTimes.iterator();
        while(iterator.hasNext()){
            int time = iterator.next();
            if(time == instructionCycle){
                int index = scheduledArrivalTimes.indexOf(time);
                System.out.println("Process " + scheduledArrivalFileLocations.get(index) + " arrived at time = " + instructionCycle + " instruction(s) executed.");
                Kernel.createNewProcess(scheduledArrivalFileLocations.get(index));
                iterator.remove();
                scheduledArrivalFileLocations.remove(index);
            }
        }
    }

    private void finalizeProgram(){
        System.out.println("All processes finished.");
        System.exit(0);
    }

    private synchronized void assignNewRunningProcess(){
        currentRunningProcessMemoryImage = readyQueue.remove();
        currentRunningProcessMemoryImage.setProcessState(ProcessState.RUNNING);
        System.out.println("Scheduling event occurred: Process chosen to run.");
        printQueues();

        System.out.println(Kernel.getMemory());
    }

    private synchronized void moveCurrentRunningProcessToMemory() {
        moveProcessToMemory(currentRunningProcessMemoryImage);
    }

    public synchronized static void moveProcessToMemory(ProcessMemoryImage p){
        while (!p.canFitInMemory()) {
            swapOutToDisk(getProcessToSwapOutToDisk());
            Memory.compactMemory();
        }
        int[] bounds = p.getNewPossibleMemoryBounds();
        int newLowerBound = bounds[0];
        int newUpperBound = bounds[1];
        swapInFromDisk(p.getPCB().getTempLocation(), newLowerBound, newUpperBound);
    }

    public synchronized static ProcessMemoryImage getProcessToSwapOutToDisk(){
        ProcessMemoryImage p = null;
        if(!blockedQueue.isEmpty())
            p = ((ArrayDeque<ProcessMemoryImage>)blockedQueue).getLast();
        else if(!readyQueue.isEmpty())
            p = ((ArrayDeque<ProcessMemoryImage>)readyQueue).getLast();
        return p;
    }

    public static synchronized void swapOutToDisk(ProcessMemoryImage p){
        String location = "src/temp/PID_"+ p.getPCB().getProcessID() +".ser";
        p.setTempLocation(location);
        serializeProcess(p, location);

        // Clear memory partition
        Memory.clearMemoryPartition(p.getPCB().getLowerMemoryBoundary(), p.getPCB().getUpperMemoryBoundary());

        // Remove process from tracked inMemoryProcessMemoryImages
        Iterator<ProcessMemoryImage> iterator = inMemoryProcessMemoryImages.iterator();
        while(iterator.hasNext()){
            ProcessMemoryImage temp = iterator.next();
            if(temp.getPCB().getProcessID() == p.getPCB().getProcessID())
                iterator.remove();
        }

        System.out.println("Process with PID = "+ p.getPCB().getProcessID() +" swapped out to disk.\n");
    }

    public static synchronized void swapInFromDisk(String location, int newLowerBound, int newUpperBound){
        ProcessMemoryImage p = deserializeProcess(location);
        p.setTempLocation("---");
        p.setLowerMemoryBoundary(newLowerBound);
        p.setUpperMemoryBoundary(newUpperBound);
        Memory.fillMemoryPartitionWithProcess(p);
        inMemoryProcessMemoryImages.add(p);

        System.out.println("Process with PID = "+ p.getPCB().getProcessID() +" swapped in from disk.\n");
    }

    private static synchronized void serializeProcess(ProcessMemoryImage p, String location){
        try {
            FileOutputStream fileOut = new FileOutputStream(location);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(p);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    private static synchronized ProcessMemoryImage deserializeProcess(String location){
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

    private synchronized void preemptCurrentRunningProcess() {
        readyQueue.add(currentRunningProcessMemoryImage);
        currentRunningProcessMemoryImage.setProcessState(ProcessState.READY);
    }

    private synchronized static boolean hasNestedInstruction(String instruction){
        String[] words = instruction.split(" ");
        return words[0].equals("assign") && words[2].equals("readFile");
    }

    private synchronized static String getInnerInstruction(String fullInstruction){
        String[] words = fullInstruction.split(" ");
        return words[2] + " " + words[3];
    }

    private synchronized static boolean isInnerInstructionAlreadyExecuted(String instruction){
        for(String[] pair : executedInnerInstructions){
            if(pair[0].equals(instruction) && pair[1].equals( getInnerInstruction(instruction) ))
                return true;
        }
        return false;
    }

    private void executeInnerInstruction(String instruction){
        String innerInstruction = getInnerInstruction(instruction);
        executeInstruction(innerInstruction);

        String[] instrAndInnerInstrPair = new String[2];
        instrAndInnerInstrPair[0] = instruction;
        instrAndInnerInstrPair[1] = innerInstruction;
        executedInnerInstructions.add(instrAndInnerInstrPair);
    }

    private void executeInstruction(String instruction){
        try {
            Interpreter.interpretAndIncrementInstructionCycle(instruction, currentRunningProcessMemoryImage);
        }
        catch (InvalidInstructionException | VariableAssignmentException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private synchronized void finishCurrentRunningProcess(){
        currentRunningProcessMemoryImage.setProcessState(ProcessState.FINISHED);
        processList.remove(currentRunningProcessMemoryImage);
        inMemoryProcessMemoryImages.remove(currentRunningProcessMemoryImage);
        readyQueue.remove(currentRunningProcessMemoryImage);
        blockedQueue.remove(currentRunningProcessMemoryImage);

        Memory.clearMemoryPartition(currentRunningProcessMemoryImage.getPCB().getLowerMemoryBoundary(),
                currentRunningProcessMemoryImage.getPCB().getUpperMemoryBoundary());
        Memory.compactMemory();

        System.out.println("Scheduling event occurred: Process finished.");
        printQueues();
    }

    public static synchronized int getNextProcessID() {
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

    public static void incrementInstructionCycleAndPrintMemory(){
        instructionCycle++;
    }

    public synchronized void addToReadyQueue(ProcessMemoryImage p) {
        readyQueue.add(p);
        p.setProcessState(ProcessState.READY);
        System.out.println("Scheduling event occurred: Process added to ready queue.");
        printQueues();
    }

    public synchronized static void printQueues(){
        printReadyQueue();
        printBlockedQueue();
        System.out.println();
    }

    public static synchronized void printReadyQueue(){
        System.out.println("Ready Queue: FRONT -> "  + readyQueue);
    }

    public static synchronized void printBlockedQueue(){
        System.out.println("Blocked Queue: FRONT -> " + blockedQueue);
    }

    public synchronized void printCurrentRunningProcess(){
        System.out.println("Current running process: " + currentRunningProcessMemoryImage.toString());
    }

    public static synchronized void addArrivedProcess(ProcessMemoryImage p) {
        processList.add(p);
    }

    public static ArrayList<ProcessMemoryImage> getInMemoryProcessMemoryImages() {
        return inMemoryProcessMemoryImages;
    }

    public static synchronized Queue<ProcessMemoryImage> getReadyQueue() {
        return readyQueue;
    }

    public static synchronized Queue<ProcessMemoryImage> getBlockedQueue() {
        return blockedQueue;
    }

    public static synchronized ArrayList<ProcessMemoryImage> getInMemoryProcesses() {
        return inMemoryProcessMemoryImages;
    }

    //public synchronized void addBurstTime(int linesOfCode) {burstTimesList.add(linesOfCode);}
}
