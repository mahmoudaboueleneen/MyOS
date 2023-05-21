package main.kernel;

import java.util.Scanner;

public class SystemCallHandler {
    /** System calls are the process’s way of requesting a service from the OS.
     * In order for a process to be able to use any of the available hardware, it makes a request,
       system call, to the operating system.
    */
    public SystemCallHandler(){}

    //TODO: Finish all methods.

    public synchronized void readDataFromFileOnDisk(String fileName){

    }

    public synchronized void writeDataToFileOnDisk(String fileName, String data){
        // .. the program writes the data to the file. Assume that the file doesn't exist and should always be created.
        //          --> generate file with data and put it src/generated_files/
    }

    public synchronized void printToScreen(String data){
        System.out.println(data);
    }

    public synchronized String takeInputFromUser(){
        Scanner sc = new Scanner(System.in);
        String stringInput = sc.nextLine();
        return stringInput;
    }

    public synchronized void readDataFromMemory(String fileName){

    }

    public synchronized void writeDataToMemory(String variableName, String variableData, int currentRunningProcessID){
        // take as input variableName and variableData and searches for first reserved spot (marked with "---") in process memory data to put this variable
        // memory.writeword
    }


}
