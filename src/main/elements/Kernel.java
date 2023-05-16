package main.elements;

import main.MyOS;

public class Kernel {

    public Kernel(){}

    public Process createNewProcess(String programFilePath){
        int linesOfCode = MyOS.getInterpreter().countFileLinesOfCode(programFilePath);
        System.out.println("Lines of code:" + linesOfCode);

        /* Memory size to be reserved for process =    5 (five PCB instance variables) +
                                                       3 (each process needs enough space for three variables) +
                                                       linesOfCode (each line of code will be stored as a memory word,
                                                                    and to simplify we will just fill in the
                                                                    memory word's variable name as "Instruction"
                                                                    and its data as the un-parsed line of code itself).
          We will assume, as provided in proj. desc., that the memory is large enough for any of the provided processes
          by default.
        */
        int processMemorySize = 5 + 3 + linesOfCode;


        /* Next, find space for the process if available (find lower and upper memory bounds) */
        int lowerMemoryBound = 0;
        int upperMemoryBound = 0;
        boolean[] occupied = MyOS.getMemory().getOccupied();

        for (int i = 0; i < occupied.length; i++) {
            // If we find any space in the memory,
            if(occupied[i] = false){
                // return the starting address of this space
                lowerMemoryBound = i;
                break;
            }
        }

        if(lowerMemoryBound + processMemorySize > 40){
            //
        }


        /* Now, occupy the memory

         */


        //Process p = new Process();
        //System.out.println("Process created");
        return null;
    }

    public void readyProcess(Process p){

    }

    public void runProcess(Process p){

    }

    public void blockProcess(Process p, Mutex m){

    }

    public void terminateProcess(Process p){

    }



}
