package main.exceptions;

public class VariableAssignmentException extends Exception{
    public VariableAssignmentException() {
        super("Error: Variable name already initialized for this Process.");
    }

    public VariableAssignmentException(String message) {
        super(message);
    }
}
