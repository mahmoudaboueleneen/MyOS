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
    private static String lastReadFileContent;

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
        System.out.println("Instruction to be executed: '" + instruction + "'\n");
        interpret(instruction,currentRunningProcessMemoryImage);
        System.out.print("Instruction successfully executed. Incrementing instruction cycle...");
        Scheduler.incrementInstructionCycleAndPrintMemory();
    }

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
                    throw new InvalidInstructionException("Invalid instruction syntax, assign statement requires 2 parameters.");

                if (words[2].equals("input")) {
                    SystemCallHandler.printToScreen("Please enter a value:");
                    String inputString = SystemCallHandler.takeInputFromUser();
                    assignVariableValue(words[1], inputString, currentRunningProcessMemoryImage);
                }
                else if (words[2].equals("readFile")) {
                    if(words.length != 4)
                        throw new InvalidInstructionException("Invalid instruction syntax, readFile statement requires 1 parameter.");
                    assignVariableValue(words[1], lastReadFileContent, currentRunningProcessMemoryImage);
                }
                else
                    assignVariableValue(words[1], words[2], currentRunningProcessMemoryImage);
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
                lastReadFileContent = SystemCallHandler.readDataFromFileOnDisk((String) word.getVariableData());
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

    private static void assignVariableValue(String varName, String varData, ProcessMemoryImage p) throws VariableAssignmentException {
        Memory.assignMemoryWordValueByName(varName, varData, p);
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