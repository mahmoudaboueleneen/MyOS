package main.elements;

import main.exceptions.VariableAssignmentException;
import main.kernel.Kernel;
import main.kernel.Scheduler;
import main.kernel.SystemCallHandler;

public abstract class MemoryManager {
    private static final int MEMORY_SIZE = 40;
    private static final MemoryWord[] memoryArray = new MemoryWord[MEMORY_SIZE];
    private static final boolean[] reservedArray = new boolean[MEMORY_SIZE];


    public static void compactMemory(){
        clearMemory();
        fillMemoryWithProcessesWhichShouldBeInIt();
    }


    private static void clearMemory(){
        clearMemoryPartition(0,MEMORY_SIZE - 1);
    }


    public static void clearMemoryPartition(int lowerMemoryBound, int upperMemoryBound){
        for (int i = lowerMemoryBound; i < upperMemoryBound+1; i++){
            clearMemoryWord(i);
        }
    }


    private static void clearMemoryWord(int address){
        memoryArray[address] = null;
        reservedArray[address] = false;
    }


    private static void fillMemoryWithProcessesWhichShouldBeInIt(){
        int i = 0;
        for(ProcessMemoryImage p : Scheduler.getInMemoryProcessMemoryImages()){
            p.setLowerMemoryBoundary(i);
            p.setUpperMemoryBoundary(i + Kernel.getPCBSize() + Kernel.getDataSize() + p.getInstructions().length - 1);
            fillMemoryPartitionWithProcess(p);

            i = i + Kernel.getPCBSize() + Kernel.getDataSize() + p.getInstructions().length;
        }
    }


    public static void fillMemoryPartitionWithProcess(ProcessMemoryImage p) {
        fillPCBMemorySpace(p);
        fillDataMemorySpace(p);
        fillInstructionsMemorySpace(p);
    }


    private static void fillPCBMemorySpace(ProcessMemoryImage p){
        int i = p.getPCB().getLowerMemoryBoundary();
        writeMemoryWord(i, new MemoryWord("PROCESS_ID", p.getPCB().getProcessID()) );
        writeMemoryWord(i+1, new MemoryWord("PROCESS_STATE", p.getPCB().getProcessState()) );
        writeMemoryWord(i+2, new MemoryWord("PROGRAM_COUNTER", p.getPCB().getProgramCounter()) );
        writeMemoryWord(i+3, new MemoryWord("LOWER_MEM_BOUND", p.getPCB().getLowerMemoryBoundary()) );
        writeMemoryWord(i+4, new MemoryWord("UPPER_MEM_BOUND", p.getPCB().getUpperMemoryBoundary()) );
        writeMemoryWord(i+5, new MemoryWord("TEMP_LOCATION", p.getPCB().getTempLocation()==null?"---":p.getPCB().getTempLocation()) );
    }


    private static void fillDataMemorySpace(ProcessMemoryImage p){
        int i = p.getPCB().getLowerMemoryBoundary() + Kernel.getPCBSize();
        for(MemoryWord var : p.getVariables()){
            if(var == null) writeMemoryWord(i, new MemoryWord("---", "---"));
            else writeMemoryWord(i, new MemoryWord(var.getVariableName(), var.getVariableData()));
            i++;
        }
    }


    private static void fillInstructionsMemorySpace(ProcessMemoryImage p){
        int i = p.getPCB().getLowerMemoryBoundary() + Kernel.getPCBSize() + Kernel.getDataSize();
        for(String instruction : p.getInstructions()){
            writeMemoryWord(i, new MemoryWord("INSTRUCTION", instruction));
            i++;
        }
    }


    public static void writeMemoryWord(int address, MemoryWord word) {
        memoryArray[address] = word;
        reservedArray[address] = true;
    }


    public static MemoryWord readMemoryWord(int address) {
        return memoryArray[address];
    }


    /*
     * Used primarily when first creating a process
     * and just marking a memory partition as
     * reserved, until the process's data is moved
     * to this partition.
     */
    public static void allocateMemoryPartition(int lowerMemoryBound, int upperMemoryBound){
        for (int i = lowerMemoryBound; i < upperMemoryBound+1; i++){
            allocateMemoryWord(i);
        }
    }


    private static void allocateMemoryWord(int address){
        reservedArray[address] = true;
    }


    public static void deallocateMemoryPartition(int lowerMemoryBound, int upperMemoryBound){
        for (int i = lowerMemoryBound; i < upperMemoryBound+1; i++){
            deallocateMemoryWord(i);
        }
    }


    private static void deallocateMemoryWord(int address){
        reservedArray[address] = false;
    }


    public static MemoryWord getMemoryWordByName(String givenVariableName, int processID){
        // Search memory for process with given processID
        for (int i = 0; i < memoryArray.length; i++){
            if ( isIndexAtTheGivenProcessID(i, processID) ) {

                // Search process memory space for the required memory word
                for (int j = i+1; j < memoryArray.length; j++){
                    if(memoryArray[j].getVariableName().equals("PROCESS_ID"))
                        return null; // we reached another Process without finding the word.

                    if (memoryArray[j].getVariableName().equals(givenVariableName))
                        return SystemCallHandler.readDataFromMemory(j);
                }
            }
        }
        return null;
    }


    public static void setMemoryWordValue(String varName, String varData, ProcessMemoryImage p) {
        int processID = p.getPCB().getProcessID();

        // Search memory for process with given processID
        for (int i = 0; i < memoryArray.length; i++){
            if( isIndexAtTheGivenProcessID(i, processID)) {

                // Search process memory space for the required memory word
                for (int j = i+1; j < memoryArray.length; j++){
                    if(memoryArray[j].getVariableName().equals("PROCESS_ID"))
                        return; // we reached another Process without finding the word.

                    if (memoryArray[j].getVariableName().equals(varName)){
                        SystemCallHandler.writeDataToMemory(j, new MemoryWord(varName, varData));
                        return;
                    }
                }
            }
        }
    }


    public static void initializeVariableInMemory(String varName, String varData, ProcessMemoryImage p) throws VariableAssignmentException {
        int processID = p.getPCB().getProcessID();

        // Search memory for process with given processID
        for (int i = 0; i < memoryArray.length; i++){
            if( isIndexAtTheGivenProcessID(i, processID)) {

                // Search process memory space for a free data variable space
                for (int j = i+1; j < memoryArray.length; j++){
                    if(memoryArray[j].getVariableName().equals("PROCESS_ID"))
                        return; // we reached another Process without finding the word.

                    if (memoryArray[j].getVariableName().equals(varName))
                        throw new VariableAssignmentException(); // var with same name already assigned for this process.

                    if (memoryArray[j].getVariableName().equals("---")) { // free space found.
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
        return MemoryManager.isMemoryWordOccupied(i) && memoryArray[i]!=null &&
                memoryArray[i].getVariableName().equals("PROCESS_ID") &&
                memoryArray[i].getVariableData().equals(processID);
    }


    public static void printMemory() {
        StringBuilder sb = new StringBuilder();
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
        System.out.println(sb);
    }


    public static MemoryWord[] getMemoryArray() {
        return memoryArray;
    }


    public static boolean[] getReservedArray() {
        return reservedArray;
    }


    public static boolean isMemoryWordOccupied(int address) {
        return reservedArray[address];
    }

}


