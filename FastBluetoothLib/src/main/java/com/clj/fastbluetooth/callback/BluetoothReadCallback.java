package com.clj.fastbluetooth.callback;


import com.clj.fastbluetooth.exception.BluetoothException;

public interface BluetoothReadCallback {

    void onReadError(BluetoothException e);

    void onDataReceive(byte[] data);
}
