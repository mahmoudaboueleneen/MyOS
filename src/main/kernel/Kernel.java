package main.kernel;

import main.elements.*;
import main.translators.Interpreter;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public abstract class Kernel {
    // Mutexes for the three available resources.
    private static Mutex userInputMutex;
    private static Mutex userOutputMutex;
    private static Mutex fileMutex;

    // Instance that we will run the scheduling on with the
    // inputted instructionsPerTimeSlice.
    private static Scheduler scheduler;

    // Fields to handle process arrivals.
    private static final Scanner inp = new Scanner(System.in);
    private static int instructionsPerTimeSlice;
    private static List<Integer> scheduledArrivalTimes;
    private static List<String> scheduledArrivalFileLocations;

    // System-wide global fields.
    private static final int PCB_SIZE = 6;
    private static final int DATA_SIZE = 3;
    private static final int PROCESS_COUNT = 3;


    public static void initDefaultConditions(){
        initializeArrays();

        instructionsPerTimeSlice = 2;

        addDefaultArrivalTimes();

        addDefaultArrivalFileLocations();

        initializeResources();

        initializeScheduler();

        runSchedulingAlgorithm();
    }


    public static void init(){
        initializeArrays();

        inputInstructionsPerTimeSlice();

        inputArrivalTimes();

        addDefaultArrivalFileLocations();

        initializeResources();

        initializeScheduler();

        runSchedulingAlgorithm();
    }


    private static void initializeArrays(){
        scheduledArrivalTimes = new ArrayList<>();
        scheduledArrivalFileLocations = new ArrayList<>();
    }


    private static void addDefaultArrivalTimes(){
        scheduledArrivalTimes.add(0);
        scheduledArrivalTimes.add(1);
        scheduledArrivalTimes.add(4);
    }


    private static void addDefaultArrivalFileLocations(){
        scheduledArrivalFileLocations.add("src/disk/disk.program_files/Program_1.txt");
        scheduledArrivalFileLocations.add("src/disk/disk.program_files/Program_2.txt");
        scheduledArrivalFileLocations.add("src/disk/disk.program_files/Program_3.txt");
    }


    private static void initializeResources(){
        userInputMutex = new Mutex();
        userOutputMutex = new Mutex();
        fileMutex = new Mutex();
    }


    private static void initializeScheduler(){
        scheduler = new Scheduler(instructionsPerTimeSlice, scheduledArrivalTimes, scheduledArrivalFileLocations);
    }


    private static void runSchedulingAlgorithm(){
        System.out.println("Inputs received, running...");
        while(true)
            scheduler.executeRoundRobin();
    }


    private static void inputInstructionsPerTimeSlice(){
        Scanner inp = new Scanner(System.in);
        System.out.println("Enter the instructions per time slice (round robin):");

        try {
            instructionsPerTimeSlice = inp.nextInt();
        } catch (InputMismatchException e) {
            System.out.println("ERROR: Instructions per time slice must be an integer.");
            exitProgram();
        }

        if(instructionsPerTimeSlice <= 0){
            System.out.println("ERROR: Instructions per time slice must be greater than zero.");
            exitProgram();
        }
    }


    private static void inputArrivalTimes(){
        System.out.println("Enter the arrival times of P1, P2 and P3 in order" +
                           " respectively (How many instructions have been executed at arrival time)");

        for (int i = 0; i < PROCESS_COUNT; i++) {
            try {
                scheduledArrivalTimes.add(inp.nextInt());
            } catch (InputMismatchException e) {
                System.out.println("ERROR: Arrival time must be an integer.");
                exitProgram();
            }

            if(scheduledArrivalTimes.get(i)<0){
                System.out.println("ERROR: Arrival time cannot be negative.");
                exitProgram();
            }
        }

        if(!arrivalTimeArrayHasTimeZero()){
            System.out.println("ERROR: One of the processes MUST arrive at time 0.");
            exitProgram();
        }
    }


    private static boolean arrivalTimeArrayHasTimeZero(){
        for(int time : scheduledArrivalTimes)
            if (time == 0)
                return true;
        return false;
    }


    public static void exitProgram(){
        System.out.println("Exiting...");
        System.exit(0);
    }


    public static void createNewProcess(String programFilePath) {
        ProcessMemoryImage p = createProcessMemoryImage(programFilePath);

        makeSpaceForProcessInMemoryIfNeeded(p);

        int[] bounds = p.getNewPossibleMemoryBounds();
        int lowerBound = bounds[0];
        int upperBound = bounds[1];
        int processID = acquireUniquePID();

        allocateMemoryPartition(lowerBound,upperBound);
        ProcessControlBlock pcb = initializePCB(processID, lowerBound, upperBound);
        p.setProcessControlBlock(pcb);
        linkToSchedulingQueue(p);
        finalizeProcessCreation(p);
    }


    private static ProcessMemoryImage createProcessMemoryImage(String programFilePath){
        return new ProcessMemoryImage(Interpreter.getInstructionsFromFile(programFilePath));
    }


    private static void makeSpaceForProcessInMemoryIfNeeded(ProcessMemoryImage p){
        while(!p.canFitInMemory()){
            Scheduler.swapOutToDisk( Scheduler.getProcessToSwapOutToDisk());
            Memory.compactMemory();
        }
    }


    private static int acquireUniquePID(){
        return Scheduler.getNextProcessID();
    }


    private static void allocateMemoryPartition(int lowerBound, int upperBound){
        Memory.allocateMemoryPartition(lowerBound, upperBound);
    }


    private static ProcessControlBlock initializePCB(int processID, int lowerBound, int upperBound){
        return new ProcessControlBlock(processID, lowerBound, upperBound);
    }


    private static void linkToSchedulingQueue(ProcessMemoryImage p){
        Scheduler.addArrivedProcess(p);
        Kernel.getScheduler().addToReadyQueue(p);
    }


    private static void finalizeProcessCreation(ProcessMemoryImage p){
        Memory.fillMemoryPartitionWithProcess(p);
        Scheduler.getInMemoryProcesses().add(p);
    }


    public static Mutex getUserInputMutex() {
        return userInputMutex;
    }


    public static Mutex getUserOutputMutex() {
        return userOutputMutex;
    }


    public static Mutex getFileMutex() {
        return fileMutex;
    }


    public static Scheduler getScheduler() {
        return scheduler;
    }


    public static int getPCBSize(){
        return PCB_SIZE;
    }


    public static int getDataSize(){
        return DATA_SIZE;
    }
}
