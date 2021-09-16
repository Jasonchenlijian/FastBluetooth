package com.clj.fastbluetooth.exception;

import java.io.Serializable;


public class BluetoothException implements Serializable {

    private static final long serialVersionUID = 8004414918500865564L;

    public static final int ERROR_DISCONNECTION = 100;
    public static final int ERROR_IO = 101;

    private int code;
    private String description;

    public BluetoothException(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public BluetoothException setCode(int code) {
        this.code = code;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public BluetoothException setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        return "BleException { " +
               "code=" + code +
               ", description='" + description + '\'' +
               '}';
    }
}
