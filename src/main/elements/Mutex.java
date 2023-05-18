package main.elements;

import main.MyOS;

import java.util.ArrayDeque;
import java.util.Queue;

public class Mutex {

    public enum MutexValue{
        ZERO,   // Resource unavailable.
        ONE     // Resource available.
    }
    private MutexValue value;
    private final Queue<ProcessMemoryImage> blockedQueue;
    private int ownerID;

    public Mutex(){
        this.blockedQueue = new ArrayDeque<ProcessMemoryImage>();
    }

    public void semWait(ProcessMemoryImage processMemoryImage){
        if (this.value == MutexValue.ONE) {
            // Acquire resource
            this.ownerID = processMemoryImage.getPCB().getProcessID();
            this.value = MutexValue.ZERO;
        }
        else {
            // Block process
            this.blockedQueue.add(processMemoryImage);
            processMemoryImage.getPCB().setProcessState(ProcessState.BLOCKED);
            MyOS.getScheduler().getBlockedQueue().add(processMemoryImage);
        }

    }

    public void semSignal(ProcessMemoryImage processMemoryImage){
        if(this.ownerID == processMemoryImage.getPCB().getProcessID()) {
            if (this.blockedQueue.isEmpty()) {
                // Release resource
                this.value = MutexValue.ONE;
            }
            else {
                // Release a process
                ProcessMemoryImage releasedProcessMemoryImage = this.blockedQueue.remove();
                releasedProcessMemoryImage.getPCB().setProcessState(ProcessState.READY);
                MyOS.getScheduler().getReadyQueue().add(releasedProcessMemoryImage);
                // Reassign the resource to the released process
                this.ownerID = releasedProcessMemoryImage.getPCB().getProcessID();
            }
        }
    }

}
