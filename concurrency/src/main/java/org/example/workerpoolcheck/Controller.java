package org.example.workerpoolcheck;

public class Controller {
    public static void main(String[] args) {
        Worker worker = new Worker();
        Thread thread = new Thread(worker);
        thread.start();

        worker.submit(new Task(3));
        worker.submit(new Task(4));
        worker.submit(new Task(5));

        worker.shutdown();
    }
}
