package main.elements;

public class Memory {
    private final MemoryWord[] memory;
    private final boolean[] occupied;

    public Memory(){
        memory = new MemoryWord[40];
        occupied = new boolean[40];
    }

    public MemoryWord[] getMemory() {
        return memory;
    }
    public boolean[] getOccupied() {
        return occupied;
    }

    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MEMORY: \n");

        for (int i = 0; i < memory.length; i++) {
            String varName;
            Object varData;

            if(memory[i] == null) {
                varName = " ";
                varData = " ";
            }

            else {
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
            sb.append(i + ": {" + varName +  "," + varData + "} \n");
        }

        return sb.toString();
    }

    public synchronized boolean isOccupied(int address) {return occupied[address];}

    public synchronized MemoryWord readMemoryWord(int address) {
        return memory[address];
    }

    public synchronized void writeMemoryWord(int address, MemoryWord word) {
        memory[address] = word;
        occupied[address] = true;
    }

    public synchronized void clearMemoryWord(int address){
        memory[address] = null;
    }

    public synchronized void allocateMemoryWord(int address){
        occupied[address] = true;
    }

    public synchronized void deallocateMemoryWord(int address){
        occupied[address] = false;
    }

    public synchronized void allocateMemoryPartition(int lowerMemoryBound, int upperMemoryBound){
        for (int i = lowerMemoryBound; i < upperMemoryBound; i++){
            allocateMemoryWord(i);
        }
    }

    public synchronized void deallocateMemoryPartition(int lowerMemoryBound, int upperMemoryBound){
        for (int i = lowerMemoryBound; i < upperMemoryBound; i++){
            deallocateMemoryWord(i);
        }
    }

    public synchronized void fillMemoryPartition(ProcessMemoryImage p, int lowerMemoryBound, int upperMemoryBound) {
        int adr = lowerMemoryBound;

        // PCB Space
        writeMemoryWord(adr, new MemoryWord("PROCESS_ID", p.getPCB().getProcessID()) );
        writeMemoryWord(adr + 1, new MemoryWord("PROCESS_STATE", p.getPCB().getProcessState()) );
        writeMemoryWord(adr + 2, new MemoryWord("PROGRAM_COUNTER", p.getPCB().getProgramCounter()) );
        writeMemoryWord(adr + 3, new MemoryWord("LOWER_MEM_BOUND", lowerMemoryBound) );
        writeMemoryWord(adr + 4, new MemoryWord("UPPER_MEM_BOUND", upperMemoryBound) );
        writeMemoryWord(adr + 5, new MemoryWord("TEMP_LOCATION", p.getPCB().getTempLocation()==null?"---":p.getPCB().getTempLocation()) );

        // Data space ('---' means reserved, acts as placeholder until actual values are added when the process starts executing)
        writeMemoryWord(adr + 6, new MemoryWord("---", "---"));
        writeMemoryWord(adr + 7, new MemoryWord("---", "---"));
        writeMemoryWord(adr + 8, new MemoryWord("---", "---"));

        // Instructions space
        int j = adr + 9;
        for(String instruction : p.getInstructions()){
            writeMemoryWord(j, new MemoryWord("INSTRUCTION", instruction));
            j++;
        }
    }

//  Empties memory block
    public synchronized void clearMemoryPartition(int lowerMemoryBound, int upperMemoryBound){
        for (int i = lowerMemoryBound; i < upperMemoryBound; i++){
            clearMemoryWord(i);
        }
    }

//  Moves everything in the memory to the top to make space below.
//  Uses dynamic memory partitioning.
    //TODO: Finish method.
    public synchronized void compactMemory(){
        deallocateMemoryPartition(0,39);
        //Check on scheduler to see which processes to keep in memory, loading them in order



    }

    public synchronized int getStartingIndexOfProcessDataInMemory(ProcessMemoryImage p){
        return 6 + getStartingIndexOfProcessInMemory(p);
    }

    //TODO: Finish method.
    public synchronized int getStartingIndexOfProcessInMemory(ProcessMemoryImage p){
        for(int i = 0; i < memory.length; i++){
            //
        }
        return 0;
    }





}


