package com.daxiniot.bluetoothtemperature;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "BluetoothTemperature";
    /**
     * Y坐标轴默认最大值
     */
    private final float AXIS_MAX = 40;
    /**
     * Y坐标轴默认最小值
     */
    private final float AXIS_MIN = 20;
    /**
     * 手机蓝牙适配器
     */
    private BluetoothAdapter mBluetoothAdapter;
    /**
     * 蓝牙通信socket
     */
    private BluetoothSocket mSocket;
    /**
     * 蓝牙设备集合
     */
    private List<MyDevice> mDevices;
    /**
     * 设备列表控件适配器
     */
    private ArrayAdapter<MyDevice> mAdapter;
    /**
     * 温度显示控件
     */
    private TextView mTvTemperature;
    /**
     * 图表控件
     */
    private LineChart mLineChart;
    /**
     * 图表数据集
     */
    private LineDataSet mLineDataSet;
    /**
     * 图表数据
     */
    private LineData mLineData;
    /**
     * 蓝牙设备列表对话框
     */
    private AlertDialog mDeviceListDialog;
    /**
     * 图表显示的数据个数
     */
    private int xIndex = 8;

    /**
     * 记录推退出时间
     */
    private long mExitTime = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothUtil.READ_DATA:
                    String data = (String) msg.obj;
                    float temperature;
                    try {
                        temperature = Float.valueOf(data) / 100;
                    } catch (NumberFormatException e) {
                        break;
                    }
                    Log.i(TAG, "handleMessage: temperature=" + temperature);
                    //如果温度值超过默认Y坐标最大值，reset最大值
                    if (temperature > AXIS_MAX) {
                        YAxis yAxisLeft = mLineChart.getAxisLeft();
                        yAxisLeft.resetAxisMaximum();
                    }
                    //如果温度值小于默认Y坐标最小值，reset最小值
                    if (temperature < AXIS_MIN) {
                        YAxis yAxisLeft = mLineChart.getAxisLeft();
                        yAxisLeft.resetAxisMinimum();
                    }
                    //构造方法的字符格式这里如果小数不足2位,会以0补足
                    DecimalFormat decimalFormat = new DecimalFormat(".00");
                    String temperatureStr = decimalFormat.format(temperature);
                    mTvTemperature.setText(temperatureStr + " ℃");
                    int entryCount = mLineDataSet.getEntryCount();
                    if (entryCount < 8) {
                        mLineDataSet.addEntry(new Entry(++entryCount, temperature));
                    } else {
                        mLineDataSet.addEntry(new Entry(++xIndex, temperature));
                        mLineDataSet.removeFirst();
                    }
                    mLineData.notifyDataChanged();
                    mLineChart.notifyDataSetChanged();
                    mLineChart.invalidate();
                    break;
                default:

            }
        }
    };
    /**
     * 广播监听器：负责接收搜索到蓝牙的广播
     */
    private BroadcastReceiver mDeviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //更新UI
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                String address = device.getAddress();
                boolean bonded = false;
                Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
                //检查设备是否已配对
                for (BluetoothDevice tempDevice : bondedDevices) {
                    if (tempDevice.getAddress().equals(address)) {
                        bonded = true;
                    }
                }
                //刷新设备显示列表
                MyDevice myDevice = new MyDevice();
                myDevice.setName(name);
                myDevice.setAddress(address);
                myDevice.setBonded(bonded);
                mDevices.add(myDevice);
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //控件初始化
        mTvTemperature = (TextView) findViewById(R.id.tv_temperature);
        mLineChart = (LineChart) findViewById(R.id.chart);
        //初始化蓝牙设备列表对话框
        initDeviceDialog();
        //初始化图表属性
        initChart();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //判断手机是否有蓝牙
        if (!BluetoothUtil.isBluetoothSupported()) {
            Toast.makeText(MainActivity.this, "手机上没有蓝牙，应用退出", Toast.LENGTH_SHORT).show();
            finish();
        }
        //注册设备发现广播接收器
        BluetoothUtil.registerDeviceFoundReceiver(mDeviceFoundReceiver, MainActivity.this);
    }

    /**
     * 初始化蓝牙设备列表对话框
     */
    private void initDeviceDialog() {
        View DialogView = getLayoutInflater().inflate(R.layout.dialog_scan_device, null);
        mDeviceListDialog = new AlertDialog.Builder(MainActivity.this).setView(DialogView).create();
        Button cancelScanBtn = (Button) DialogView.findViewById(R.id.btn_cancel_scan);
        cancelScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDeviceListDialog.dismiss();
            }
        });
        ListView deviceListView = (ListView) DialogView.findViewById(R.id.lvw_devices);
        //初始化蓝牙列表数据
        mDevices = new ArrayList<>();
        mAdapter = new ArrayAdapter<MyDevice>(MainActivity.this,
                android.R.layout.simple_list_item_1, mDevices);
        deviceListView.setAdapter(mAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mDeviceListDialog.dismiss();
                final String address = mDevices.get(i).getAddress();
                final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.show();
                BluetoothUtil.connectDevice(address, new ConnectCallback() {
                    @Override
                    public void onSuccess(BluetoothSocket socket) {
                        progressDialog.dismiss();
                        mSocket = socket;
                        BluetoothUtil.writeData(socket, "1");
                        BluetoothUtil.readData(socket, mHandler, BluetoothUtil.READ_DATA);
                    }

                    @Override
                    public void onFailure() {
                        progressDialog.dismiss();
                    }
                });
            }
        });
    }

    /**
     * 初始化图表属性
     */
    private void initChart() {
        //图表不可拖拽
        mLineChart.setDragEnabled(false);
        //设置X轴属性
        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        //设置Y轴属性
        YAxis yAxisLeft = mLineChart.getAxisLeft();
        yAxisLeft.setTextColor(Color.WHITE);
        yAxisLeft.setAxisMaximum(AXIS_MAX);
        yAxisLeft.setAxisMinimum(AXIS_MIN);
        YAxis yAxisRight = mLineChart.getAxisRight();
        yAxisRight.setTextColor(Color.WHITE);
        //隐藏右侧的Y轴
        yAxisRight.setEnabled(false);
        //设置图表数据
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 0));
        mLineDataSet = new LineDataSet(entries, "temperature");
        mLineData = new LineData(mLineDataSet);
        mLineChart.setData(mLineData);
        mLineChart.invalidate();

    }

    /**
     * 创建菜单
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * 菜单点击事件回调方法
     *
     * @param item 被点击的菜单项
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan_device:
                //判断蓝牙是否已经打开
                if (!mBluetoothAdapter.isEnabled()) {
                    Toast.makeText(MainActivity.this, "请先打开蓝牙", Toast.LENGTH_SHORT).show();
                    return true;
                }
                //检查定位权限是否已经开启
                if (!BluetoothUtil.isLocationPermissionEnabled(MainActivity.this)) {
                    BluetoothUtil.requestLocationPermission(MainActivity.this);
                    return true;
                }
                //开始扫描设备
                startDiscoveryDevice();
                return true;
            case R.id.disconnect_device:
                try {
                    mSocket.close();
                    mTvTemperature.setText("____ ℃");
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BluetoothUtil.REQUEST_LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //赋予权限之后继续扫描设备
                startDiscoveryDevice();
            } else {
                Toast.makeText(MainActivity.this, "定位权限未授予 无法完成设备扫描", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * 开始扫描蓝牙设备
     */
    private void startDiscoveryDevice() {
        mDevices.clear();
        mAdapter.notifyDataSetChanged();
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
        mDeviceListDialog.show();
    }

    /**
     * 再按一次退出程序：如果用户在2秒之内连续点击返回键则退出应用
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                Toast.makeText(MainActivity.this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();
            } else {
                //退出应用
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //退出应用时反注册设备监听器
        unregisterReceiver(mDeviceFoundReceiver);
    }

}
