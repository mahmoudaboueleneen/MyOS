package main.elements;

import main.kernel.Scheduler;

public class Memory {
    private static MemoryWord[] memory;
    private static boolean[] occupied;

    public Memory(){
        memory = new MemoryWord[40];
        occupied = new boolean[40];
    }

    public static synchronized void compactMemory(){
        clearMemory();
        fillMemoryWithProcessesWhichShouldBeInIt();
    }

    private static void clearMemory(){
        clearMemoryPartition(0,39);
    }

    public static synchronized void clearMemoryPartition(int lowerMemoryBound, int upperMemoryBound){
        for (int i = lowerMemoryBound; i < upperMemoryBound+1; i++){
            clearMemoryWord(i);
        }
    }

    public static synchronized void clearMemoryWord(int address){
        memory[address] = null;
        occupied[address] = false;
    }

    private static void fillMemoryWithProcessesWhichShouldBeInIt(){
        int nextStartingIndex = 0;
        for(ProcessMemoryImage p : Scheduler.getInMemoryProcessMemoryImages()){
            int lowerBound = nextStartingIndex;
            int upperBound = lowerBound + p.getProcessMemorySize();
            fillMemoryPartition(p, lowerBound, upperBound);
            nextStartingIndex = upperBound + 1;
        }
    }

    public static synchronized void fillMemoryPartition(ProcessMemoryImage p, int lowerMemoryBound, int upperMemoryBound) {
        // '---' means reserved, acts as placeholder.

        writeMemoryWord(lowerMemoryBound, new MemoryWord("PROCESS_ID", p.getPCB().getProcessID()) );
        writeMemoryWord(lowerMemoryBound + 1, new MemoryWord("PROCESS_STATE", p.getPCB().getProcessState()) );
        writeMemoryWord(lowerMemoryBound + 2, new MemoryWord("PROGRAM_COUNTER", p.getPCB().getProgramCounter()) );
        writeMemoryWord(lowerMemoryBound + 3, new MemoryWord("LOWER_MEM_BOUND", lowerMemoryBound) );
        writeMemoryWord(lowerMemoryBound + 4, new MemoryWord("UPPER_MEM_BOUND", upperMemoryBound) );
        writeMemoryWord(lowerMemoryBound + 5, new MemoryWord("TEMP_LOCATION", p.getPCB().getTempLocation()==null?"---":p.getPCB().getTempLocation()) );

        int i = lowerMemoryBound + 6;
        for(MemoryWord var : p.getVariables()){
            if(var == null)
                writeMemoryWord(i, new MemoryWord("---", "---"));
            else
                writeMemoryWord(i, new MemoryWord(var.getVariableName(), var.getVariableData()));
            i++;
        }

        int j = lowerMemoryBound + 9;
        for(String instruction : p.getInstructions()){
            writeMemoryWord(j, new MemoryWord("INSTRUCTION", instruction));
            j++;
        }
    }

    public static synchronized void writeMemoryWord(int address, MemoryWord word) {
        memory[address] = word;
        occupied[address] = true;
    }

    public static synchronized MemoryWord readMemoryWord(int address) {
        return memory[address];
    }

    public static synchronized void allocateMemoryPartition(int lowerMemoryBound, int upperMemoryBound){
        for (int i = lowerMemoryBound; i < upperMemoryBound+1; i++){
            allocateMemoryWord(i);
        }
    }

    public static synchronized void allocateMemoryWord(int address){
        occupied[address] = true;
    }

    public static synchronized void deallocateMemoryPartition(int lowerMemoryBound, int upperMemoryBound){
        for (int i = lowerMemoryBound; i < upperMemoryBound+1; i++){
            deallocateMemoryWord(i);
        }
    }

    public static synchronized void deallocateMemoryWord(int address){
        occupied[address] = false;
    }

    @Override
    public synchronized String toString () {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < memory.length; i++) {
            String varName;
            Object varData;
            if(!isMemoryWordOccupied(i)) {
                varName = " ";
                varData = " ";
            } else {
                varName = memory[i].getVariableName();
                if(memory[i].getVariableData() instanceof Integer){
                    varData = memory[i].getVariableData();
                }
                else if (memory[i].getVariableData() instanceof ProcessState){
                    varData = ((ProcessState) memory[i].getVariableData()).name();
                }
                else {
                    varData = memory[i].getVariableData();
                }
            }
            sb.append(i);
            sb.append(": {");
            sb.append(varName);
            sb.append(",");
            sb.append(varData);
            sb.append("} \n");
        }
        return sb.toString();
    }

    public static MemoryWord[] getMemory() {
        return memory;
    }

    public boolean[] getOccupied() {
        return occupied;
    }

    public synchronized static boolean isMemoryWordOccupied(int address) {
        return occupied[address];
    }

}


