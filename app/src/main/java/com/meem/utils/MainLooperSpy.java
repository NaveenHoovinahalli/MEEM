package com.meem.utils;

import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Log;

import java.lang.reflect.Field;

/**
 * Considering the Android accessory framework's dependency on life-cycle model of Android and the totally asynchronous nature of cable
 * connect and disconnect events, if somebody wants to see exactly what all are happening on the main thread, here is a class that uses
 * reflection to see what all are going on in the main-thread's message queue
 * <p/>
 * Remember: all life-cycle events are posted by Android framework classes to the main thread's message queue as simple messages.
 * <p/>
 * Prerequisites: To use this class, you need to have a thorough understanding of Java and Android concepts of threading, message queues,
 * looper and handlers. Source code of ActivityThread.java of ASOP is a must to understand the message codes in the dumped messages:
 * <p/>
 * https://android.googlesource.com/platform/frameworks/base.git/+/master/core/ java/android/app/ActivityThread.java
 * <p/>
 * Courtesy: https://corner.squareup.com/2013/12/android-main-thread-2.html
 * <p/>
 * WARNING: DO NOT USE IN PRODUCTION CODE
 *
 * @author Arun T A
 */

public class MainLooperSpy {
    private static String tag = "MainLooperSpy";

    private final Field messagesField;
    private final Field nextField;
    private final MessageQueue mainMessageQueue;

    public MainLooperSpy() {
        try {
            Field queueField = Looper.class.getDeclaredField("mQueue");
            queueField.setAccessible(true);
            messagesField = MessageQueue.class.getDeclaredField("mMessages");
            messagesField.setAccessible(true);
            nextField = Message.class.getDeclaredField("next");
            nextField.setAccessible(true);
            Looper mainLooper = Looper.getMainLooper();
            mainMessageQueue = (MessageQueue) queueField.get(mainLooper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void dumpQueue() {
        try {
            Message nextMessage = (Message) messagesField.get(mainMessageQueue);
            Log.d(tag, "Begin dumping queue");
            dumpMessages(nextMessage);
            Log.d(tag, "End dumping queue");
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void dumpMessages(Message message) throws IllegalAccessException {
        if (message != null) {
            Log.d(tag, message.toString());
            Message next = (Message) nextField.get(message);
            dumpMessages(next);
        }
    }
}
