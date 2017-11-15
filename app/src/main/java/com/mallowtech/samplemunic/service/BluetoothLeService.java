/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mallowtech.samplemunic.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.List;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    public final static String ACTION_GATT_CONNECTED = BluetoothLeService.class.getName() + ".ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_CONNECTING = BluetoothLeService.class.getName() + ".ACTION_GATT_CONNECTING";
    public final static String ACTION_GATT_DISCONNECTED = BluetoothLeService.class.getName() + ".ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = BluetoothLeService.class.getName() + ".ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = BluetoothLeService.class.getName() + ".ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA_RAW = BluetoothLeService.class.getName() + ".EXTRA_DATA_RAW";
    public final static String EXTRA_UUID_CHAR = BluetoothLeService.class.getName() + ".EXTRA_UUID_CHAR";
    private final IBinder mBinder = new LocalBinder();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private State mConnectionState = State.DISCONNECTED;
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            broadcastUpdateCharacterstic(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic,
                                         final int status) {
            Log.w("Testing : ", "onCharacteristicRead STATUS: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdateCharacterstic(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt,
                                            final int status,
                                            final int newState) {

            Log.d("Testing : ", "onConnectionStateChange: status=" + status + ", newState=" + newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) { // status 2 is Connected the bluetooth
                setConnectionState(State.CONNECTED, true);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Attempts to discover services after successful connection.
                        Log.i("Testing : ", "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
                    }
                }, 500);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {  // status 0 is Disconnected the bluetooth
                Log.i("Testing : ", "Disconnected from GATT server.");
                // Make sure we tidy up. On certain devices reusing a Gatt after a disconnection
                // can cause problems.
                disconnect();
                setConnectionState(State.DISCONNECTED, true);
                Log.i("Testing : ", "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            Log.w("Testing : ", "onServicesDiscovered : STATUS " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdateCharacterstic(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_UUID_CHAR, characteristic.getUuid().toString());
        // Always try to add the RAW value
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            intent.putExtra(EXTRA_DATA_RAW, data);
        }
        sendBroadcast(intent);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        final boolean retVal;
        if (mBluetoothAdapter == null || address == null) {
            Log.w("Testing : ", "BluetoothAdapter not initialized or unspecified address.");
            retVal = false;

            // Previously connected device.  Try to reconnect.
        } else if (mBluetoothDeviceAddress != null
                && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {

            Log.d("Testing : ", "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                Log.d("Testing : ", "Connection attempt OK.");
                setConnectionState(State.CONNECTING, true);
                retVal = true;
            } else {
                Log.w("Testing : ", "Connection attempt failed.");
                setConnectionState(State.DISCONNECTED, true);
                retVal = false;
            }
        } else {

            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            if (device == null) {
                Log.w("Testing : ", "Device not found.  Unable to connect.");
                retVal = false;
            } else {
                // We want to directly connect to the device, so we are setting the autoConnect
                // parameter to false.

                Log.d("Testing : ", "Trying to create a new connection.");
                mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
                mBluetoothDeviceAddress = address;
                setConnectionState(State.CONNECTING, true);
                retVal = true;
            }
        }

        return retVal;
    }

    private synchronized void setConnectionState(final State newState, final boolean broadCast) {
        Log.i("Testing : ", "setConnectionState : newState " + newState);
        mConnectionState = newState;
        final String broadcastAction;
        switch (newState) {
            case CONNECTED:
                broadcastAction = ACTION_GATT_CONNECTED;
                break;
            case CONNECTING:
                broadcastAction = ACTION_GATT_CONNECTING;
                break;
            case DISCONNECTED:
                broadcastAction = ACTION_GATT_DISCONNECTED;
                break;
            default:
                throw new IllegalArgumentException("Unknown state: " + newState);
        }

        if (broadCast) {
            Log.i("Testing : ", "Broadcasting " + broadcastAction);
            broadcastUpdate(broadcastAction);
        }

    }

    private synchronized void readAllCharState(BluetoothGattCharacteristic characteristic) {
        int i = 0;
        i++;
        Log.d("Testing : ", "readAllCharState: " + i + " : " + true);
        readCharacteristic(characteristic);
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w("Testing : ", "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        // Reusing a Gatt after disconnecting can cause problems
        mBluetoothGatt = null;
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e("Testing : ", "Unable to initialize BluetoothManager.");
                return false;
            } else {
                Log.d("Testing : ", "initialize BluetoothManager.");
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e("Testing : ", "Unable to obtain a BluetoothAdapter.");
            return false;
        } else {
            Log.d("Testing : ", "initialize BluetoothAdapter.");
        }

        return true;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(final BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w("Testing : ", "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void readAllCharacteristics(final BluetoothGattService services) {
        int count = 0;
        if (services != null) {
            Log.d("Testing : ", "run:Reading all characteristics... " + true);
            Log.d("Testing : ", "run: service UUID " + count + ": " + services.getUuid());
            for (final BluetoothGattCharacteristic characteristic : services.getCharacteristics()) {
                Log.d("Testing : ", "run: characteristic UUID" + count + ": " + characteristic.getUuid());
                Log.d("Testing : ", "run: characteristic Values" + count + ": " + characteristic.getValue());
                try {
                    readAllCharState(characteristic);
                    count++;
                } catch (Exception e) {
                    Log.d("Testing : ", "readAllCharacteristics: " + e.getLocalizedMessage());
                }
            }
        }
        Log.d("Testing : ", "run: " + count);
    }

//    public void readAllCharacteristics(final BluetoothGattCharacteristic locationGattCharacteristic, final BluetoothGattCharacteristic speedGattCharacteristic) {
//        new Thread(new Runnable() {
//            public void run() {
//                try {
//                    if (locationGattCharacteristic != null) {
//                        Log.d("readAllCharacteristics", "run:Local UUID " + locationGattCharacteristic.getUuid());
//                        if (locationGattCharacteristic.getValue() != null) {
//                            Log.d("readAllCharacteristics", "run:Local  " + new String(locationGattCharacteristic.getValue()));
//                        } else {
//                            Log.d("readAllCharacteristics", "run:Local  " + null);
//                        }
//                        readCharacteristic(locationGattCharacteristic);
//                    }
//                } catch (Exception e) {
//                    Log.d(TAG, "readAllCharacteristics: " + e.getLocalizedMessage());
//                }
//            }
//        }).start();
//
//        new Thread(new Runnable() {
//            public void run() {
//                try {
//                    if (speedGattCharacteristic != null) {
//                        Log.d("readAllCharacteristics", "run:Speed UUID " + speedGattCharacteristic.getUuid());
//                        if (speedGattCharacteristic.getValue() != null) {
//                            Log.d("readAllCharacteristics", "run:Local  " + new String(speedGattCharacteristic.getValue()));
//                        } else {
//                            Log.d("readAllCharacteristics", "run:Local  " + null);
//                        }
//                        readCharacteristic(speedGattCharacteristic);
//                    }
//                } catch (Exception e) {
//                    Log.d(TAG, "readAllCharacteristics: " + e.getLocalizedMessage());
//                }
//            }
//        }).start();
//    }
//
//
//    /**
//     * Enables or disables notification on a give characteristic.
//     *
//     * @param characteristic Characteristic to act on.
//     * @param enabled        If true, enable notification.  False otherwise.
//     */
//    public void setCharacteristicNotification(final BluetoothGattCharacteristic characteristic, final boolean enabled) {
//        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.w(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
//        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
//    }


    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    private enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}