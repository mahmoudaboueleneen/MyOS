package main;

import main.elements.*;

public class MyOS {
    private static Mutex userInputMutex;
    private static Mutex userOutputMutex;
    private static Mutex fileMutex;
    private static Interpreter interpreter;
    private static Scheduler scheduler;
    private static Memory memory;
    private static Kernel kernel;

    public static Mutex getUserInputMutex() {return userInputMutex;}
    public static Mutex getUserOutputMutex() {return userOutputMutex;}
    public static Mutex getFileMutex() {return fileMutex;}
    public static Interpreter getInterpreter() {return interpreter;}
    public static Scheduler getScheduler() {return scheduler;}
    public static Memory getMemory() {return memory;}

    public MyOS(){
        userInputMutex = new Mutex();
        userOutputMutex = new Mutex();
        fileMutex = new Mutex();
        interpreter = new Interpreter();
        scheduler = new Scheduler();
        memory = new Memory();
        kernel = new Kernel();
    }

    public void runProgram(String programFilePath){
        kernel.createNewProcess(programFilePath);
    }

}
