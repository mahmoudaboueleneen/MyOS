package main.kernel;

import main.elements.Memory;
import main.elements.MemoryWord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public abstract class SystemCallHandler {

    public static String readDataFromFileOnDisk(String fileName){
        StringBuilder sb = new StringBuilder();
        try {
            File myObj = new File("src/generated_files/" + fileName);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                sb.append(data);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Error: Reading from file failed, file not found.");
            //e.printStackTrace();
        }
        return sb.toString();
    }

    public static void writeDataToFileOnDisk(String fileName, String data){
        try {
            FileWriter myWriter = new FileWriter("src/generated_files/" + fileName);
            myWriter.write(data);
            myWriter.close();
            System.out.println("Generated file src/generated_files/" + fileName);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static void printToScreen(String data){
        System.out.println(data);
        System.out.println();
    }

    public static String takeInputFromUser(){
        Scanner sc = new Scanner(System.in);
        return sc.nextLine();
    }

    public static MemoryWord readDataFromMemory(int address){
        return Memory.getMemoryArray()[address];
    }

    public static void writeDataToMemory(int address, MemoryWord word){
        Memory.writeMemoryWord(address, word);
    }


}
