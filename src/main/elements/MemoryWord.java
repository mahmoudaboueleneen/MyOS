package main.elements;

public class MemoryWord {
    private String variableName;
    private Object variableData;

    public MemoryWord(String variableName, Object variableData){
        this.variableName = variableName;
        this.variableData = variableData;
    }
    public synchronized String getVariableName() {return variableName;}
    public synchronized Object getVariableData() {return variableData;}
}
