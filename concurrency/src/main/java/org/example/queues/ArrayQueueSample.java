package org.example.queues;

public class ArrayQueueSample {
    private final Queue<Integer> queue = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();
    private final int maxSize;

    public ArrayQueueSample(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive: " + size);
        }
        this.maxSize = size;
    }

    public void add(int value) {
        lock.lock();
        try{
            while(queue.size() >= maxSize) {
                notFull.await();
            }
            queue.add(value);
            notEmpty.signal();
        }
        catch(InterruptedException e){
            System.out.println("Thread interrupted while waiting to add to the queue.");
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting to add to the queue.");
        } finally {
            lock.unlock();
        }
    }

    public int remove() {
        lock.lock();
        try{
            while(queue.isEmpty()){
                notEmpty.await();
            }
            int value = queue.remove();
            notFull.signal();
            return value;
        }
        catch(InterruptedException e){
            System.out.println("Thread interrupted while waiting to remove from the queue.");
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting to remove from the queue.");
        } finally {
            lock.unlock();
        }
    }

}

