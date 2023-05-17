package main.system.elements;

public class Process {
    private ProcessControlBlock processControlBlock;
    //private String[] variableNames;
    //private Object[] variableData;
    private String[] instructions;

    public Process(int lowerMemoryBoundary, int upperMemoryBoundary, int linesOfCode, String[] instructions){
        this.processControlBlock = new ProcessControlBlock(lowerMemoryBoundary, upperMemoryBoundary);
        //this.variableNames = new String[3];
        //this.variableData = new Object[3];
        this.instructions = instructions;
    }

    public ProcessControlBlock getPCB() {
        return processControlBlock;
    }
    //public String[] getVariableNames() {return variableNames;}
    //public Object[] getVariableData() {return variableData;}
    public String[] getInstructions() {return instructions;}

}
