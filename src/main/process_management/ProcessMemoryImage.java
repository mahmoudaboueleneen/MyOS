package main.process_management;

import main.kernel.Kernel;
import main.memory_management.MemoryManager;
import main.memory_management.MemoryWord;

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

    public ProcessControlBlock getPCB() {
        return processControlBlock;
    }

    public MemoryWord[] getVariables() {
        return variables;
    }

    public String[] getInstructions() {
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

        MemoryWord word = MemoryManager.getMemoryWordByName("PROGRAM_COUNTER", this.getPCB().getProcessID());
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

    public int getProcessMemorySize(){
        return Kernel.getPCBSize()+ Kernel.getDataSize() + this.instructions.length;
    }

    public boolean canFitInMemory(){
        boolean canFitInMemory = false;
        int lowerBound = 0;
        int processMemorySize = this.getProcessMemorySize();
        boolean[] reserved = MemoryManager.getReservedArray();
        for(int i = 0; i < reserved.length; i++) {
            if(!reserved[i]){
                lowerBound = i;
                canFitInMemory = true; // temporary assignment, check after
                break;
            }
        }
        if(lowerBound + processMemorySize > MemoryManager.getMemoryArray().length)
            canFitInMemory = false; // the actual permanent assignment

        return canFitInMemory;
    }

    public int[] getNewPossibleMemoryBounds(){
        boolean canFitInMemory = false;
        int lowerBound = 0;
        int upperBound = 0;
        int processMemorySize = this.getProcessMemorySize();
        boolean[] reserved = MemoryManager.getReservedArray();
        for(int i = 0; i < reserved.length; i++) {
            if(!reserved[i]){
                lowerBound = i;
                canFitInMemory = true;
                break;
            }
        }
        if(lowerBound + processMemorySize > MemoryManager.getMemoryArray().length)
            canFitInMemory = false;

        if(canFitInMemory){
            upperBound = lowerBound + processMemorySize - 1;
            return new int[]{lowerBound,upperBound};
        }
        return null; // shouldn't be reached
    }

    public void setProcessState(ProcessState processState) {
        this.getPCB().setProcessState(processState);

        MemoryWord word = MemoryManager.getMemoryWordByName("PROCESS_STATE", this.getPCB().getProcessID());
        if (word == null)
            return;
        word.setVariableData(processState);
    }

    public void setLowerMemoryBoundary(int newLowerBound) {
        this.getPCB().setLowerMemoryBoundary(newLowerBound);

        MemoryWord word = MemoryManager.getMemoryWordByName("LOWER_MEM_BOUND", this.getPCB().getProcessID());
        if (word == null)
            return;
        word.setVariableData(newLowerBound);
    }

    public void setUpperMemoryBoundary(int newUpperBound) {
        this.getPCB().setUpperMemoryBoundary(newUpperBound);

        MemoryWord word = MemoryManager.getMemoryWordByName("UPPER_MEM_BOUND", this.getPCB().getProcessID());
        if (word == null)
            return;
        word.setVariableData(newUpperBound);
    }

    public void setTempLocation(String newTempLocation) {
        this.getPCB().setTempLocation(newTempLocation);

        MemoryWord word = MemoryManager.getMemoryWordByName("TEMP_LOCATION", this.getPCB().getProcessID());
        if (word == null)
            return;
        word.setVariableData(newTempLocation);
    }

}
