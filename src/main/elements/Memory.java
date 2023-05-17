package main.elements;

import main.exceptions.MemoryOutOfBoundException;

public class Memory {
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
    // Print memory in human-readable format.
    // Should be used every clock cycle.
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Memory:\n");
        sb.append("--------------------\n");
        for (int i = 0; i < memory.length; i++) {
            String varName;
            String varData;
            if(memory[i] == null){
                varName = "empty";
                varData = "empty";
            }
            else{
                varName = memory[i].getVariableName();
                varData = memory[i].getVariableData();
            }
            sb.append(i + " | " + varName +  " | " + varData + " | \n");
        }
        sb.append("--------------------");
        return sb.toString();
    }

    /*
    public void write(int address, MemoryWord word) throws MemoryOutOfBoundException {
        if (address < 0 || address >= memory.length) {
            throw new MemoryOutOfBoundException();
        }
        memory[address] = word;
        occupied[address] = true;
    }

    public MemoryWord read(int address) throws MemoryOutOfBoundException {
        if (address < 0 || address >= memory.length) {
            throw new MemoryOutOfBoundException();
        }
        return memory[address];
    }

    public boolean isOccupied(int address) throws MemoryOutOfBoundException {
        if (address < 0 || address >= memory.length) {
            throw new MemoryOutOfBoundException();
        }
        return occupied[address];
    }

    */

}


