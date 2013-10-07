package com.troche.glass.ardrone;

/**
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

import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Represents the ARDrone quadcopter and includes methods (commands) to fly it
 *
 * For a reference of the commands read the ARDrone Developer Guide
 */
public class Ardrone {
    // Debug
    private static final String TAG = "Ardrone";
    private static final boolean D = false;

    // Glass Sensor constants
    private static final int HEADING_THRESHOLD = 5;
    private static final int PITCH_THRESHOLD = 5;
    private static final int ROLL_THRESHOLD = 5;
    private static final int ROLL_MAX_VALUE = 90;
    private static final int PITCH_MAX_VALUE = 90;
    private static final int ALTITUDE_MAX_VALUE = 70;

    // ARDrone UDP connections
    private static final String ARDRONE_IP = "192.168.1.1";
    private static final Integer COMMANDS_PORT = 5556;
    private static final Integer NAVDATA_PORT = 5554;
    private InetAddress ardroneInetAddress;

    private static int seq = 1; // The Sequence Number for commands

    public Ardrone() {
        seq = 1;
        try {
            ardroneInetAddress = InetAddress.getByName(ARDRONE_IP);
        } catch (UnknownHostException e) {
            Log.e(TAG, "Error when initializing Ardrone InetAddress", e);
        }
    }

    public void takeoff(){
        //flatTrim();
        atRef(true);
    }

    public void land(){
        atRef(false);
    }

    /**
     * Gets sensor data from Google Glass and transforms it to ARDrone flying data
     * @param roll
     * @param pitch
     * @param yaw
     */
    public void move(float roll, float pitch, float yaw, boolean isInElevationMode){
        float droneRoll, dronePitch, droneVerticalSpeed, droneYaw;

        droneRoll = dronePitch = droneVerticalSpeed = droneYaw = 0f;

        if (Math.abs(roll) > ROLL_THRESHOLD){
            droneRoll = roll / ROLL_MAX_VALUE;
        }

        if (Math.abs(pitch) > PITCH_THRESHOLD){
            if (isInElevationMode){
                droneVerticalSpeed = pitch / ALTITUDE_MAX_VALUE;
            }
            else {
                dronePitch = pitch / PITCH_MAX_VALUE;
            }
        }

        move(droneRoll, dronePitch, droneVerticalSpeed, droneYaw);
    }

    private void move(float roll, float pitch, float verticalSpeed, float yaw){
        if (roll==0f && pitch==0f && verticalSpeed==0f && yaw==0f){
            hover();
        }
        else{
            String params = arrayToString(new float[]{roll, pitch, verticalSpeed, yaw});
            sendCommand("PCMD", ",1"+params);
        }
    }

    // Reset emergency mode
    public void reset(){
        atRef(false, true);
        atRef(false, false);
    }

    public void hover(){
        sendCommand("PCMD", ",0,0,0,0,0");
    }

    public void flatTrim(){
        sendCommand("FTRIM", ",");
    }

    /**
     * takeoffFlag   -- True: Takeoff / False: Land
     * emergencyFlag -- True: Turn off the engines
     */
    private void atRef(boolean takeoffFlag, boolean emergencyFlag){
        int param = 0x11540000;
        if (takeoffFlag) param += 0x200;
        if (emergencyFlag) param += 0x100;
        sendCommand("REF", "," + param);
    }

    private void atRef(boolean takeoffFlag){
        atRef(takeoffFlag, false);
    }

    private void sendCommand(String command, String params){
        String atCommand = "AT*" + command + "=" + (seq++) + params + "\r";
        new UdpPacketSenderTask().execute(ardroneInetAddress, COMMANDS_PORT, atCommand);
    }

    private String arrayToString(float[] array){
        StringBuilder result = new StringBuilder("");
        for(int i=0; i < array.length; i++){
            result.append(",");
            result.append(Float.floatToIntBits(array[i]));
        }
        return result.toString();
    }
}
