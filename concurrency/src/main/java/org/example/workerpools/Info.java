package org.example.workerpools;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class Info implements Delayed {
    private String taskName;
    private int taskId;
    private long epochSeconds;

    public Info(String taskName, int taskId, long milliseconds) {
        this.taskName = taskName;
        this.taskId = taskId;
        this.epochSeconds = milliseconds;
    }

    public String getTaskName() {
        return taskName;
    }

    public int getTaskId() {
        return taskId;
    }

    public long getEpochSeconds() {
        return epochSeconds;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long remainingMillis = epochSeconds * 1000 - System.currentTimeMillis();
        return unit.convert(remainingMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        return Long.compare(this.getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
    }
}
