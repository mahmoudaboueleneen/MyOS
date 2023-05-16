package main.elements;

public class Process {
    private ProcessControlBlock processControlBlock;
    private Object[] variables;
    private String[] instructions;

    public Process(int lowerMemoryBoundary, int upperMemoryBoundary, int linesOfCode){
        this.processControlBlock = new ProcessControlBlock(lowerMemoryBoundary, upperMemoryBoundary);
        this.variables = new Object[3];
        this.instructions = new String[linesOfCode];

    }

    public ProcessControlBlock getPCB() {
        return processControlBlock;
    }

}
