package com.mallowtech.samplemunic.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.mallowtech.samplemunic.BluetoothLeDeviceStore;
import com.mallowtech.samplemunic.BluetoothLeScanner;
import com.mallowtech.samplemunic.R;
import com.mallowtech.samplemunic.datamodel.ScanDeviceModel;
import com.mallowtech.samplemunic.utils.BluetoothUtils;
import com.mallowtech.samplemunic.utils.MunicUtils;

import java.util.ArrayList;

import uk.co.alt236.bluetoothlelib.device.BluetoothLeDevice;

public class MainActivity extends AppCompatActivity {
    private Button btnScan;
    private BluetoothUtils mBluetoothUtils;
    private ListView scanListView;
    private CustomScanAdapter adapter;
    private ArrayList<ScanDeviceModel> deviceScanList = new ArrayList<>();
    private ProgressBar mProgressBar;
    private BluetoothLeScanner mScanner;
    private BluetoothLeDeviceStore mDeviceStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnScan = (Button) findViewById(R.id.connect_button);
        scanListView = (ListView) findViewById(R.id.scan_list_view);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        mProgressBar.setVisibility(View.GONE);
        mDeviceStore = new BluetoothLeDeviceStore();
        mBluetoothUtils = new BluetoothUtils(this);
        mScanner = new BluetoothLeScanner(mLeScanCallback, mBluetoothUtils);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceScanList.clear();
                adapter.notifyDataSetChanged();
                startScanPrepare(MainActivity.this);
            }
        });

        adapter = new CustomScanAdapter(deviceScanList, MainActivity.this);
        scanListView.setAdapter(adapter);
        scanListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mScanner.scanLeDevice(-1, false);
                Intent in = new Intent(MainActivity.this, MunicActivity.class);
                in.putExtra(MunicUtils.MAC_ADDRESS, deviceScanList.get(position).getMacAddress());
                in.putExtra(MunicUtils.DEVICE_NAME, deviceScanList.get(position).getDeviceName());
                startActivity(in);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScanner.scanLeDevice(-1, false);
    }

    public void startScanPrepare(final Context context) {
        // The COARSE_LOCATION permission is only needed after API 23 to do a BTLE scan
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult((Activity) context,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, new PermissionsResultAction() {
                        @Override
                        public void onGranted() {
                            startScan();
                        }

                        @Override
                        public void onDenied(String permission) {
                            Toast.makeText(context,
                                    R.string.permission_not_granted_coarse_location,
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
        } else {
            startScan();

        }
    }

    private void startScan() {
        final boolean isBluetoothOn = mBluetoothUtils.isBluetoothOn();
        final boolean isBluetoothLePresent = mBluetoothUtils.isBluetoothLeSupported();
        mBluetoothUtils.askUserToEnableBluetoothIfNeeded();
        if (isBluetoothOn && isBluetoothLePresent) {
            mProgressBar.setVisibility(View.VISIBLE);
            getScanList();
        }
    }


    private void getScanList() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
                mScanner.scanLeDevice(-1, true);
                mProgressBar.setVisibility(View.GONE);

            }
        }, 1500);
    }

    public class CustomScanAdapter extends ArrayAdapter<ScanDeviceModel> {
        private ArrayList<ScanDeviceModel> dataSet;
        Context mContext;

        // View lookup cache
        private class ViewHolder {
            TextView txtUUID;
            TextView values;
        }

        public CustomScanAdapter(ArrayList<ScanDeviceModel> data, Context context) {
            super(context, R.layout.row_item, data);
            this.dataSet = data;
            this.mContext = context;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ScanDeviceModel dataModel = getItem(position);
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.scan_list_row_item, parent, false);
                viewHolder.txtUUID = (TextView) convertView.findViewById(R.id.name);
                viewHolder.values = (TextView) convertView.findViewById(R.id.values);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.txtUUID.setText(dataModel.getDeviceName());
            viewHolder.values.setText(dataModel.getMacAddress());
            // Return the completed view to render on screen
            return convertView;
        }
    }

    private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            final BluetoothLeDevice deviceLe = new BluetoothLeDevice(device, rssi, scanRecord, System.currentTimeMillis());
            mDeviceStore.addDevice(deviceLe);
            deviceScanList.clear();
            for (final BluetoothLeDevice leDevice : mDeviceStore.getDeviceList()) {
                String leDeviceName = leDevice.getName();
                if (leDeviceName != null && leDeviceName.length() > 0) {
                    deviceScanList.add(new ScanDeviceModel(leDevice.getName(), leDevice.getAddress()));
                } else {
                    deviceScanList.add(new ScanDeviceModel(getString(R.string.unknown), leDevice.getAddress()));
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }
    };
}