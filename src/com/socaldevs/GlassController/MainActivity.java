package com.socaldevs.GlassController;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.*;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements View.OnClickListener {

    public static final UUID uuid = UUID.fromString("843af4f0-ea91-11e2-91e2-0800200c9a66");
    public static final String name = "GlassController";
    private boolean isGlass = false;

    private BluetoothAdapter mBluetoothAdapter = null;

    private TextView tv = null;

    //private ConnectThread mGlassConn = null;

    private PrintStream su = null;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        tv = (TextView) findViewById(R.id.textView);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            //Phone does not support Bluetooth
            tv.setText("Bluetooth not supported or disabled");
        }

        if(Build.DEVICE.toLowerCase().contains("glass"))
            isGlass = true;

        findViewById(R.id.setIpButton).setOnClickListener(this);
    }



    @Override
    public void onResume(){
        super.onResume();
        //connect();
    }

    public void connect(){
        append("connect()");
        if(isGlass){
            append("Starting to listen on BT");
            if(!isMyServiceRunning()){
                Intent i = new Intent(this, GlassServer.class);
                startService(i);
                append("Started service");
            }
        }

        if(!isGlass){
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            BluetoothDevice glass = null;
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    // Add the name and address to an array adapter to show in a ListView
                    if(device.getName().toLowerCase().contains("glass"))
                        glass = device;
                }
            }
            if(glass == null){
                tv.setText("Glass device not found");
            }else{
                /*if(mGlassConn != null)
                    mGlassConn.cancel();
                mGlassConn = new ConnectThread(glass);
                append("Connecting to Glass...");
                mGlassConn.start();*/
            }
        }
    }

    @Override
    public void onClick(View v){
        if(v.getId() == R.id.setIpButton){
            String ip = ((EditText) findViewById(R.id.ip)).getText().toString();
            try {
                Process suProcess = Runtime.getRuntime().exec("su");
                su = new PrintStream(suProcess.getOutputStream());
                su.println("adb connect "+ip);
                su.println("adb -s "+ ip+":5555 shell");
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    private void send(int code){
        if(su != null)
            su.println("input keyevent "+String.valueOf(code));
    }

    public void append(final String text) {
        tv.post(new Runnable() {
            @Override
            public void run() {
                tv.setText(tv.getText()+"\n"+text);
            }
        });
    }

    private boolean downPressed = false;
    private boolean downLong = false;
    private boolean upPressed = false;
    private boolean upLong = false;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(isGlass)
            return super.dispatchKeyEvent(event);

        int action = event.getAction();
        int keyCode = event.getKeyCode();

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN && event.isLongPress()) {
                    //select - 23
                    append("Select");
                    //mGlassConn.send(23);
                    send(23);
                    upLong = true;
                } else if (action == KeyEvent.ACTION_UP && !upLong) { //key is released and it is not a long press
                    //move right - 22
                    append("Right");
                    //mGlassConn.send(22);
                    send(22);
                    upPressed = true;
                }
                if (action == KeyEvent.ACTION_DOWN) {
                    upPressed = true;
                }
                if (action == KeyEvent.ACTION_UP) {
                    upPressed = false;
                    upLong = false;
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN && event.isLongPress()) {
                    //go down - 4
                    append("Down");
                    //mGlassConn.send(4);
                    send(4);
                    downLong = true;
                } else if (action == KeyEvent.ACTION_UP && !downLong) { //key is released and it is not a long press
                    //move left - 21
                    append("Left");
                    //mGlassConn.send(21);
                    send(21);
                    downPressed = true;
                }
                if (action == KeyEvent.ACTION_DOWN) {
                    downPressed = true;
                }
                if (action == KeyEvent.ACTION_UP) {
                    downPressed = false;
                    downLong = false;
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        private OutputStream mOutputStream;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        @Override
        public void run() {
            Log.i("Client", "run() of ConnectThread");
            append("run() of ConnectThread");
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                append("Trying to connect...");
                Log.i("Client", "trying to connect...");
                mmSocket.connect();
                append("Connected");
                Log.i("Client", "connected");
                mOutputStream = mmSocket.getOutputStream();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

        }

        public void send(int cmd) {
            if(mmSocket.isConnected()){
                try {
                    mOutputStream.write(cmd);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                append("Disconnected, reconnecting");
                connect();
            }
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (GlassServer.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
