package com.mallowtech.samplemunic.datamodel;

/**
 * Created by manikandan on 16/06/17.
 */
public class ScanDeviceModel {

    String macAddress, deviceName;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public ScanDeviceModel(String deviceName, String deviceUUID) {
        this.deviceName = deviceName;
        this.macAddress = deviceUUID;
    }
}
