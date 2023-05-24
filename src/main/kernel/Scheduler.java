package main.kernel;

import main.elements.*;
import main.exceptions.InvalidInstructionException;
import main.exceptions.VariableAssignmentException;

import java.io.*;
import java.util.*;

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
    private static List<Object[]> executedInnerInstructions;

    public Scheduler(int instructionsPerTimeSlice, List<Integer> scheduledArrivalTimes, List<String> scheduledArrivalFileLocations){
        processList = new ArrayList<>();
        inMemoryProcessMemoryImages = new ArrayList<>();
        readyQueue = new ArrayDeque<>();
        blockedQueue = new ArrayDeque<>();
        this.instructionsPerTimeSlice = instructionsPerTimeSlice;
        maximumUsedPID = -1;
        this.scheduledArrivalTimes = scheduledArrivalTimes;
        this.scheduledArrivalFileLocations = scheduledArrivalFileLocations;
        executedInnerInstructions = new ArrayList<>();
    }

    public void executeRoundRobin(){
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
        int processID = currentRunningProcessMemoryImage.getPCB().getProcessID();

        while(instrsLeftInTimeSlice > 0) {
            printCurrentRunningProcess();
            if (runningPMI.hasNextInstruction())
            {
                String instruction = Interpreter.getNextProcessInstruction(runningPMI);
                if ( hasNestedInstruction(instruction)
                     && !isInnerInstructionAlreadyExecuted(instruction, processID) )
                    executeInnerInstruction(instruction, processID);
                else {
                    runningPMI.incrementPC();
                    executeInstruction(instruction);
                }
                instrsLeftInTimeSlice--;
                checkAndHandleProcessArrivals();

                ProcessState state = runningPMI.getPCB().getProcessState();
                if (state == ProcessState.BLOCKED)
                    return; // end time slice
            }
            else {
                finishCurrentRunningProcess();
                return;
            }
        }
        if(runningPMI.hasNextInstruction())
            preemptCurrentRunningProcess();
        else
            finishCurrentRunningProcess();
        currentRunningProcessMemoryImage = null;
    }

    private void assignNewRunningProcess(){
        currentRunningProcessMemoryImage = readyQueue.remove();
        currentRunningProcessMemoryImage.setProcessState(ProcessState.RUNNING);
        System.out.println("PROCESS CHOSEN TO RUN: " + currentRunningProcessMemoryImage);
        printQueues();
    }

    private void preemptCurrentRunningProcess() {
        readyQueue.add(currentRunningProcessMemoryImage);
        currentRunningProcessMemoryImage.setProcessState(ProcessState.READY);
    }

    private void finishCurrentRunningProcess(){
        currentRunningProcessMemoryImage.setProcessState(ProcessState.FINISHED);
        processList.remove(currentRunningProcessMemoryImage); //frees up its PID for use when PID counter starts from 0 again.
        readyQueue.remove(currentRunningProcessMemoryImage);
        blockedQueue.remove(currentRunningProcessMemoryImage);
        System.out.println("PROCESS FINISHED: " + currentRunningProcessMemoryImage);
        printQueues();
    }

    public static void moveProcessToMemory(ProcessMemoryImage p){
        while (!p.canFitInMemory()) {
            swapOutToDisk(getProcessToSwapOutToDisk());
            Memory.compactMemory();
        }
        int[] bounds = p.getNewPossibleMemoryBounds();
        int newLowerBound = bounds[0];
        int newUpperBound = bounds[1];
        swapInFromDisk(p, newLowerBound, newUpperBound);
    }

    public static void swapOutToDisk(ProcessMemoryImage p){
        String location = "src/temp/PID_" + p.getPCB().getProcessID() + ".ser";
        p.setTempLocation(location);
        p.setLowerMemoryBoundary(-1); // -1 denotes that it's not in memory
        p.setUpperMemoryBoundary(-1);
        serializeProcess(p, location);

        // Effectively inMemoryProcessMemoryImages.remove(p)
        Iterator<ProcessMemoryImage> iterator = inMemoryProcessMemoryImages.iterator();
        while(iterator.hasNext()){
            ProcessMemoryImage temp = iterator.next();
            if(temp.getPCB().getProcessID() == p.getPCB().getProcessID())
                iterator.remove();
        }
        System.out.println("PROCESS SWAPPED OUT TO DISK: PID = " + p.getPCB().getProcessID() + "\n");
    }

    public static void swapInFromDisk(ProcessMemoryImage p, int newLowerBound, int newUpperBound){
        deserializeProcess(p.getPCB().getTempLocation()); // Useless but ok
        p.setTempLocation("---");
        p.setLowerMemoryBoundary(newLowerBound);
        p.setUpperMemoryBoundary(newUpperBound);
        Memory.fillMemoryPartitionWithProcess(p);
        inMemoryProcessMemoryImages.add(p);
        System.out.println("PROCESS SWAPPED IN FROM ISK: PID = " + p.getPCB().getProcessID() + "\n");
    }

    public static ProcessMemoryImage getProcessToSwapOutToDisk(){
        for(ProcessMemoryImage each : processList)
            if(each.getPCB().getProcessState().equals(ProcessState.FINISHED))
                return each;

        ProcessMemoryImage p = null;
        if(!blockedQueue.isEmpty())
            p = ((ArrayDeque<ProcessMemoryImage>)blockedQueue).getLast();
        else if(!readyQueue.isEmpty())
            p = ((ArrayDeque<ProcessMemoryImage>)readyQueue).getLast();
        return p;
    }

    private static void serializeProcess(ProcessMemoryImage p, String location){
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

    private static ProcessMemoryImage deserializeProcess(String location){
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


    private static boolean hasNestedInstruction(String instruction){
        String[] words = instruction.split(" ");
        return words[0].equals("assign") && words[2].equals("readFile")
               || words[0].equals("assign") && words[2].equals("input");
    }

    private static String getInnerInstruction(String fullInstruction){
        String[] words = fullInstruction.split(" ");

        if(words[0].equals("assign") && words[2].equals("readFile"))
            return words[2] + " " + words[3];                            // e.g. 'readFile b'
        else if (words[0].equals("assign") && words[2].equals("input"))
            return words[2];                                             // e.g. 'input'

        return null;
    }

    private static boolean isInnerInstructionAlreadyExecuted(String instruction, int processID){
        for(Object[] each : executedInnerInstructions){
            if(each[0].equals(processID) && each[1].equals(instruction) && each[2].equals( getInnerInstruction(instruction) ))
                return true;
        }
        return false;
    }

    private void executeInnerInstruction(String instruction, int processID){
        String innerInstruction = getInnerInstruction(instruction);
        executeInstruction(innerInstruction);

        Object[] executedInnerInstr = new Object[3];
        executedInnerInstr[0] = processID;
        executedInnerInstr[1] = instruction;
        executedInnerInstr[2] = innerInstruction;
        executedInnerInstructions.add(executedInnerInstr);
    }

    private void executeInstruction(String instruction){
        try {
            Interpreter.interpretAndIncrementInstructionCycle(instruction, currentRunningProcessMemoryImage);
        }
        catch (InvalidInstructionException | VariableAssignmentException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void checkAndHandleFirstProcessArrivals(){
        checkAndHandleProcessArrivals();
        firstArrivalsHandled = true;
    }

    private void checkAndHandleProcessArrivals(){
        if(instructionCycle == 0){
            System.out.println("MEMORY STARTING STATE:");
            System.out.println(Kernel.getMemory());
        }

        System.out.println("\n*******************************************************" +
                 "*******************************************************************");

        System.out.println("CURRENT INSTRUCTION CYCLE = " + instructionCycle + " instruction(s) executed:\n");

        // Create arrived processes and remove them from scheduled arrivals afterwards.
        Iterator<Integer> iterator = scheduledArrivalTimes.iterator();
        while(iterator.hasNext()){
            int time = iterator.next();
            if(time == instructionCycle){
                int index = scheduledArrivalTimes.indexOf(time);
                System.out.println("PROCESS ARRIVED: " + scheduledArrivalFileLocations.get(index) + "\n");
                Kernel.createNewProcess(scheduledArrivalFileLocations.get(index));
                iterator.remove();
                scheduledArrivalFileLocations.remove(index);
            }
        }
    }

    public static int getNextProcessID() {
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

    public static void incrementInstructionCycle(){
        instructionCycle++;
    }

    public static void addArrivedProcess(ProcessMemoryImage p) {
        processList.add(p);
    }

    public void addToReadyQueue(ProcessMemoryImage p) {
        readyQueue.add(p);
        p.setProcessState(ProcessState.READY);
        System.out.println("PROCESS ADDED TO READY QUEUE: " + p);
        printQueues();
    }

    private void finalizeProgram(){
        System.out.println("All processes finished.");
        Kernel.exitProgram();
    }

    public static void printQueues(){
        printReadyQueue();
        printBlockedQueue();
        System.out.println();
    }

    public static void printReadyQueue(){
        System.out.println("READY QUEUE: -> "  + readyQueue);
    }

    public static void printBlockedQueue(){
        System.out.println("BLOCKED QUEUE: -> " + blockedQueue);
    }

    public void printCurrentRunningProcess(){
        System.out.println("CURRENT RUNNING PROCESS: " + currentRunningProcessMemoryImage.toString());
    }

    public static ArrayList<ProcessMemoryImage> getInMemoryProcessMemoryImages() {
        return inMemoryProcessMemoryImages;
    }

    public static Queue<ProcessMemoryImage> getReadyQueue() {
        return readyQueue;
    }

    public static Queue<ProcessMemoryImage> getBlockedQueue() {
        return blockedQueue;
    }

    public static ArrayList<ProcessMemoryImage> getInMemoryProcesses() {
        return inMemoryProcessMemoryImages;
    }

}
