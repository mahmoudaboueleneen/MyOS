package main.elements;

import main.kernel.Kernel;

import java.util.ArrayDeque;
import java.util.Queue;

public class Mutex {

    public enum MutexValue{
        ZERO,   // Resource is taken.
        ONE     // Resource is free.
    }
    private MutexValue value;
    private final Queue<ProcessMemoryImage> blockedQueue;
    private int ownerID;

    public Mutex(){
        this.value = MutexValue.ONE;
        this.blockedQueue = new ArrayDeque<>();
        this.ownerID = -1;
    }

    /**
     * Attempt to acquire resource.
     */
    public synchronized void semWait(ProcessMemoryImage processMemoryImage){
        if (this.value == MutexValue.ONE) {
//          Acquire resource
            this.ownerID = processMemoryImage.getPCB().getProcessID();
            this.value = MutexValue.ZERO;
        }
        else {
//          Block process
            this.blockedQueue.add(processMemoryImage);
            processMemoryImage.getPCB().setProcessState(ProcessState.BLOCKED);
            Kernel.getScheduler().getBlockedQueue().add(processMemoryImage);
        }

    }

    /**
     * Attempt to release resource.
      */
    public synchronized void semSignal(ProcessMemoryImage processMemoryImage){
        if(this.ownerID == processMemoryImage.getPCB().getProcessID()) {
            if (this.blockedQueue.isEmpty()) {
//              Release resource
                this.value = MutexValue.ONE;
            }
            else {
//              Unblock a process
                ProcessMemoryImage releasedProcessMemoryImage = this.blockedQueue.remove();
                releasedProcessMemoryImage.getPCB().setProcessState(ProcessState.READY);
                Kernel.getScheduler().getReadyQueue().add(releasedProcessMemoryImage);
//              Reassign the resource to the newly unblocked process
                this.ownerID = releasedProcessMemoryImage.getPCB().getProcessID();
            }
        }
    }

}
