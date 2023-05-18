package main;

import main.elements.Interpreter;
import main.kernel.Kernel;
import main.elements.Memory;
import main.elements.Mutex;
import main.kernel.Scheduler;
import main.kernel.SystemCall;

public class MyOS {
    private static Mutex userInputMutex;
    private static Mutex userOutputMutex;
    private static Mutex fileMutex;
    private static Interpreter interpreter;
    private static Scheduler scheduler;
    private static Memory memory;
    private static Kernel kernel;
    private static SystemCall systemCall;

    public static Mutex getUserInputMutex() {return userInputMutex;}
    public static Mutex getUserOutputMutex() {return userOutputMutex;}
    public static Mutex getFileMutex() {return fileMutex;}
    public static Interpreter getInterpreter() {return interpreter;}
    public static Scheduler getScheduler() {return scheduler;}
    public static Memory getMemory() {return memory;}
    public static Kernel getKernel() {return kernel;}
    public static SystemCall getSystemCall() {return systemCall;}

    public MyOS(int roundRobinTimeSlice){
        userInputMutex = new Mutex();
        userOutputMutex = new Mutex();
        fileMutex = new Mutex();
        interpreter = new Interpreter();
        scheduler = new Scheduler(roundRobinTimeSlice);
        memory = new Memory();
        kernel = new Kernel();
        systemCall = new SystemCall();
    }

    public void createProcess(String programFilePath){
        new Thread(() -> kernel.createNewProcess(programFilePath)).start();
    }
}
