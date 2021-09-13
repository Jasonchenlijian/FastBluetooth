package com.clj.fastbluetooth.callback;

import android.bluetooth.BluetoothDevice;

public interface BluetoothScanCallback {

    void onScanStarted(boolean success);

    void onScanning(BluetoothDevice bluetoothDevice);

}
