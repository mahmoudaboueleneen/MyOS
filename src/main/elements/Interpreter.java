package main.elements;

import main.kernel.Kernel;
import main.exceptions.InvalidInstructionException;
import main.kernel.Scheduler;

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

    public static synchronized void interpretAndIncrementInstructionCycle(String instruction, ProcessMemoryImage currentRunningProcessMemoryImage) throws InvalidInstructionException {
        System.out.println("Instruction to be executed: '" + instruction + "'\n");
        interpret(instruction,currentRunningProcessMemoryImage);
        System.out.println("Instruction executed successfully!\nIncrementing instruction cycle...");
        Scheduler.incrementInstructionCycleAndPrintMemory();
    }

    //TODO: Change words[i] to ithWord.
    public static synchronized void interpret(String instruction, ProcessMemoryImage currentRunningProcessMemoryImage) throws InvalidInstructionException {
        String[] words = instruction.split(" ");
        String firstWord = words[0];

        switch (firstWord) {
            case "print" -> {
                if (words.length != 2)
                    throw new InvalidInstructionException("Invalid instruction syntax, print statement requires 1 parameter");
                MemoryWord word = getVariableWordFromProcessDataMemory(words[1],currentRunningProcessMemoryImage);
                String x = (String) word.getVariableData();
                Kernel.getSystemCallHandler().printToScreen(x);
            }

            case "assign" -> {
                if (words.length < 3)
                    throw new InvalidInstructionException("Invalid instruction syntax, assign statement requires 2 parameters.");

                if (words[2].equals("input")) {
                    Kernel.getSystemCallHandler().printToScreen("Please enter a value:");
                    String inputString = Kernel.getSystemCallHandler().takeInputFromUser();
                    writeVariableWordToProcessDataMemory(words[1], inputString, currentRunningProcessMemoryImage);
                }
                else if (words[2].equals("readFile")) {
                    if(words.length != 4)
                        throw new InvalidInstructionException("Invalid instruction syntax, readFile statement requires 1 parameter.");
                    writeVariableWordToProcessDataMemory(words[1], lastReadFileContent, currentRunningProcessMemoryImage);
                }
                else
                    writeVariableWordToProcessDataMemory(words[1], words[2], currentRunningProcessMemoryImage);
            }

            case "writeFile" -> {
                if (words.length != 3)
                    throw new InvalidInstructionException("Invalid instruction syntax, writeFile statement requires 2 parameters.");
                MemoryWord memWord1 = getVariableWordFromProcessDataMemory(words[1],currentRunningProcessMemoryImage);
                MemoryWord memWord2 = getVariableWordFromProcessDataMemory(words[2],currentRunningProcessMemoryImage);

                Kernel.getSystemCallHandler().writeDataToFileOnDisk((String) memWord1.getVariableData(), (String) memWord2.getVariableData());
            }

            case "readFile" -> {
                if (words.length != 2)
                    throw new InvalidInstructionException("Invalid instruction syntax, readFile statement requires 1 parameter.");
                MemoryWord word = getVariableWordFromProcessDataMemory(words[1],currentRunningProcessMemoryImage);
                lastReadFileContent = Kernel.getSystemCallHandler().readDataFromFileOnDisk((String) word.getVariableData());
            }

            case "printFromTo" -> {
                if (words.length < 3)
                    throw new InvalidInstructionException("Invalid instruction syntax, assign statement requires 2 parameters.");
                MemoryWord memWord1 = getVariableWordFromProcessDataMemory(words[1],currentRunningProcessMemoryImage);
                MemoryWord memWord2 = getVariableWordFromProcessDataMemory(words[2],currentRunningProcessMemoryImage);
                if ( isInteger( (String) memWord1.getVariableData() ) && isInteger( (String) memWord2.getVariableData() ) ) {
                    int a = Integer.parseInt((String) memWord1.getVariableData());
                    int b = Integer.parseInt((String) memWord2.getVariableData());
                    StringBuilder sb = new StringBuilder();
                    for (int i = a + 1; i < b; i++) {
                        sb.append(i).append(" ");
                    }
                    Kernel.getSystemCallHandler().printToScreen(sb.toString());
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

    private static void writeVariableWordToProcessDataMemory(String varName, String varData, ProcessMemoryImage p){
        MemoryWord[] memory = Memory.getMemoryArray();
        int currProcessID = p.getPCB().getProcessID();

        // Search memory for process
        for(int i = 0; i < memory.length; i++){
            if(Memory.isMemoryWordOccupied(i) && memory[i].getVariableName().equals("PROCESS_ID") && memory[i].getVariableData().equals(currProcessID)) {

                // Search process memory for empty data space
                for(int j = i+Kernel.getPCBSize(); j < i+Kernel.getPCBSize()+Kernel.getDataSize(); j++){
                    if(memory[j].getVariableName().equals("---")) {
                        MemoryWord word = new MemoryWord(varName, varData);
                        Kernel.getSystemCallHandler().writeDataToMemory(j, word);
                        p.addVariable(word);
                        return;
                    }
                }

            }
        }
    }

    private static MemoryWord getVariableWordFromProcessDataMemory(String varName, ProcessMemoryImage p){
        MemoryWord[] memory = Memory.getMemoryArray();
        int currProcessID = p.getPCB().getProcessID();

        // Search memory for process
        for(int i = 0; i < memory.length; i++){
            if(Memory.isMemoryWordOccupied(i) && memory[i].getVariableName().equals("PROCESS_ID") && memory[i].getVariableData().equals(currProcessID)){

                // Search process memory for empty data space
                for(int j = i+6; j < i+9; j++){
                    if(memory[j].getVariableName().equals(varName)) {
                        MemoryWord word;
                        word = Kernel.getSystemCallHandler().readDataFromMemory(j);
                        return word;
                    }
                }

            }
        }

        return null; //shouldn't be reached
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