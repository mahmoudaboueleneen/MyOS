package main.elements;

import java.io.Serializable;

public class MemoryWord implements Serializable {
    private String variableName;
    private Object variableData;

    public MemoryWord(String variableName, Object variableData){
        this.variableName = variableName;
        this.variableData = variableData;
    }
    public String getVariableName() {return variableName;}
    public Object getVariableData() {return variableData;}
    public void setVariableName(String variableName) {this.variableName = variableName;}
    public void setVariableData(Object variableData) {this.variableData = variableData;}
}
