package main.elements;

import java.io.Serializable;

public class ProcessMemoryImage implements Serializable {
    private ProcessControlBlock processControlBlock;
    private final MemoryWord[] variables;
    private final String[] instructions;

    public ProcessMemoryImage(String[] instructions){
        this.variables = new MemoryWord[3];
        this.instructions = instructions;
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
        if(processControlBlock.getProgramCounter() == processControlBlock.getUpperMemoryBoundary())
            return false;
        return true;
    }

    public void incrementPC(){
        this.getPCB().setProgramCounter( this.getPCB().getProgramCounter() + 1);
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("PID: " + getPCB().getProcessID() + ", ");
        sb.append("State: " + getPCB().getProcessState() + ", ");
        sb.append("PC: " + getPCB().getProgramCounter() + ", ");
        sb.append("Lower Bound: " + getPCB().getLowerMemoryBoundary() + ", ");
        sb.append("Upper Bound: " + getPCB().getUpperMemoryBoundary());
        sb.append(",}");
        return sb.toString();
    }
}
