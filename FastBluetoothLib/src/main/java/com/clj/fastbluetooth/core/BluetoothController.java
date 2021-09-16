package com.clj.fastbluetooth.core;

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
import com.clj.fastbluetooth.callback.BluetoothReadCallback;
import com.clj.fastbluetooth.callback.BluetoothScanCallback;
import com.clj.fastbluetooth.callback.BluetoothWriteCallback;
import com.clj.fastbluetooth.exception.BluetoothException;
import com.clj.fastbluetooth.util.CycleThread;
import com.clj.fastbluetooth.utils.BluetoothLog;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BluetoothController {

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothScanCallback mScanCallback;
    private BluetoothReadCallback mReadCallback;
    private BluetoothWriteCallback mWriteCallback;
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

    // 线程标记
    private boolean flag_isSearching = false;

    private static final int BT_START_FAIL = 100;                  // 开启扫描失败
    private static final int BT_START_SUC = 101;                  // 开启扫描失败
    private static final int BT_SCAN_FINISHED = 102;                    // 搜索完成标记
    private static final int BT_START_CONNECT = 104;              // 开始连接
    private static final int BT_CONNECT_OK = 105;                 // 连接完成
    private static final int BT_CONNECT_ERROR = 106;       // socket连接失败
    private static final int BT_OPEN_FAIL = 107;           // 蓝牙打开失败

    private ScanThread mScanThread = null;
    private CollectionThread mCollectionThread = null;
    private ConnectThread mConnectThread = null;
    private WriteThread mWriteThread = null;
    private ReadThread mReadThread = null;

    private BluetoothSocket mSocket = null;

    private BluetoothDevice mTargetDevice = null;

    public static BluetoothController getInstance() {
        return BluetoothScannerHolder.sBluetoothScanner;
    }

    private static class BluetoothScannerHolder {
        private static final BluetoothController sBluetoothScanner = new BluetoothController();
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

        flag_containScanParams = (names != null && names.length >= 1) || (mac != null);

        // 如果设置了自动连接，那必须要有搜索条件
        if (autoConnect && !flag_containScanParams) {
            BluetoothLog.w("names or macs search condition must not be empty, if autoConnect is used");
            if (callback != null) {
                callback.onScanStarted(false);
            }
            return;
        }

        this.names = names;
        this.mac = mac;
        this.timeout = timeout;
        this.autoConnect = autoConnect;
        this.mScanCallback = callback;

        // 每次开启新的搜索线程，重置参数
        resetParams();

        flag_isSearching = true;

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
    public void cancelScan() {
        // 注销蓝牙广播接收器
        unregisterScanReceiver();

        // 标记搜索状态
        flag_isSearching = false;

        // 关闭搜索
        if (FastBluetooth.getInstance().getBluetoothAdapter() != null
                && FastBluetooth.getInstance().getBluetoothAdapter().isDiscovering()) {
            FastBluetooth.getInstance().getBluetoothAdapter().cancelDiscovery();
        }
    }

    private void resetParams() {
        searchedDeviceQueue.clear();
        collectionDeviceList.clear();
        mTargetDevice = null;
        mSocket = null;
    }

    /**
     * 连接
     */
    public void connect(BluetoothDevice device) {
        if (mConnectThread != null && mConnectThread.isAlive()) {
            mConnectThread.interrupt();
        }
        mConnectThread = null;
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    /**
     * 发送数据
     */
    public void write(byte[] command, BluetoothWriteCallback callback) {
        this.mWriteCallback = callback;

        if (mWriteThread != null && mWriteThread.isAlive()) {
            mWriteThread.interrupt();
        }
        mWriteThread = null;
        mWriteThread = new WriteThread(command);
        mWriteThread.start();
    }

    /**
     * 发送数据
     */
    public void read(BluetoothReadCallback callback) {
        this.mReadCallback = callback;

        if (mReadThread != null && mReadThread.isAlive()) {
            mReadThread.interrupt();
        }
        mReadThread = null;
        mReadThread = new ReadThread();
        mReadThread.start();
    }

    public void stopDataRead() {
        if (mReadThread != null && mReadThread.isAlive()) {
            mReadThread.cancel();
        }
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
                    }
                }
            }

            // 扫描结束
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                BluetoothLog.w("ACTION_DISCOVERY_FINISHED");
                sendMsg(BT_SCAN_FINISHED, null);
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

            if (timeout > 1000) {
                sendMsgDelayed(BT_SCAN_FINISHED, null, timeout);
            }
        }
    }

    /**
     * 设备筛选线程
     */
    private class CollectionThread extends Thread {
        @Override
        public void run() {
            while (flag_isSearching) {
                if (searchedDeviceQueue.size() > 0) {
                    BluetoothDevice device = searchedDeviceQueue.poll();
                    if (device != null && !collectionDeviceList.contains(device)) {
                        collectionDeviceList.add(device);
                        if (mScanCallback != null) {
                            mScanCallback.onScanning(device);
                        }

                        if (flag_containScanParams) {
                            String deviceName = device.getName();
                            String deviceMac = device.getAddress();

                            if (TextUtils.equals(deviceMac, mac)) {
                                mTargetDevice = device;
                                sendMsg(BT_SCAN_FINISHED, null);
                            } else if (names != null && names.length > 0) {
                                if (!TextUtils.isEmpty(deviceName)) {
                                    for (String n : names) {
                                        if (TextUtils.equals(n, deviceName)) {
                                            mTargetDevice = device;
                                            sendMsg(BT_SCAN_FINISHED, null);
                                        }
                                    }
                                }
                            }
                        }
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
            sendMsg(BT_START_CONNECT, null);

            // 获取socket的过程
            try {
                mSocket = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (Exception e) {
                sendMsg(BT_CONNECT_ERROR, null);
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
                sendMsg(BT_CONNECT_ERROR, null);
                return;
            }

            sendMsg(BT_CONNECT_OK, null);
        }
    }

    /**
     * 写
     */
    private class WriteThread extends Thread {

        private final byte[] mCommand;

        WriteThread(byte[] command) {
            mCommand = command;
        }

        @Override
        public void run() {
            super.run();
            if (mSocket == null || !mSocket.isConnected()) {
                if (mWriteCallback != null) {
                    mWriteCallback.onWriteError(new BluetoothException(BluetoothException.ERROR_DISCONNECTION, null));
                }
                return;
            }
            try {
                OutputStream os = mSocket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(os);
                dos.write(mCommand);
                dos.flush();
                os.flush();
                if (mWriteCallback != null) {
                    mWriteCallback.onWriteSuccess(mCommand);
                }
            } catch (IOException e) {
                if (mWriteCallback != null) {
                    mWriteCallback.onWriteError(new BluetoothException(BluetoothException.ERROR_IO, null));
                }
            }
        }
    }

    /**
     * 读
     */
    private class ReadThread extends CycleThread {

        DataInputStream dis = null;
        byte[] buffer = new byte[0];

        @Override
        public void begin() {
            super.begin();
            if (mSocket == null || !mSocket.isConnected()) {
                if (mReadCallback != null) {
                    mReadCallback.onReadError(new BluetoothException(BluetoothException.ERROR_DISCONNECTION, null));
                }
                return;
            }
            try {
                InputStream in = mSocket.getInputStream();
                dis = new DataInputStream(in);
            } catch (IOException e) {
                if (mReadCallback != null) {
                    mReadCallback.onReadError(new BluetoothException(BluetoothException.ERROR_IO, null));
                }
            }
        }

        @Override
        public void end() {
            super.end();
            if (mSocket.isConnected()) {
                try {
                    dis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void cycle() {
            super.cycle();
            if (dis == null) {
                cancel();
                return;
            }

            try {
                int len = dis.available();
                if (len == 0) {
                    if (buffer.length > 0) {
                        if (mReadCallback != null) {
                            mReadCallback.onDataReceive(buffer);
                        }
                        buffer = new byte[0];
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    buffer = new byte[len];
                    dis.read(buffer);
                }

//                while (len != 0) {
//                    buffer = new byte[len];
//                    dis.read(buffer);
//                    try {
//                        Thread.sleep(10);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    len = dis.available();
//                }
//                if (buffer.length > 0) {
//                    if (mReadCallback != null) {
//                        mReadCallback.onDataReceive(buffer);
//                    }
//                }
            } catch (IOException e) {
                cancel();
                e.printStackTrace();
                if (mReadCallback != null) {
                    mReadCallback.onReadError(new BluetoothException(BluetoothException.ERROR_IO, null));
                }
            }
        }
    }

    private final Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BT_OPEN_FAIL:
                    if (mScanCallback != null) {
                        mScanCallback.onBlueNotEnable();
                    }
                    break;

                case BT_START_FAIL:
                    cancelScan();
                    if (mScanCallback != null) {
                        mScanCallback.onScanStarted(false);
                    }
                    break;

                case BT_START_SUC:
                    if (mScanCallback != null) {
                        mScanCallback.onScanStarted(true);
                    }
                    break;

                case BT_SCAN_FINISHED:
                    removeMsg(BT_SCAN_FINISHED);

                    // 如果还在搜索，说明是超时了
                    if (flag_isSearching) {
                        cancelScan();
                    }
                    if (flag_containScanParams) {
                        if (mScanCallback != null) {
                            mScanCallback.onScanFinished(mTargetDevice != null);
                        }
                    } else {
                        if (mScanCallback != null) {
                            mScanCallback.onScanFinished(false);
                        }
                    }
                    if (autoConnect && mTargetDevice != null) {
                        connect(mTargetDevice);
                    }
                    break;

                case BT_START_CONNECT:
                    if (mScanCallback != null) {
                        mScanCallback.onStartConnect();
                    }
                    break;

                case BT_CONNECT_ERROR:
                    if (mScanCallback != null) {
                        mScanCallback.onConnectError();
                    }
                    break;

                case BT_CONNECT_OK:
                    if (mScanCallback != null) {
                        mScanCallback.onConnectSuc();
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
