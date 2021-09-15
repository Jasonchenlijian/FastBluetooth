package com.clj.fastbluetooth.callback;

import android.bluetooth.BluetoothDevice;

public interface BluetoothScanCallback {

    void onBlueNotEnable();

    void onScanStarted(boolean success);

    void onScanning(BluetoothDevice bluetoothDevice);

    void onScanFinished(boolean findMatchDeviceAndConnect);

    void onStartConnect();

    void onConnectError();

    void onConnectSuc();

}
