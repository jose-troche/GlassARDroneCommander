package com.troche.glass.ardrone;

/**
 * Created by Jose Troche on 10/12/2013.
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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Navdata associated with the ARDrone
 */
public class Navdata {
    // Debugging
    private static final String TAG = "Navdata";
    private static final boolean D = true;

    public boolean isReceivingData = false;

    private static final Integer NAVDATA_PORT = 5554;
    private static final short NAVDATA_MAX_SIZE = 4096;
    private static final int NAVDATA_HEADER = 0x55667788;

    // Basic info
    public int state;
    public int sequence;
    public int visionFlag;

    // Demo navdata
    public short flyState;
    public short controlState;
    public int batteryPercentage;
    public float pitch;  // Pitch in milli-degrees
    public float roll;    // Roll in milli-degrees
    public float yaw;    // Yaw in milli-degrees
    public int altitude; // cm
    public int velocityX;
    public int velocityY;
    public int velocityZ;

    private final NavdataReaderThread navdataReaderThread;

    public Navdata(){
        navdataReaderThread = new NavdataReaderThread();
        navdataReaderThread.start();
    }

    public synchronized void destroy(){
        navdataReaderThread.cancel();
    }

    private class NavdataReaderThread extends Thread {
        private static final long SLEEP = 1000 * 10;
        private boolean keepRunning;
        private DatagramSocket socket;

        public NavdataReaderThread() {
            keepRunning = true;

            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(1);
            } catch (SocketException e) {
                Log.e(TAG, "Navdata socket was unable to be initialized.", e);
                socket = null;
            }
        }

        public void run(){
            try{
                InetAddress ardroneInetAddress = InetAddress.getByName(Ardrone.ARDRONE_IP);

                byte[] outBuf = {0x01, 0x00, 0x00, 0x00};
                DatagramPacket outPacket =
                        new DatagramPacket(outBuf, outBuf.length, ardroneInetAddress, NAVDATA_PORT);

                byte[] inBuf = new byte[NAVDATA_MAX_SIZE];
                DatagramPacket inPacket = new DatagramPacket(inBuf, inBuf.length);

                while (keepRunning){
                    try {
                        socket.send(outPacket);
                    } catch (IOException e) {
                        Log.e(TAG, "Error when sending data to ARDrone NAVDATA port ", e);
                        isReceivingData = false;
                        sleep(SLEEP);
                        continue;
                    }

                    try{
                        inBuf = new byte[NAVDATA_MAX_SIZE];
                        inPacket = new DatagramPacket(inBuf, inBuf.length);
                        socket.receive(inPacket);
                    }
                    catch (IOException e){
                        Log.e(TAG, "Error when receiving data from ARDrone NAVDATA port ", e);
                        isReceivingData = false;
                        continue;
                    }

                    parseRawNavdata(inPacket.getData());

                    sleep(SLEEP);
                }

            } catch (InterruptedException e) {
            } catch (UnknownHostException e) {
                Log.e(TAG, "Error when getting ardroneInetAddress in NavdataReaderThread ", e);
            } finally {
                if (socket != null) socket.close();
            }
        }

