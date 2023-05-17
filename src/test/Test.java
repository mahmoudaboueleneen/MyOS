package test;

import main.MyOS;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class Test {
    private static MyOS testOS;
    private static Scanner inp = new Scanner(System.in);
    private static int[] arrival;
    private static String[] locations;

    static class Counter implements Runnable {
        private int count = 0;

        @Override
        public void run() {
            int i = 0;
            while (true) {
                synchronized (this) {
                    if(count == arrival[i]){
                        System.out.println("Process arrived: " + locations[i]);
                        testOS.runProgram(locations[i]);
                        i++;
                    }
                    if(i == arrival.length) {
                        System.out.println("All processes successfully created! \n");
                        System.out.println(MyOS.getMemory());
                        return;  // Here we finally exit from the thread
                    }
                    count++;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public synchronized int getCount() {
            return count;
        }
    }

    public static void main(String[] args){
        int tq;
        int maxProccessIndex = 0;
        float avgWait = 0;
        float avgTT = 0;

        System.out.print("Enter the round-robin time slice: \n");
        tq = inp.nextInt();

        // We anticipate the arrival of our 3 processes only.
        arrival = new int[3];

        locations = new String[3];
        locations[0] = "src/program_files/Program_1.txt";
        locations[1] = "src/program_files/Program_2.txt";
        locations[2] = "src/program_files/Program_3.txt";

        // Begin testing by creating an instance.
        testOS = new MyOS(tq);

        // Arrival time will be in seconds
        System.out.println("Enter the arrival time of P1, P2 and P3 in order respectively (integer only)");
        for(int i = 0; i < 3; i++)
            arrival[i] = (int) (inp.nextInt());

        System.out.println("Starting timer..");

        // Warm up Java VM first.
        // Runs should be enough to run for 2-10 seconds.
        for(int i=-10000; i<10000 ;i++) {}

        // Sort arrival times and arrived processes in order.
        for (int i = 0; i < arrival.length - 1; i++) {
            for (int j = 0; j < arrival.length - i - 1; j++) {
                if (arrival[j] > arrival[j + 1]) {
                    int temp = arrival[j];
                    arrival[j] = arrival[j + 1];
                    arrival[j + 1] = temp;

                    String temp2 = locations[j];
                    locations[j] = locations[j + 1];
                    locations[j + 1] = temp2;
                }
            }
        }

        // Beginning timer, incrementing timer until the first process scheduled to arrive arrives.
        Counter counter = new Counter();
        Thread thread = new Thread(counter);
        thread.start();

        //System.out.println("Count after 5 seconds: " + counter.getCount());
        //System.currentTimeMillis()
    }

}
