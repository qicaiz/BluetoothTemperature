package com.daxiniot.bluetoothtemperature;

import android.bluetooth.BluetoothSocket;

/**
 * Created by Administrator on 2018/6/22.
 */

public interface ConnectCallback {
    void onSuccess(BluetoothSocket socket);
    void onFailure();
}
