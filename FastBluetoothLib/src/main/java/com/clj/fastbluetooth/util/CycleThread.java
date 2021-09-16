package com.clj.fastbluetooth.util;

public class CycleThread extends Thread {

    private volatile boolean isCycleRun = false;

    public CycleThread() {

    }

    public void begin() {

    }

    public void cycle() {

    }

    public void end() {

    }

    public void cancel() {
        if (this.isCycleRun) {
            this.isCycleRun = false;
            this.interrupt();
        }
    }

    @Override
    public synchronized void start() {
        this.isCycleRun = true;
        super.start();
    }

    @Override
    public final void run() {
        this.begin();

        while (this.isCycleRun) {
            try {
                this.cycle();
            } catch (Exception var2) {
                var2.printStackTrace();
            }
        }

        this.end();
    }

}
