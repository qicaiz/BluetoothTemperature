package com.daxiniot.bluetoothtemperature;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class BluetoothUtil {

    public final static int READ_DATA = 1;
    public final static int WRITE_DATA = 2;

    private final static BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    public static void initBluetooth() {

    }

    public static boolean isBluetoothSupported() {
        return mBluetoothAdapter != null;
    }

    public static boolean isBluetoothEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    public static void registerDeviceFoundReceiver(BroadcastReceiver receiver, AppCompatActivity activity) {
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(receiver, intentFilter);
    }

    public void unregisterDeviceFoundReceiver(BroadcastReceiver receiver, AppCompatActivity activity) {
        activity.unregisterReceiver(receiver);
    }

    public static boolean isDiscovering() {
        return mBluetoothAdapter.isDiscovering();
    }

    public static void cancelDiscovery() {
        mBluetoothAdapter.cancelDiscovery();
    }

    public void discoveryDevice() {
        mBluetoothAdapter.startDiscovery();
    }

    public static void connectDevice(final String address, final ConnectCallback callback) {

        new Thread() {
            @Override
            public void run() {
                mBluetoothAdapter.cancelDiscovery();
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
                try {
                    BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuid);
                    socket.connect();
                    //连接成功读取数据
                    callback.onSuccess(socket);
                } catch (IOException e) {
                    callback.onFailure();
                    e.printStackTrace();
                }
                super.run();
            }
        }.start();
    }

    public static void readData(final BluetoothSocket socket, final Handler handler, final int messageWhat) {
        new Thread() {
            @Override
            public void run() {
                try {
                    InputStream inputStream = socket.getInputStream();
                    byte[] data = new byte[1024];
                    int len = 0;
                    while (len != -1) {
                        if (inputStream.available() <= 0) {//inputStream接收的数据是一段段的，如果不先
                            continue;
                        } else {
                            try {
                                Thread.sleep(300);//等待0.3秒，让数据接收完整
                                len = inputStream.read(data);
                                String result = new String(data, 0, len, "utf-8");
                                Message message = new Message();
                                message.what = messageWhat;
                                message.obj = result;
                                handler.sendMessage(message);

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void writeData(Handler handler, int messageWhat) {

    }

    public void connectHeartBeat(Handler handler) {

    }

    public static boolean isLocationPermissionEnabled(AppCompatActivity activity) {
        int permissionResult = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissionResult == PackageManager.PERMISSION_GRANTED;
    }


}
