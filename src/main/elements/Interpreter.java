package main.elements;

import main.kernel.Kernel;
import main.exceptions.InvalidInstructionException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Interpreter {

    public Interpreter(){}

    // Reads full program file and returns String[] of the instructions
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

    // Fetch next instruction to be executed
    public synchronized String getNextProcessInstruction(ProcessMemoryImage p){
        int base = p.getPCB().getLowerMemoryBoundary() + 8;
        int offset = p.getPCB().getProgramCounter();
        return (String) Kernel.getMemory().readMemoryWord(base+offset).getVariableData();
    }

    // Decode & execute instruction
    public synchronized void interpret(String instruction) throws InvalidInstructionException {
        // Get all words in the instruction (separated by spaces)
        String[] words = instruction.split(" ");

        String firstWord = words[0];

        switch (firstWord){
            case "print":
                //MyOS.getSystemCall().printToScreen();
                break;
            case "assign":

                break;
            case "writeFile":

                break;
            case "readFile":

                break;
            case "printFromTo":

                break;
            case "semWait":
                // mutex
                break;
            case "semSignal":
                // mutex
                break;
            default:
                throw new InvalidInstructionException();
        }

    }



}