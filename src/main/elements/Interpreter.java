package main.elements;

import main.kernel.Kernel;
import main.exceptions.InvalidInstructionException;
import main.kernel.Scheduler;

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
    // CHANGE words[i] to ithWord
    public synchronized void interpret(String instruction, ProcessMemoryImage currentRunningProcessMemoryImage) throws InvalidInstructionException {
        String[] words = instruction.split(" ");
        int currPID = currentRunningProcessMemoryImage.getPCB().getProcessID();

        String firstWord = words[0];
        //String secondWord = words[1];
        //String thirdWord = words[2];
        //String fourthWord = words[3];

        // Initialized when "readFile" instruction is used.
        String contentOfReadFile;

        switch (firstWord) {
            case "print" -> {       // 'print x'
                if (words.length != 2)
                    throw new InvalidInstructionException("Invalid instruction syntax, print statement requires 1 parameter");
                MemoryWord word = getVariableWordFromProcessDataMemory(words[1],currPID);
                String x = (String) word.getVariableData();
                Kernel.getSystemCallHandler().printToScreen(x);
            }

            case "assign" -> {      // 'assign x y' where x is variable and y is value.
                if (words.length < 3)
                    throw new InvalidInstructionException("Invalid instruction syntax, assign statement requires 2 parameters.");
                if (words[2].equals("input")) { // 'assign b input'
                    Kernel.getSystemCallHandler().printToScreen("Please enter a value:");
                    String inputString = Kernel.getSystemCallHandler().takeInputFromUser();
                    writeVariableWordToProcessDataMemory(words[1], inputString, currPID);
                } else if (words[2].equals("readFile")) { // 'assign b readFile a
                    if(words.length != 4) throw new InvalidInstructionException("Invalid instruction syntax, readFile statement requires 1 parameter.");
                    contentOfReadFile = Kernel.getSystemCallHandler().readDataFromFileOnDisk(words[3]);
                    writeVariableWordToProcessDataMemory(words[1], contentOfReadFile, currPID);
                    if(currentRunningProcessMemoryImage.hasNextInstruction())
                        Scheduler.incrementTimesProgramCounterShouldBeIncremented();
                    Kernel.incrementInstructionCycle();
                    Kernel.checkNewArrival();
                    System.out.println(Kernel.getMemory());
                } else
                    writeVariableWordToProcessDataMemory(words[1], words[2], currPID);
            }

            case "writeFile" -> {       // 'writeFile x y' where x is filename and y is data
                if (words.length != 3)
                    throw new InvalidInstructionException("Invalid instruction syntax, writeFile statement requires 2 parameters.");
                MemoryWord memWord1 = getVariableWordFromProcessDataMemory(words[1],currPID);
                MemoryWord memWord2 = getVariableWordFromProcessDataMemory(words[2],currPID);
                Kernel.getSystemCallHandler().writeDataToFileOnDisk((String) memWord1.getVariableData(), (String) memWord2.getVariableData());
            }

            /*
             * Trivial case, as 'readFile file_name' does nothing useful
             * on its own. It should be used as a nested instruction to
             * be of any real use.
             */
            case "readFile" -> {        // 'readFile x' where x is filename
                if (words.length != 2)
                    throw new InvalidInstructionException("Invalid instruction syntax, readFile statement requires 1 parameter.");
                MemoryWord word = getVariableWordFromProcessDataMemory(words[1],currPID);
                contentOfReadFile = Kernel.getSystemCallHandler().readDataFromFileOnDisk((String) word.getVariableData());
            }

            case "printFromTo" -> {     // 'printFromTo x y'
                if (words.length < 3)
                    throw new InvalidInstructionException("Invalid instruction syntax, assign statement requires 2 parameters.");
                MemoryWord memWord1 = getVariableWordFromProcessDataMemory(words[1],currPID);
                MemoryWord memWord2 = getVariableWordFromProcessDataMemory(words[2],currPID);
                if ( isInteger( (String) memWord1.getVariableData() ) && isInteger( (String) memWord2.getVariableData() ) ) {
                    int a = Integer.parseInt((String) memWord1.getVariableData());
                    int b = Integer.parseInt((String) memWord2.getVariableData());
                    StringBuilder sb = new StringBuilder();
                    for (int i = a + 1; i < b; i++) {
                        sb.append(i+" ");
                    }
                    Kernel.getSystemCallHandler().printToScreen(sb.toString());
                } else
                    throw new InvalidInstructionException("Invalid instruction syntax, printFromTo statement requires 2 integer numbers.");
            }

            case "semWait" -> { // 'semWait RESOURCE'
                if (words.length != 2)
                    throw new InvalidInstructionException("Invalid instruction syntax, semWait statement requires 1 parameter.");
                switch (words[1]) {
                    case "userInput" -> Kernel.getUserInputMutex().semWait(currentRunningProcessMemoryImage);
                    case "userOutput" -> Kernel.getUserOutputMutex().semWait(currentRunningProcessMemoryImage);
                    case "file" -> Kernel.getFileMutex().semWait(currentRunningProcessMemoryImage);
                    default -> throw new InvalidInstructionException("Invalid resource name: must be userInput, userOutput or file");
                }
            }

            case "semSignal" -> { // 'semSignal RESOURCE'
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
        if(currentRunningProcessMemoryImage.hasNextInstruction())
            Scheduler.incrementTimesProgramCounterShouldBeIncremented();
        Kernel.incrementInstructionCycle();
        Kernel.checkNewArrival();
        System.out.println(Kernel.getMemory());
    }

    private static void writeVariableWordToProcessDataMemory(String varName, String varData, int currProcessID){
        MemoryWord[] memory = Kernel.getMemory().getMemory();
        boolean[] occupied = Kernel.getMemory().getOccupied();
        // Search memory for process
        for(int i = 0; i < memory.length; i++){
            if(occupied[i] && memory[i].getVariableName().equals("PROCESS_ID") && memory[i].getVariableData().equals(currProcessID)) {
                // Search process memory for empty data space
                for(int j = i+6; j < i+9; j++){
                    if(memory[j].getVariableName().equals("---")) {
                        MemoryWord word = new MemoryWord(varName, varData);
                        Kernel.getSystemCallHandler().writeDataToMemory(j, word);
                        return;
                    }
                }
            }
        }
    }

    private static MemoryWord getVariableWordFromProcessDataMemory(String varName, int currProcessID){
        MemoryWord[] memory = Kernel.getMemory().getMemory();
        boolean[] occupied = Kernel.getMemory().getOccupied();
        // Search memory for process
        for(int i = 0; i < memory.length; i++){
            if(occupied[i] && memory[i].getVariableName().equals("PROCESS_ID") && memory[i].getVariableData().equals(currProcessID)){
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