package main.synchronization;

import main.kernel.Scheduler;
import main.process_management.ProcessMemoryImage;
import main.process_management.ProcessState;

import java.util.ArrayDeque;
import java.util.Queue;

public class Mutex {

    public enum MutexValue{
        ZERO,   // Resource is taken.
        ONE     // Resource is free.
    }

    // Mutex fields.
    private MutexValue value;
    private final Queue<ProcessMemoryImage> blockedQueue;
    private int ownerID;

    public Mutex(){
        this.value = MutexValue.ONE;
        this.blockedQueue = new ArrayDeque<>();
        this.ownerID = -1;
    }

    public synchronized void semWait(ProcessMemoryImage p){
        if (this.value == MutexValue.ONE) {
            // Acquire resource
            this.ownerID = p.getPCB().getProcessID();
            this.value = MutexValue.ZERO;
        } else {
            // Block process
            this.blockedQueue.add(p);
            p.setProcessState(ProcessState.BLOCKED);
            Scheduler.getBlockedQueue().add(p);
            System.out.println("PROCESS BLOCKED: " + p);
            Scheduler.printQueues();
        }
    }

    public synchronized void semSignal(ProcessMemoryImage p){
        if (this.ownerID == p.getPCB().getProcessID()) {
            if (this.blockedQueue.isEmpty()) {
                // Release resource
                this.value = MutexValue.ONE;
            } else {
                // Unblock a process
                ProcessMemoryImage releasedPMI = this.blockedQueue.remove();
                Scheduler.getBlockedQueue().remove(releasedPMI);
                Scheduler.getReadyQueue().add(releasedPMI);
                releasedPMI.setProcessState(ProcessState.READY);
                // Reassign the resource to the newly unblocked process
                this.ownerID = releasedPMI.getPCB().getProcessID();
            }
        }
    }

}
