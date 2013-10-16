/*
 * Created by Jose Troche on 10/5/13.
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

package com.troche.glass.ardrone;

import android.app.ActionBar;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * This is the main Activity that displays and sends sensor data
 */
public class MainActivity extends Activity implements
        SensorEventListener, TextToSpeech.OnInitListener {

    // Debugging
    private static final String TAG = "ARDroneCommanderMainActivity";
    private static final boolean D = true;

    // ARDrone object
    private Ardrone ardrone;

    // Sensor data
    private SensorManager mSensorManager;
    private Sensor mSensorRotation;
    private Sensor mSensorGyroscope;
    private Float mInitialHeading = null;
    private int mHeadingCount = 0;
    private float[] mRotationMatrix;
    private float[] mOrientation;
    private float mHeading = 0;
    private float mPitch = 0;
    private float mRoll = 0;
    private float mYawSpeed = 0;

    // Text to Speech
    private TextToSpeech mSpeech;

    // Layout Views
    private TextView mTextSensorData;
    private TextView mTextOutput;
    private TextView mTextInput;
    private ToggleButton mTakeoffToggle;
    private ToggleButton mElevationToggle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        ardrone = new Ardrone();

        // Set up the window layout
        setContentView(R.layout.main);

        // Do not allow to sleep
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Toggle Buttons
        mTakeoffToggle = (ToggleButton) findViewById(R.id.takeoff_toggle);
        mElevationToggle = (ToggleButton) findViewById(R.id.elevation_toggle);

        // Sensor init
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mSensorRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mRotationMatrix = new float[16];
        mOrientation = new float[3];

        // Text to Speech init
        mSpeech = new TextToSpeech(this, this);

        // Initialize text views
        mTextSensorData = (TextView) findViewById(R.id.text_sensor_data);
        mTextOutput = (TextView) findViewById(R.id.text_output);
        mTextInput = (TextView) findViewById(R.id.text_input);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");
        startSensorTracking();
        showWifiSSID();
    }

    @Override
    public synchronized void onPause() {
        speak(R.string.voice_bye);
        super.onPause();
        stopSensorTracking();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onDestroy() {
        ardrone.land();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mSpeech.shutdown();
        super.onDestroy();

        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(subTitle);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Hide items if Ardrone took off
        if (mTakeoffToggle.isChecked()) return false;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reset_emergency:
                ardrone.reset();
                return true;
            case R.id.flat_trim:
                ardrone.flatTrim();
                return true;
        }
        return false;
    }

    /**
     * Displays the Wifi SSID info in status bar
     */
    private void showWifiSSID(){
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        if (ssid != null){
            setStatus(getString(R.string.title_connected_to, ssid));
        }
        else {
            setStatus(getString(R.string.title_not_connected));
        }
    }

    /**
     * Sets the high level command being sent to the Ardrone
     */
    private void setCommandText(String text) {
        mTextOutput.setText(text);
    }

    public void onTakeoffToggleClicked(View view) {
        boolean on = ((ToggleButton) view).isChecked();

        if (on) {
            setCommandText("Takeoff");
            ardrone.takeoff();
            speak(R.string.voice_takeoff);

        } else {
            setCommandText("Land");
            ardrone.land();
            speak(R.string.voice_land);
        }

        invalidateOptionsMenu();
    }

    public void onElevationToggleClicked(View view) {
        boolean on = ((ToggleButton) view).isChecked();
        speak(on ? R.string.voice_elevation_on : R.string.voice_elevation_off);
    }

    public void onFlipButtonClicked(View view){
        ardrone.flipLeft();
    }

    private void speak(int voiceCommandId){
        mSpeech.speak(getString(voiceCommandId), TextToSpeech.QUEUE_FLUSH, null);
    }


    private void startSensorTracking(){
        // Reset initial heading
        mInitialHeading = null;
        // Start listening to sensor data
        mSensorManager.registerListener(this, mSensorRotation, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorGyroscope, SensorManager.SENSOR_DELAY_UI);
    }

    private void stopSensorTracking(){
        // Stop listening to sensor data
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onInit(int status) {
        // Called when the text-to-speech engine is initialized. Nothing to do here.
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Called when sensor accuracy changes. Nothing to do here.
    }

    public void onSensorChanged(SensorEvent event) {
        String sensorData;

        switch (event.sensor.getType()){
            case Sensor.TYPE_GYROSCOPE:
                mYawSpeed = -event.values[1];
                break;

            case Sensor.TYPE_ROTATION_VECTOR:
                SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
                SensorManager.remapCoordinateSystem(mRotationMatrix,
                        SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationMatrix);
                SensorManager.getOrientation(mRotationMatrix, mOrientation);

                toDegrees(mOrientation);

                if (mHeadingCount < 100) {
                    mInitialHeading = mOrientation[0];
                    mHeadingCount++;
                }

                mHeading = mOrientation[0]; // - mInitialHeading;
                mPitch = -mOrientation[1];
                mRoll = mOrientation[2];
                break;
        }

        sensorData = String.format("Pitch: %+03.0f  Roll: %+03.0f  YawSpeed: %+01.2f",
                mPitch, mRoll, mYawSpeed);

        mTextSensorData.setText(sensorData);

        ardrone.move(mRoll, mPitch, mYawSpeed, mElevationToggle.isChecked());

        mTextInput.setText("Flash Drive? " + ardrone.navdata.isFlashDriveReady() +
                ". Battery: " + ardrone.navdata.batteryPercentage + "%" +
                ".\nReceiving Data? " + ardrone.navdata.isReceivingData);
    }

    /**
     * Converts an array of float radians into degrees
     * @param v the array to be converted
     */
    private void toDegrees(float [] v){
        for (int i=0; i<v.length; i++) v[i]=(float)Math.toDegrees(v[i]);
    }

}
