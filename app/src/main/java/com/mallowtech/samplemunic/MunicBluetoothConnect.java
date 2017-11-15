package com.mallowtech.samplemunic;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.mallowtech.samplemunic.service.BluetoothLeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.co.alt236.bluetoothlelib.resolvers.GattAttributeResolver;
import uk.co.alt236.bluetoothlelib.util.ByteUtils;

/**
 * Created by manikandan on 16/06/17.
 */
public class MunicBluetoothConnect {
    private Context mContext;
    public static final String TAG = "Testing : ";
    public BluetoothLeService mBluetoothLeService;
    private State mCurrentState = State.DISCONNECTED;
    private static final String LIST_UUID = "UUID";
    private static final String LIST_NAME = "NAME";
    public static final String customBroadcastReceiver = "com.sample.munic";
    private String gattUUID, gattUUIDDesc, dateAsArray, dateAsString, currentState;
    private String deviceMacAddress;
    private static BluetoothGattCharacteristic locationGattCharacteristic = null;
    private static BluetoothGattCharacteristic speedGattCharacteristic = null;
    private static BluetoothGattService municService = null;
    private int i = 0;

    public MunicBluetoothConnect(Context context, String deviceMacAddress) {
        this.mContext = context;
        this.deviceMacAddress = deviceMacAddress;
        setCurrentState(mCurrentState);
    }

