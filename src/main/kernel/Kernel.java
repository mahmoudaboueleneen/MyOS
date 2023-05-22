package main.kernel;

import main.elements.*;

import java.util.InputMismatchException;
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
    private static int[] scheduledArrivalTimes;
    private static String[] scheduledArrivalFileLocations;
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
        scheduledArrivalTimes = new int[]{0,1,4};
        scheduledArrivalFileLocations = new String[]{"src/program_files/Program_1.txt", "src/program_files/Program_2.txt", "src/program_files/Program_3.txt"};
        new Kernel(2);
        scheduler.executeRoundRobin();
    }

    public static void init(){
        scheduledArrivalTimes = new int[3];
        scheduledArrivalFileLocations = new String[]{"src/program_files/Program_1.txt", "src/program_files/Program_2.txt", "src/program_files/Program_3.txt"};
        inputInstructionsPerTimeSlice();
        inputArrivalTimes();
        new Kernel(instructionsPerTimeSlice);
        scheduler.executeRoundRobin();
    }

    public static void inputInstructionsPerTimeSlice(){
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

    public static void inputArrivalTimes(){
        Scanner inp = new Scanner(System.in);
        System.out.println("Enter the arrival times of P1, P2 and P3 in order respectively (How many instructions have been executed at arrival time)");
        for(int i=0; i<3; i++) {
            try {
                scheduledArrivalTimes[i] = (inp.nextInt());
            } catch (InputMismatchException e) {
                System.out.println("ERROR: Arrival time must be an integer.");
                System.out.println("Exiting ...");
                System.exit(0);
            }

            if(scheduledArrivalTimes[i]<0){
                System.out.println("ERROR: Arrival time cannot be negative.");
                System.out.println("Exiting ...");
                System.exit(0);
            }
        }
    }

    public static synchronized Object[] canFitWhereInMemory(int linesOfCodeCount){
        boolean canFit = false;
        int lowerMemoryBound = 0;
        int upperMemoryBound = 0;
        int processMemorySize = PCB_SIZE + DATA_SIZE + linesOfCodeCount;

        boolean[] occupied = Kernel.getMemory().getOccupied();

        for(int i = 0; i < occupied.length; i++) {
            if(!occupied[i]){
                lowerMemoryBound = i;
                canFit = true;
                break;
            }
        }
        if(lowerMemoryBound + processMemorySize > memory.getMemory().length)
            canFit = false;

        upperMemoryBound = lowerMemoryBound + processMemorySize - 1;
        Object[] result = new Object[4];
        result[0] = canFit;
        result[1] = lowerMemoryBound;
        result[2] = upperMemoryBound;
        result[3] = processMemorySize;
        return result;
    }

    /**
     * Implementation of process creation here follows
     * (as much as possible) the real 5-step procedure
     * used in real OS process creation.
     */
//  TODO: Finish method.
    public static synchronized void createNewProcess(String programFilePath) {
        int linesOfCodeCount = interpreter.countFileLinesOfCode(programFilePath);
        Object[] canFitWhereInMemory = canFitWhereInMemory(linesOfCodeCount);

        System.out.println("    Acquiring unique PID");
        int processID = Kernel.getScheduler().getNextProcessID();

        if( (boolean) canFitWhereInMemory[0] ){
            System.out.println("    Allocating memory space");
            Kernel.getMemory().allocateMemoryPartition((Integer) canFitWhereInMemory[1], (Integer) canFitWhereInMemory[2]);

            System.out.println("    Initializing PCB");
            ProcessControlBlock pcb = new ProcessControlBlock(processID, (Integer) canFitWhereInMemory[1], (Integer) canFitWhereInMemory[2]);

            System.out.println("    Linking to scheduling queue");
            ProcessMemoryImage p = new ProcessMemoryImage( Kernel.getInterpreter().getInstructionsFromFile(programFilePath) );
            p.setProcessControlBlock(pcb);
            Kernel.getScheduler().addArrivedProcess(p);
            Kernel.getScheduler().addToReadyQueue(p);
            Kernel.getScheduler().addBurstTime( (Integer) canFitWhereInMemory[3] );

            System.out.println("    Finalizing process creation");
            Kernel.getMemory().fillMemoryPartition(p, (Integer) canFitWhereInMemory[1], (Integer) canFitWhereInMemory[2]);
            Kernel.getScheduler().getInMemoryProcesses().add(p);

            System.out.println("    Process created successfully\n");
        }

        else {
            /* retrieve a not running process from the Scheduler's processes ArrayList
             * move this process to disk (serialize)
             * remove this process from memory (set occupied to false)
             * compactMemory();
             * check for space again
             * keep removing and reorganizing until space is found
             * move our new process to memory
             */

            //System.out.println("Process added to memory.");

//          Finalize Process creation ???
            //
            //System.out.println("Process created successfully.\n");
        }
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
