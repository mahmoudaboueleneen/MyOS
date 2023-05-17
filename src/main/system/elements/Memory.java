package main.system.elements;

public class Memory {
    /* We will use Dynamic Memory Partitioning:
       Processes occupy the memory after one another with no spacing between them,
       compactMemory() method in this class is called when changes are made to the memory to ensure this.
     * A process will occupy the memory in the order:
       PCB (5 words), Data/Variables (3 words), Instructions (n words)
    */
    private MemoryWord[] memory;
    private boolean[] occupied;

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
    /* Prints memory in human-readable format, should be used every clock cycle. */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MEMORY:\n");
        // sb.append("Memory Address: {Variable, Data}\n");
        // sb.append("--------------------\n");
        for (int i = 0; i < memory.length; i++) {
            String varName;
            Object varData;
            if(memory[i] == null){
                varName = " ";
                varData = " ";
            }
            else{
                varName = memory[i].getVariableName();
                if(memory[i].getVariableData() instanceof Integer){
                    varData = (Integer) memory[i].getVariableData();
                }
                else if(memory[i].getVariableData() instanceof ProcessState){
                    varData = ((ProcessState) memory[i].getVariableData()).name();
                }
                else{
                    varData = (String) memory[i].getVariableData();
                }
            }
            sb.append(i + ": {" + varName +  "," + varData + "} \n");
        }
        // sb.append("--------------------");
        return sb.toString();
    }

    public MemoryWord readMemoryWord(int address) {
        if (address < 0 || address >= 40) {
            System.out.println("Memory address out of bound.");
            return null;
        }
        return memory[address];
    }
    public void writeMemoryWord(int address, MemoryWord word) {
        if (address < 0 || address >= 40) {
            System.out.println("Memory address out of bound.");
            return;
        }
        memory[address] = word;
        occupied[address] = true;
    }
    public boolean isOccupied(int address) {
        if (address < 0 || address >= 40) {
            System.out.println("Memory address out of bound.");
            return true;
        }
        return occupied[address];
    }

    // Allocates memory block for a process
    public void allocateMemoryPartition(Process p, int lowerMemoryBound, int upperMemoryBound) {
        int adr = lowerMemoryBound;

        // Allocate space for PCB
        writeMemoryWord(adr, new MemoryWord("PROCESS_ID", p.getPCB().getProcessID()) );
        writeMemoryWord(adr + 1, new MemoryWord("PROCESS_STATE", p.getPCB().getProcessState()) );
        writeMemoryWord(adr + 2, new MemoryWord("PROGRAM_COUNTER", p.getPCB().getProgramCounter()) );
        writeMemoryWord(adr + 3, new MemoryWord("LOWER_MEM_BOUND", lowerMemoryBound) );
        writeMemoryWord(adr + 4, new MemoryWord("UPPER_MEM_BOUND", upperMemoryBound) );

        // Allocate space for Data/Variables (starts as empty but fills up with data when process starts executing and initializing its own variables)
        writeMemoryWord(adr + 5, new MemoryWord("---", "---"));
        writeMemoryWord(adr + 6, new MemoryWord("---", "---"));
        writeMemoryWord(adr + 7, new MemoryWord("---", "---"));

        // Allocate space for Instructions
        int j = adr + 8;
        for(String instruction : p.getInstructions()){
            writeMemoryWord(j, new MemoryWord("INSTRUCTION", instruction));
            j++;
        }
    }

    // Deallocates memory block and empties it
    public void deallocateMemoryPartition(int lowerMemoryBound, int upperMemoryBound){
        for (int i = lowerMemoryBound; i < upperMemoryBound; i++){
            memory[i] = null;
            occupied[i] = false;
        }
    }

    // Dynamic memory partitioning, moves everything in the memory to the top to make space below
    public void compactMemory(){
        deallocateMemoryPartition(0,39);

        // Check on scheduler to see which processes to keep in memory, loading them in order

    }

}


