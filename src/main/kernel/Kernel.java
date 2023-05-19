package main.kernel;

import main.elements.Interpreter;
import main.elements.ProcessMemoryImage;
import main.elements.Memory;
import main.elements.Mutex;

import java.util.ArrayList;
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

    public Kernel(int roundRobinTimeSlice){
        userInputMutex = new Mutex();
        userOutputMutex = new Mutex();
        fileMutex = new Mutex();
        interpreter = new Interpreter();
        scheduler = new Scheduler(roundRobinTimeSlice);
        memory = new Memory();
        systemCallHandler = new SystemCallHandler();
    }

    public static void initDefault(){
        int INSTRUCTIONS_PER_TIME_SLICE = 2;
        System.out.print("Instructions per time slice:" + INSTRUCTIONS_PER_TIME_SLICE + "\n");

        int[] arrivalTimes = {0,1,4};
        String[] arrivalLocations = {"src/program_files/Program_1.txt",
                                     "src/program_files/Program_2.txt",
                                     "src/program_files/Program_3.txt"
        };
        kernel = new Kernel(2);

//      Beginning counter representing time, incrementing timer until the first process scheduled to arrive arrives
        Counter counter = new Counter();
        System.out.println("Initializing System Timer.. \n");
        counter.run(arrivalTimes, arrivalLocations);
    }

    public static void init(){
        Scanner inp = new Scanner(System.in);
        int instructionsPerTimeSlice;

        System.out.print("Enter the instructions per time slice (round robin): \n");
        instructionsPerTimeSlice = inp.nextInt();

        int[] arrivalTimes = new int[3];
        String[] arrivalLocations = {"src/program_files/Program_1.txt",
                                     "src/program_files/Program_2.txt",
                                     "src/program_files/Program_3.txt"
        };
        kernel = new Kernel(instructionsPerTimeSlice);

        System.out.println("Enter the arrival time of P1, P2 and P3 in order respectively (integer only)");
        for(int i = 0; i < 3; i++)
            arrivalTimes[i] = (inp.nextInt());

//      Sort arrival times and arrived processes in order
        for (int i = 0; i < arrivalTimes.length - 1; i++) {
            for (int j = 0; j < arrivalTimes.length - i - 1; j++) {
                if (arrivalTimes[j] > arrivalTimes[j + 1]) {
                    int temp = arrivalTimes[j];
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
        System.out.println("Initializing System Timer.. \n");
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

    public synchronized void createNewProcess(String programFilePath) {
//      Create ProcessMemoryImage and arrive at Scheduler
        Object[] canFitInMemory = canFitInMemory(programFilePath);
        ProcessMemoryImage p = new ProcessMemoryImage( (Integer) canFitInMemory[1],
                                                       (Integer) canFitInMemory[2],
                                                       Kernel.getInterpreter().getInstructionsFromFile(programFilePath) );
        Kernel.getScheduler().addArrivedProcess(p);
        Kernel.getScheduler().addToReadyQueue(p);
        Kernel.getScheduler().addBurstTime( (Integer) canFitInMemory[3] );

        if( (boolean) canFitInMemory[0] ){
//          Allocate block of memory for Process
            p.getPCB().setLowerMemoryBoundary( (Integer) canFitInMemory[1] );
            p.getPCB().setUpperMemoryBoundary( (Integer) canFitInMemory[2] );
            Kernel.getMemory().allocateMemoryPartition(p, (Integer) canFitInMemory[1], (Integer) canFitInMemory[2]);
            Kernel.getScheduler().getInMemoryProcesses().add(p);
            System.out.println("Process added to memory");

//          Finalize Process creation ???
            //
            System.out.println("Process created successfully \n");
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
        }
    }

    static class Counter implements Runnable {
        private float count = 0;

        @Override
        public void run() {}

        public void run(int[] arrivalTimes, String[] arrivalLocations) {
            int i = 0;
            List<Thread> threads = new ArrayList<>();

            while (true) {
                // MyOS.getScheduler().executeRoundRobin();
                synchronized (this) {
                    while(i < arrivalTimes.length && Math.round(count * 10.0) / 10.0 == arrivalTimes[i]){
                        System.out.println("Process " + arrivalLocations[i] + " arrived @ Time = " + count);
                        final int locationIndex = i;
                        Thread thread = new Thread(() -> kernel.createNewProcess(arrivalLocations[locationIndex]));
                        thread.start();
                        threads.add(thread);
                        i++;
                    }
                    if(i == arrivalTimes.length) {
//                      Wait for all threads to finish
                        for (Thread thread : threads) {
                            try {
                                thread.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("All processes successfully created! \n");
                        System.out.println(Kernel.getMemory());
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
