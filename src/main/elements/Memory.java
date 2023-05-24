package main.elements;

import main.exceptions.VariableAssignmentException;
import main.kernel.Kernel;
import main.kernel.Scheduler;
import main.kernel.SystemCallHandler;

public class Memory {
    private static MemoryWord[] memoryArray;
    private static boolean[] reservedArray;

    public Memory(){
        memoryArray = new MemoryWord[40];
        reservedArray = new boolean[40];
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
        reservedArray[address] = false;
    }

    private static void fillMemoryWithProcessesWhichShouldBeInIt(){
        int i = 0;
        for(ProcessMemoryImage p : Scheduler.getInMemoryProcessMemoryImages()){
            p.setLowerMemoryBoundary(i);
            p.setUpperMemoryBoundary(i + Kernel.getPCBSize() + Kernel.getDataSize() + p.getInstructions().length);
            fillMemoryPartitionWithProcess(p);

            i++;
        }
    }

    public static synchronized void fillMemoryPartitionWithProcess(ProcessMemoryImage p) {
        fillPCBMemorySpace(p);
        fillDataMemorySpace(p);
        fillInstructionsMemorySpace(p);
    }

    private static synchronized void fillPCBMemorySpace(ProcessMemoryImage p){
        int i = p.getPCB().getLowerMemoryBoundary();
        writeMemoryWord(i, new MemoryWord("PROCESS_ID", p.getPCB().getProcessID()) );
        writeMemoryWord(i+1, new MemoryWord("PROCESS_STATE", p.getPCB().getProcessState()) );
        writeMemoryWord(i+2, new MemoryWord("PROGRAM_COUNTER", p.getPCB().getProgramCounter()) );
        writeMemoryWord(i+3, new MemoryWord("LOWER_MEM_BOUND", p.getPCB().getLowerMemoryBoundary()) );
        writeMemoryWord(i+4, new MemoryWord("UPPER_MEM_BOUND", p.getPCB().getUpperMemoryBoundary()) );
        writeMemoryWord(i+5, new MemoryWord("TEMP_LOCATION", p.getPCB().getTempLocation()==null?"---":p.getPCB().getTempLocation()) );
    }

    private static synchronized void fillDataMemorySpace(ProcessMemoryImage p){
        int i = p.getPCB().getLowerMemoryBoundary() + Kernel.getPCBSize();
        for(MemoryWord var : p.getVariables()){
            if(var == null) writeMemoryWord(i, new MemoryWord("---", "---"));
            else writeMemoryWord(i, new MemoryWord(var.getVariableName(), var.getVariableData()));
            i++;
        }
    }

    private static synchronized void fillInstructionsMemorySpace(ProcessMemoryImage p){
        int i = p.getPCB().getLowerMemoryBoundary() + Kernel.getPCBSize() + Kernel.getDataSize();
        for(String instruction : p.getInstructions()){
            writeMemoryWord(i, new MemoryWord("INSTRUCTION", instruction));
            i++;
        }
    }

    public static synchronized void writeMemoryWord(int address, MemoryWord word) {
        memoryArray[address] = word;
        reservedArray[address] = true;
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
        reservedArray[address] = true;
    }

    public static synchronized void deallocateMemoryPartition(int lowerMemoryBound, int upperMemoryBound){
        for (int i = lowerMemoryBound; i < upperMemoryBound+1; i++){
            deallocateMemoryWord(i);
        }
    }

    public static synchronized void deallocateMemoryWord(int address){
        reservedArray[address] = false;
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

    public boolean[] getReservedArray() {
        return reservedArray;
    }

    public synchronized static boolean isMemoryWordOccupied(int address) {
        return reservedArray[address];
    }

    public synchronized static MemoryWord getMemoryWordByName(String givenVariableName, int processID){

        for (int i = 0; i < memoryArray.length; i++){
            if ( isIndexAtTheGivenProcessID(i, processID) ) {
                for (int j = i + Kernel.getPCBSize(); j < i + Kernel.getPCBSize() + Kernel.getDataSize(); j++){
                    if (memoryArray[j].getVariableName().equals(givenVariableName))
                        return SystemCallHandler.readDataFromMemory(j);
                }
            }
        }
        return null;
    }

    public synchronized static void assignMemoryWordValueByName(String varName, String varData, ProcessMemoryImage p) throws VariableAssignmentException {
        int processID = p.getPCB().getProcessID();

        for (int i = 0; i < memoryArray.length; i++){
            if( isIndexAtTheGivenProcessID(i, processID)) {
                for (int j = i+Kernel.getPCBSize(); j < i+Kernel.getPCBSize()+Kernel.getDataSize(); j++){
                    if (memoryArray[j].getVariableName().equals(varName))
                        throw new VariableAssignmentException();

                    if (memoryArray[j].getVariableName().equals("---")) {
                        MemoryWord word = new MemoryWord(varName, varData);
                        SystemCallHandler.writeDataToMemory(j, word);
                        p.addVariable(word);
                        return;
                    }
                }
            }
        }
    }

    private static boolean isIndexAtTheGivenProcessID(int i, int processID){
        return Memory.isMemoryWordOccupied(i) && memoryArray[i]!=null &&
                memoryArray[i].getVariableName().equals("PROCESS_ID") &&
                memoryArray[i].getVariableData().equals(processID);
    }

}


