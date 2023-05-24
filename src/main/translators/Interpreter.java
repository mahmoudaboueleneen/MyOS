package main.translators;

import main.elements.MemoryManager;
import main.elements.MemoryWord;
import main.elements.ProcessMemoryImage;
import main.exceptions.VariableAssignmentException;
import main.kernel.Kernel;
import main.exceptions.InvalidInstructionException;
import main.kernel.Scheduler;
import main.kernel.SystemCallHandler;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public abstract class Interpreter {

    // Each of these arrays has one cell for each process, holds
    // return value of last executed input/readFile instructions
    // respectively.
    private static final String[] inputReturnedContents = new String[3];
    private static final String[] readFileReturnedContents = new String[3];

    public static String getNextProcessInstruction(ProcessMemoryImage p){
        int base = p.getPCB().getLowerMemoryBoundary() + 9;
        int offset = p.getPCB().getProgramCounter();
        return (String) MemoryManager.readMemoryWord(base + offset).getVariableData();
    }

    public static void interpretAndIncrementInstructionCycle(String instruction, ProcessMemoryImage currentRunningProcessMemoryImage) throws InvalidInstructionException, VariableAssignmentException {
        System.out.println("INSTRUCTION TO BE EXECUTED: '" + instruction + "'\n");

        interpret(instruction,currentRunningProcessMemoryImage);

        System.out.println("Instruction interpreted.\n");

        System.out.println("MEMORY AFTER INTERPRETING & EXECUTING INSTR.:");

        MemoryManager.printMemory();

        System.out.println("Incrementing instruction cycle...");

        Scheduler.incrementInstructionCycle();
    }


    private static void interpret(String instruction, ProcessMemoryImage p) throws InvalidInstructionException, VariableAssignmentException {
        String[] words = instruction.split(" ");

        String firstWord = words[0];

        switch (firstWord) {
            case "print" -> {
                if (words.length != 2)
                    throw new InvalidInstructionException("Invalid instruction syntax, print statement requires 1 parameter");
                MemoryWord word = getVariable(words[1],p);
                String x = (String) word.getVariableData();
                SystemCallHandler.printToScreen(x);
            }

            case "assign" -> {
                if (words.length < 3)
                    throw new InvalidInstructionException("Invalid instruction syntax, assign statement requires 2 parameters at least.");

                if (words[2].equals("input")) {
                    if(words.length != 3)
                       throw new InvalidInstructionException("Invalid instruction syntax, should be 'assign VAR_NAME input'");
                    MemoryManager.initializeVariableInMemory(words[1], inputReturnedContents[p.getPCB().getProcessID()], p);
                }
                else if (words[2].equals("readFile")) {
                    if(words.length != 4)
                        throw new InvalidInstructionException("Invalid instruction syntax, readFile statement requires 1 parameter.");
                    MemoryManager.initializeVariableInMemory(words[1], readFileReturnedContents[p.getPCB().getProcessID()], p);
                }
                else
                    MemoryManager.initializeVariableInMemory(words[1], words[2], p);
            }

            case "input" -> {
                SystemCallHandler.printToScreen("Please enter a value:");
                inputReturnedContents[p.getPCB().getProcessID()] = SystemCallHandler.takeInputFromUser();
            }

            case "writeFile" -> {
                if (words.length != 3)
                    throw new InvalidInstructionException("Invalid instruction syntax, writeFile statement requires 2 parameters.");
                MemoryWord memWord1 = getVariable(words[1],p);
                MemoryWord memWord2 = getVariable(words[2],p);

                SystemCallHandler.writeDataToFileOnDisk((String) memWord1.getVariableData(), (String) memWord2.getVariableData());
            }

            case "readFile" -> {
                if (words.length != 2)
                    throw new InvalidInstructionException("Invalid instruction syntax, readFile statement requires 1 parameter.");
                MemoryWord word = getVariable(words[1],p);
                readFileReturnedContents[p.getPCB().getProcessID()] = SystemCallHandler.readDataFromFileOnDisk((String) word.getVariableData());
            }

            case "printFromTo" -> {
                if (words.length < 3)
                    throw new InvalidInstructionException("Invalid instruction syntax, assign statement requires 2 parameters.");
                MemoryWord memWord1 = getVariable(words[1],p);
                MemoryWord memWord2 = getVariable(words[2],p);
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
                    case "userInput" -> Kernel.getUserInputMutex().semWait(p);
                    case "userOutput" -> Kernel.getUserOutputMutex().semWait(p);
                    case "file" -> Kernel.getFileMutex().semWait(p);
                    default -> throw new InvalidInstructionException("Invalid resource name: must be userInput, userOutput or file");
                }
            }

            case "semSignal" -> {
                if (words.length != 2)
                    throw new InvalidInstructionException("Invalid instruction syntax, semWait statement requires 1 parameter.");
                switch (words[1]) {
                    case "userInput" -> Kernel.getUserInputMutex().semSignal(p);
                    case "userOutput" -> Kernel.getUserOutputMutex().semSignal(p);
                    case "file" -> Kernel.getFileMutex().semSignal(p);
                    default -> throw new InvalidInstructionException("Invalid resource name: must be userInput, userOutput or file");
                }
            }

            default -> throw new InvalidInstructionException();
        }
    }

    private static MemoryWord getVariable(String varName, ProcessMemoryImage p){
        int processID = p.getPCB().getProcessID();
        return MemoryManager.getMemoryWordByName(varName,processID);
    }


    public static String[] getInstructionsFromFile(String filePath){
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


    private static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}