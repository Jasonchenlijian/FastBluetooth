package com.clj.fastbluetooth.scan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.clj.fastbluetooth.FastBluetooth;
import com.clj.fastbluetooth.callback.BluetoothScanCallback;
import com.clj.fastbluetooth.utils.BluetoothLog;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BluetoothScanner {

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private boolean flag_isSearching = false;                   // 是否处于搜索状态

    private BluetoothScanCallback bluetoothScanCallback;
    private String[] names;
    private String mac;
    private long timeout;
    private boolean autoConnect;
    private boolean flag_containScanParams;

    // 搜索到的蓝牙设备全部存放在集合中
    // 队列是一种特殊的线性表，它只允许在表的前端进行删除操作，而在表的后端进行插入操作。
    // LinkedList类实现了Queue接口，因此我们可以把LinkedList当成Queue来用
    // 队列是先进先出，栈是后进先出
    private final Queue<BluetoothDevice> searchedDeviceQueue = new ConcurrentLinkedQueue<>();

    // 临时存储在集合中的蓝牙设备的最大值
    private static final int MAX_SIZE = 1000;

    // 搜索到并筛选出来的蓝牙设备
    private final List<BluetoothDevice> collectionDeviceList = new ArrayList<>();

    // 设备筛选线程标记
    private boolean flag_collection = false;

    private static final int BT_START_FAIL = 100;                  // 开启扫描失败
    private static final int BT_START_SUC = 101;                  // 开启扫描失败
    private static final int BT_SEARCHED = 102;                    // 搜索完成标记
    private static final int MSG_START_CONNECT = 104;              // 开始连接
    private static final int MSG_CONNECT_OK = 105;                 // 连接完成
    private static final int MSG_SOCKET_CONNECT_ERROR = 106;       // socket连接失败
    private static final int STATE_BLUE_OPEN_FAIL = 107;           // 蓝牙打开失败

    private ScanThread mScanThread = null;
    private CollectionThread mCollectionThread = null;
    private ConnectThread mConnectThread = null;

    private BluetoothSocket mSocket = null;

    private BluetoothDevice mTargetDevice = null;


    public static BluetoothScanner getInstance() {
        return BluetoothScannerHolder.sBluetoothScanner;
    }

    private static class BluetoothScannerHolder {
        private static final BluetoothScanner sBluetoothScanner = new BluetoothScanner();
    }

    public synchronized void scan(String[] names, String mac, long timeout,
                                  boolean autoConnect, final BluetoothScanCallback callback) {

        if (flag_isSearching) {
            BluetoothLog.w("scan action already exists, complete the previous scan action first");
            if (callback != null) {
                callback.onScanStarted(false);
            }
            return;
        }

        if ((names == null || names.length < 1) && (mac == null)) {
            flag_containScanParams = false;
        } else {
            flag_containScanParams = true;
        }

        // 如果设置了自动连接，那必须要有搜索条件
        if (autoConnect && !flag_containScanParams) {
            BluetoothLog.w("names or macs search condition must not be empty, if autoConnect is used");
            if (callback != null) {
                callback.onScanStarted(false);
            }
            return;
        }

        flag_isSearching = true;
        flag_collection = true;

        this.names = names;
        this.mac = mac;
        this.timeout = timeout;
        this.autoConnect = autoConnect;
        this.bluetoothScanCallback = callback;

        searchedDeviceQueue.clear();
        collectionDeviceList.clear();

        // 注册蓝牙扫描过程广播监听器
        registerScanReceiver();

        // 开启扫描线程
        if (mScanThread != null && mScanThread.isAlive()) {
            mScanThread.interrupt();
        }
        mScanThread = null;
        mScanThread = new ScanThread();
        mScanThread.start();

        if (mCollectionThread != null && mCollectionThread.isAlive()) {
            mCollectionThread.interrupt();
        }
        mCollectionThread = null;
        mCollectionThread = new CollectionThread();
        mCollectionThread.start();
    }

    /**
     * 中断搜索
     */
    private void cancelScan() {
        // 注销蓝牙广播接收器
        unregisterScanReceiver();

        // 标记搜索状态
        flag_isSearching = false;
        flag_collection = false;

        // 关闭搜索
        if (FastBluetooth.getInstance().getBluetoothAdapter() != null
                && FastBluetooth.getInstance().getBluetoothAdapter().isDiscovering()) {
            FastBluetooth.getInstance().getBluetoothAdapter().cancelDiscovery();
        }
    }

    /**
     * 连接
     */
    private void connect() {
        if (mConnectThread != null && mConnectThread.isAlive()) {
            mConnectThread.interrupt();
        }
        mConnectThread = null;
        mConnectThread = new ConnectThread(mTargetDevice);
        mConnectThread.start();
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
                    if (searchedDeviceQueue.size() < MAX_SIZE) {
                        searchedDeviceQueue.offer(device);
                        searchedDeviceQueue.notifyAll();
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

    /**
     * 设备搜索线程
     */
    private class ScanThread extends Thread {
        @Override
        public void run() {
            // 启动搜索
            sendMsgDelayed(BT_START_FAIL, null, 2000);
            while (!FastBluetooth.getInstance().getBluetoothAdapter().startDiscovery()) {
                try {
                    Thread.sleep(100);  // 由于可能蓝牙没有打开，启动会失败
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            removeMsg(BT_START_FAIL);

            sendMsg(BT_START_SUC, null);

            // 等待搜索结果
            while (flag_isSearching) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            sendMsg(BT_SEARCHED, null);
        }
    }

    /**
     * 设备筛选线程
     */
    private class CollectionThread extends Thread {
        @Override
        public void run() {
            while (flag_collection) {
                if (searchedDeviceQueue.size() > 0) {
                    BluetoothDevice device = searchedDeviceQueue.poll();
                    if (device != null && !collectionDeviceList.contains(device)) {
                        collectionDeviceList.add(device);
                        if (bluetoothScanCallback != null) {
                            bluetoothScanCallback.onScanning(device);
                        }

                        if (flag_containScanParams) {
                            String deviceName = device.getName();
                            String deviceMac = device.getAddress();

                            if (TextUtils.equals(deviceMac, mac)) {
                                mTargetDevice = device;
                                cancelScan();
                                connect();
                            } else if (names != null && names.length > 0) {
                                if (!TextUtils.isEmpty(deviceName)) {
                                    for (String n : names) {
                                        if (TextUtils.equals(n, deviceName)) {
                                            mTargetDevice = device;
                                            cancelScan();
                                            connect();
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    try {
                        searchedDeviceQueue.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 连接蓝牙线程
     */
    private class ConnectThread extends Thread {
        private final BluetoothDevice mDevice;

        ConnectThread(BluetoothDevice device) {
            mDevice = device;
        }

        @Override
        public void run() {
            sendMsg(MSG_START_CONNECT, null);

            // 获取socket的过程
            try {
                mSocket = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (Exception e) {
                sendMsg(MSG_SOCKET_CONNECT_ERROR, null);
                return;
            }

            // 如果还在扫描，停止扫描
            if (FastBluetooth.getInstance().getBluetoothAdapter() != null
                    && FastBluetooth.getInstance().getBluetoothAdapter().isDiscovering()) {
                FastBluetooth.getInstance().getBluetoothAdapter().cancelDiscovery();
            }

            // 尝试连接2次
            for (int i = 0; mSocket != null && !mSocket.isConnected() && i < 2; i++) {
                try {
                    mSocket.connect();
                    sleep(200);
                } catch (IOException e) {
                    BluetoothLog.e("socket connect异常(第" + i + "次): " + e.getMessage());
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // 如果连接失败，清除配对信息
            if (mSocket == null || !mSocket.isConnected()) {
                // 清除手机蓝牙设置里所有设备的配对
                if (FastBluetooth.getInstance().getBluetoothAdapter() != null) {
                    Set<BluetoothDevice> pairedDevices = FastBluetooth.getInstance().getBluetoothAdapter().getBondedDevices();
                    if (pairedDevices != null && pairedDevices.size() > 0) {
                        for (BluetoothDevice device : pairedDevices) {
                            if (!TextUtils.isEmpty(device.getAddress()) && TextUtils.equals(device.getAddress(), mTargetDevice.getAddress())) {
                                try {
                                    Method m = device.getClass().getMethod("removeBond", (Class[]) null);
                                    m.invoke(device, (Object[]) null);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                sendMsg(MSG_SOCKET_CONNECT_ERROR, null);
                return;
            }

            sendMsg(MSG_CONNECT_OK, null);
        }
    }

    private final Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STATE_BLUE_OPEN_FAIL:
                    if (bluetoothScanCallback != null) {
                        bluetoothScanCallback.onBlueNotEnable();
                    }
                    break;

                case BT_START_FAIL:
                    flag_isSearching = false;
                    unregisterScanReceiver();
                    if (bluetoothScanCallback != null) {
                        bluetoothScanCallback.onScanStarted(false);
                    }
                    break;

                case BT_START_SUC:
                    if (bluetoothScanCallback != null) {
                        bluetoothScanCallback.onScanStarted(true);
                    }
                    break;

                case BT_SEARCHED:
                    if (flag_containScanParams) {
                        if (bluetoothScanCallback != null) {
                            bluetoothScanCallback.onScanFinished(mTargetDevice != null);
                        }
                    } else {
                        if (bluetoothScanCallback != null) {
                            bluetoothScanCallback.onScanFinished(false);
                        }
                    }
                    break;

                case MSG_START_CONNECT:
                    if (bluetoothScanCallback != null) {
                        bluetoothScanCallback.onStartConnect();
                    }
                    break;

                case MSG_SOCKET_CONNECT_ERROR:
                    if (bluetoothScanCallback != null) {
                        bluetoothScanCallback.onConnectError();
                    }
                    break;

                case MSG_CONNECT_OK:
                    if (bluetoothScanCallback != null) {
                        bluetoothScanCallback.onConnectSuc();
                    }
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }

        }
    };

    /**
     * 发送Message
     */
    private void sendMsg(int what, Object obj) {
        Message msg = mHandler.obtainMessage();
        msg.what = what;
        if (obj != null) {
            msg.obj = obj;
        }
        mHandler.sendMessage(msg);
    }

    /**
     * 延时发送Message
     */
    private void sendMsgDelayed(int what, Object obj, long time) {
        Message msg = mHandler.obtainMessage();
        msg.what = what;
        if (obj != null) {
            msg.obj = obj;
        }
        mHandler.sendMessageDelayed(msg, time);
    }

    /**
     * 移除Message
     */
    private void removeMsg(int what) {
        mHandler.removeMessages(what);
    }

}
