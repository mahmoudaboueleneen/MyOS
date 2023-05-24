package main.elements;

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

        public synchronized int getProcessID() {
            return processID;
        }

        public synchronized ProcessState getProcessState() {
            return processState;
        }

        public synchronized void setProcessState(ProcessState processState) {
            this.processState = processState;
        }

        public synchronized int getProgramCounter() {
            return programCounter;
        }

        public synchronized void setProgramCounter(int programCounter) {
            this.programCounter = programCounter;
        }

        public synchronized int getLowerMemoryBoundary() {
            return lowerMemoryBoundary;
        }

        public synchronized void setLowerMemoryBoundary(int lowerMemoryBoundary) {
            this.lowerMemoryBoundary = lowerMemoryBoundary;
        }

        public synchronized int getUpperMemoryBoundary() {
            return upperMemoryBoundary;
        }

        public synchronized void setUpperMemoryBoundary(int upperMemoryBoundary) {
            this.upperMemoryBoundary = upperMemoryBoundary;
        }

        public synchronized String getTempLocation() {
            return tempLocation;
        }

        public synchronized void setTempLocation(String tempLocation) {
            this.tempLocation = tempLocation;
        }
}
