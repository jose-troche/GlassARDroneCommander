package com.troche.glass.ardrone;

/**
 * Created by Jose Troche on 10/14/2013.
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

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;


/**
 * Asynchronous task to read navdata from ARDrone
 * The parameter passed to execute are:
 * Navdata navdata - the navdata object that will be populated after parsing
 */
public class NavdataReaderTask extends AsyncTask<Object, Void, Void> {
    // Debugging
    private static final String TAG = "NavdataReaderTask";
    private static final boolean D = false;

    // Constants
    private static final Integer NAVDATA_PORT = 5554;
    private static final short NAVDATA_MAX_SIZE = 4096;
    private static final int NAVDATA_HEADER = 0x55667788;

    // The navdata object that will be updated with fresh data
    Navdata navdata;

    @Override
    protected Void doInBackground(Object... params) {
        navdata = (Navdata) params[0];

        short optionId, optionSize;
        byte[] optionData;

        DatagramChannel channel = null;

        try{
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.connect(new InetSocketAddress(Ardrone.ARDRONE_IP, NAVDATA_PORT));

            // The heartbeat to start and keep the channel connected
            ByteBuffer heartBeat = ByteBuffer.allocate(1);
            heartBeat.put((byte) 0x1);

            // The datagram packet received from the ARDrone
            ByteBuffer packet = ByteBuffer.allocate(NAVDATA_MAX_SIZE);
            if (ByteOrder.LITTLE_ENDIAN != packet.order()) {
                packet.order(ByteOrder.LITTLE_ENDIAN);
            }

            while(true){
                // Keep the channel open
                heartBeat.flip();
                channel.write(heartBeat);

                packet.clear();
                if(channel.read(packet) > 0){
                    packet.flip();

                    if (packet.getInt() != NAVDATA_HEADER){
                        Log.e(TAG, "Wrong navdata header. Skipping rest this set of data.");
                        continue;
                    }

                    navdata.state = packet.getInt();
                    navdata.sequence = packet.getInt();
                    navdata.visionFlag = packet.getInt();

                    do { // Loop through options
                        optionId = packet.getShort();
                        optionSize = packet.getShort();
                        optionData = new byte[optionSize - 4];
                        packet.get(optionData);
                        if (optionId == 0){
                            parseDemoNavdata(optionData);
                        }
                        if(D) Log.d(TAG, "Option ID: " + optionId + ", Size: " + optionSize);
                    } while (optionId != -1);
                }
            }
        }
        catch (IOException e){
            Log.e(TAG, "Error when reading navdata", e);
        }
        finally{
            navdata.isReceivingData = false;
            try{
                if (channel != null) channel.close();
            }
            catch(IOException e){}
        }

        return null;
    }

    private void parseDemoNavdata(byte[] data){
        ByteBuffer demoNavdata = ByteBuffer.wrap(data);
        if (ByteOrder.LITTLE_ENDIAN != demoNavdata.order()) {
            demoNavdata.order(ByteOrder.LITTLE_ENDIAN);
        }

        navdata.flyState = demoNavdata.getShort();
        navdata.controlState = demoNavdata.getShort();
        navdata.batteryPercentage = demoNavdata.getInt();
        navdata.pitch = demoNavdata.getFloat();
        navdata.roll = demoNavdata.getFloat();
        navdata.yaw = demoNavdata.getFloat();
        navdata.altitude = demoNavdata.getInt();
        navdata.velocityX = demoNavdata.getInt();
        navdata.velocityY = demoNavdata.getInt();
        navdata.velocityZ = demoNavdata.getInt();
    }
}
