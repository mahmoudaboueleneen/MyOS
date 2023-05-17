package main.system.elements;

import main.MyOS;
import main.system.elements.Process;
import main.system.elements.ProcessState;

import java.util.ArrayDeque;
import java.util.Queue;

public class Mutex {

    public enum MutexValue{
        ZERO,   // Resource unavailable.
        ONE     // Resource available.
    }
    private MutexValue value;
    private Queue<Process> queue;
    private int ownerID;

    public Mutex(){
        this.queue = new ArrayDeque<Process>();
    }

    public void semWait(Process process){
        if (this.value == MutexValue.ONE) {
            // Acquire resource
            this.ownerID = process.getPCB().getProcessID();
            this.value = MutexValue.ZERO;
        }
        else {
            // Block process
            this.queue.add(process);
            process.getPCB().setProcessState(ProcessState.BLOCKED);
            MyOS.getScheduler().getBlockedQueue().add(process);
        }

    }

    public void semSignal(Process process){
        if(this.ownerID == process.getPCB().getProcessID()) {
            if (this.queue.isEmpty()) {
                // Release resource
                this.value = MutexValue.ONE;
            }
            else {
                // Release a process
                Process releasedProcess = this.queue.remove();
                releasedProcess.getPCB().setProcessState(ProcessState.READY);
                MyOS.getScheduler().getReadyQueue().add(releasedProcess);
                // Reassign the resource to the released process
                this.ownerID = releasedProcess.getPCB().getProcessID();
            }
        }
    }


//    public static Mutex userInputMutex;
//
//    public void interpretMutex(){
//        switch(){
//            case "userInput":
//
//                break;
//            case "userOutput":
//
//                break;
//            case "File":
//
//                break;
//        }
//    }
}
