package com.daxiniot.bluetoothtemperature;

/**
 * Created by qicaiz on 3/18/2018.
 */

public class MyDevice {
    private String name;
    private String address;
    private boolean bonded;

    public MyDevice() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isBonded() {
        return bonded;
    }

    public void setBonded(boolean bonded) {
        this.bonded = bonded;
    }

    @Override
    public String toString() {
        if(bonded){
            return name+"  "+address+"  已配对";
        }
        return name+"  "+address+"  未配对";
    }
}
