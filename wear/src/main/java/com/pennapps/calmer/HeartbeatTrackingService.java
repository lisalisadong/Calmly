package com.pennapps.calmer;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.Random;

/**
 * Created by QingxiaoDong on 1/23/16.
 */
public class HeartbeatTrackingService extends WearableListenerService implements SensorEventListener {
    private SensorManager mSensorManager;
    private int currentValue=0;
    private static final String LOG_TAG = "CalmerWearService";
    //private IBinder binder = new HeartbeatServiceBinder();
    //private OnChangeListener onChangeListener;
    private GoogleApiClient mGoogleApiClient;
    private int counter = 0;

    /*
    // interface to pass a heartbeat value to the implementing class
    public interface OnChangeListener {
        void onValueChanged(int newValue);
    }
    */

    /**
     * Binder for this service. The binding activity passes a listener we send the heartbeat to.
     */
    /*
    public class HeartbeatServiceBinder extends Binder {
        public void setChangeListener(OnChangeListener listener) {
            onChangeListener = listener;
            // return currently known value
            listener.onValueChanged(currentValue);
        }

    }
    */

//    @Override
//    public IBinder onBind(Intent intent) {
//        return binder;
//    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //super.onStartCommand(intent, flags, startId);
        // register us as a sensor listener
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        // delay SENSOR_DELAY_UI is sufficient
        boolean res = mSensorManager.registerListener(this, mHeartRateSensor,  SensorManager.SENSOR_DELAY_UI);
        Log.d(LOG_TAG, " sensor registered: " + (res ? "yes" : "no"));
        //mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();
        return Service.START_STICKY;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(LOG_TAG, "onConnected: " + connectionHint);
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(LOG_TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(LOG_TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();

        // return Service.START_STICKY;
//        // register us as a sensor listener
//        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//        Sensor mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
//        // delay SENSOR_DELAY_UI is sufficiant
//        boolean res = mSensorManager.registerListener(this, mHeartRateSensor,  SensorManager.SENSOR_DELAY_UI);
//        Log.d(LOG_TAG, " sensor registered: " + (res ? "yes" : "no"));
//        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();
//        mGoogleApiClient.connect();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        mGoogleApiClient.disconnect();
        Log.d(LOG_TAG, " sensor unregistered");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // is this a heartbeat event and does it have data?
        if(sensorEvent.sensor.getType()==Sensor.TYPE_HEART_RATE && sensorEvent.values.length>0 ) {
            //Log.d(LOG_TAG, "length of sensor value: " + sensorEvent.values.length);
            Random random = new Random();
            int newValue = Math.round(sensorEvent.values[0]);
            newValue = newValue - 15 + random.nextInt(5);
            //int newValue = random.nextInt(10) + 85;
            //Log.d(LOG_TAG,sensorEvent.sensor.getName() + " changed to: " + newValue);
            // only do something if the value is not 0.
            if(currentValue != newValue && newValue>0 && counter % 10 == 0) {
                // save the new value
                currentValue = newValue;
                Log.d(LOG_TAG,"grabbed value: " + newValue);
                sendMessageToHandheld(Integer.toString(newValue));

                /*
                // send the value to the listener
                if(onChangeListener!=null) {
                    Log.d(LOG_TAG,"sending new value to listener: " + newValue);
                    onChangeListener.onValueChanged(newValue);
                    sendMessageToHandheld(Integer.toString(newValue));
                }
                */
            }
            counter++;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    /**
     * sends a string message to the connected handheld using the google api client (if available)
     * @param message
     */
    private void sendMessageToHandheld(final String message) {

        if (mGoogleApiClient == null)
            return;


        // use the api client to send the heartbeat value to our handheld
        final PendingResult<NodeApi.GetConnectedNodesResult> nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        nodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                final List<Node> nodes = result.getNodes();
                if (nodes != null) {
                    for (int i=0; i<nodes.size(); i++) {
                        final Node node = nodes.get(i);
                        Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), message, null);

                        Log.d(LOG_TAG, "sending a message to handheld: " + node.getId());
                    }
                }
            }
        });

    }

}
