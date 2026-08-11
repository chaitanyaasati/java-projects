package org.example;

import java.util.concurrent.BlockingQueue;

public class Worker implements Runnable {

    private final BlockingQueue<Info> queue;

    public Worker(BlockingQueue<Info> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Info info = queue.take(); // blocks until delay <= 0; re-evaluated if an earlier task is added
                System.out.println("Processed " + info.getTaskId() + " , " + info.getTaskName());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