        public synchronized void cancel(){
            keepRunning = false;
        }
    }

    private synchronized void parseRawNavdata(byte [] inBuf){
        try{
            ByteBuffer rawNavdata = ByteBuffer.wrap(inBuf);
            if (ByteOrder.LITTLE_ENDIAN != rawNavdata.order()) {
                rawNavdata.order(ByteOrder.LITTLE_ENDIAN);
            }

            if (rawNavdata.getInt() != NAVDATA_HEADER){
                Log.e(TAG, "Wrong navdata header. Ignoring the rest.");
                return;
            }
            isReceivingData = true;

            state = rawNavdata.getInt();
            sequence = rawNavdata.getInt();
            if(D) Log.d(TAG, "Received navdata with seq: " + sequence);
            visionFlag = rawNavdata.getInt();

            short optionId, optionSize;
            byte[] optionData;

            do { // Loop through options
                optionId = rawNavdata.getShort();
                optionSize = rawNavdata.getShort();
                if(D) Log.d(TAG, "Option ID: " + optionId + ", Size: " + optionSize);
                if (optionSize <= 4) break;
                optionData = new byte[optionSize - 4];
                rawNavdata.get(optionData);
                if (optionId == 0){
                    parseDemoNavdata(optionData);
                }

            } while (optionId != 0);


        }
        catch (Exception e){
            Log.e(TAG, "Error when parsing navdata", e);
        }
    }

    private synchronized void parseDemoNavdata(byte[] optionData){
        try {
            ByteBuffer demoNavdata = ByteBuffer.wrap(optionData);
            if (ByteOrder.LITTLE_ENDIAN != demoNavdata.order()) {
                demoNavdata.order(ByteOrder.LITTLE_ENDIAN);
            }

            flyState = demoNavdata.getShort();
            controlState = demoNavdata.getShort();
            batteryPercentage = demoNavdata.getInt();
            pitch = demoNavdata.getFloat();
            roll = demoNavdata.getFloat();
            yaw = demoNavdata.getFloat();
            altitude = demoNavdata.getInt();
            velocityX = demoNavdata.getInt();
            velocityY = demoNavdata.getInt();
            velocityZ = demoNavdata.getInt();
        }
        catch (Exception e){
            Log.e(TAG, "Error when parsing optionData of navdata", e);
        }
    }

    public boolean isFlying(){
        return getStateFlag(StateFlag.FLY) == 1;
    }

    public boolean isFlashDriveReady(){
        return getStateFlag(StateFlag.USB) == 1;
    }

    public boolean isBatteryLow(){
        return getStateFlag(StateFlag.VBAT_LOW) == 1;
    }

    public boolean isInEmergencyMode(){
        return getStateFlag(StateFlag.EMERGENCY) == 1;
    }

    public int getStateFlag(StateFlag flag){
        return ( state >> flag.ordinal() ) & 1;
    }

    private enum StateFlag{
        FLY, /* FLY : (0) ardrone is landed, (1) ardrone is flying */
        VIDEO,  /* VIDEO : (0) video disable, (1) video enable */
        VISION,  /* VISION : (0) vision disable, (1) vision enable */
        CONTROL,  /* CONTROL ALGO : (0) euler angles control, (1) angular speed control */
        ALTITUDE,  /* ALTITUDE CONTROL ALGO : (0) altitude control inactive (1) altitude control active */
        USER_FEEDBACK_START,  /* USER feedback : Start button state */
        COMMAND_CONTROL_ACK,  /* Control command ACK : (0) None, (1) one received */
        CAMERA,  /* CAMERA : (0) camera not ready, (1) Camera ready */
        TRAVELLING,  /* Travelling : (0) disable, (1) enable */
        USB,  /* USB key : (0) usb key not ready, (1) usb key ready */
        NAVDATA_DEMO, /* Navdata demo : (0) All navdata, (1) only navdata demo */
        NAVDATA_BOOTSTRAP, /* Navdata bootstrap : (0) options sent in all or demo mode, (1) no navdata options sent */
        MOTORS, /* Motors status : (0) Ok, (1) Motors problem */
        COM_LOST, /* Communication Lost : (1) com problem, (0) Com is ok */
        SOFTWARE_FAULT, /* Software fault detected - user should land as quick as possible (1) */
        VBAT_LOW, /* VBat low : (1) too low, (0) Ok */
        USER_EL, /* User Emergency Landing : (1) User EL is ON, (0) User EL is OFF*/
        TIMER_ELAPSED, /* Timer elapsed : (1) elapsed, (0) not elapsed */
        MAGNETO_NEEDS_CALIB, /* Magnetometer calibration state : (0) Ok, no calibration needed, (1) not ok, calibration needed */
        ANGLES_OUT_OF_RANGE, /* Angles : (0) Ok, (1) out of range */
        WIND, /* WIND: (0) ok, (1) Too much wind */
        ULTRASOUND, /* Ultrasonic sensor : (0) Ok, (1) deaf */
        CUTOUT, /* Cutout system detection : (0) Not detected, (1) detected */
        PIC_VERSION, /* PIC Version number OK : (0) a bad version number, (1) version number is OK */
        ATCODEC_THREAD_ON, /* ATCodec thread ON : (0) thread OFF (1) thread ON */
        NAVDATA_THREAD_ON, /* Navdata thread ON : (0) thread OFF (1) thread ON */
        VIDEO_THREAD_ON, /* Video thread ON : (0) thread OFF (1) thread ON */
        ACQ_THREAD_ON, /* Acquisition thread ON : (0) thread OFF (1) thread ON */
        CTRL_WATCHDOG, /* CTRL watchdog : (1) delay in control execution (> 5ms), (0) control is well scheduled */
        ADC_WATCHDOG9, /* ADC Watchdog : (1) delay in uart2 dsr (> 5ms), (0) uart2 is good */
        COM_WATCHDOG, /* Communication Watchdog : (1) com problem, (0) Com is ok */
        EMERGENCY /* Emergency landing : (0) no emergency, (1) emergency */
    }

}