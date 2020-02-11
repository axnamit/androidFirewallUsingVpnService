package com.example.vpnwithoutndk;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class Singleton {
    private static final Singleton ourInstance = new Singleton();
    private MutableLiveData<Boolean> booleanMutableLiveData = new MutableLiveData<>();

    public static Singleton getInstance() {
        return ourInstance;
    }

    private Singleton() {
    }
    public void setNetBool(Boolean booleanMutableLiveData) {
        this.booleanMutableLiveData.setValue(booleanMutableLiveData);
    }

    public LiveData<Boolean> getNetBool() {
        return this.booleanMutableLiveData;
    }

}
