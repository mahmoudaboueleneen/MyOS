package main.kernel;

import main.elements.Memory;
import main.elements.MemoryWord;
import main.elements.ProcessMemoryImage;
import main.MyOS;
import main.elements.Mutex;

public class Kernel {

    public Kernel(){}

    public void reorganizeMemory(){

    }

    public Object[] canFitInMemory(String programFilePath){
        boolean canFit = false;
        int linesOfCode = MyOS.getInterpreter().countFileLinesOfCode(programFilePath);
        int PCB_INSTANCE_VARIABLES = 5;
        int DATA_VARIABLES = 3;

        int processMemorySize = PCB_INSTANCE_VARIABLES + DATA_VARIABLES + linesOfCode;

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

    public synchronized void createNewProcess(String programFilePath) {
        // Create process & arrive at scheduler
        Object[] canFitInMemory = canFitInMemory(programFilePath);
        ProcessMemoryImage p = new ProcessMemoryImage( (Integer) canFitInMemory[1],
                                 (Integer) canFitInMemory[2],
                                 (Integer) canFitInMemory[3],
                                  MyOS.getInterpreter().getInstructionsFromFile(programFilePath)
                                );
        MyOS.getScheduler().addArrivedProcess(p);
        MyOS.getScheduler().addToReadyQueue(p);
        MyOS.getScheduler().addBurstTime( (Integer) canFitInMemory[3] );

        if( (boolean) canFitInMemory(programFilePath)[0] ){
            // Add the process to memory
            p.getPCB().setLowerMemoryBoundary( (Integer) canFitInMemory[1] );
            p.getPCB().setUpperMemoryBoundary( (Integer) canFitInMemory[2] );
            MyOS.getMemory().allocateMemoryPartition(p, (Integer) canFitInMemory[1], (Integer) canFitInMemory[2]);
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

    public void readyProcess(ProcessMemoryImage p){

    }

    public void runProcess(ProcessMemoryImage p){

    }

    public void blockProcess(ProcessMemoryImage p, Mutex m){

    }

    public void terminateProcess(ProcessMemoryImage p){

    }

}
