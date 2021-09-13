package com.clj.fastbluetooth.scan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import com.clj.fastbluetooth.FastBluetooth;
import com.clj.fastbluetooth.callback.BluetoothScanCallback;
import com.clj.fastbluetooth.utils.BluetoothLog;

import java.util.Iterator;
import java.util.Set;

public class BluetoothScanner {

    private boolean flag_isSearching = false;                   // 是否处于搜索状态

    public static BluetoothScanner getInstance() {
        return BluetoothScannerHolder.sBluetoothScanner;
    }

    private static class BluetoothScannerHolder {
        private static final BluetoothScanner sBluetoothScanner = new BluetoothScanner();
    }

    public synchronized void scan(String[] names, String[] macs, long timeout,
                                  boolean autoConnect, final BluetoothScanCallback callback) {

        if (flag_isSearching) {
            BluetoothLog.w("scan action already exists, complete the previous scan action first");
            if (callback != null) {
                callback.onScanStarted(false);
            }
            return;
        }

        flag_isSearching = true;

        Set<BluetoothDevice> boundedDevices = FastBluetooth.getInstance().getBluetoothAdapter().getBondedDevices();
        Iterator<BluetoothDevice> it = boundedDevices.iterator();
        while (it.hasNext()) {
            BluetoothDevice device = it.next();
            if (TextUtils.equals(device.getName(), mDeviceName)) {
                flag_isSearching = true;
                mDevice = device;
                sendMsg(BT_FOUND, null);
                break;
            }
        }

        // 注册蓝牙扫描过程广播监听器
        registerScanReceiver();


    }

    private void registerScanReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        FastBluetooth.getInstance().getContext().getApplicationContext().registerReceiver(blueScanReceiver, filter);
    }

    private void unregisterScanReceiver() {
        try {
            FastBluetooth.getInstance().getContext().getApplicationContext().unregisterReceiver(blueScanReceiver);
        } catch (IllegalArgumentException e) {
            BluetoothLog.e("unregisterScanReceiver catch: " + e.getMessage());
        }
    }

    private final BroadcastReceiver blueScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // 开始扫描
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                BluetoothLog.w("ACTION_DISCOVERY_STARTED");
            }

            // 发现设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    String name = device.getName();
                    if (!TextUtils.isEmpty(name)) {
                        LogUtils.w(TAG, "发现设备：" + name);
                        if ((name.equalsIgnoreCase(mDeviceName))) {
                            cancelSearch();
                            mDevice = device;
                            sendMsg(BT_FOUND, null);
                        }
                    }
                }
            }

            // 扫描结束
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                BluetoothLog.w("ACTION_DISCOVERY_FINISHED");

                flag_isSearching = false;
                unregisterScanReceiver();
            }
        }
    };


}
