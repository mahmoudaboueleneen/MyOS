package main.kernel;

import main.elements.*;
import main.exceptions.InvalidInstructionException;
import main.exceptions.VariableAssignmentException;
import main.translators.Interpreter;

import java.io.*;
import java.util.*;

public class Scheduler {
    // Fields with which scheduler is instantiated.
    private final int instructionsPerTimeSlice;
    private final List<Integer> scheduledArrivalTimes;
    private final List<String> scheduledArrivalFileLocations;

    // Lists for use by the scheduler to keep track of things.
    private static List<ProcessMemoryImage> primaryProcessList;
    private static List<ProcessMemoryImage> finishedProcessList;
    private static List<ProcessMemoryImage> inMemoryProcessMemoryImages;

    // Queues for use in scheduling.
    private static Queue<ProcessMemoryImage> readyQueue;
    private static Queue<ProcessMemoryImage> blockedQueue;

    // Other fields for use in scheduling.
    private ProcessMemoryImage currentRunningProcessMemoryImage;
    private static int instructionCycle;
    private boolean firstArrivalsHandled;
    private static List<Object[]> executedInnerInstructions;

    // Field used for unique PID assignment.
    private static int maximumUsedPID;


    public Scheduler(int instructionsPerTimeSlice, List<Integer> scheduledArrivalTimes, List<String> scheduledArrivalFileLocations){
        this.instructionsPerTimeSlice = instructionsPerTimeSlice;
        this.scheduledArrivalTimes = scheduledArrivalTimes;
        this.scheduledArrivalFileLocations = scheduledArrivalFileLocations;

        primaryProcessList = new ArrayList<>();
        finishedProcessList = new ArrayList<>();
        inMemoryProcessMemoryImages = new ArrayList<>();

        readyQueue = new ArrayDeque<>();
        blockedQueue = new ArrayDeque<>();

        executedInnerInstructions = new ArrayList<>();

        maximumUsedPID = -1;
    }


    public void executeRoundRobin(){
        int instrsLeftInTimeSlice = instructionsPerTimeSlice;

        if(!firstArrivalsHandled)
            checkAndHandleFirstProcessArrivals();

        if(readyQueue.isEmpty())
            finalizeProgram();

        ProcessMemoryImage nextInLine = readyQueue.element();
        if (!inMemoryProcessMemoryImages.contains(nextInLine))
            moveProcessFromDiskToMemory(nextInLine);

        assignNewRunningProcess();

        ProcessMemoryImage process = currentRunningProcessMemoryImage;

        int processID = currentRunningProcessMemoryImage.getPCB().getProcessID();

        while(instrsLeftInTimeSlice > 0) {
            printCurrentRunningProcess();
            if (process.hasNextInstruction())
            {
                String instruction = Interpreter.getNextProcessInstruction(process);
                if ( hasNestedInstruction(instruction)
                     && !isInnerInstructionAlreadyExecuted(instruction, processID) )
                    executeInnerInstruction(instruction, processID);
                else {
                    process.incrementPC();
                    executeInstruction(instruction);
                }
                instrsLeftInTimeSlice--;
                checkAndHandleProcessArrivals();

                ProcessState state = process.getPCB().getProcessState();
                if (state == ProcessState.BLOCKED)
                    return; // end time slice
            }
            else {
                finishCurrentRunningProcess();
                return;
            }
        }
        if(process.hasNextInstruction())
            preemptCurrentRunningProcess();
        else
            finishCurrentRunningProcess();

        currentRunningProcessMemoryImage = null;
    }


    private void checkAndHandleFirstProcessArrivals(){
        checkAndHandleProcessArrivals();
        firstArrivalsHandled = true;
    }


    private void checkAndHandleProcessArrivals(){
        if(instructionCycle == 0)
            printMemoryStartingState();
        printLineBreak();
        printCurrentInstructionCycle();
        createArrivedProcessesAndRemoveFromScheduledArrivals();
    }


    private static void printMemoryStartingState(){
        System.out.println("MEMORY STARTING STATE:");
        MemoryManager.printMemory();
    }


    private static void printLineBreak(){
        System.out.println("\n*********************" +
                "**********************************" +
                "**********************************" +
                "*********************************");
    }


    private static void printCurrentInstructionCycle(){
        System.out.println("CURRENT INSTRUCTION CYCLE = "
                + instructionCycle +
                " instruction(s) executed:\n");
    }


