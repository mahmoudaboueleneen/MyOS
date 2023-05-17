package test;

import main.MyOS;

public class Test2 {
    private static MyOS testOS;

    public static void main(String[] args){
        testOS = new MyOS(2);

        System.out.println(MyOS.getMemory());
    }
}
