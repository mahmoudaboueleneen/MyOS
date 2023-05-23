package main.kernel;

import main.elements.*;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class Kernel {
    // Mutexes for the three available resources.
    private static Mutex userInputMutex;
    private static Mutex userOutputMutex;
    private static Mutex fileMutex;
    // Instances of the main classes
    private static Interpreter interpreter;
    private static Scheduler scheduler;
    private static Memory memory;
    private static SystemCallHandler systemCallHandler;
    // Fields to handle process arrivals
    private static int instructionsPerTimeSlice;
    private static List<Integer> scheduledArrivalTimes;
    private static List<String> scheduledArrivalFileLocations;
    // System-wide global fields
    private static final int PCB_SIZE = 6;
    private static final int DATA_SIZE = 3;


    public Kernel(int instructionsPerTimeSlice){
        userInputMutex = new Mutex();
        userOutputMutex = new Mutex();
        fileMutex = new Mutex();
        interpreter = new Interpreter();
        scheduler = new Scheduler(instructionsPerTimeSlice, scheduledArrivalTimes, scheduledArrivalFileLocations);
        memory = new Memory();
        systemCallHandler = new SystemCallHandler();
    }

    public static void initDefaultConditions(){
        scheduledArrivalTimes = new ArrayList<>();
        scheduledArrivalFileLocations = new ArrayList<>();
        scheduledArrivalFileLocations.add("src/program_files/Program_1.txt");
        scheduledArrivalFileLocations.add("src/program_files/Program_2.txt");
        scheduledArrivalFileLocations.add("src/program_files/Program_3.txt");
        new Kernel(2);

        System.out.println("Inputs received, initializing...");

        scheduler.executeRoundRobin();
    }

    public static void init(){
        scheduledArrivalTimes = new ArrayList<>();
        scheduledArrivalFileLocations = new ArrayList<>();
        scheduledArrivalFileLocations.add("src/program_files/Program_1.txt");
        scheduledArrivalFileLocations.add("src/program_files/Program_2.txt");
        scheduledArrivalFileLocations.add("src/program_files/Program_3.txt");
        inputInstructionsPerTimeSlice();
        inputArrivalTimes();
        new Kernel(instructionsPerTimeSlice);

        System.out.println("Inputs received, initializing...");

        while(true)
            scheduler.executeRoundRobin();
    }

    private static void inputInstructionsPerTimeSlice(){
        Scanner inp = new Scanner(System.in);
        System.out.print("Enter the instructions per time slice (round robin):");
        System.out.println();

        try {
            instructionsPerTimeSlice = inp.nextInt();
        } catch (InputMismatchException e) {
            System.out.println("ERROR: Instructions per time slice must be an integer.");
            System.out.println("Exiting ...");
            System.exit(0);
        }

        if(instructionsPerTimeSlice <= 0){
            System.out.println("ERROR: Instructions per time slice must be greater than zero.");
            System.out.println("Exiting ...");
            System.exit(0);
        }
    }

    private static void inputArrivalTimes(){
        Scanner inp = new Scanner(System.in);
        System.out.println("Enter the arrival times of P1, P2 and P3 in order respectively (How many instructions have been executed at arrival time)");
        for(int i=0; i<3; i++) {
            try {
                scheduledArrivalTimes.add(inp.nextInt());
            } catch (InputMismatchException e) {
                System.out.println("ERROR: Arrival time must be an integer.");
                System.out.println("Exiting ...");
                System.exit(0);
            }

            if(scheduledArrivalTimes.get(i)<0){
                System.out.println("ERROR: Arrival time cannot be negative.");
                System.out.println("Exiting ...");
                System.exit(0);
            }
        }

        if(!hasTimeZero()){
            System.out.println("ERROR: One of the processes MUST arrive at time 0.");
            System.out.println("Exiting ...");
            System.exit(0);
        }
    }

    private static boolean hasTimeZero(){
        boolean found = false;
        for(int time : scheduledArrivalTimes){
            if (time == 0){
                found = true;
                break;
            }
        }
        return found;
    }

    public static synchronized void createNewProcess(String programFilePath) {
        ProcessMemoryImage p = new ProcessMemoryImage( Kernel.getInterpreter().getInstructionsFromFile(programFilePath) );
        while(!p.canFitInMemory()){
            Scheduler.swapOutToDisk( Scheduler.getProcessToSwapOutToDisk());
            Memory.compactMemory();
        }

        int[] bounds = p.getNewPossibleMemoryBounds();
        int lowerBound = bounds[0];
        int upperBound = bounds[1];

        System.out.println("    Acquiring unique PID...");
        int processID = Scheduler.getNextProcessID();

        System.out.println("    Allocating memory space...");
        Memory.allocateMemoryPartition(lowerBound, upperBound);

        System.out.println("    Initializing PCB...");
        ProcessControlBlock pcb = new ProcessControlBlock(processID, lowerBound, upperBound);

        System.out.println("    Linking to scheduling queue...\n");
        p.setProcessControlBlock(pcb);
        Scheduler.addArrivedProcess(p);
        Kernel.getScheduler().addToReadyQueue(p);
        //Kernel.getScheduler().addBurstTime( (Integer) canFitWhereInMemory[3] );

        System.out.println("    Finalizing process creation...");
        Memory.fillMemoryPartition(p, lowerBound, upperBound);
        Scheduler.getInMemoryProcesses().add(p);

        System.out.println("    Process created successfully!\n");
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

    public static Interpreter getInterpreter() {
        return interpreter;
    }

    public static Scheduler getScheduler() {
        return scheduler;
    }

    public static Memory getMemory() {
        return memory;
    }

    public static SystemCallHandler getSystemCallHandler() {
        return systemCallHandler;
    }

    public static int getPCBSize(){
        return PCB_SIZE;
    }

    public static int getDataSize(){
        return DATA_SIZE;
    }

}