    private void createArrivedProcessesAndRemoveFromScheduledArrivals(){
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
        primaryProcessList.remove(currentRunningProcessMemoryImage); //frees up its PID for use when PID counter starts from 0 again.
        finishedProcessList.add(currentRunningProcessMemoryImage);
        readyQueue.remove(currentRunningProcessMemoryImage);
        blockedQueue.remove(currentRunningProcessMemoryImage);
        System.out.println("PROCESS FINISHED: " + currentRunningProcessMemoryImage);
        printQueues();
    }


    public static void moveProcessFromDiskToMemory(ProcessMemoryImage p){
        while (!p.canFitInMemory()) {
            swapOutToDisk(getProcessToSwapOutToDisk());
            MemoryManager.compactMemory();
        }
        int[] bounds = p.getNewPossibleMemoryBounds();
        int newLowerBound = bounds[0];
        int newUpperBound = bounds[1];
        swapInFromDisk(p, newLowerBound, newUpperBound);
    }


    public static ProcessMemoryImage getProcessToSwapOutToDisk(){
        for(ProcessMemoryImage each : finishedProcessList)
            if(each.getPCB().getProcessState().equals(ProcessState.FINISHED))
                return each;

        ProcessMemoryImage p = null;
        if(!blockedQueue.isEmpty())
            p = ((ArrayDeque<ProcessMemoryImage>)blockedQueue).getLast();
        else if(!readyQueue.isEmpty())
            p = ((ArrayDeque<ProcessMemoryImage>)readyQueue).getLast();
        return p;
    }


    public static void swapOutToDisk(ProcessMemoryImage p){
        String location = "src/disk/disk.temp/PID_" + p.getPCB().getProcessID() + ".ser";
        p.setTempLocation(location);
        // -1 denotes that it's not in memory.
        p.setLowerMemoryBoundary(-1);
        p.setUpperMemoryBoundary(-1);
        serializeProcess(p, location);

        // Effectively does inMemoryProcessMemoryImages.remove(p)
        Iterator<ProcessMemoryImage> iterator = inMemoryProcessMemoryImages.iterator();
        while(iterator.hasNext()){
            ProcessMemoryImage temp = iterator.next();
            if(temp.getPCB().getProcessID() == p.getPCB().getProcessID())
                iterator.remove();
        }
        System.out.println("PROCESS SWAPPED OUT TO DISK: PID = " + p.getPCB().getProcessID() + "\n");
    }


    private static void swapInFromDisk(ProcessMemoryImage p, int newLowerBound, int newUpperBound){
        deserializeProcess(p.getPCB().getTempLocation());
        p.setTempLocation("---");
        p.setLowerMemoryBoundary(newLowerBound);
        p.setUpperMemoryBoundary(newUpperBound);
        MemoryManager.fillMemoryPartitionWithProcess(p);
        inMemoryProcessMemoryImages.add(p);
        System.out.println("PROCESS SWAPPED IN FROM ISK: PID = " + p.getPCB().getProcessID() + "\n");
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


    private static void deserializeProcess(String location){
        try {
            FileInputStream fileIn = new FileInputStream(location);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            ProcessMemoryImage p = (ProcessMemoryImage) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException e ) {
            e.printStackTrace();
        }
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


    public static int getNextProcessID() {
        final int MAX_POSSIBLE_PID = 100;
        maximumUsedPID++;
        if(maximumUsedPID > MAX_POSSIBLE_PID){
            maximumUsedPID = 0;
            for(int currentPID=0; currentPID<101; currentPID++) {
                for (ProcessMemoryImage p : primaryProcessList) {
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
        primaryProcessList.add(p);
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


    private static void printReadyQueue(){
        System.out.println("READY QUEUE: -> "  + readyQueue);
    }


    private static void printBlockedQueue(){
        System.out.println("BLOCKED QUEUE: -> " + blockedQueue);
    }


    private void printCurrentRunningProcess(){
        System.out.println("CURRENT RUNNING PROCESS: " + currentRunningProcessMemoryImage.toString());
    }


    public static List<ProcessMemoryImage> getInMemoryProcessMemoryImages() {
        return inMemoryProcessMemoryImages;
    }


    public static Queue<ProcessMemoryImage> getReadyQueue() {
        return readyQueue;
    }


    public static Queue<ProcessMemoryImage> getBlockedQueue() {
        return blockedQueue;
    }


    public static List<ProcessMemoryImage> getInMemoryProcesses() {
        return inMemoryProcessMemoryImages;
    }

}
