package main.process_management;

import java.io.Serializable;

public class ProcessControlBlock implements Serializable {
        private final int processID;
        private ProcessState processState;
        private int programCounter;
        private int lowerMemoryBoundary;
        private int upperMemoryBoundary;
        private String tempLocation;

        public ProcessControlBlock(int processID, int lowerMemoryBoundary, int upperMemoryBoundary){
            this.processID = processID;
            this.processState = ProcessState.NEW;
            this.programCounter = 0;
            this.lowerMemoryBoundary = lowerMemoryBoundary;
            this.upperMemoryBoundary = upperMemoryBoundary;
        }

        public int getProcessID() {
            return processID;
        }

        public ProcessState getProcessState() {
            return processState;
        }

        public void setProcessState(ProcessState processState) {
            this.processState = processState;
        }

        public int getProgramCounter() {
            return programCounter;
        }

        public void setProgramCounter(int programCounter) {
            this.programCounter = programCounter;
        }

        public int getLowerMemoryBoundary() {
            return lowerMemoryBoundary;
        }

        public void setLowerMemoryBoundary(int lowerMemoryBoundary) {
            this.lowerMemoryBoundary = lowerMemoryBoundary;
        }

        public int getUpperMemoryBoundary() {
            return upperMemoryBoundary;
        }

        public void setUpperMemoryBoundary(int upperMemoryBoundary) {
            this.upperMemoryBoundary = upperMemoryBoundary;
        }

        public String getTempLocation() {
            return tempLocation;
        }

        public void setTempLocation(String tempLocation) {
            this.tempLocation = tempLocation;
        }
}
