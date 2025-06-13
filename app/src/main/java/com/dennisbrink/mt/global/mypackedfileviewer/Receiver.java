package com.dennisbrink.mt.global.mypackedfileviewer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Objects;

public class Receiver extends BroadcastReceiver implements IZipApplication {

    private IZipLibraryActivityListener zipLibraryActivityListener;

    // this will be the link to the activity and must be set from the activity itself. In order to do
    // so the activity must be of type IZipLibraryActivityListener. Using polymorphism the activity will
    // implement the interface and this it can be passes as a type IZipLibraryActivityListener. We also
    // force the activity to have the method we want to execute is the correct action comes in.
    public void setZipLibraryActivity(IZipLibraryActivityListener zipLibraryActivityListener) {
        this.zipLibraryActivityListener = zipLibraryActivityListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("DB1", "Receiver.class: (onReceive) Receiver reached with action " + intent.getAction());
        if(this.zipLibraryActivityListener != null){

            switch(Objects.requireNonNull(intent.getAction())){
                case DELETE_TOUCH_POINTS:
                    this.zipLibraryActivityListener.clearTouchPointSequence();
                    break;
                case DELETE_APP_DATA:
                    this.zipLibraryActivityListener.clearAllAppData();
                    break;
                case DELETE_EXTRA_DATA:
                    this.zipLibraryActivityListener.clearExtraData();
                    break;
                case DELETE_CACHED_DATA:
                    this.zipLibraryActivityListener.clearCachedData();
                    break;
                default:
                    break;
            }

        }
    }

}
