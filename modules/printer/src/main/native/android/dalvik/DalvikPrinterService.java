/*
 * Copyright (c) 2022, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.helloandroid;

import static android.app.Activity.RESULT_OK;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class DalvikPrinterService {

    private static final String TAG = Util.TAG;
    private static final int REQUEST_ENABLE_BT = 10001;
    private static final UUID APPLICATION_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final Activity activity;
    private final boolean debug;
    private final BluetoothAdapter adapter;

    private final byte[] cc    = new byte[] {0x1B, 0x21, 0x00}; // 0- normal size text
    private final byte[] bb    = new byte[] {0x1B, 0x21, 0x08}; // 1- only bold text
    private final byte[] bb2   = new byte[] {0x1B, 0x21, 0x20}; // 2- bold with medium text
    private final byte[] bb3   = new byte[] {0x1B, 0x21, 0x10}; // 3- bold with large text
    private final byte[] CL_CF = new byte[] {0x0a, 0x0a};

    public DalvikPrinterService(Activity activity) {
        this.activity = activity;
        this.debug = Util.isDebug();

        adapter = BluetoothAdapter.getDefaultAdapter();
        if (!adapter.isEnabled()) {
            Log.v(TAG, "DalvikPrinter, BT adapter not enabled");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            IntentHandler intentHandler = new IntentHandler() {
                @Override
                public void gotActivityResult (int requestCode, int resultCode, Intent intent) {
                    if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
                        Log.v(TAG, "DalvikPrinter, BT adapter enabled");
                        findPairedDevices();
                    }
                }
            };

            if (activity == null) {
                Log.e(TAG, "Activity not found. This service is not allowed when "
                        + "running in background mode or from wearable");
                return;
            }
            Util.setOnActivityResultHandler(intentHandler);

            // A dialog will appear requesting user permission to enable Bluetooth
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            findPairedDevices();
        }
    }

    private void findPairedDevices() {
        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        if (pairedDevices.isEmpty()) {
            Log.v(TAG, "DalvikPrinter, no paired devices found");
            return;
        }
        for (BluetoothDevice device : pairedDevices) {
            if (debug) {
                Log.v(TAG, "DalvikPrinter, BTDevice found: " + device.getName() + ":" + device.getAddress());
            }
            detectedBTDevice(device.getName(), device.getAddress());
        }
    }

    private void print(final String message, final String address, final long timeout) {
        if (message == null || message.isEmpty()) {
            Log.e(TAG, "DalvikPrinter: Invalid message: message was null or empty");
            return;
        }
        if (address == null || address.isEmpty()) {
            Log.e(TAG, "DalvikPrinter: Invalid address: address was null or empty");
            return;
        }
        if (debug) {
            Log.d(TAG, "DalvikPrinter: Printing message: " + message + " to address: " + address);
        }

        Thread printThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final BluetoothDevice btDevice = adapter.getRemoteDevice(address);
                    final BluetoothSocket btSocket = btDevice.createRfcommSocketToServiceRecord(APPLICATION_UUID);
                    adapter.cancelDiscovery();

                    if (debug) {
                        Log.d(TAG, "DalvikPrinter: Connecting device: " + btDevice);
                    }
                    if (!btSocket.isConnected()) {
                        btSocket.connect();
                    }
                    if (debug) {
                        Log.d(TAG, "DalvikPrinter: Device is connected: " + btSocket.isConnected());
                    }

                    OutputStream os = null;
                    try {
                        os = btSocket.getOutputStream();
                        os.write(cc);
                        os.write(message.getBytes());
                        os.write(CL_CF);
                        os.flush();
                        try {
                            Thread.sleep(timeout);
                        } catch (InterruptedException ie) {
                        }
                    } finally {
                        if (os != null) {
                            os.close();
                        }
                        btSocket.close();
                    }
                    if (debug) {
                        Log.d(TAG, "DalvikPrinter: Done printing");
                    }
                } catch (IOException ex) {
                    Log.e(TAG, "DalvikPrinter: Error printing: " + ex);
                }
            }
        });
        printThread.start();
    }

    private native void detectedBTDevice(String name, String address);
}