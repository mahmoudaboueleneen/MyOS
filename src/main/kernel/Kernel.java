package main.kernel;

import main.elements.Process;
import main.MyOS;
import main.elements.Mutex;

public class Kernel {

    public Kernel(){}

    public void reorganizeMemory(){

    }

    public Object[] canFitInMemory(String programFilePath){
        boolean canFit = false;
        int linesOfCode = MyOS.getInterpreter().countFileLinesOfCode(programFilePath);
        /* Memory size to be reserved for process = 5 (five PCB instance variables) +
                                                    3 (each process needs enough space for three variables) +
                                                    linesOfCode (each line of code will be stored as a memory word,
                                                              and to simplify we will just fill in the memory word's
                                                              variable name as "Instruction" and its data as the
                                                              un-parsed line of code itself).

          We will assume, as provided in proj. desc., that the memory is large enough for any of the provided processes
          by default.
          */
        int processMemorySize = 5 + 3 + linesOfCode;

        // Next, search for space in the memory for the process
        int lowerMemoryBound = 0;
        int upperMemoryBound = 0;
        boolean[] occupied = MyOS.getMemory().getOccupied();
        for(int i = 0; i < occupied.length; i++) {
        // If we find any space in the memory, get the starting address of this space
            if(!occupied[i]){
                lowerMemoryBound = i;
                canFit = true;
                break;
            }
        }
        // Calculate the memory size of the process  and see if it can fit
        if(lowerMemoryBound + processMemorySize > 40)
            canFit = false;
        upperMemoryBound = lowerMemoryBound + processMemorySize - 1;
        Object[] result = new Object[4];
        result[0] = canFit;
        result[1] = lowerMemoryBound;
        result[2] = upperMemoryBound;
        result[3] = processMemorySize;
        return result;
    }

    public void createNewProcess(String programFilePath) {
        if( (boolean) canFitInMemory(programFilePath)[0] ){
            // Create process, arrive at scheduler & add its burst time to scheduler
            Process p = new Process( (Integer) canFitInMemory(programFilePath)[1],
                                     (Integer) canFitInMemory(programFilePath)[2],
                                     (Integer) canFitInMemory(programFilePath)[3],
                                      MyOS.getInterpreter().getInstructionsFromFile(programFilePath)
                                    );
            MyOS.getScheduler().addArrivedProcess( p );
            MyOS.getScheduler().addBurstTime( (Integer) canFitInMemory(programFilePath)[3] );
            System.out.println("Process arrived at scheduler");

            // Now, Add the process to memory
            p.getPCB().setLowerMemoryBoundary( (Integer) canFitInMemory(programFilePath)[1] );
            p.getPCB().setUpperMemoryBoundary( (Integer) canFitInMemory(programFilePath)[2] );
            MyOS.getMemory().allocateMemoryPartition(p, (Integer) canFitInMemory(programFilePath)[1], (Integer) canFitInMemory(programFilePath)[2]);
            MyOS.getScheduler().getInMemoryProcesses().add(p);
            System.out.println("Process added to memory");

            // Finalize process creation ???
            //
            System.out.println("Process created successfully \n");
        }
        else{
            // retrieve a not running process from the Scheduler's processes ArrrayList,
            // move this process to disk (serialize)
            // remove this process from memory (set occupied to false)
            // compactMemory();
            // check for space again
            // keep removing and reorganizing until space is found
            // move our new process to memory
        }
    }

    public void readyProcess(Process p){

    }

    public void runProcess(Process p){

    }

    public void blockProcess(Process p, Mutex m){

    }

    public void terminateProcess(Process p){

    }

    // System Calls

}
