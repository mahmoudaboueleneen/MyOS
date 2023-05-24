package main.elements;

import main.exceptions.VariableAssignmentException;
import main.kernel.Kernel;
import main.exceptions.InvalidInstructionException;
import main.kernel.Scheduler;
import main.kernel.SystemCallHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Interpreter {
    private static String temp;

    public synchronized String[] getInstructionsFromFile(String filePath){
        ArrayList<String> res = new ArrayList<>();
        try {
            File fileObj = new File(filePath);
            Scanner myReader = new Scanner(fileObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                res.add(data);
            }
            myReader.close();
        }
        catch (FileNotFoundException e) {
            System.out.println("Error: File not found, exiting.");
            e.printStackTrace();
        }
        return res.toArray(new String[0]);
    }

    public static synchronized int countFileLinesOfCode(String filePath){
        int linesOfCode = 0;
        try {
            File fileObj = new File(filePath);
            Scanner myReader = new Scanner(fileObj);
            while (myReader.hasNextLine()) {
                myReader.nextLine();
                linesOfCode++;
            }
            myReader.close();
        }
        catch (FileNotFoundException e) {
            System.out.println("Error: File not found, exiting.");
            e.printStackTrace();
        }
        return linesOfCode;
    }

    public synchronized String getNextProcessInstruction(ProcessMemoryImage p){
        int base = p.getPCB().getLowerMemoryBoundary() + 9;
        int offset = p.getPCB().getProgramCounter();
        return (String) Memory.readMemoryWord(base+offset).getVariableData();
    }

    public static synchronized void interpretAndIncrementInstructionCycle(String instruction, ProcessMemoryImage currentRunningProcessMemoryImage) throws InvalidInstructionException, VariableAssignmentException {
        System.out.println("INSTRUCTION TO BE EXECUTED: '" + instruction + "'\n");
        interpret(instruction,currentRunningProcessMemoryImage);
        System.out.println("Instruction successfully executed!\n");
        System.out.println("MEMORY AFTER EXECUTING INSTR.:");
        System.out.println(Kernel.getMemory());
        System.out.println("Incrementing instruction cycle...");
        Scheduler.incrementInstructionCycleAndPrintMemory();
    }

    //TODO: Change words[n] to nthWord.
    public static synchronized void interpret(String instruction, ProcessMemoryImage currentRunningProcessMemoryImage) throws InvalidInstructionException, VariableAssignmentException {
        String[] words = instruction.split(" ");
        String firstWord = words[0];

        switch (firstWord) {
            case "print" -> {
                if (words.length != 2)
                    throw new InvalidInstructionException("Invalid instruction syntax, print statement requires 1 parameter");
                MemoryWord word = getVariable(words[1],currentRunningProcessMemoryImage);
                String x = (String) word.getVariableData();
                SystemCallHandler.printToScreen(x);
            }

            case "assign" -> {
                if (words.length < 3)
                    throw new InvalidInstructionException("Invalid instruction syntax, assign statement requires 2 parameters at least.");

                if (words[2].equals("input")) {
                    if(words.length != 3)
                       throw new InvalidInstructionException("Invalid instruction syntax, should be 'assign VAR_NAME input'");
                    Memory.initializeVariableInMemory(words[1], temp, currentRunningProcessMemoryImage);
                }
                else if (words[2].equals("readFile")) {
                    if(words.length != 4)
                        throw new InvalidInstructionException("Invalid instruction syntax, readFile statement requires 1 parameter.");
                    Memory.initializeVariableInMemory(words[1], temp, currentRunningProcessMemoryImage);
                }
                else
                    Memory.initializeVariableInMemory(words[1], words[2], currentRunningProcessMemoryImage);
            }

            case "input" -> {
                SystemCallHandler.printToScreen("Please enter a value:");
                temp = SystemCallHandler.takeInputFromUser();
            }

            case "writeFile" -> {
                if (words.length != 3)
                    throw new InvalidInstructionException("Invalid instruction syntax, writeFile statement requires 2 parameters.");
                MemoryWord memWord1 = getVariable(words[1],currentRunningProcessMemoryImage);
                MemoryWord memWord2 = getVariable(words[2],currentRunningProcessMemoryImage);

                SystemCallHandler.writeDataToFileOnDisk((String) memWord1.getVariableData(), (String) memWord2.getVariableData());
            }

            case "readFile" -> {
                if (words.length != 2)
                    throw new InvalidInstructionException("Invalid instruction syntax, readFile statement requires 1 parameter.");
                MemoryWord word = getVariable(words[1],currentRunningProcessMemoryImage);
                temp = SystemCallHandler.readDataFromFileOnDisk((String) word.getVariableData());
            }

            case "printFromTo" -> {
                if (words.length < 3)
                    throw new InvalidInstructionException("Invalid instruction syntax, assign statement requires 2 parameters.");
                MemoryWord memWord1 = getVariable(words[1],currentRunningProcessMemoryImage);
                MemoryWord memWord2 = getVariable(words[2],currentRunningProcessMemoryImage);
                if ( isInteger( (String) memWord1.getVariableData() ) && isInteger( (String) memWord2.getVariableData() ) ) {
                    int a = Integer.parseInt((String) memWord1.getVariableData());
                    int b = Integer.parseInt((String) memWord2.getVariableData());
                    StringBuilder sb = new StringBuilder();
                    for (int i = a + 1; i < b; i++) {
                        sb.append(i).append(" ");
                    }
                    SystemCallHandler.printToScreen(sb.toString());
                } else
                    throw new InvalidInstructionException("Invalid instruction syntax, printFromTo statement requires 2 integer numbers.");
            }

            case "semWait" -> {
                if (words.length != 2)
                    throw new InvalidInstructionException("Invalid instruction syntax, semWait statement requires 1 parameter.");
                switch (words[1]) {
                    case "userInput" -> Kernel.getUserInputMutex().semWait(currentRunningProcessMemoryImage);
                    case "userOutput" -> Kernel.getUserOutputMutex().semWait(currentRunningProcessMemoryImage);
                    case "file" -> Kernel.getFileMutex().semWait(currentRunningProcessMemoryImage);
                    default -> throw new InvalidInstructionException("Invalid resource name: must be userInput, userOutput or file");
                }
            }

            case "semSignal" -> {
                if (words.length != 2)
                    throw new InvalidInstructionException("Invalid instruction syntax, semWait statement requires 1 parameter.");
                switch (words[1]) {
                    case "userInput" -> Kernel.getUserInputMutex().semSignal(currentRunningProcessMemoryImage);
                    case "userOutput" -> Kernel.getUserOutputMutex().semSignal(currentRunningProcessMemoryImage);
                    case "file" -> Kernel.getFileMutex().semSignal(currentRunningProcessMemoryImage);
                    default -> throw new InvalidInstructionException("Invalid resource name: must be userInput, userOutput or file");
                }
            }
            default -> throw new InvalidInstructionException();
        }
    }

    private static MemoryWord getVariable(String varName, ProcessMemoryImage p){
        int processID = p.getPCB().getProcessID();
        return Memory.getMemoryWordByName(varName,processID);
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}