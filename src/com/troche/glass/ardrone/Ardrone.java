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
    private static final String TAG = "Ardrone";
    // ARDrone UDP connections
    private static final String ARDRONE_IP = "192.168.1.225";
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
        seq = 1;
        flatTrim();
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

    public void hover(){
        sendCommand("PCMD", ",0,0,0,0,0");
    }

    public void flatTrim(){
        sendCommand("FTRIM", ",");
    }

    // Low level AT Commands

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



    // Helper functions
    // Float.floatToIntBits(f);
    private String arrayToString(int[] array){
        StringBuilder result = new StringBuilder("");
        for(int i=0; i < array.length; i++){
            result.append(",");
            result.append(array[i]);
        }
        return result.toString();
    }

    private void sendCommand(String command, String params){
        String atCommand = "AT*" + command + "=" + (seq++) + params + "\r";
        new UdpPacketSenderTask().execute(ardroneInetAddress, COMMANDS_PORT, atCommand);
    }

}
