package com.clj.fastbluetooth;


import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;

import com.clj.fastbluetooth.callback.BluetoothReadCallback;
import com.clj.fastbluetooth.callback.BluetoothScanCallback;
import com.clj.fastbluetooth.callback.BluetoothWriteCallback;
import com.clj.fastbluetooth.core.BluetoothController;
import com.clj.fastbluetooth.utils.BluetoothLog;

public class FastBluetooth {

    private Context context;
    private BluetoothAdapter bluetoothAdapter;

    public static FastBluetooth getInstance() {
        return FastBluetoothHolder.sFastBluetooth;
    }

    private static class FastBluetoothHolder {
        private static final FastBluetooth sFastBluetooth = new FastBluetooth();
    }

    public void init(Application app) {
        if (context == null && app != null) {
            context = app;
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
    }

    public Context getContext() {
        return context;
    }

    public boolean isSupportBluetooth() {
        return context.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public boolean isBlueEnable() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public FastBluetooth enableLog(boolean enable) {
        BluetoothLog.isPrint = enable;
        return this;
    }

    public void fastScan(BluetoothScanCallback callback) {
        scan(null, null, -1, false, callback);
    }

    public void scanWithParams(String[] names, String mac,
                               BluetoothScanCallback callback) {
        scan(names, mac, -1, false, callback);
    }

    public void scanWithParams(String[] names, String mac, long timeout,
                               BluetoothScanCallback callback) {
        scan(names, mac, timeout, false, callback);
    }

    public void scanAndConnect(String[] names, String mac,
                               BluetoothScanCallback callback) {
        scan(names, mac, -1, true, callback);
    }

    public void scanAndConnect(String[] names, String mac, long timeout,
                               BluetoothScanCallback callback) {
        scan(names, mac, timeout, true, callback);
    }

    private void scan(String[] names, String mac, long timeout,
                      boolean autoConnect, BluetoothScanCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BluetoothScanCallback can not be Null!");
        }

        if (!isBlueEnable()) {
            BluetoothLog.e("Bluetooth not enable!");
            callback.onScanStarted(false);
            return;
        }

        BluetoothController.getInstance().scan(names, mac, timeout, autoConnect, callback);
    }

    public void write(byte[] command, BluetoothWriteCallback callback) {
        BluetoothController.getInstance().write(command, callback);
    }

    public void openDataRead(BluetoothReadCallback callback) {
        BluetoothController.getInstance().read(callback);
    }

    public void stopDataRead(){
        BluetoothController.getInstance().stopDataRead();
    }

    public void destroy() {

    }


}
