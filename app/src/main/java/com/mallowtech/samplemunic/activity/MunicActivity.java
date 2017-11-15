package com.mallowtech.samplemunic.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mallowtech.samplemunic.MunicBluetoothConnect;
import com.mallowtech.samplemunic.R;
import com.mallowtech.samplemunic.adapter.CustomAdapter;
import com.mallowtech.samplemunic.datamodel.DataModel;
import com.mallowtech.samplemunic.utils.MunicUtils;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MunicActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private ListView listView;
    private IntentFilter mIntentFilter = null;
    private String macAddress = null, deviceName;
    private MunicBluetoothConnect municBluetoothConnect;
    private ArrayList<DataModel> dataModelList = new ArrayList<>();
    private CustomAdapter adapter;
    private Timer timer = null;
    private ProgressBar mProgressBar;
    private Button txtConnection;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_munic);
        macAddress = getIntent().getStringExtra(MunicUtils.MAC_ADDRESS);
        deviceName = getIntent().getStringExtra(MunicUtils.DEVICE_NAME);
        setTitle(deviceName);
        listView = (ListView) findViewById(R.id.list_view);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        txtConnection = (Button) findViewById(R.id.txt_connection);
        mProgressBar.setVisibility(View.GONE);
        municBluetoothConnect = new MunicBluetoothConnect(MunicActivity.this, macAddress);
        municBluetoothConnect.startBluetoothLeService();
        adapter = new CustomAdapter(dataModelList, MunicActivity.this);
        listView.setAdapter(adapter);

        txtConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (txtConnection.getText().toString().equalsIgnoreCase("DISCONNECTED")) {
                    connectMunicDevice();
                }
            }
        });

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                connectMunicDevice();
            }
        }, 1500);
    }

    private void connectMunicDevice() {
        mProgressBar.setVisibility(View.VISIBLE);
        dataModelList.clear();
        municBluetoothConnect.bluetoothGattConnect(); // for connected the bluetooth
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(municBluetoothConnect.customBroadcastReceiver)) {
                try {
                    String gattUUID = intent.getStringExtra("gattUUID");
                    String gattUUIDDesc = intent.getStringExtra("gattUUIDDesc");
                    String dateAsString = intent.getStringExtra("dateAsString");
                    String dateAsArray = intent.getStringExtra("dateAsArray");
                    String currentState = intent.getStringExtra("currentState");
                    txtConnection.setText("" + currentState);
                    Toast.makeText(context, currentState, Toast.LENGTH_LONG).show();
                    if (currentState.equalsIgnoreCase("DATA AVAILABLE")) {
                        if (gattUUID != null && dateAsString != null) {
                            if (gattUUID.equalsIgnoreCase("0000ed20-0000-1000-8000-ff805f9b34fb")) {
                                gattUUID = "location";
                            }
                            if (gattUUID.equalsIgnoreCase("0000ed18-0000-1000-8000-ff805f9b34fb")) {
                                gattUUID = "obd_speed";
                            }
                            if (gattUUID.equalsIgnoreCase("0000ed0d-0000-1000-8000-ff805f9b34fb")) {
                                gattUUID = "ignition";
                            }
                            if (gattUUID.equalsIgnoreCase("0000ed15-0000-1000-8000-ff805f9b34fb")) {
                                gattUUID = "obd_connected_protocol";
                            }
                            if (gattUUID.equalsIgnoreCase("0000ed22-0000-1000-8000-ff805f9b34fb")) {
                                gattUUID = "behave_id";
                            }
                            if (gattUUID.equalsIgnoreCase("0000ed17-0000-1000-8000-ff805f9b34fb")) {
                                gattUUID = "obd_rpm";
                            }
                            if (gattUUID.equalsIgnoreCase("0000ed23-0000-1000-8000-ff805f9b34fb")) {
                                gattUUID = "ext_batt";
                            }
                            if (gattUUID.equalsIgnoreCase("0000ed01-0000-1000-8000-ff805f9b34fb")) {
                                gattUUID = "dashboard_mileage";
                            }
                            if (gattUUID.equalsIgnoreCase("0000ed24-0000-1000-8000-ff805f9b34fb")) {
                                gattUUID = "dashboard_fuel";
                            }
                            if (gattUUID.equalsIgnoreCase("0000ed25-0000-1000-8000-ff805f9b34fb")) {
                                gattUUID = "vehicle_state";
                            }
                            if (gattUUID.equalsIgnoreCase("0000ed26-0000-1000-8000-ff805f9b34fb")) {
                                gattUUID = "obd_fuel";
                            }
                            if (gattUUID.equalsIgnoreCase("0000ed1d-0000-1000-8000-ff805f9b34fb")) {
                                gattUUID = "obd_vin";
                            }
                            dataModelList.add(new DataModel(gattUUID, "Values Array: " + dateAsArray + " Values String: " + dateAsString));
                            adapter.notifyDataSetChanged();
                            listView.setSelection(dataModelList.size() - 1);
                        }
                    } else if (currentState.equalsIgnoreCase("DISCOVERED")) {
                        simulateReadCharacteristic();
                    } else if (currentState.equalsIgnoreCase("DISCONNECTED")) {
                        mProgressBar.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onReceive:editedLogReceiver " + e.getLocalizedMessage(), e);
                }
            }
        }
    };

    public void simulateReadCharacteristic() {
        mProgressBar.setVisibility(View.GONE);
        if (timer == null) {
            timer = new Timer();
            TimerTask hourlyTask = new TimerTask() {
                @Override
                public void run() {
                    if (municBluetoothConnect != null) {
                        municBluetoothConnect.readAllCharacteristic();
                    }
                }
            };
            timer.schedule(hourlyTask, 0l, 10000);// 1000 every 10 seconds
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        municBluetoothConnect.unBindServiceConnection();
        if (broadcastReceiver != null) {
            try {
                unregisterReceiver(broadcastReceiver);
            } catch (Exception e) {
                Log.e(TAG, "onDestroy:broadcastReceiver " + e.getLocalizedMessage());
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        municBluetoothConnect.unregisterGattReceiver();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mIntentFilter == null) {
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(municBluetoothConnect.customBroadcastReceiver);
        }
        municBluetoothConnect.registerGattReceiver();
        registerReceiver(broadcastReceiver, mIntentFilter);
    }
}