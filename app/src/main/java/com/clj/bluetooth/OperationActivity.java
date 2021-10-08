package com.clj.bluetooth;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.clj.fastbluetooth.FastBluetooth;
import com.clj.fastbluetooth.callback.BluetoothReadCallback;
import com.clj.fastbluetooth.callback.BluetoothWriteCallback;
import com.clj.fastbluetooth.exception.BluetoothException;
import com.clj.fastbluetooth.util.ConvertUtils;

import java.nio.charset.Charset;

public class OperationActivity extends AppCompatActivity {

    private static final String TAG = OperationActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();
    }

    private void initView() {
        findViewById(R.id.btn_write).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastBluetooth.getInstance().write(ConvertUtils.string2Bytes("abcdefg"), new BluetoothWriteCallback() {
                    @Override
                    public void onWriteError(BluetoothException e) {
                        Log.e(TAG, "onWriteError");
                    }

                    @Override
                    public void onWriteSuccess(byte[] data) {
                        Log.e(TAG, "onWriteSuccess");
                    }
                });
            }
        });

        findViewById(R.id.btn_listen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastBluetooth.getInstance().openDataRead(new BluetoothReadCallback() {
                    @Override
                    public void onReadError(BluetoothException e) {
                        Log.e(TAG, "onReadError");
                    }

                    @Override
                    public void onDataReceive(byte[] data) {
                        Log.e(TAG, "onDataReceive: " + new String(data, Charset.defaultCharset()));
                    }
                });
            }
        });

        findViewById(R.id.btn_stop_listen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastBluetooth.getInstance().stopDataRead();
            }
        });
    }
}
