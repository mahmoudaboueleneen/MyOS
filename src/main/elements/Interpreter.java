package main.elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Interpreter {

    public Interpreter(){}

    // Read file and count lines of code to determine memory size to reserve for process
    // Called by the Kernel when creating process
    public int countFileLinesOfCode(String filePath){
        System.out.println("Counting lines of code...");
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

    //public

}