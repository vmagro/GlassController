package com.socaldevs.GlassController;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.*;

public class GlassServer extends Service {

    private BluetoothAdapter mBluetoothAdapter = null;
    private ServerThread t = null;

    private boolean threadIsRunning = false;

    @Override
    public void onCreate(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        t = new ServerThread(mBluetoothAdapter);
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!threadIsRunning)
            t.start();
        return Service.START_STICKY;
    }

    private class ServerThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        private boolean canceled = false;

        public ServerThread(BluetoothAdapter adapter) {
            BluetoothServerSocket tmp = null;
            try {
                tmp = adapter.listenUsingRfcommWithServiceRecord(MainActivity.name, MainActivity.uuid);
            } catch (IOException e) { }
            mmServerSocket = tmp;
            canceled = false;
        }

        public void run() {
            threadIsRunning = true;
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (!canceled) {
                try {
                    Log.i("Server", "waiting...");
                    socket = mmServerSocket.accept();
                    Log.i("Server", "accepted connection");
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    new ConnectedThread(socket).start();
                    break;
                }
            }
            threadIsRunning = false;
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                canceled = true;
                mmServerSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        private PrintStream shell = null;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            Log.i("Server", "Connected");
        }

        public void run() {
            try{
                Runtime.getRuntime().exec("adb connect localhost").waitFor();
                shell = new PrintStream(Runtime.getRuntime().exec("adb -s localhost:5555 shell").getOutputStream());
            }catch(IOException ioex){
                ioex.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    if(mmInStream.available() > 0){
                        Log.i("Server", mmInStream.available()+" bytes available");
                        // Read from the InputStream
                        int b = mmInStream.read();
                        Log.i("Server", "Received: "+String.valueOf(b));
                        shell.println("input keyevent "+String.valueOf(b));
                        Log.i("Server", "process completed");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
