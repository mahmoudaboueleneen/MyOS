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

    public synchronized MemoryWord readMemoryWord(int address) {
        return memory[address];
    }

    public synchronized void writeMemoryWord(int address, MemoryWord word) {
        memory[address] = word;
        occupied[address] = true;
    }

    public synchronized boolean isOccupied(int address) {
        return occupied[address];
    }

    // Allocates memory block for a process
    public synchronized void allocateMemoryPartition(ProcessMemoryImage p, int lowerMemoryBound, int upperMemoryBound) {
        int adr = lowerMemoryBound;

        // Allocate space for PCB
        writeMemoryWord(adr, new MemoryWord("PROCESS_ID", p.getPCB().getProcessID()) );
        writeMemoryWord(adr + 1, new MemoryWord("PROCESS_STATE", p.getPCB().getProcessState()) );
        writeMemoryWord(adr + 2, new MemoryWord("PROGRAM_COUNTER", p.getPCB().getProgramCounter()) );
        writeMemoryWord(adr + 3, new MemoryWord("LOWER_MEM_BOUND", lowerMemoryBound) );
        writeMemoryWord(adr + 4, new MemoryWord("UPPER_MEM_BOUND", upperMemoryBound) );
        writeMemoryWord(adr + 5, new MemoryWord("TEMP_LOCATION", p.getPCB().getTempLocation()==null?"---":p.getPCB().getTempLocation()) );

        // Allocate space for Data/Variables (starts as empty but fills up with data when process starts executing and initializing its own variables)
        writeMemoryWord(adr + 6, new MemoryWord("---", "---"));
        writeMemoryWord(adr + 7, new MemoryWord("---", "---"));
        writeMemoryWord(adr + 8, new MemoryWord("---", "---"));

        // Allocate space for Instructions
        int j = adr + 9;
        for(String instruction : p.getInstructions()){
            writeMemoryWord(j, new MemoryWord("INSTRUCTION", instruction));
            j++;
        }

    }

    // Deallocates memory block and empties it
    public synchronized void deallocateMemoryPartition(int lowerMemoryBound, int upperMemoryBound){
        for (int i = lowerMemoryBound; i < upperMemoryBound; i++){
            memory[i] = null;
            occupied[i] = false;
        }
    }

    // Dynamic memory partitioning, moves everything in the memory to the top to make space below
    public synchronized void compactMemory(){
        deallocateMemoryPartition(0,39);

        // Check on scheduler to see which processes to keep in memory, loading them in order

    }

}


