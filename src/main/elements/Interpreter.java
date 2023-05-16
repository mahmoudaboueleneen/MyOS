package main.elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Interpreter {

    public Interpreter(){}

    public void interpretProgram(String filePath){
        System.out.println("Loading program...");

        try {
            File fileObj = new File(filePath);
            Scanner myReader = new Scanner(fileObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                System.out.println(data);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Error: File not found, exit.");
            e.printStackTrace();
            return;
        }

        System.out.println("Executing program...");

    }


}