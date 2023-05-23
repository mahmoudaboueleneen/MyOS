package main.elements;

import main.kernel.Kernel;

import java.io.Serializable;

public class ProcessMemoryImage implements Serializable {
    private ProcessControlBlock processControlBlock;
    private final MemoryWord[] variables;
    private final String[] instructions;

    public ProcessMemoryImage(String[] instructions){
        this.variables = new MemoryWord[3];
        this.instructions = instructions;
    }

    public synchronized void addVariable(MemoryWord word){
        for(int i = 0; i < variables.length; i++)
            if(variables[i] == null)
                variables[i] = word;
    }

    public synchronized ProcessControlBlock getPCB() {
        return processControlBlock;
    }

    public synchronized MemoryWord[] getVariables() {
        return variables;
    }

    public synchronized String[] getInstructions() {
        return instructions;
    }

    public void setProcessControlBlock(ProcessControlBlock processControlBlock) {
        this.processControlBlock = processControlBlock;
    }

    public boolean hasNextInstruction(){
        if(processControlBlock.getProgramCounter() >= instructions.length - 1)
            return false;
        return true;
    }

    public void incrementPC(){this.getPCB().setProgramCounter( this.getPCB().getProgramCounter() + 1);}

    @Override
    public String toString(){
        return "{" +
                "PID: " + getPCB().getProcessID() + ", " +
                "State: " + getPCB().getProcessState() + ", " +
                "PC: " + getPCB().getProgramCounter() + ", " +
                "Lower Bound: " + getPCB().getLowerMemoryBoundary() + ", " +
                "Upper Bound: " + getPCB().getUpperMemoryBoundary() +
                ",}";
    }

    public synchronized int getProcessMemorySize(){
        return Kernel.getPCBSize()+ Kernel.getDataSize() + this.instructions.length;
    }

    public synchronized boolean canFitInMemory(){
        boolean canFitInMemory = false;
        int lowerBound = 0;
        int processMemorySize = this.getProcessMemorySize();
        boolean[] occupied = Kernel.getMemory().getOccupied();
        for(int i = 0; i < occupied.length; i++) {
            if(!occupied[i]){
                lowerBound = i;
                canFitInMemory = true; // temporary assignment, check after
                break;
            }
        }
        if(lowerBound + processMemorySize > Memory.getMemory().length)
            canFitInMemory = false; // the actual permanent assignment

        return canFitInMemory;
    }

    public synchronized int[] getNewPossibleMemoryBounds(){
        boolean canFitInMemory = false;
        int lowerBound = 0;
        int upperBound = 0;
        int processMemorySize = this.getProcessMemorySize();
        boolean[] occupied = Kernel.getMemory().getOccupied();
        for(int i = 0; i < occupied.length; i++) {
            if(!occupied[i]){
                lowerBound = i;
                canFitInMemory = true;
                break;
            }
        }
        if(lowerBound + processMemorySize > Memory.getMemory().length)
            canFitInMemory = false;

        if(canFitInMemory){
            upperBound = lowerBound + processMemorySize - 1;
            int[] bounds = new int[]{lowerBound,upperBound};
            return bounds;
        }
        return null; // shouldn't be reached
    }




}
