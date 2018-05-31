package com.daxiniot.bluetoothtemperature;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    /**手机蓝牙适配器*/
    BluetoothAdapter mBluetoothAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //判断手机是否有蓝牙
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter==null){
            Toast.makeText(MainActivity.this, "手机上没有蓝牙，应用退出", Toast.LENGTH_SHORT).show();
            finish();
        }
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
                //跳转至蓝牙搜索页面
                startActivity(new Intent(MainActivity.this,DevicesActivity.class));
            }

        }
        return true;
    }
}
