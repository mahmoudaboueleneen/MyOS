package main.system.elements;

import main.MyOS;
import main.system.exceptions.InvalidInstructionException;
import main.system.elements.Process;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Interpreter {

    public Interpreter(){}

    // Reads full program file and returns String[] of the instructions
    public String[] getInstructionsFromFile(String filePath){
        ArrayList<String> res = new ArrayList<String>();

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

    // Reads file and count lines of code to determine memory size to reserve for process. Called by the Kernel when checking if process can fit in memory
    public int countFileLinesOfCode(String filePath){
        // System.out.println("Counting lines of code...");
        int linesOfCode = 0;

        try {
            File fileObj = new File(filePath);
            Scanner myReader = new Scanner(fileObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                //System.out.println(data);
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

    // Fetch next instruction to be executed by inferring from PC
    public String getNextProcessInstruction(Process p){
        int base = p.getPCB().getLowerMemoryBoundary() + 8;
        int offset = p.getPCB().getProgramCounter();
        return (String) MyOS.getMemory().readMemoryWord(base+offset).getVariableData();
    }

    // Decode & execute instruction
    public void interpret(String instruction) throws InvalidInstructionException {
        // Get all words in the instruction (separated by spaces)
        String[] words = instruction.split(" ");

        String firstWord = words[0];

        switch (firstWord){
            case "print":

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

                break;
            case "semSignal":

                break;
            default:
                throw new InvalidInstructionException();
        }

    }



}