package org.example.workerpools;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class WorkerPool {

    private final List<Thread> threads = new ArrayList<>();

    public WorkerPool(BlockingQueue<Info> queue, int poolSize) {
        for (int i = 1; i <= poolSize; i++) {
            Thread thread = new Thread(new Worker(queue), "Worker-" + i);
            threads.add(thread);
            thread.start();
        }
    }

    public void shutdown() {
        for (Thread thread : threads) {
            thread.interrupt();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
