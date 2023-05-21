package main.elements;

import main.kernel.Kernel;
import main.exceptions.InvalidInstructionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Interpreter {

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

    public synchronized int countFileLinesOfCode(String filePath){
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
        return (String) Kernel.getMemory().readMemoryWord(base+offset).getVariableData();
    }

    //TODO: Finish method:
    // NEED TO HANDLE WHEN INPUT IS ANOTHER INSTRUCTION e.g. assign b readFile a
    // NEED TO MAKE INTERPRETER CALL Kernel.getScheduler().programCounterShouldBeIncremented()
    public synchronized void interpret(String instruction, ProcessMemoryImage currentRunningProcessMemoryImage) throws InvalidInstructionException {
        String[] words = instruction.split(" ");
        String firstWord = words[0];

        switch (firstWord) {
            case "print" -> {
                if (words.length < 2)
                    throw new InvalidInstructionException("Invalid instruction syntax, print statement requires 1 parameter");
                Kernel.getSystemCallHandler().printToScreen(words[1]);
                Kernel.getScheduler().programCounterShouldBeIncremented();
            }
            case "assign" -> {
                if (words.length < 3)
                    throw new InvalidInstructionException("Invalid instruction syntax, assign statement requires 2 parameters.");
                if (words[2].equals("input")) {
                    Kernel.getSystemCallHandler().printToScreen("Please enter a value");
                    String inp = Kernel.getSystemCallHandler().takeInputFromUser();
                    Kernel.getSystemCallHandler().writeDataToMemory(words[1], inp, currentRunningProcessMemoryImage.getPCB().getProcessID());
                } else
                    Kernel.getSystemCallHandler().writeDataToMemory(words[1], words[2], currentRunningProcessMemoryImage.getPCB().getProcessID());
                Kernel.getScheduler().programCounterShouldBeIncremented();
            }
            case "writeFile" -> {
                if (words.length != 3)
                    throw new InvalidInstructionException("Invalid instruction syntax, writeFile statement requires 2 parameters.");
                Kernel.getSystemCallHandler().writeDataToFileOnDisk(words[1], words[2]);
                Kernel.getScheduler().programCounterShouldBeIncremented();
            }
            case "readFile" -> {
                if (words.length != 2)
                    throw new InvalidInstructionException("Invalid instruction syntax, readFile statement requires 1 parameter.");
                Kernel.getSystemCallHandler().readDataFromFileOnDisk(words[1]);
                Kernel.getScheduler().programCounterShouldBeIncremented();
            }
            case "printFromTo" -> {
                if (words.length < 3)
                    throw new InvalidInstructionException("Invalid instruction syntax, assign statement requires 2 parameters.");
                if (isInteger(words[1]) && isInteger(words[2])) {
                    int a = Integer.parseInt(words[1]);
                    int b = Integer.parseInt(words[2]);
                    for (int i = a + 1; i < b; i++) {
                        Kernel.getSystemCallHandler().printToScreen(i + "");
                    }
                } else
                    throw new InvalidInstructionException("Invalid instruction syntax, printFromTo statement requires 2 integer numbers.");
                Kernel.getScheduler().programCounterShouldBeIncremented();
            }
            case "semWait" -> {
                if (words.length != 2)
                    throw new InvalidInstructionException("Invalid instruction syntax, semWait statement requires 1 parameter.");
                switch (words[1]) {
                    case "userInput" -> Kernel.getUserInputMutex().semWait(currentRunningProcessMemoryImage);
                    case "userOutput" -> Kernel.getUserOutputMutex().semWait(currentRunningProcessMemoryImage);
                    case "file" -> Kernel.getFileMutex().semWait(currentRunningProcessMemoryImage);
                    default ->
                            throw new InvalidInstructionException("Invalid resource name: must be userInput, userOutput or file");
                }
                Kernel.getScheduler().programCounterShouldBeIncremented();
            }
            case "semSignal" -> {
                if (words.length != 2)
                    throw new InvalidInstructionException("Invalid instruction syntax, semWait statement requires 1 parameter.");
                switch (words[1]) {
                    case "userInput" -> Kernel.getUserInputMutex().semSignal(currentRunningProcessMemoryImage);
                    case "userOutput" -> Kernel.getUserOutputMutex().semSignal(currentRunningProcessMemoryImage);
                    case "file" -> Kernel.getFileMutex().semSignal(currentRunningProcessMemoryImage);
                    default ->
                            throw new InvalidInstructionException("Invalid resource name: must be userInput, userOutput or file");
                }
                Kernel.getScheduler().programCounterShouldBeIncremented();
            }
            default -> throw new InvalidInstructionException();
        }

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