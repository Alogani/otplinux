package com.alogani.otplinux;


import com.alogani.otplinux.scenes.CounterScene;
import javafx.application.Platform;
import javafx.scene.control.Labeled;

import java.util.ArrayList;
import java.util.List;

import static com.alogani.otplinux.Main.timeProvider;

/*
This class instantiate a thread which do these things :
- get a token from a CounterScene class
- set automaticaly the countdown value on a label/control
- update automaticaly the otp password on a given label
see also CounterScene
 */
public class CountdownThread extends Thread {
    public CounterScene scene;

    // used to to terminate all threads
    final private static List<CountdownThread> runningThreads = new ArrayList<>();

    // Flags to control thread
    private boolean terminateFlag = false;
    private boolean runFlag;

    private boolean isRunning;


    public CountdownThread(CounterScene window, boolean runFlag) {
        super();
        runningThreads.add(this);
        this.scene = window;
        this.runFlag = runFlag;
        this.start();
    }

// COMMANDING THE THREAD
    public void pauseAndWait() {
        runFlag = false;
        // waiting permits to avoid conflict
        while (isRunning)
            customSleep(10);
    }

    public void unPause() {
        runFlag = true;
    }

    static void terminateAll() {
        for (CountdownThread thread : runningThreads)
            thread.terminateFlag = true;
    }

// EXECUTION CODE
    private void customSleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ignored) {
            runFlag = false;
        }
    }

    private void updateLabel(Labeled labeled, String text) {
        Platform.runLater(() -> labeled.setText(text));
    }

    @Override
    public void run() {
        long counter;
        while (!terminateFlag) {
            while (runFlag && !terminateFlag) {
                while (runFlag && !terminateFlag) {
                    isRunning = true;
                    /* RUNNING CODE */
                    try {
                        counter = scene.getCurrentToken().timeBeforeReset();
                        updateLabel(scene.getCounterLabel(), String.valueOf(counter));
                        if (counter == scene.getCurrentToken().intervalTOTP_OR_counterHOTP)
                            updateLabel(scene.getPasswordLabel(), scene.getCurrentToken().getOTP()); // update OTP here only when counter resets to avoid some Platform.runlater
                    } catch (NullPointerException ignored) {
                        // Error here should mean that no token is selected, thus thread must go idle
                        runFlag = false;
                    }

                    isRunning = false;
                    // Sleep exactly the right amount of time, if selection changed, must be handled by the events
                    customSleep((timeProvider.currentTimeMillis() / 1000 + 1) * 1000 - timeProvider.currentTimeMillis());
                }
            }
            customSleep(50); // stay idle
        }
    }
}
