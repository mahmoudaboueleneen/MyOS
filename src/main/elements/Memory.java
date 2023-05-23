package main.elements;

import main.kernel.Kernel;
import main.kernel.Scheduler;
import main.kernel.SystemCallHandler;

public class Memory {
    private static MemoryWord[] memoryArray;
    private static boolean[] occupiedArray;

    public Memory(){
        memoryArray = new MemoryWord[40];
        occupiedArray = new boolean[40];
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
        memoryArray[address] = null;
        occupiedArray[address] = false;
    }

    private static void fillMemoryWithProcessesWhichShouldBeInIt(){
        int nextStartingIndex = 0;
        for(ProcessMemoryImage p : Scheduler.getInMemoryProcessMemoryImages()){
            int lowerBound = nextStartingIndex;
            int upperBound = lowerBound + p.getProcessMemorySize()-1;
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
        memoryArray[address] = word;
        occupiedArray[address] = true;
    }

    public static synchronized MemoryWord readMemoryWord(int address) {
        return memoryArray[address];
    }

    /*
     * Used primarily when first creating a process
     * and just marking a memory partition as
     * reserved, until the process's data is moved
     * to this partition.
     */
    public static synchronized void allocateMemoryPartition(int lowerMemoryBound, int upperMemoryBound){
        for (int i = lowerMemoryBound; i < upperMemoryBound+1; i++){
            allocateMemoryWord(i);
        }
    }

    public static synchronized void allocateMemoryWord(int address){
        occupiedArray[address] = true;
    }

    public static synchronized void deallocateMemoryPartition(int lowerMemoryBound, int upperMemoryBound){
        for (int i = lowerMemoryBound; i < upperMemoryBound+1; i++){
            deallocateMemoryWord(i);
        }
    }

    public static synchronized void deallocateMemoryWord(int address){
        occupiedArray[address] = false;
    }

    @Override
    public synchronized String toString () {
        StringBuilder sb = new StringBuilder();
        sb.append("MEMORY\n");
        for (int i = 0; i < memoryArray.length; i++) {
            String varName;
            Object varData;
            if(!isMemoryWordOccupied(i)) {
                varName = " ";
                varData = " ";
            } else {
                varName = memoryArray[i].getVariableName();
                if(memoryArray[i].getVariableData() instanceof Integer){
                    varData = memoryArray[i].getVariableData();
                }
                else if (memoryArray[i].getVariableData() instanceof ProcessState){
                    varData = ((ProcessState) memoryArray[i].getVariableData()).name();
                }
                else {
                    varData = memoryArray[i].getVariableData();
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

    public static MemoryWord[] getMemoryArray() {
        return memoryArray;
    }

    public boolean[] getOccupied() {
        return occupiedArray;
    }

    public synchronized static boolean isMemoryWordOccupied(int address) {
        return occupiedArray[address];
    }

    public synchronized static MemoryWord findMemoryWordByName(String givenVariableName, int processID){
        for (int i = 0; i < memoryArray.length; i++){
            if (isIndexAtTheGivenProcessID(i, processID)){
                for (int j = i + Kernel.getPCBSize(); j < i + Kernel.getPCBSize() + Kernel.getDataSize(); j++){
                    if (memoryArray[j].getVariableName().equals(givenVariableName))
                        return readMemoryWord(j);
                }
            }
        }
        return null;
    }

    private static boolean isIndexAtTheGivenProcessID(int i, int processID){
        return Memory.isMemoryWordOccupied(i) &&
                memoryArray[i].getVariableName().equals("PROCESS_ID") &&
                memoryArray[i].getVariableData().equals(processID);
    }

}


