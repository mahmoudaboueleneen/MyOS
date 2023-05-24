package main.elements;

import main.kernel.Kernel;
import main.kernel.Scheduler;

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
    public synchronized void semWait(ProcessMemoryImage p){
        if (this.value == MutexValue.ONE) {
//          Acquire resource
            this.ownerID = p.getPCB().getProcessID();
            this.value = MutexValue.ZERO;
        }
        else {
//          Block process
            this.blockedQueue.add(p);
            p.setProcessState(ProcessState.BLOCKED);
            Scheduler.getBlockedQueue().add(p);
            System.out.print("Scheduling event occurred: Process blocked.\n");
            Scheduler.printQueues();
        }

    }

    /**
     * Attempt to release resource.
      */
    public synchronized void semSignal(ProcessMemoryImage p){
        if(this.ownerID == p.getPCB().getProcessID()) {
            if (this.blockedQueue.isEmpty()) {
//              Release resource
                this.value = MutexValue.ONE;
            }
            else {
//              Unblock a process
                ProcessMemoryImage releasedPMI = this.blockedQueue.remove();
                Scheduler.getBlockedQueue().remove(releasedPMI);
                Scheduler.getReadyQueue().add(releasedPMI);
                releasedPMI.setProcessState(ProcessState.READY);
//              Reassign the resource to the newly unblocked process
                this.ownerID = releasedPMI.getPCB().getProcessID();
            }
        }
    }

}
