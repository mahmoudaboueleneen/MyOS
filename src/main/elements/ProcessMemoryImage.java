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
            if(variables[i] == null){
                variables[i] = word;
                return;
            }
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
        if(processControlBlock.getProgramCounter() > instructions.length - 1)
            return false;
        return true;
    }

    public void incrementPC(){
        int newValue = this.getPCB().getProgramCounter() + 1;
        this.getPCB().setProgramCounter(newValue);

        MemoryWord word = Memory.getMemoryWordByName("PROGRAM_COUNTER", this.getPCB().getProcessID());
        if (word == null)
            return;
        word.setVariableData(newValue);
    }

    @Override
    public String toString(){
        return "{" +
                "PID: " + getPCB().getProcessID() + ", " +
                "State: " + getPCB().getProcessState() + ", " +
                "PC: " + getPCB().getProgramCounter() + ", " +
                "Lower Bound: " + getPCB().getLowerMemoryBoundary() + ", " +
                "Upper Bound: " + getPCB().getUpperMemoryBoundary() + ", " +
                "Temp. Location:" + getPCB().getTempLocation() +
                "}";
    }

    public synchronized int getProcessMemorySize(){
        return Kernel.getPCBSize()+ Kernel.getDataSize() + this.instructions.length;
    }

    public synchronized boolean canFitInMemory(){
        boolean canFitInMemory = false;
        int lowerBound = 0;
        int processMemorySize = this.getProcessMemorySize();
        boolean[] reserved = Kernel.getMemory().getReservedArray();
        for(int i = 0; i < reserved.length; i++) {
            if(!reserved[i]){
                lowerBound = i;
                canFitInMemory = true; // temporary assignment, check after
                break;
            }
        }
        if(lowerBound + processMemorySize > Memory.getMemoryArray().length)
            canFitInMemory = false; // the actual permanent assignment

        return canFitInMemory;
    }

    public synchronized int[] getNewPossibleMemoryBounds(){
        boolean canFitInMemory = false;
        int lowerBound = 0;
        int upperBound = 0;
        int processMemorySize = this.getProcessMemorySize();
        boolean[] reserved = Kernel.getMemory().getReservedArray();
        for(int i = 0; i < reserved.length; i++) {
            if(!reserved[i]){
                lowerBound = i;
                canFitInMemory = true;
                break;
            }
        }
        if(lowerBound + processMemorySize > Memory.getMemoryArray().length)
            canFitInMemory = false;

        if(canFitInMemory){
            upperBound = lowerBound + processMemorySize - 1;
            return new int[]{lowerBound,upperBound};
        }
        return null; // shouldn't be reached
    }

    public synchronized void setProcessState(ProcessState processState) {
        this.getPCB().setProcessState(processState);

        MemoryWord word = Memory.getMemoryWordByName("PROCESS_STATE", this.getPCB().getProcessID());
        if (word == null)
            return;
        word.setVariableData(processState);
    }

    public synchronized void setLowerMemoryBoundary(int newLowerBound) {
        this.getPCB().setLowerMemoryBoundary(newLowerBound);

        MemoryWord word = Memory.getMemoryWordByName("LOWER_MEM_BOUND", this.getPCB().getProcessID());
        if (word == null)
            return;
        word.setVariableData(newLowerBound);
    }

    public synchronized void setUpperMemoryBoundary(int newUpperBound) {
        this.getPCB().setUpperMemoryBoundary(newUpperBound);

        MemoryWord word = Memory.getMemoryWordByName("UPPER_MEM_BOUND", this.getPCB().getProcessID());
        if (word == null)
            return;
        word.setVariableData(newUpperBound);
    }

    public synchronized void setTempLocation(String newTempLocation) {
        this.getPCB().setTempLocation(newTempLocation);

        MemoryWord word = Memory.getMemoryWordByName("TEMP_LOCATION", this.getPCB().getProcessID());
        if (word == null)
            return;
        word.setVariableData(newTempLocation);
    }

}
