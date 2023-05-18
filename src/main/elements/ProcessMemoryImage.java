package main.elements;

import java.io.Serializable;

public class ProcessMemoryImage implements Serializable {
    private ProcessControlBlock processControlBlock;
    private MemoryWord[] variables;
    private String[] instructions;

    public ProcessMemoryImage(int lowerMemoryBoundary, int upperMemoryBoundary, int linesOfCode, String[] instructions){
        this.processControlBlock = new ProcessControlBlock(lowerMemoryBoundary, upperMemoryBoundary);
        this.variables = new MemoryWord[3];
        this.instructions = instructions;
    }

    public ProcessControlBlock getPCB() {
        return processControlBlock;
    }
    public MemoryWord[] getVariables() {return variables;}
    public String[] getInstructions() {return instructions;}

}
