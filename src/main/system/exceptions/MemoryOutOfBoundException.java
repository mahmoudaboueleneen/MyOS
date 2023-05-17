package main.system.exceptions;

public class MemoryOutOfBoundException extends Exception{
    public MemoryOutOfBoundException() {
        super("Invalid Memory Address(es)");
    }
}
