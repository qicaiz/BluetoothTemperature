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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    /**手机蓝牙适配器*/
    private BluetoothAdapter mBluetoothAdapter;
    private ListView mDeviceListView;
    /**蓝牙通信socket*/
    BluetoothSocket mSocket;
    /**蓝牙设备集合*/
    List<MyDevice> mDevices;
    /**设备列表控件适配器*/
    ArrayAdapter<MyDevice> mAdapter;
    TextView tv;

    private BroadcastReceiver mDeviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                //更新UI
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                String address = device.getAddress();
                boolean bonded = false;
                Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
                //检查设备是否已配对
                for(BluetoothDevice tempDevice:bondedDevices){
                    if(tempDevice.getAddress().equals(address)){
                        bonded = true;
                    }
                }
                //刷新设备显示列表
                MyDevice myDevice =  new MyDevice();
                myDevice.setName(name);
                myDevice.setAddress(address);
                myDevice.setBonded(bonded);
                mDevices.add(myDevice);
                mAdapter.notifyDataSetChanged();
                Log.i("tag", "onReceive: name="+name);
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = findViewById(R.id.tv);
        //判断手机是否有蓝牙
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter==null){
            Toast.makeText(MainActivity.this, "手机上没有蓝牙，应用退出", Toast.LENGTH_SHORT).show();
            finish();
        }
        //注册设备发现监听器
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mDeviceFoundReceiver,intentFilter);
        mDevices = new ArrayList<>();
        mAdapter = new ArrayAdapter<MyDevice>(MainActivity.this,
                android.R.layout.simple_list_item_1,mDevices);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.scan_device){
            //判断蓝牙是否已经打开
            if(!mBluetoothAdapter.isEnabled()){
                Toast.makeText(MainActivity.this,"请先打开蓝牙",Toast.LENGTH_SHORT).show();
            } else {
                //检查定位权限
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED){

                    mDevices.clear();
                    mAdapter.notifyDataSetChanged();
                    //展示设备列表对话框
                    showDeviceListDialog();
                    //开始搜索
                    if(mBluetoothAdapter.isDiscovering()){
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    mBluetoothAdapter.startDiscovery();
                }else{
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1);
                }

            }

        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==1){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                mDevices.clear();
                mAdapter.notifyDataSetChanged();
                if(mBluetoothAdapter.isDiscovering()){
                    mBluetoothAdapter.cancelDiscovery();
                }
                mBluetoothAdapter.startDiscovery();
                showDeviceListDialog();
            } else {
                Toast.makeText(MainActivity.this,"action found is not granted.",Toast.LENGTH_LONG).show();
            }

        }
    }
    private String mResult="";

    /**
     * 展示设备列表对话框
     */
    private void showDeviceListDialog(){
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_scan_device,null);
        final AlertDialog deviceListDialog = new AlertDialog.Builder(MainActivity.this)
                .setView(dialogView)
                .create();

        Button cancleBtn= dialogView.findViewById(R.id.btn_cancel_scan);
        mDeviceListView = dialogView.findViewById(R.id.lvw_devices);
        mDeviceListView.setAdapter(mAdapter);
        mDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                deviceListDialog.dismiss();
                final String name = mDevices.get(i).getName();
                final String address = mDevices.get(i).getAddress();
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
                try {
                    mSocket = device.createRfcommSocketToServiceRecord(uuid);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                new Thread(){
                    @Override
                    public void run() {
                        mBluetoothAdapter.cancelDiscovery();
                        try {
                            // TODO: 5/31/2018  连接等待对话框
                            mSocket.connect();
                            // TODO: 5/31/2018 连接成功
                            new Thread(){
                                @Override
                                public void run() {

                                    try {
                                        InputStream inputStream = mSocket.getInputStream();
                                        byte[] data = new byte[1024];
                                        int len = 0;

                                        while (len != -1) {
                                            if (inputStream.available() > 0 == false) {//inputStream接收的数据是一段段的，如果不先
                                                continue;
                                            } else {
                                                try {
                                                    Thread.sleep(300);//等待0.5秒，让数据接收完整
                                                    len = inputStream.read(data);
                                                    mResult = new String(data,0,len, "utf-8");
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            tv.setText(mResult);
                                                        }
                                                    });

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
                        } catch (IOException e) {
                            // TODO: 5/31/2018 连接失败
                            try {
                                mSocket.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            e.printStackTrace();
                        }
                        super.run();
                    }
                }.start();
            }
        });
        cancleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deviceListDialog.dismiss();
            }
        });

        deviceListDialog.show();

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //退出应用时反注册设备监听器
        unregisterReceiver(mDeviceFoundReceiver);
    }
}
