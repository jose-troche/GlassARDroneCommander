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
    private static final float YAW_THRESHOLD = 0.6f;
    private static final float YAW_MAX_VALUE = 1f;
    private static final int ROLL_THRESHOLD = 5;
    private static final int ROLL_MAX_VALUE = 90;
    private static final int PITCH_THRESHOLD = 5;
    private static final int PITCH_MAX_VALUE = 90;
    private static final int ALTITUDE_MAX_VALUE = 70;

    // ARDrone UDP connections
    public static final String ARDRONE_IP = "192.168.1.1";
    private static final Integer COMMANDS_PORT = 5556;

    private InetAddress ardroneInetAddress;

    // ARDrone navdata
    Navdata navdata;

    private static int seq = 1; // The Sequence Number for commands

    public Ardrone() {
        seq = 1;
        try {
            ardroneInetAddress = InetAddress.getByName(ARDRONE_IP);
            setConfig("general:navdata_demo", "TRUE");
            setConfig("video:video_on_usb", "TRUE");
            navdata = new Navdata();
        } catch (UnknownHostException e) {
            Log.e(TAG, "Error when initializing Ardrone InetAddress", e);
        }
    }

    public void takeoff(){
        atRef(true);
    }

    public void land(){
        atRef(false);
    }

    // Reset emergency mode
    public void reset(){
        atRef(false, true);
        atRef(false, false);
    }

    public void flatTrim(){
        //sendCommand("FTRIM", ",");
        toggleVideoRecording();
    }

    private static boolean isRecording = false;
    public void toggleVideoRecording(){
        if (!isRecording){
            setConfig("video:video_codec", "130");
            isRecording = true;
        }
        else{
            setConfig("video:video_codec", "0");
            isRecording = false;
        }
    }

    public void setConfig(String key, String value){
        sendCommand("CONFIG", "," + '"' + key + '"' + "," + '"' + value + '"' );
    }

    public void hover(){
        sendCommand("PCMD", ",0,0,0,0,0");
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

        if (Math.abs(pitch) > PITCH_THRESHOLD){
            if (isInElevationMode){
                droneVerticalSpeed = pitch / ALTITUDE_MAX_VALUE;
            }
            else {
                dronePitch = pitch / PITCH_MAX_VALUE;
            }
        }

        if (Math.abs(roll) > ROLL_THRESHOLD){
            droneRoll = roll / ROLL_MAX_VALUE;
        }

        if (Math.abs(yaw) > YAW_THRESHOLD){
            droneYaw = (yaw - Math.signum(yaw)*YAW_THRESHOLD);// / YAW_MAX_VALUE;
        }

        atPcmd(droneRoll, dronePitch, droneVerticalSpeed, droneYaw);
    }

    public void flipLeft(){
        animate(Animation.FLIP_LEFT);
    }

    private void atPcmd(float roll, float pitch, float verticalSpeed, float yaw){
        if (roll==0f && pitch==0f && verticalSpeed==0f && yaw==0f){
            hover();
        }
        else{
            String params = arrayToString(new float[]{roll, pitch, verticalSpeed, yaw});
            sendCommand("PCMD", ",1"+params);
        }
    }

    private void atPcmdMag(float roll, float pitch, float verticalSpeed, float yaw){
        if (roll==0f && pitch==0f && verticalSpeed==0f && yaw==0f){
            hover();
        }
        else{
            String params = arrayToString(new float[]{roll, pitch, verticalSpeed, yaw, yaw, 5/360});
            sendCommand("PCMD_MAG", ",7"+params);
        }
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

    // Animation AT command example : AT*CONFIG=seq#,"control:flight_anim","3,2"
    // The parameter is a string containing the animation number/code and its duration timeout,
    // separated with a comma.
    private void animate(Animation animation){
        int animationCode = animation.ordinal();
        setConfig("control:flight_anim", animationCode + "," + AnimationTimeouts[animationCode]);
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

    // From ARDrone_SDK_2_0_1/ARDroneLib/Soft/Common/config.h
    private static enum Animation {
        PHI_M30_DEG,
        PHI_30_DEG,
        THETA_M30_DEG,
        THETA_30_DEG,
        THETA_20DEG_YAW_200DEG,
        THETA_20DEG_YAW_M200DEG,
        TURNAROUND,
        TURNAROUND_GODOWN,
        YAW_SHAKE,
        YAW_DANCE,
        PHI_DANCE,
        THETA_DANCE,
        VZ_DANCE,
        WAVE,
        PHI_THETA_MIXED,
        DOUBLE_PHI_THETA_MIXED,
        FLIP_AHEAD,
        FLIP_BEHIND,
        FLIP_LEFT,
        FLIP_RIGHT
    }

    // From ARDrone_SDK_2_0_1/ARDroneLib/Soft/Common/navdata_common.h
    private static final int[] AnimationTimeouts = {
        1000,  // PHI_M30_DEG
        1000,  // PHI_30_DEG
        1000,  // THETA_M30_DEG
        1000,  // THETA_30_DEG
        1000,  // THETA_20DEG_YAW_200DEG
        1000,  // THETA_20DEG_YAW_M200DEG
        5000,  // TURNAROUND
        5000,  // TURNAROUND_GODOWN
        2000,  // YAW_SHAKE
        5000,  // YAW_DANCE
        5000,  // PHI_DANCE
        5000,  // THETA_DANCE
        5000,  // VZ_DANCE
        5000,  // WAVE
        5000,  // PHI_THETA_MIXED
        5000,  // DOUBLE_PHI_THETA_MIXED
        15,  // FLIP_AHEAD
        15,  // FLIP_BEHIND
        15,  // FLIP_LEFT
        15  // FLIP_RIGHT
    };
}