    // Code to manage Service lifecycle.
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName componentName, final IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            Log.d(TAG, "onServiceConnected: Service Connected" + mBluetoothLeService);
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Automatically connects to the device upon successful start-up initialization.
                    mBluetoothLeService.connect(deviceMacAddress);
                }
            }, 500);
        }

        @Override
        public void onServiceDisconnected(final ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected: Service Disconnected" + false);
            mBluetoothLeService = null;
        }
    };


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(customBroadcastReceiver);
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                setCurrentState(State.CONNECTED);
                currentState = "CONNECTED";
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                setCurrentState(State.DISCONNECTED);
                currentState = "DISCONNECTED";
            } else if (BluetoothLeService.ACTION_GATT_CONNECTING.equals(action)) {
                setCurrentState(State.CONNECTING);
                currentState = "CONNECTING";
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                currentState = "DISCOVERED";
                i = 0;
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(context, mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                currentState = "DATA AVAILABLE";
                i = 0;
                final String noData = context.getString(R.string.no_data);
                final String uuid = intent.getStringExtra(BluetoothLeService.EXTRA_UUID_CHAR);
                Log.d(TAG, "onReceive: uuid" + uuid);
                final byte[] dataArr = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA_RAW);
                Log.d(TAG, "onReceive: dataArr" + dataArr);
                gattUUID = tryString(uuid, noData);
                Log.d(TAG, "onReceive: gattUUID" + gattUUID);
                gattUUIDDesc = GattAttributeResolver.getAttributeName(uuid, context.getString(R.string.unknown));
                Log.d(TAG, "onReceive: gattUUIDDesc" + gattUUIDDesc);
                dateAsArray = ByteUtils.byteArrayToHexString(dataArr);
                Log.d(TAG, "onReceive: gattUUIDDesc" + gattUUIDDesc);
                dateAsString = new String(dataArr);
                broadcastIntent.putExtra("gattUUID", gattUUID);
                broadcastIntent.putExtra("gattUUIDDesc", gattUUIDDesc);
                broadcastIntent.putExtra("dateAsArray", dateAsArray);
                broadcastIntent.putExtra("dateAsString", dateAsString);
            }
            if (currentState.equalsIgnoreCase("DISCONNECTED")) {
                i++;
                Log.d(TAG, "mGattUpdateReceiver: Retry Option " + i);
                if (i < 4) {
                    bluetoothGattConnect(); // for connected the bluetooth
                }
            }
            broadcastIntent.putExtra("currentState", currentState);
            context.sendBroadcast(broadcastIntent);
        }
    };


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(final Context context, final List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            Log.d(TAG, "displayGattServices: GATT Services list is NULL " + true);
            return;
        } else {
            Log.d(TAG, "displayGattServices: GATT Services list SIZE " + gattServices.size());
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    createGattServiceList(context, gattServices);
                }
            }, 500);
        }
    }

    public State getCurrentState() {
        return mCurrentState;
    }

    public void setCurrentState(State mCurrentState) {
        this.mCurrentState = mCurrentState;
    }

    public void createGattServiceList(final Context context, final List<BluetoothGattService> gattServices) {
        Log.d(TAG, "createGattServiceList:gattServices SIZE " + gattServices.size());
        Toast.makeText(mContext, "Gatt services Size : " + gattServices.size(), Toast.LENGTH_LONG).show();
        final String unknownServiceString = context.getString(R.string.unknown_service);
        final String unknownCharaString = context.getString(R.string.unknown_characteristic);
        final List<Map<String, String>> gattServiceData = new ArrayList<>();

        // Loops through available GATT Services.
        String uuid;
        for (final BluetoothGattService gattService : gattServices) {
            final Map<String, String> currentServiceData = new HashMap<>();
            uuid = gattService.getUuid().toString();
            Log.d(TAG, "createGattServiceList:UUID " + uuid + "\n Service Name : " + GattAttributeResolver.getAttributeName(uuid, unknownServiceString));
//            if (uuid.equalsIgnoreCase("0000aa52-b956-4754-86eb-ff805f9b34fb")) {
//                Toast.makeText(mContext, "Munic services discovered", Toast.LENGTH_LONG).show();
                Log.d(TAG, "createGattServiceList:UUID " + uuid + "\n Service Name : " + "MUNIC");
                municService = gattService;
                currentServiceData.put(LIST_NAME, GattAttributeResolver.getAttributeName(uuid, unknownServiceString));
                currentServiceData.put(LIST_UUID, uuid);
                gattServiceData.add(currentServiceData);

                final List<Map<String, String>> gattCharacteristicGroupData = new ArrayList<>();
                final List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                final List<BluetoothGattCharacteristic> charas = new ArrayList<>();

//            // Loops through available Characteristics.
                for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    charas.add(gattCharacteristic);
                    final Map<String, String> currentCharaData = new HashMap<>();
                    uuid = gattCharacteristic.getUuid().toString();
                    currentCharaData.put(LIST_NAME, GattAttributeResolver.getAttributeName(uuid, unknownCharaString));
                    currentCharaData.put(LIST_UUID, uuid);
                    gattCharacteristicGroupData.add(currentCharaData);
                }
//            } else {
//                Log.d(TAG, "createGattServiceList:ELSE Other services");
//            }
        }
    }

    public static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTING);
        return intentFilter;
    }

    private static String tryString(final String string, final String fallback) {
        if (string == null) {
            return fallback;
        } else {
            return string;
        }
    }

    public enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    public void startBluetoothLeService() {
        final Intent gattServiceIntent = new Intent(mContext, BluetoothLeService.class);
        mContext.bindService(gattServiceIntent, mServiceConnection, mContext.BIND_AUTO_CREATE);
    }

    public void unBindServiceConnection() {
        Log.d(TAG, "unBindServiceConnection: Service connection unBind" + true);
        if (mServiceConnection != null) {
            mContext.unbindService(mServiceConnection);
            mBluetoothLeService = null;
        }
    }

    public void registerGattReceiver() {
        mContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(deviceMacAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    public void unregisterGattReceiver() {
        try {
            mContext.unregisterReceiver(mGattUpdateReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void bluetoothGattConnect() {
        Log.d(TAG, "bluetoothGattConnect: " + true);
        if (mBluetoothLeService != null) {
            // Below is the actual code for connecting to actual bluetooth device
            mBluetoothLeService.connect(deviceMacAddress);
        } else {
            Log.d(TAG, "bluetoothGattConnect: null " + true);
        }
    }

    public void bluetoothGattDisConnect() {
        if (mBluetoothLeService != null) {
            // Below is the actual code for disconnecting from actual bluetooth device
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
        } else {
            Log.d(TAG, "bluetoothGattDisConnect: null " + true);
        }
    }

    public void readAllCharacteristic() {
        // Below commented code is the actual code to fetch the data from Munic device. Our target is to make this code working.
        if (mBluetoothLeService != null) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeService.readAllCharacteristics(municService);
                }
            }, 500);
        }
    }
}