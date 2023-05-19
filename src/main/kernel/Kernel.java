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

//  Instance of kernel to be used internally
    private static Kernel kernel;

    public static Mutex getUserInputMutex() {return userInputMutex;}
    public static Mutex getUserOutputMutex() {return userOutputMutex;}
    public static Mutex getFileMutex() {return fileMutex;}
    public static Interpreter getInterpreter() {return interpreter;}
    public static Scheduler getScheduler() {return scheduler;}
    public static Memory getMemory() {return memory;}
    public static SystemCallHandler getSystemCallHandler() {return systemCallHandler;}

    public Kernel(int instructionsPerTimeSlice){
        userInputMutex = new Mutex();
        userOutputMutex = new Mutex();
        fileMutex = new Mutex();
        interpreter = new Interpreter();
        scheduler = new Scheduler(instructionsPerTimeSlice);
        memory = new Memory();
        systemCallHandler = new SystemCallHandler();
    }

    public static void initDefault(){
        final int INSTRUCTIONS_PER_TIME_SLICE = 2;
        System.out.print("Instructions per time slice:" + INSTRUCTIONS_PER_TIME_SLICE + "\n");

        float[] arrivalTimes = {0,1,4};
        String[] arrivalLocations = {"src/program_files/Program_1.txt",
                                     "src/program_files/Program_2.txt",
                                     "src/program_files/Program_3.txt"
                                    };
        kernel = new Kernel(2);

//      Beginning counter representing time, incrementing timer until the first process scheduled to arrive arrives
        Counter counter = new Counter();
        System.out.println("\nInitializing System Timer.. \n");
        counter.run(arrivalTimes, arrivalLocations);
    }

    public static void init(){
        Scanner inp = new Scanner(System.in);
        int instructionsPerTimeSlice;

        System.out.print("Enter the instructions per time slice (round robin): \n");
        try {
            instructionsPerTimeSlice = inp.nextInt();
        } catch (InputMismatchException e) {
            System.out.println("Error: Instructions per time slice must be an integer.\nExiting ...");
            return;
        }
        if(instructionsPerTimeSlice<=0){
            System.out.println("ERROR: Instructions per time slice must be greater than zero.\nExiting ...");
            return;
        }

        float[] arrivalTimes = new float[3];
        String[] arrivalLocations = {"src/program_files/Program_1.txt",
                                     "src/program_files/Program_2.txt",
                                     "src/program_files/Program_3.txt"
                                    };
        kernel = new Kernel(instructionsPerTimeSlice);

        System.out.println("Enter the arrival times of P1, P2 and P3 in order respectively (Integer only)");
        for(int i = 0; i < 3; i++) {
            try {
                arrivalTimes[i] = (inp.nextInt());
            } catch (InputMismatchException e) {
                System.out.println("Error: Arrival time must be an integer.\nExiting ...");
                return;
            }
            if(arrivalTimes[i]<0){
                System.out.println("ERROR: Arrival time cannot be negative.\nExiting ...");
                return;
            }
        }

//      Sort arrival times and arrived processes in order
        for (int i = 0; i < arrivalTimes.length - 1; i++) {
            for (int j = 0; j < arrivalTimes.length - i - 1; j++) {
                if (arrivalTimes[j] > arrivalTimes[j + 1]) {
                    float temp = arrivalTimes[j];
                    arrivalTimes[j] = arrivalTimes[j + 1];
                    arrivalTimes[j + 1] = temp;

                    String temp2 = arrivalLocations[j];
                    arrivalLocations[j] = arrivalLocations[j + 1];
                    arrivalLocations[j + 1] = temp2;
                }
            }
        }

//      Beginning counter representing time, incrementing timer until the first process scheduled to arrive arrives
        Counter counter = new Counter();
        System.out.println("\nInitializing System Timer.. \n");
        counter.run(arrivalTimes, arrivalLocations);

    }

    public synchronized Object[] canFitInMemory(String programFilePath){
        boolean canFit = false;
        int linesOfCode = Kernel.getInterpreter().countFileLinesOfCode(programFilePath);
        int PCB_INSTANCE_VARIABLES = 6;
        int DATA_VARIABLES = 3;
        int processMemorySize = PCB_INSTANCE_VARIABLES + DATA_VARIABLES + linesOfCode;

//      Search for space in the memory
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

//      Check if process can fit in the found memory space
        if(lowerMemoryBound + processMemorySize > 40)
            canFit = false;

//      Return canFit flag + (possible) assigned memory boundaries for the process
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
     * (as much as possible) the real procedure used in
     * real OS process creation.
     */
    public synchronized void createNewProcess(String programFilePath) {
        Object[] canFitInMemory = canFitInMemory(programFilePath);

//      Begin by getting next unique PID from the scheduler
        System.out.println("    Acquiring unique PID");
        int processID = Kernel.getScheduler().getNextProcessID();

        if( (boolean) canFitInMemory[0] ){
//          Allocate memory space for the process
            System.out.println("    Allocating memory space");
            Kernel.getMemory().allocateMemoryPartition((Integer) canFitInMemory[1], (Integer) canFitInMemory[2]);

//          Initialize the Process Control Block
            System.out.println("    Initializing PCB");
            ProcessControlBlock pcb = new ProcessControlBlock(processID, (Integer) canFitInMemory[1], (Integer) canFitInMemory[2]);

//          Linking process to scheduling queue and changing process state from 'NEW' to 'READY'
//          and creating other data structures (ProcessMemoryImage object)
            System.out.println("    Linking to scheduling queue");
            ProcessMemoryImage p = new ProcessMemoryImage( (Integer) canFitInMemory[1], (Integer) canFitInMemory[2], Kernel.getInterpreter().getInstructionsFromFile(programFilePath) );
            p.setProcessControlBlock(pcb);
            Kernel.getScheduler().addArrivedProcess(p);
            Kernel.getScheduler().addToReadyQueue(p);
            Kernel.getScheduler().addBurstTime( (Integer) canFitInMemory[3] );

//          Finalize Process creation
            System.out.println("    Finalizing process creation");
            Kernel.getMemory().fillMemoryPartition(p, (Integer) canFitInMemory[1], (Integer) canFitInMemory[2]);
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

    static class Counter {
        private float count = 0;

        public void run(float[] arrivalTimes, String[] arrivalLocations) {
            final int[] i = {0};
            List<Thread> threads = new ArrayList<>();

            while (true) {
                synchronized (this) {
                    float round = (float) ( Math.round(count * 10.0) / 10.0);
                    while (i[0] < arrivalTimes.length && round == arrivalTimes[i[0]]) {
                        System.out.println("Process \"" + arrivalLocations[i[0]]+ "\" arrived at time = " + arrivalTimes[i[0]] + " sec");
                        final int locationIndex = i[0];
                        Thread thread = new Thread(() -> kernel.createNewProcess(arrivalLocations[locationIndex]));
                        thread.start();
                        threads.add(thread);
                        i[0]++;
                    }
                    if(i[0] == arrivalTimes.length) {
//                      Wait for all threads to finish
                        for (Thread thread : threads) {
                            try {
                                thread.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("\n*************************************\n");
                        System.out.println("All processes successfully created!");
                        System.out.println("\n*************************************\n");
                        System.out.println(Kernel.getMemory());
                        Kernel.getScheduler().printReadyQueue();
                        Kernel.getScheduler().printBlockedQueue();
                        System.out.println("\n*************************************");
                        return;
                    }
                    count += 0.2;
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
