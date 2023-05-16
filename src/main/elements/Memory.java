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


