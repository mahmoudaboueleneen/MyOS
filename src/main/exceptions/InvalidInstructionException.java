package main.exceptions;

public class InvalidInstructionException extends Exception{
    public InvalidInstructionException() {
        super("Invalid instruction syntax.");
    }
}
