package test;

import main.MyOS;

public class Test {
    private static MyOS testOS;

    public static void main(String[] args){
        testOS = new MyOS();

        // Run first program
        testOS.runProgram("src/program_files/Program_1.txt");

    }

}
