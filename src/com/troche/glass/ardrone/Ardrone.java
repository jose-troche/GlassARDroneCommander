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

        atPcmd(droneRoll, dronePitch, droneVerticalSpeed, droneYaw);
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

    /*
    ANIMATION
    Note : The MAYDAY_TIMEOUT array contains defaults durations for each flight animations. Note : The FLIP
animations are only available on AR.Drone 2.0
AT command example : AT*CONFIG=605,"control:flight_anim","3,2"
API use example :
char param[20];
snprintf (param, sizeof (param), "%d,%d", ARDRONE_ANIMATION_FLIP_LEFT, MAYDAY_TIMEOUT[ARDRONE_-
ANIMATION_FLIP_LEFT]);
     */
    // From ARDrone_SDK_2_0_1/ARDroneLib/Soft/Common/navdata_common.h


    /* Timeout for mayday maneuvers*/
//    static const int32_t MAYDAY_TIMEOUT[ARDRONE_NB_ANIM_MAYDAY] = {
//        1000,  // ARDRONE_ANIM_PHI_M30_DEG
//                1000,  // ARDRONE_ANIM_PHI_30_DEG
//                1000,  // ARDRONE_ANIM_THETA_M30_DEG
//                1000,  // ARDRONE_ANIM_THETA_30_DEG
//                1000,  // ARDRONE_ANIM_THETA_20DEG_YAW_200DEG
//                1000,  // ARDRONE_ANIM_THETA_20DEG_YAW_M200DEG
//                5000,  // ARDRONE_ANIM_TURNAROUND
//                5000,  // ARDRONE_ANIM_TURNAROUND_GODOWN
//                2000,  // ARDRONE_ANIM_YAW_SHAKE
//                5000,  // ARDRONE_ANIM_YAW_DANCE
//                5000,  // ARDRONE_ANIM_PHI_DANCE
//                5000,  // ARDRONE_ANIM_THETA_DANCE
//                5000,  // ARDRONE_ANIM_VZ_DANCE
//                5000,  // ARDRONE_ANIM_WAVE
//                5000,  // ARDRONE_ANIM_PHI_THETA_MIXED
//                5000,  // ARDRONE_ANIM_DOUBLE_PHI_THETA_MIXED
//                15,  // ARDRONE_ANIM_FLIP_AHEAD
//                15,  // ARDRONE_ANIM_FLIP_BEHIND
//                15,  // ARDRONE_ANIM_FLIP_LEFT
//                15,  // ARDRONE_ANIM_FLIP_RIGHT
//    };

}
