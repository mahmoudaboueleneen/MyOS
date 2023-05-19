package main.elements;

import java.io.Serializable;

public class ProcessMemoryImage implements Serializable {
    private ProcessControlBlock processControlBlock;
    private final MemoryWord[] variables;
    private final String[] instructions;

    public ProcessMemoryImage(int lowerMemoryBoundary, int upperMemoryBoundary, String[] instructions){
        this.processControlBlock = null;
        this.variables = new MemoryWord[3];
        this.instructions = instructions;
    }

    public synchronized ProcessControlBlock getPCB() {
        return processControlBlock;
    }
    public synchronized MemoryWord[] getVariables() {return variables;}
    public synchronized String[] getInstructions() {return instructions;}
    public void setProcessControlBlock(ProcessControlBlock processControlBlock) {this.processControlBlock = processControlBlock;}

    public String toString(){return getPCB().getProcessID() + "";}
}
