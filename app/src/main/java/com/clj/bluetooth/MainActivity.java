package com.clj.bluetooth;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.clj.fastbluetooth.FastBluetooth;
import com.clj.fastbluetooth.callback.BluetoothReadCallback;
import com.clj.fastbluetooth.callback.BluetoothScanCallback;
import com.clj.fastbluetooth.callback.BluetoothWriteCallback;
import com.clj.fastbluetooth.exception.BluetoothException;
import com.clj.fastbluetooth.util.ConvertUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FastBluetooth.getInstance().init(getApplication());

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkPermissions();
            }
        }, 2000);

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

        findViewById(R.id.btn_read).setOnClickListener(new View.OnClickListener() {
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

        findViewById(R.id.btn_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastBluetooth.getInstance().stopDataRead();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void test() {
        FastBluetooth.getInstance().scanAndConnect(new String[]{"BRT2021040001"}, null, -1, new BluetoothScanCallback() {
            @Override
            public void onBlueNotEnable() {
                Log.w(TAG, "onBlueNotEnable");
            }

            @Override
            public void onScanStarted(boolean success) {
                Log.w(TAG, "onScanStarted: " + success);
            }

            @Override
            public void onScanning(BluetoothDevice bluetoothDevice) {
                Log.w(TAG, "onScanning: " + bluetoothDevice.getName() + "  " + bluetoothDevice.getAddress());
            }

            @Override
            public void onScanFinished(boolean findMatchDeviceAndConnect) {
                Log.w(TAG, "onScanFinished: " + findMatchDeviceAndConnect);
            }

            @Override
            public void onStartConnect() {
                Log.w(TAG, "onStartConnect");
            }

            @Override
            public void onConnectError() {
                Log.w(TAG, "onConnectError");
            }

            @Override
            public void onConnectSuc() {
                Log.w(TAG, "onConnectSuc");
            }
        });
    }

    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.notifyTitle)
                            .setMessage(R.string.gpsNotifyMsg)
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton(R.string.setting,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })

                            .setCancelable(false)
                            .show();
                } else {
                    test();
                }
                break;
        }
    }

    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

}
