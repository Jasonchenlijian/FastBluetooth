package com.clj.fastbluetooth.callback;

import com.clj.fastbluetooth.exception.BluetoothException;

public interface BluetoothWriteCallback {

    void onWriteError(BluetoothException e);

    void onWriteSuccess(byte[] data);
}
