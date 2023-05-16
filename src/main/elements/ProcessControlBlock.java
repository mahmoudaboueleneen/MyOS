package main.elements;

import main.MyOS;

public class ProcessControlBlock {
        private int processID;
        private ProcessState processState;
        private int programCounter;
        private int lowerMemoryBoundary;
        private int upperMemoryBoundary;

        public ProcessControlBlock(int lowerMemoryBoundary, int upperMemoryBoundary){
            this.processID = MyOS.getScheduler().getNextProcessID();
            this.processState = ProcessState.NEW;
            this.programCounter = 0;
            this.lowerMemoryBoundary = lowerMemoryBoundary;
            this.upperMemoryBoundary = upperMemoryBoundary;
        }

        public int getProcessID() {return processID;}
        public void setProcessID(int processID) {this.processID = processID;}
        public ProcessState getProcessState() {return processState;}
        public void setProcessState(ProcessState processState) {this.processState = processState;}
        public int getProgramCounter() {return programCounter;}
        public void setProgramCounter(int programCounter) {this.programCounter = programCounter;}
        public int getLowerMemoryBoundary() {return lowerMemoryBoundary;}
        public void setLowerMemoryBoundary(int lowerMemoryBoundary) {this.lowerMemoryBoundary = lowerMemoryBoundary;}
        public int getUpperMemoryBoundary() {return upperMemoryBoundary;}
        public void setUpperMemoryBoundary(int upperMemoryBoundary) {this.upperMemoryBoundary = upperMemoryBoundary;}

}
