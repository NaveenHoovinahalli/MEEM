package com.meem.utils;

/**
 * Simple ring buffer implementation (for logs)
 *
 * @author Arun T A
 */

public class LogRingBuffer {
    public static final int CAPACITY = 500;
    private String[] buffer = new String[CAPACITY];
    private int position = 0;
    private boolean full = false;

    public synchronized void append(String message) {
        buffer[position] = message;
        position = (position + 1) % CAPACITY;
        if (position == 0) {
            full = true;
        }
    }

    public synchronized int size() {
        return full ? CAPACITY : position;
    }

    public synchronized String get(int i) {
        return buffer[(position - i - 1 + CAPACITY) % CAPACITY];
    }
}