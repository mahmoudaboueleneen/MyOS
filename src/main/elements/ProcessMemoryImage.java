package main.elements;

import java.io.Serializable;

public class ProcessMemoryImage implements Serializable {
    private final ProcessControlBlock processControlBlock;
    private final MemoryWord[] variables;
    private final String[] instructions;

    public ProcessMemoryImage(int lowerMemoryBoundary, int upperMemoryBoundary, int linesOfCode, String[] instructions){
        this.processControlBlock = new ProcessControlBlock(lowerMemoryBoundary, upperMemoryBoundary);
        this.variables = new MemoryWord[3];
        this.instructions = instructions;
    }

    public synchronized ProcessControlBlock getPCB() {
        return processControlBlock;
    }
    public synchronized MemoryWord[] getVariables() {return variables;}
    public synchronized String[] getInstructions() {return instructions;}

}
