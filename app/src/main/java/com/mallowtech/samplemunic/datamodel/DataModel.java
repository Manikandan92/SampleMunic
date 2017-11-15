package com.mallowtech.samplemunic.datamodel;

/**
 * Created by manikandan on 16/06/17.
 */
public class DataModel {

    String UUID, value;

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public DataModel(String UUID, String value) {
        this.UUID = UUID;
        this.value = value;
    }
}
