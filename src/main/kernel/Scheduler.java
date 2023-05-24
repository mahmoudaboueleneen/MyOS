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

        currentRunningProcessMemoryImage = null;
    }

    private synchronized void assignNewRunningProcess(){
        currentRunningProcessMemoryImage = readyQueue.remove();
        currentRunningProcessMemoryImage.setProcessState(ProcessState.RUNNING);
        System.out.println("PROCESS CHOSEN TO RUN: " + currentRunningProcessMemoryImage);
        printQueues();
    }

    private synchronized void preemptCurrentRunningProcess() {
        readyQueue.add(currentRunningProcessMemoryImage);
        currentRunningProcessMemoryImage.setProcessState(ProcessState.READY);
    }

    private synchronized void finishCurrentRunningProcess(){
        currentRunningProcessMemoryImage.setProcessState(ProcessState.FINISHED);
        readyQueue.remove(currentRunningProcessMemoryImage);
        blockedQueue.remove(currentRunningProcessMemoryImage);
        System.out.println("PROCESS FINISHED: " + currentRunningProcessMemoryImage);
        printQueues();
    }

    public synchronized static void moveProcessToMemory(ProcessMemoryImage p){
        while (!p.canFitInMemory()) {
            swapOutToDisk(getProcessToSwapOutToDisk());
            Memory.compactMemory();
        }
        int[] bounds = p.getNewPossibleMemoryBounds();
        int newLowerBound = bounds[0];
        int newUpperBound = bounds[1];
        swapInFromDisk(p, newLowerBound, newUpperBound);
    }

    public static synchronized void swapOutToDisk(ProcessMemoryImage p){
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

    public static synchronized void swapInFromDisk(ProcessMemoryImage p, int newLowerBound, int newUpperBound){
        deserializeProcess(p.getPCB().getTempLocation()); // Useless but ok
        p.setTempLocation("---");
        p.setLowerMemoryBoundary(newLowerBound);
        p.setUpperMemoryBoundary(newUpperBound);
        Memory.fillMemoryPartitionWithProcess(p);
        inMemoryProcessMemoryImages.add(p);
        System.out.println("PROCESS SWAPPED IN FROM ISK: PID = " + p.getPCB().getProcessID() + "\n");
    }

    public synchronized static ProcessMemoryImage getProcessToSwapOutToDisk(){
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


    private synchronized static boolean hasNestedInstruction(String instruction){
        String[] words = instruction.split(" ");
        return words[0].equals("assign") && words[2].equals("readFile")
               || words[0].equals("assign") && words[2].equals("input");
    }

    private synchronized static String getInnerInstruction(String fullInstruction){
        String[] words = fullInstruction.split(" ");

        if(words[0].equals("assign") && words[2].equals("readFile"))
            return words[2] + " " + words[3];   // "readFile b"
        else if (words[0].equals("assign") && words[2].equals("input"))
            return words[2];    // input

        return null;
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
                System.out.println("PROCESS ARRIVED: " + scheduledArrivalFileLocations.get(index) + "\n");
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
        System.out.println("PROCESS ADDED TO READY QUEUE: " + p);
        printQueues();
    }

    public synchronized static void printQueues(){
        printReadyQueue();
        printBlockedQueue();
        System.out.println();
    }

    public static synchronized void printReadyQueue(){
        System.out.println("READY QUEUE: -> "  + readyQueue);
    }

    public static synchronized void printBlockedQueue(){
        System.out.println("BLOCKED QUEUE: -> " + blockedQueue);
    }

    public synchronized void printCurrentRunningProcess(){
        System.out.println("CURRENT RUNNING PROCESS: " + currentRunningProcessMemoryImage.toString());
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
