package main.kernel;

import main.elements.*;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class Kernel {
    private static Mutex userInputMutex;
    private static Mutex userOutputMutex;
    private static Mutex fileMutex;
    private static Interpreter interpreter;
    private static Scheduler scheduler;
    private static Memory memory;
    private static SystemCallHandler systemCallHandler;

    private static Kernel kernel;

    private static Scanner inp;
    private static int instructionsPerTimeSlice;
    private static int[] processArrivalTimes;
    private static String[] processArrivalLocations;
    private static boolean allProcessesScheduledToArriveHaveArrived;
    private static boolean allArrivedProcessesFinished;
    private static int instructionCycle;
    private static int nextArrivalTimesIndex;


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


    public Kernel(int instructionsPerTimeSlice){
        userInputMutex = new Mutex();
        userOutputMutex = new Mutex();
        fileMutex = new Mutex();
        interpreter = new Interpreter();
        scheduler = new Scheduler(instructionsPerTimeSlice);
        memory = new Memory();
        systemCallHandler = new SystemCallHandler();
    }


//    public static void initDefaultConditions(){
//        int INSTRUCTIONS_PER_TIME_SLICE = 2;
//        System.out.println("Instructions per time slice: " + INSTRUCTIONS_PER_TIME_SLICE + "\n");
//        processArrivalTimes = new int[]{0, 1, 4};
//        arrivalLocations = new String[]{"src/program_files/Program_1.txt", "src/program_files/Program_2.txt", "src/program_files/Program_3.txt"};
//        System.out.println("Initializing..\n");
//        kernel = new Kernel(INSTRUCTIONS_PER_TIME_SLICE);
//
//        List<Thread> threads = new ArrayList<>();
//        int i = 0;
//
//        while(true) {
//            kernel.getScheduler().executeRoundRobinWithInstructions();
//
//            for (int processArrivalTime : processArrivalTimes) {
//                if (processArrivalTimes[i] == kernel.getScheduler().getCurrentInstructionCycle()) {
//                    System.out.println("Process " + arrivalLocations[i] + " arrived at time = " + processArrivalTimes[i] + " sec");
//                    final int locationIndex = i; //so compiler won't cry about using non-final var in thread below.
//                    Thread thread = new Thread(() -> kernel.createNewProcess(arrivalLocations[locationIndex]));
//                    thread.start();
//                    threads.add(thread);
//                }
//            }
//            i++;
//
//            waitForProcessCreationThreadsToFinish(threads);
//
//            System.out.println("\n*************************************\n");
//            System.out.println("All processes successfully created!");
//            System.out.println("\n*************************************\n");
//            System.out.println(Kernel.getMemory());
//            Kernel.getScheduler().printReadyQueue();
//            Kernel.getScheduler().printBlockedQueue();
//            System.out.println("\n*************************************");
//            return;
//        }
//    }
//
//
//    public synchronized static void waitForProcessCreationThreadsToFinish(List<Thread> threads){
//        for (Thread thread : threads) {
//            waitForThreadToFinish(thread);
//        }
//    }
//
//
//    public synchronized static void waitForThreadToFinish(Thread thread){
//        try { thread.join(); }
//        catch (InterruptedException e) { e.printStackTrace(); }
//    }

    public static void init(){
        inp = new Scanner(System.in);
        inputInstructionsPerTimeSlice();
        initializeProcessArrivalTimes();
        initializeProcessArrivalLocations();
        kernel = new Kernel(instructionsPerTimeSlice);
        inputArrivalTimes();
        sortArrivalTimesLocations();

        while(true) {
            for (int i = nextArrivalTimesIndex; i < processArrivalTimes.length; i++) {
                if (instructionCycle == processArrivalTimes[i]) {
                    System.out.println("Process " + processArrivalLocations[i] + " arrived at time = " + processArrivalTimes[i] + " instruction(s) executed.");
                    kernel.createNewProcess(processArrivalLocations[i]);
                    nextArrivalTimesIndex = i + 1;
                } else {
                    if (nextArrivalTimesIndex > processArrivalTimes.length)
                        allProcessesScheduledToArriveHaveArrived = true;
                    break;
                }
            }

            if (allProcessesScheduledToArriveHaveArrived) {
                System.out.println("\n**********************************************\n");
                System.out.println("All processes scheduled to arrive have arrived.");
                System.out.println("\n**********************************************\n");
            } else {
                System.out.println("\n**********************************************\n");
            }

            scheduler.executeRoundRobinTimeSlice();

            if (instructionCycle > processArrivalTimes[processArrivalTimes.length - 1])
                if (allArrivedProcessesFinished) {
                    finalizeOutput();
                    return;
                }
        }
    }

    public static void initializeProcessArrivalTimes(){
        processArrivalTimes = new int[3];
    }

    public static void initializeProcessArrivalLocations(){
        processArrivalLocations = new String[]
                {"src/program_files/Program_1.txt",
                "src/program_files/Program_2.txt",
                "src/program_files/Program_3.txt"
                };
    }

    public static void inputInstructionsPerTimeSlice(){
        System.out.print("Enter the instructions per time slice (round robin): \n");
        try {
            instructionsPerTimeSlice = inp.nextInt();
        } catch (InputMismatchException e) {
            System.out.println("Error: Instructions per time slice must be an integer.\nExiting ...");
            return;
        }
        if(instructionsPerTimeSlice <= 0){
            System.out.println("ERROR: Instructions per time slice must be greater than zero.\nExiting ...");
            return;
        }
    }

    public static void inputArrivalTimes(){
        System.out.println("Enter the arrival times of P1, P2 and P3 in order respectively (How many instructions have been executed at arrival time)");
        for(int i=0; i<3; i++) {
            try {
                processArrivalTimes[i] = (inp.nextInt());
            } catch (InputMismatchException e) {
                System.out.println("Error: Arrival time must be an integer.\nExiting ...");
                return;
            }
            if(processArrivalTimes[i]<0){
                System.out.println("ERROR: Arrival time cannot be negative.\nExiting ...");
                return;
            }
        }
        System.out.println();
    }

    public static void sortArrivalTimesLocations(){

        for (int i=0; i<processArrivalTimes.length-1; i++) {
            for (int j=0; j<processArrivalTimes.length-i-1; j++) {
                if (processArrivalTimes[j] > processArrivalTimes[j+1]) {
                    int temp = processArrivalTimes[j];
                    processArrivalTimes[j] = processArrivalTimes[j+1];
                    processArrivalTimes[j+1] = temp;

                    String temp2 = processArrivalLocations[j];
                    processArrivalLocations[j] = processArrivalLocations[j+1];
                    processArrivalLocations[j+1] = temp2;
                }
            }
        }

    }

    public static void incrementInstructionCycle(){
        instructionCycle++;
    }

    public static void allArrivedProcessesFinished(){
        allArrivedProcessesFinished = true;
    }

    //TODO: Finish method.
    public static void finalizeOutput(){

    }

//    public static void initOld(){
//        Scanner inp = new Scanner(System.in);
//        int instructionsPerTimeSlice;
//
//        System.out.print("Enter the instructions per time slice (round robin): \n");
//        try {
//            instructionsPerTimeSlice = inp.nextInt();
//        } catch (InputMismatchException e) {
//            System.out.println("Error: Instructions per time slice must be an integer.\nExiting ...");
//            return;
//        }
//        if(instructionsPerTimeSlice <= 0){
//            System.out.println("ERROR: Instructions per time slice must be greater than zero.\nExiting ...");
//            return;
//        }
//
//        float[] arrivalTimes = new float[3];
//        String[] arrivalLocations = {"src/program_files/Program_1.txt",
//                                     "src/program_files/Program_2.txt",
//                                     "src/program_files/Program_3.txt"
//                                    };
//        kernel = new Kernel(instructionsPerTimeSlice);
//
//        System.out.println("Enter the arrival times of P1, P2 and P3 in order respectively (How many instructions have been executed at arrival time)");
//        for(int i = 0; i < 3; i++) {
//            try {
//                arrivalTimes[i] = (inp.nextInt());
//            } catch (InputMismatchException e) {
//                System.out.println("Error: Arrival time must be an integer.\nExiting ...");
//                return;
//            }
//            if(arrivalTimes[i]<0){
//                System.out.println("ERROR: Arrival time cannot be negative.\nExiting ...");
//                return;
//            }
//        }
//
////      Sort arrival times and arrived processes in order
//        for (int i = 0; i < arrivalTimes.length - 1; i++) {
//            for (int j = 0; j < arrivalTimes.length - i - 1; j++) {
//                if (arrivalTimes[j] > arrivalTimes[j + 1]) {
//                    float temp = arrivalTimes[j];
//                    arrivalTimes[j] = arrivalTimes[j + 1];
//                    arrivalTimes[j + 1] = temp;
//
//                    String temp2 = arrivalLocations[j];
//                    arrivalLocations[j] = arrivalLocations[j + 1];
//                    arrivalLocations[j + 1] = temp2;
//                }
//            }
//        }
//
////      Beginning counter representing time, incrementing timer until the first process scheduled to arrive arrives
//        Counter counter = new Counter();
//        System.out.println("\nInitializing System Timer.. \n");
//        counter.run(arrivalTimes, arrivalLocations);
//    }


//  TODO: Clean up method.
    public static synchronized Object[] canFitWhereInMemory(int linesOfCodeCount){
        boolean canFit = false;
        int instructionsSize = linesOfCodeCount;
        int PCB_SIZE = 6;
        int DATA_SIZE = 3;
        int processMemorySize = PCB_SIZE + DATA_SIZE + instructionsSize;
        int lowerMemoryBound = 0;
        int upperMemoryBound;
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
     * Implementation of Process creation here follows
     * (as much as possible) the real 5-step procedure
     * used in real OS process creation.
     */
//  TODO: Finish method.
    public synchronized void createNewProcess(String programFilePath) {
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

//    static class Counter {
//        private float count = 0;
//
//        public void run(float[] arrivalTimes, String[] arrivalLocations) {
//            final int[] i = {0};
//            List<Thread> threads = new ArrayList<>();
//
//            while (true) {
//                synchronized (this) {
//                    float round = (float) ( Math.round(count * 10.0) / 10.0);
//                    while (i[0] < arrivalTimes.length && round == arrivalTimes[i[0]]) {
//                        System.out.println("Process \"" + arrivalLocations[i[0]]+ "\" arrived at time = " + arrivalTimes[i[0]] + " sec");
//                        final int locationIndex = i[0];
//                        Thread thread = new Thread(() -> kernel.createNewProcess(arrivalLocations[locationIndex]));
//                        thread.start();
//                        threads.add(thread);
//                        i[0]++;
//                    }
//                    if(i[0] == arrivalTimes.length) {
////                      Wait for all threads to finish
//                        for (Thread thread : threads) {
//                            try {
//                                thread.join();
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        System.out.println("\n*************************************\n");
//                        System.out.println("All processes successfully created!");
//                        System.out.println("\n*************************************\n");
//                        System.out.println(Kernel.getMemory());
//                        Kernel.getScheduler().printReadyQueue();
//                        Kernel.getScheduler().printBlockedQueue();
//                        System.out.println("\n*************************************");
//                        return;
//                    }
//                    count += 0.2;
//                }
//                try {
//                    Thread.sleep(200);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }

}
