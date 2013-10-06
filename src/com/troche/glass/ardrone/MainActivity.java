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
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
    private static final String TAG = "ARDroneCommander";
    private static final boolean D = true;

    // ARDrone object
    private Ardrone ardrone;

    // Sensor data
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Float mInitialHeading;
    private float[] mRotationMatrix;
    private float[] mOrientation;

    // Text to Speech
    private TextToSpeech mSpeech;

    // Sensor constants
    private static final int HEADING_THRESHOLD_POS = 15;
    private static final int HEADING_THRESHOLD_NEG = -15;
    private static final int PITCH_THRESHOLD_POS = 15;
    private static final int PITCH_THRESHOLD_NEG = -15;
    private static final int ROLL_THRESHOLD_POS = 15;
    private static final int ROLL_THRESHOLD_NEG = -15;

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
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
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
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        stopSensorTracking();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setCommandText("Quit\n");
        speak(R.string.voice_bye);
        mSpeech.shutdown();

        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(resId);
        }
    }

    private void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(subTitle);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent;
        switch (item.getItemId()) {
            case R.id.insecure_connect_scan:
                //startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
        }
        return false;
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
    }

    public void onElevationToggleClicked(View view) {
        boolean on = ((ToggleButton) view).isChecked();
        speak(on ? R.string.voice_elevation_on : R.string.voice_elevation_off);
    }

    private void speak(int voiceCommandId){
        mSpeech.speak(getString(voiceCommandId), TextToSpeech.QUEUE_FLUSH, null);
    }


    private void startSensorTracking(){
        // Start listening to sensor data
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
    }

    private void stopSensorTracking(){
        // Stop listening to sensor data
        mSensorManager.unregisterListener(this);

        // Reset initial heading
        mInitialHeading = null;
    }

    @Override
    public void onInit(int status) {
        // Called when the text-to-speech engine is initialized. Nothing to do here.
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Called when sensor accuracy changes. Nothing to do here.
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) return;

        String sensorData;
        float heading, pitch, roll;
        boolean commandSent = true;

        SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
        SensorManager.remapCoordinateSystem(mRotationMatrix,
                SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationMatrix);
        SensorManager.getOrientation(mRotationMatrix, mOrientation);

        toDegrees(mOrientation);

        if (mInitialHeading == null) {mInitialHeading = mOrientation[0];}
        heading = mOrientation[0];
        pitch = -mOrientation[1];
        roll = mOrientation[2];

        sensorData = String.format("Pitch: %+03.0f  Roll: %+03.0f  Heading: %+03.0f",
                pitch, roll, heading);

        mTextSensorData.setText(sensorData);

        if (triggerCommand(pitch, PITCH_THRESHOLD_POS)){
            if (mElevationToggle.isChecked()){
                setCommandText("Up");
            }
            else {
                setCommandText("Backward");
            }
        }
        else if (triggerCommand(pitch, PITCH_THRESHOLD_NEG)){
            if (mElevationToggle.isChecked()){
                setCommandText("Down");
            }
            else {
                setCommandText("Forward");
            }
        }
        else{
            commandSent = false;
        }

        if (triggerCommand(roll, ROLL_THRESHOLD_POS)){
            setCommandText("Right");
        }
        else if (triggerCommand(roll, ROLL_THRESHOLD_NEG)){
            setCommandText("Left");
        }
        else if (!commandSent){
            setCommandText("None");
            ardrone.hover();
        }
    }

    /**
     * Determines if a command should be triggered depending on the sensor values
     **/
    private boolean triggerCommand(float value, int threshold){
        return threshold > 0 ? value > threshold : value < threshold;
    }

    /**
     * Converts an array of float radians into degrees
     * @param v the array to be converted
     */
    private void toDegrees(float [] v){
        for (int i=0; i<v.length; i++) v[i]=(float)Math.toDegrees(v[i]);
    }

}
