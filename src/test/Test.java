package test;

import main.MyOS;

public class Test {
    private static MyOS testOS;

    public static void runProvidedPrograms(){
        // Run first program
        testOS.getInterpreter().interpretProgram("src/program_files/Program_1.txt");


    }

    public static void main(String[] args){
        testOS = new MyOS();
        runProvidedPrograms();

    }

}
