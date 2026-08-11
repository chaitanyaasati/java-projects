package org.example.queues;

public class LinkedListQueue<T> {
    private static class Node<T> {
        T item;
        Node<T> next;
        Node(T item) { this.item = item; }
    }

    private final int capacity;
    private final AtomicInteger count = new AtomicInteger();

    // head.item is always null; head.next is the actual first element (if any).
    private Node<T> head;
    // last is the actual last node; last.next is always null.
    private Node<T> last;

    private final ReentrantLock putLock = new ReentrantLock();
    private final Condition notFull = putLock.newCondition();

    private final ReentrantLock takeLock = new ReentrantLock();
    private final Condition notEmpty = takeLock.newCondition();

    public LinkedListQueue(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive: " + size);
        }
        this.capacity = size;
        this.head = new Node<>(null);
        this.last = this.head;
    }

    private void enqueue(Node<T> node) {
        last = last.next = node;
    }

    private T dequeue() {
        Node<T> first = head.next;
        head = first;
        T x = first.item;
        first.item = null;
        return x;
    }

    private void signalNotEmpty1() {
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    private void signalNotFull1() {
        putLock.lock();
        try {
            notFull.signal();
        } finally {
            putLock.unlock();
        }
    }

    public void add(T data) {
        int c;
        Node<T> node = new Node<>(data);
        putLock.lock();
        try {
            while (count.get() >= capacity) {
                notFull.await();
            }
            enqueue(node);
            c = count.getAndIncrement();
            if (c + 1 < capacity) {
                notFull.signal();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting to add to the queue.");
        } finally {
            putLock.unlock();
        }
        // Queue went from empty to non-empty: wake a waiting consumer.
        if (c == 0) {
            signalNotEmpty1();
        }
    }

    public T remove() {
        T x;
        int c;
        takeLock.lock();
        try {
            while (count.get() == 0) {
                notEmpty.await();
            }
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1) {
                notEmpty.signal();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting to remove from the queue.");
        } finally {
            takeLock.unlock();
        }
        // Queue went from full to non-full: wake a waiting producer.
        if (c == capacity) {
            signalNotFull1();
        }
        return x;
    }
}

