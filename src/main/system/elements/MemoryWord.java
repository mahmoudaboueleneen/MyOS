package main.system.elements;

public class MemoryWord {
    private String variableName;
    private Object variableData;

    public MemoryWord(String variableName, Object variableData){
        this.variableName = variableName;
        this.variableData = variableData;
    }

    public String getVariableName() {
        return variableName;
    }

    public Object getVariableData() {
        return variableData;
    }
}
