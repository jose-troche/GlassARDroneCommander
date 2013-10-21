package com.troche.glass.ardrone;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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

/**
 * Asynchronous task to send UDP packets to a target
 * The parameters passed to execute are:
 * InetAddress inetAddress - target inetAddress
 * Integer port - target UDP port
 * String message - the payload of the packet
 */
public class UdpPacketSenderTask extends AsyncTask<Object, Void, Void> {
    // Debugging
    private static final String TAG = "UdpPacketSenderTask";
    private static final boolean D = false;

    @Override
    protected Void doInBackground(Object... params) {
        InetAddress inetAddress = (InetAddress) params[0];
        int port = (Integer) params[1];
        String message = (String) params[2];

        byte[] messageBytes;
        DatagramSocket socket = null;
        DatagramPacket packet;

        try {
            messageBytes = message.getBytes();
            socket = new DatagramSocket();
            packet = new DatagramPacket(
                    messageBytes, messageBytes.length, inetAddress, port);
            socket.send(packet);
            if(D) Log.e(TAG, "On port "+port+" sent "+message);
        } catch (IOException e) {
            if(D) Log.e(TAG, "Failed sending UDP packet", e);
        } finally {
            if (socket != null) socket.close();
        }
        return null;
    }
}
