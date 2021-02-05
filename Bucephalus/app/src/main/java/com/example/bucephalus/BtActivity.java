package com.example.bucephalus;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BtActivity extends AppCompatActivity {

    public static BluetoothAdapter BtAdapter;
    public static BluetoothSocket BtSocket;
    InputStream inputStream = null;
    public TextView displayText;

    public static Set<BluetoothDevice> BondedDevicesSet;
    public static ArrayList<String> arrayListNameAddress = new ArrayList<String>();
    public static ArrayList<String> arrayListAddress = new ArrayList<String>();
    public static String BtServerAddress;
    public static String BtServerNameAddress;
    public static final String SERVICE_ID = "00001101-0000-1000-8000-00805f9b34fb"; //SPP UUID

    byte[] buffer = new byte[256];

    Handler handler;
    Thread t1;

    final int handlerState = 0;
    private StringBuilder recDataString = new StringBuilder();

    private static final int US_L = 1;

    Button destination_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt);

        displayText = findViewById(R.id.Textview);

        handler = new Handler(Looper.getMainLooper()){

            @Override
            public void handleMessage(@NonNull Message msg) {

                super.handleMessage(msg);

                String speed = null, pwm = null , ch = null, dh = null, cd = null;
                int state = 0;
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                                      //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("\n");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                          // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                        try{
                            String tokens[] = dataInPrint.split("\\s*,\\s*");
                            displayText.setText(" SPEED = " + tokens[1] + "\n" + " PWM = " + tokens[2] + "\n" + " CH = " + tokens[7] + "\n" + " DH = " + tokens[8] + "\n" + " CD = " + tokens[9] + "\n");
                        }catch(Exception e){
                            Toast.makeText(BtActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                        }
                        recDataString.delete(0, recDataString.length());                    //clear all string data
                        dataInPrint = " ";
                    }
                }
            }
        };

        displayText.setText(" select bluetooth device ");

        BtAdapter = BluetoothAdapter.getDefaultAdapter();

        if(!BtAdapter.isEnabled()){
            BtAdapter.enable();
        }

        BtAdapter.startDiscovery();

        BondedDevicesSet = BtAdapter.getBondedDevices();

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayListNameAddress);

        for(BluetoothDevice bt : BondedDevicesSet) {
            //this array is used for debuggin purpose and also to let the user know which device the start is sent
            arrayListNameAddress.add(bt.getName() + "\n" + bt.getAddress() + "\n");

            //this array has only the address to be used for connecting later on
            arrayListAddress.add(bt.getAddress());
        }

        BtAdapter.cancelDiscovery();

        ListView listview = findViewById(R.id.listbtdevices);
        listview.setAdapter(arrayAdapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            @Override
                                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                BtServerAddress = arrayListAddress.get(position);
                                                BtServerNameAddress = arrayListNameAddress.get(position);
                                                BluetoothDevice Btdevice = BtAdapter.getRemoteDevice(BtServerAddress);
                                                try{
                                                    BtSocket = Btdevice.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_ID));
                                                    BtSocket.connect();

                                                }catch (Exception e){
                                                    //Toast.makeText(MapsActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                                                    Toast.makeText(BtActivity.this, "is HC-05 turned on?", Toast.LENGTH_SHORT).show();
                                                }
//                                                displayText.setText(" connected to - " + BtServerNameAddress + "\n");
                                                ecutomobile thread_ecutomobile = new ecutomobile();
                                                thread_ecutomobile.start();
//                                                t1.start();
                                            }
                                        }
        );

//                                                tobesent = inputText.getText().toString();





//        debugButton.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) {
////                String btsocketinfo = "HEY!";
////                btsocketinfo = BtSocket.toString();
////                Intent i = new Intent(MapsActivity.this, CANDebugActivity.class);
////                i.putExtra("btsocketinfo", btsocketinfo);
////                startActivity(i);
//
//            }
//        });

//        Toast.makeText(this,"completed", Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onStop() {
        super.onStop();
        try{
            BtSocket.close();
        }catch(Exception e){

        }

    }

    class ecutomobile extends Thread {

        public void run() {
            // Keep looping to listen for received messages
            while (true) {
                try {
                    int bytes;
                    byte[] buffer = new byte[2048];
                    ByteArrayInputStream input = new ByteArrayInputStream(buffer);
                    inputStream = BtSocket.getInputStream();
                    bytes = inputStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    handler.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
//                    break;
                }
            }
        }
    }

}
