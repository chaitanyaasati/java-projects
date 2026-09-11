package org.example.personalpool;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Worker worker = new Worker();
        Thread thread = new Thread(worker, "api-worker");
        thread.start();

        // Tasks can be submitted individually at any time while the worker is running.
        worker.submit(new Task());
        worker.submit(new Task());
        worker.submit(new Task());

        // Stop accepting work and exit after all submitted tasks are processed.
        worker.shutdown();
        thread.join();

        System.out.println("Processing Completed");
    }
}
