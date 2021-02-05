package com.example.bucephalus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.R.color;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;

import org.w3c.dom.Text;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import static android.R.color.*;
import static com.example.bucephalus.BtActivity.BtSocket;

//blank commit, forgot to add REV 5 in previous commit message
// GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener

public class MapsActivity<State_e> extends FragmentActivity implements OnMapReadyCallback{

//    private static final float DEFAULT_ZOOM = 1;
//    GoogleApiClient mGoogleApiClient;
//    Location mLastLocation;
//    Marker mCurrLocationMarker;
//    LocationRequest mLocationRequest;
//    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
//
//
//    private static final int REQUEST_CODE = 101;
//    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 10;
//
//
//    private LatLng mDefaultLocation;
//


//    Message mapHandlerMsg;

//    Thread t1;

    //    final int handlerState = 0;

    String motor_speed_from_bt, pwm_data_from_bt, geo_ch_from_bt, geo_dh_from_bt, geo_cd_from_bt;

    //View Initialization
    public static Button debugButton;
    public static Button startNdestinationButton;
    public static Button stopButton;
    public static Button connecttocarButton;
    public static ListView listview;
    public static Switch headlight;
    public TextView displayText;
    public TextView infoText;
    public TextView destinationText;

    //BT Service Initialization
    public static Set<BluetoothDevice> BondedDevicesSet;
    public static ArrayList<String> arrayListNameAddress = new ArrayList<String>();
    public static ArrayList<String> arrayListAddress = new ArrayList<String>();
    public static String BtServerAddress;
    public static String BtServerNameAddress;
    public static final String SERVICE_ID = "00001101-0000-1000-8000-00805f9b34fb"; //SPP UUID
    public static BluetoothAdapter BtAdapter;
    public static BluetoothSocket BtSocket;
    InputStream inputStream = null;

    //Timer Initialization
    CountDownTimer timr, timr_bt;

    //Flag Initialization
    boolean destination_selected = false;
    boolean bt_failed = false;
    boolean started = false;
    boolean mLocationPermissionGranted;
    boolean connectedtocar = false;
    boolean run_thread = false;

    //Map Initialization
    SupportMapFragment mapFragment;
    FusedLocationProviderClient mFusedLocationProviderClient;
    String tobesent_destination;
    GoogleMap mMap;

    //Handler Initialization
    Handler handler;
    final int handlerState = 1;
    public StringBuilder recDataString = new StringBuilder();


    private CountDownTimer Timr;

    enum State{app_started, connected_to_car, dest_selected, destination_sent, car_started, car_stopped, destination_reached};
    enum previousState{zero, app_started, connected_to_car, dest_selected, destination_sent, car_started, car_stopped, destination_reached};
    State current_state_of_car = State.app_started;
    previousState previous_state_of_car = previousState.zero;

    ecutomobile thread_ecutomobile = new ecutomobile();

    final String tobesent_start = "START";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //View Initialization=======================================================================

        debugButton = findViewById(R.id.DebugButton);
        stopButton = findViewById(R.id.StopButton);
        startNdestinationButton = findViewById(R.id.Start_DestinationButton);
        connecttocarButton = findViewById(R.id.ConnectButton);
        listview = findViewById(R.id.listbtdevices);
        headlight = findViewById(R.id.switch1);
        destinationText = findViewById(R.id.destinationText);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        displayText = findViewById(R.id.Textview);
        infoText = findViewById(R.id.infoText);


        handler = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(@NonNull Message msg) {

                super.handleMessage(msg);

                String speed = null, pwm = null, ch = null, dh = null, cd = null;
                int state = 0;
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                                      //keep appending to string until ~
//                    int startOfLineIndex = recDataString.indexOf("*");                    // determine the end-of-line
                    int endOfLineIndex = recDataString.indexOf("#");

                    if (endOfLineIndex > 0) {                                          // make sure there data before ~
//                        String dataInPrint = recDataString.substring(startOfLineIndex, endOfLineIndex);    // extract string
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);
                        try {
                            String tokens[] = dataInPrint.split("\\s*,\\s*");
                            displayText.setText(" CH = " + tokens[7] + "\n" + " DH = " + tokens[8] + "\n" + " CD = " + tokens[9] + "\n");
                        } catch (Exception e) {
                            Toast.makeText(MapsActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                        }
//                        recDataString.delete(startOfLineIndex, endOfLineIndex);                    //clear all string data
                        recDataString.delete(0, endOfLineIndex);
                        dataInPrint = " ";
                        recDataString.setLength(0);         //MOOOOSSSSTTTTT IMPORTANT - spend entire night and including this made it work!
                    }
                }
            }
        };

        //MAP=======================================================================================

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mapFragment.getMapAsync(this);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            getCurrentLocation();
        }else{
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }

        //BLUETOOTH=================================================================================

        BtAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!BtAdapter.isEnabled()) {
            BtAdapter.enable();
        }

        BtAdapter.startDiscovery();

        /*

        BondedDevicesSet = BtAdapter.getBondedDevices();

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayListNameAddress);

        for (BluetoothDevice bt : BondedDevicesSet) {
            //this array is used for debuggin purpose and also to let the user know which device the start is sent
            arrayListNameAddress.add(bt.getName() + "\n" + bt.getAddress() + "\n");

            //this array has only the address to be used for connecting later on
            arrayListAddress.add(bt.getAddress());
        }

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
                                                    Toast.makeText(MapsActivity.this, "is HC-05 turned on?", Toast.LENGTH_SHORT).show();
                                                    bt_failed = true;
                                                    try {
                                                        BtSocket.close();
                                                    } catch (IOException ex) {
                                                        ex.printStackTrace();
                                                    }
                                                }

                                                if(bt_failed == false){
                                                    displayText.setText(" connected to - " + BtServerNameAddress + "\n");
                                                    ecutomobile thread_ecutomobile = new ecutomobile();
                                                    thread_ecutomobile.start();
//                                                    timr_bt.start();
                                                }
//                                                t1.start();
                                            }
                                        }
        );

         */


        //BUTTONS===================================================================================

        startNdestinationButton.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {

                //START BUTTON
                if(current_state_of_car == State.destination_sent || current_state_of_car == State.car_stopped){

                    try {
//                    updateMap_T.start();
                        BtSocket.getOutputStream().write(tobesent_start.getBytes());
                    } catch (Exception e) {
//                    Toast.makeText(MapsActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                        Toast.makeText(MapsActivity.this, "is HC-05 turned on?", Toast.LENGTH_SHORT).show();
                        bt_failed = true;
                    }

                    if (bt_failed == false) {
                        infoText.setText(" sent data - " + tobesent_start);
                        started = true;
                        current_state_of_car = State.car_started;
                    } else {
                        infoText.setText(" sending data failed");
                    }

                }
                //DESTINATION BUTTON
                else if(current_state_of_car == State.dest_selected){

                    try {
                        if (destination_selected == true) {
                            BtSocket.getOutputStream().write(tobesent_destination.getBytes());
                        } else {
                            Toast.makeText(MapsActivity.this, "select destination on map", Toast.LENGTH_SHORT).show();
                            bt_failed = true;
                        }
                    } catch (Exception e) {
                        Toast.makeText(MapsActivity.this, "is HC-05 turned on?", Toast.LENGTH_SHORT).show();
                        bt_failed = true;
                    }

                    if (bt_failed == false) {
                        infoText.setText(" sent destination - " + tobesent_destination);
                        current_state_of_car = State.destination_sent;
                    } else {

                    }
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (connectedtocar == true && current_state_of_car == State.car_started) {

                    final String tobesent_stop = "STOP";

                    try {
                        BtSocket.getOutputStream().write(tobesent_stop.getBytes());
//                    BtSocket.close();
                    } catch (Exception e) {
//                    Toast.makeText(MapsActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                        Toast.makeText(MapsActivity.this, "is HC-05 turned on?", Toast.LENGTH_SHORT).show();
                        bt_failed = true;
                    }

                    if (bt_failed == false) {
                        infoText.setText(" sent data - " + tobesent_stop);
                        current_state_of_car = State.car_stopped;
                    } else {
                        infoText.setText(" sending data failed");
                    }

                }
//                else if(connectedtocar == true && current_state_of_car == State.car_stopped){
//                    current_state_of_car = State.destination_sent;
//                }


            }
        });

        connecttocarButton.setOnClickListener(new View.OnClickListener() {

//            connecttocarButton.setVisibility(View.INVISIBLE);

            BluetoothSocket tmpSocket;
            InputStream tmpIn;
            OutputStream tmpOut;
            BluetoothAdapter tmpAdapter = BtAdapter;
            BluetoothDevice tmpDevice;
//            thread_ecutomobile = new ecutomobile();

            @Override
            public void onClick(View v) {

                if(current_state_of_car != State.app_started){
                    Toast.makeText(MapsActivity.this, "Already connected", Toast.LENGTH_SHORT).show();
                    return;
                }

/*
                if(thread_ecutomobile.isAlive()) {
                    Toast.makeText(MapsActivity.this, "HEy", Toast.LENGTH_SHORT).show();
                    run_thread = false;
                }
*/

                BtAdapter.cancelDiscovery();

                displayText.setText(" trying to connect to - " + "98:D3:11:FC:1C:54" + "\n");

//                BluetoothDevice Btdevice = BtAdapter.getRemoteDevice("98:D3:11:FC:1C:54");

                tmpDevice = tmpAdapter.getRemoteDevice("98:D3:11:FC:1C:54");

                if (tmpAdapter.isEnabled()) {
                    try {

                        tmpSocket = tmpDevice.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_ID));
                        tmpSocket.connect();

                    } catch (Exception e) {

                        bt_failed = true;

                        if (tmpSocket != null) {
                            try {
                                tmpSocket.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }

                            tmpSocket = null;
                        }
//                        e.printStackTrace();
//                        return null;
                    }
                }

/*
                try {
                    BtSocket = Btdevice.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_ID));
                    BtSocket.connect();
                } catch (Exception e) {
                    Toast.makeText(MapsActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                    bt_failed = true;
                    if(BtSocket == null{
                        BtSocket.close();
                    }
                }
*/

                if (bt_failed == false) {

                    displayText.setText(" connected to - " + "98:D3:11:FC:1C:54" + "\n");
                    connectedtocar = true;

                } else if (bt_failed == true) {

                    bt_failed = false;

                    displayText.setText(" trying to connect to - " + "98:D3:31:F9:5B:06" + "\n");

                    tmpDevice = tmpAdapter.getRemoteDevice("98:D3:31:F9:5B:06");

                    if (tmpAdapter.isEnabled()) {
                        try {

                            tmpSocket = tmpDevice.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_ID));
                            tmpSocket.connect();

                        } catch (Exception e) {

                            bt_failed = true;

                            if (tmpSocket != null) {

                                try {
                                    tmpSocket.close();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }

                                tmpSocket = null;
                            }
//                        e.printStackTrace();
//                        return null;
                        }
                    }

/*
                    try {
                        BtSocket = Btdevice.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_ID));
                        BtSocket.connect();
                    } catch (Exception e) {
                        Toast.makeText(MapsActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                        bt_failed = true;
                    }
*/

                    if (bt_failed == false) {

//                        connecttocarButton.getBackground().setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY);
                        displayText.setText(" connected to - " + "98:D3:31:F9:5B:06" + "\n");
                        connectedtocar = true;

                    }
                }

                if(connectedtocar == true){
                    current_state_of_car = State.connected_to_car;
                    BtAdapter = tmpAdapter;
                    BtSocket = tmpSocket;
                }

                try {

                    run_thread = true;
                    thread_ecutomobile.start();

                } catch (Exception e) {

                    Toast.makeText(MapsActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();

                }
            }


        });

        headlight.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {

                if(headlight.isChecked()){

                    final String tobesent = "HON";

                    try {
                        BtSocket.getOutputStream().write(tobesent.getBytes());
//                    BtSocket.close();
                    } catch (Exception e) {
//                    Toast.makeText(MapsActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                        Toast.makeText(MapsActivity.this, "is HC-05 turned on?", Toast.LENGTH_SHORT).show();
//                        bt_failed = true;
                    }
//                    Toast.makeText(MapsActivity.this, "HON", Toast.LENGTH_SHORT).show();
                }else{

                    final String tobesent = "HOFF";

                    try {
                        BtSocket.getOutputStream().write(tobesent.getBytes());
//                    BtSocket.close();
                    } catch (Exception e) {
//                    Toast.makeText(MapsActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                        Toast.makeText(MapsActivity.this, "is HC-05 turned on?", Toast.LENGTH_SHORT).show();
//                        bt_failed = true;
                    }
//                    Toast.makeText(MapsActivity.this, "HOFF", Toast.LENGTH_SHORT).show();
                }

            }
        });



        //STATE MACHINE TIMER=======================================================================

        Timr = new CountDownTimer(30000, 500) {
            @Override
            public void onTick(long millisUntilFinished) {

                if(current_state_of_car == State.app_started){

                    infoText.setText(" 0 - connect to bucephalus ..");
                    startNdestinationButton.setText("SET DESTINATION");
                    connecttocarButton.getBackground().setColorFilter(getResources().getColor(holo_red_light), PorterDuff.Mode.MULTIPLY);

                }else if(current_state_of_car == State.connected_to_car){

                    infoText.setText(" 1 - mark destination on the map");
                    connecttocarButton.getBackground().setColorFilter(getResources().getColor(holo_blue_light), PorterDuff.Mode.MULTIPLY);

                }else if(current_state_of_car == State.dest_selected){

                    infoText.setText(" 2 - set destination to send it to car");

                }else if(current_state_of_car == State.destination_sent){

                    infoText.setText(" 3 - start the car");
                    destinationText.setText(" heading to " + tobesent_destination);
                    startNdestinationButton.setText("START");

                } else if(current_state_of_car == State.car_started){

                    infoText.setText(" 4 - stop button is active ..");

                }else if(current_state_of_car == State.car_stopped){

                    infoText.setText(" 5 - car is stopped");

                }

            }

            @Override
            public void onFinish() {
                start();
            }
        }.start();




        //END=======================================================================================

    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            BtSocket.close();
        } catch (Exception e) {

        }

    }

    @Override
    protected void onResume() {
        super.onResume();



    }

    class ecutomobile extends Thread {

        public void run() {
            // Keep looping to listen for received messages
            while (true) {

                if(run_thread == true){

                    try {
                        int bytes;
                        byte[] buffer = new byte[256];
                        ByteArrayInputStream input = new ByteArrayInputStream(buffer);
                        inputStream = BtSocket.getInputStream();
                        bytes = inputStream.read(buffer);            //read bytes from input buffer
                        String readMessage = new String(buffer, 0, bytes);
                        // Send the obtained bytes to the UI Activity via handler
                        handler.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                    } catch (Exception e) {
//                    break;
//                    Toast.makeText(MapsActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                        Log.getStackTraceString(e);
                    }

                }else{

                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }

            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

//        Toast.makeText(MapsActivity.this, "im here 0", Toast.LENGTH_SHORT).show();

        // Do other setup activities here too, as described elsewhere in this tutorial.
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                int precision = (int) Math.pow(10,6);

                double new_latitude = (double)((int)(precision*latLng.latitude))/precision;
                double new_longitude = (double)((int)(precision*latLng.longitude))/precision;

                LatLng myloc = new LatLng(latLng.latitude, latLng.longitude);
                mMap.addMarker(new MarkerOptions().position(myloc).title("Destination"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(myloc));

//                displayText.setText("sent destination data - " + latLng.latitude + "," + latLng.longitude);
//                tobesent_destination = "GPS" + latLng.latitude + "," + latLng.longitude +"#";
                tobesent_destination = "GPS" + new_latitude + "," + new_longitude +"#";
                destination_selected = true;
                current_state_of_car = State.dest_selected;
            }
        });

        // Turn on the My Location layer and the related control on the map.
//        updateLocationUI();

        // Get the current location of the device and set the position of the map.
//        getDeviceLocation();

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(21, 57);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
//
//        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
//        mMap.getUiSettings().setZoomControlsEnabled(true);
//        mMap.getUiSettings().setCompassEnabled(true);
//        mMap.getUiSettings().setMyLocationButtonEnabled(true);
//
////        fetchLastLocation();
//
//        LatLng myLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Bucephalus"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));



//        //Initialize Google Play Services
//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (ContextCompat.checkSelfPermission(this,
//                    Manifest.permission.ACCESS_FINE_LOCATION)
//                    == PackageManager.PERMISSION_GRANTED) {
//                buildGoogleApiClient();
//                mMap.setMyLocationEnabled(true);
//            }
//        } else {
//            buildGoogleApiClient();
//            mMap.setMyLocationEnabled(true);
//        }
    }

    private void getCurrentLocation() {

        try{
            Task<Location> task = mFusedLocationProviderClient.getLastLocation();
            task.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(final Location location) {
                    mapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

                            MarkerOptions options = new MarkerOptions().position(currentLocation).title("bucephalus");
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 17));
                            mMap.addMarker(options);
                        }
                    });
                }
            });

        }catch(Exception e){

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        mLocationPermissionGranted = false;
        switch (requestCode) {
            case 44: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    getCurrentLocation();
                }
            }
        }
//        Toast.makeText(MapsActivity.this, "im here 2", Toast.LENGTH_SHORT).show();
//        updateLocationUI();
    }
}

//    private void fetchLastLocation() {
//
//        try {
//                Task locationResult = fusedLocationProviderClient.getLastLocation();
//                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
//                    @Override
//                    public void onComplete(@NonNull Task task) {
//                        if (task.isSuccessful()) {
//                            // Set the map's camera position to the current location of the device.
//                            currentLocation = task.getResult();
//                            mMap.moveCamera(currentLocation.getLatitude(), currentLocation.getLongitude());
//                        } else {
//                            mMap.moveCamera(currentLocation.getLatitude(), currentLocation.getLongitude());
//                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
//                        }
//                    }
//                });
//        }
//        catch(SecurityException e)  {
//
//        }
//
//    }




//
//    private void updateLocationUI() {
//        if (mMap == null) {
//
//            Toast.makeText(MapsActivity.this, "im here 3", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        try {
//            if (mLocationPermissionGranted) {
//                mMap.setMyLocationEnabled(true);
//                mMap.getUiSettings().setMyLocationButtonEnabled(true);
//                Toast.makeText(MapsActivity.this, "im here 4", Toast.LENGTH_SHORT).show();
//            } else {
//                mMap.setMyLocationEnabled(false);
//                mMap.getUiSettings().setMyLocationButtonEnabled(false);
//                mLastKnownLocation = null;
//                getLocationPermission();
//                Toast.makeText(MapsActivity.this, "im here 5", Toast.LENGTH_SHORT).show();
//            }
//        } catch (SecurityException e)  {
//            Log.e("Exception: %s", e.getMessage());
//        }
//    }
//
//    private void getLocationPermission() {
//        /*
//         * Request location permission, so that we can get the location of the
//         * device. The result of the permission request is handled by a callback,
//         * onRequestPermissionsResult.
//         */
//        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
//                android.Manifest.permission.ACCESS_FINE_LOCATION)
//                == PackageManager.PERMISSION_GRANTED) {
//            mLocationPermissionGranted = true;
//        } else {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
//                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
//        }
//    }
//
//    private void getDeviceLocation() {
//        /*
//         * Get the best and most recent location of the device, which may be null in rare
//         * cases when a location is not available.
//         */
//        try {
//            if (mLocationPermissionGranted) {
//                Task locationResult = mFusedLocationProviderClient.getLastLocation();
//                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
//                    @Override
//                    public void onComplete(@NonNull Task task) {
//                        if (task.isSuccessful()) {
//                            // Set the map's camera position to the current location of the device.
//                            mLastKnownLocation = (Location) task.getResult();
//                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
//                                    new LatLng(mLastKnownLocation.getLatitude(),
//                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
//                        } else {
////                            Log.d(TAG, "Current location is null. Using defaults.");
////                            Log.e(TAG, "Exception: %s", task.getException());
//                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
//                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
//                        }
//                    }
//                });
//            }
//        } catch(SecurityException e)  {
//            Log.e("Exception: %s", e.getMessage());
//        }
//    }
//
//    class updateMap extends Thread {
//
//        String test = "test it now";
//
//        public void run() {
//            // Keep looping to listen for received messages
//            while (true) {
//                try {
//                    int bytes;
////                    handler.obtainMessage(handlerState, bytes, -1, null).sendToTarget();
//                    mapHandlerMsg.what = 1;
////                    handler.sendMessage(mapHandlerMsg);
//                    byte[] buffer = new byte[2048];
//                    ByteArrayInputStream input = new ByteArrayInputStream(buffer);
//                    bytes = 1;            //read bytes from input buffer
//                    String readMessage = new String(buffer, 0, bytes);
//                    handler.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
//                } catch (Exception e) {
////                    break;
//                }
//            }
//        }
//    }




    //    protected synchronized void buildGoogleApiClient() {
//        mGoogleApiClient = new GoogleApiClient.Builder(this)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .addApi(LocationServices.API)
//                .build();
//        mGoogleApiClient.connect();
//    }

//    @Override
//    public void onConnected(Bundle bundle) {
//        mLocationRequest = new LocationRequest();
//        mLocationRequest.setInterval(1000);
//        mLocationRequest.setFastestInterval(1000);
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION)
//                == PackageManager.PERMISSION_GRANTED) {
//            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, (LocationListener) this);
//        }
//    }

//    @Override
//    public void onConnectionSuspended(int i) {
//    }

//    @Override
//    public void onLocationChanged(Location location) {
//        mLastLocation = location;
//        if (mCurrLocationMarker != null) {
//            mCurrLocationMarker.remove();
//        }
////Showing Current Location Marker on Map
//        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//        MarkerOptions markerOptions = new MarkerOptions();
//        markerOptions.position(latLng);
//        LocationManager locationManager = (LocationManager)
//                getSystemService(Context.LOCATION_SERVICE);
//        String provider = locationManager.getBestProvider(new Criteria(), true);
//        if (ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
//                        != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        Location locations = locationManager.getLastKnownLocation(provider);
//        List<String> providerList = locationManager.getAllProviders();
//        if (null != locations && null != providerList && providerList.size() > 0) {
//            double longitude = locations.getLongitude();
//            double latitude = locations.getLatitude();
//            Geocoder geocoder = new Geocoder(getApplicationContext(),
//                    Locale.getDefault());
//            try {
//                List<Address> listAddresses = geocoder.getFromLocation(latitude,
//                        longitude, 1);
//                if (null != listAddresses && listAddresses.size() > 0) {
//                    String state = listAddresses.get(0).getAdminArea();
//                    String country = listAddresses.get(0).getCountryName();
//                    String subLocality = listAddresses.get(0).getSubLocality();
//                    markerOptions.title("" + latLng + "," + subLocality + "," + state
//                            + "," + country);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
//        mCurrLocationMarker = mMap.addMarker(markerOptions);
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
//        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
//        if (mGoogleApiClient != null) {
//            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (LocationListener) this);
//        }
//    }

//    @Override
//    public void onConnectionFailed(ConnectionResult connectionResult) {
//    }
//    public boolean checkLocationPermission() {
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
//                    Manifest.permission.ACCESS_FINE_LOCATION)) {
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                        MY_PERMISSIONS_REQUEST_LOCATION);
//            } else {
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                        MY_PERMISSIONS_REQUEST_LOCATION);
//            }
//            return false;
//        } else {
//            return true;
//        }
//    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           String permissions[], int[] grantResults) {
//        switch (requestCode) {
//            case MY_PERMISSIONS_REQUEST_LOCATION: {
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    if (ContextCompat.checkSelfPermission(this,
//                            Manifest.permission.ACCESS_FINE_LOCATION)
//                            == PackageManager.PERMISSION_GRANTED) {
//                        if (mGoogleApiClient == null) {
//                            buildGoogleApiClient();
//                        }
//                        mMap.setMyLocationEnabled(true);
//                    }
//                } else {
//                    Toast.makeText(this, "permission denied",
//                            Toast.LENGTH_LONG).show();
//                }
//                return;
//            }
//        }
//    }



//}

//        mMap.

//        final Location myLocation = mMap.getMyLocation();
//        LatLng myloc = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
//        mMap.addMarker(new MarkerOptions().position(myloc).title("Bucephalus"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(myloc));

//        mMap.setMyLocationEnabled(true);
//
//        Marker mark = new Marker();
//        LatLng position = mark.getPosition();
//        textView.setText(position.toString());

//        mMap.setMyLocationEnabled(true);




//        ListView listview = findViewById(R.id.listbtdevices);
//        listview.setAdapter(arrayAdapter);
//
//        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                                            @Override
//                                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                                                BtServerAddress = arrayListAddress.get(position);
//                                                BtServerNameAddress = arrayListNameAddress.get(position);
//                                                BluetoothDevice Btdevice = BtAdapter.getRemoteDevice(BtServerAddress);
//                                                try{
//                                                    BtSocket = Btdevice.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_ID));
//                                                    BtSocket.connect();
//                                                }catch (Exception e){
//                                                    //Toast.makeText(MapsActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
//                                                    Toast.makeText(MapsActivity.this, "is HC-05 turned on?", Toast.LENGTH_SHORT).show();
//                                                    bt_failed = true;
//                                                }
//
//                                                if(bt_failed == false){
//                                                    displayText.setText(" connected to - " + BtServerNameAddress + "\n");
////                                                    ecutomobile thread_ecutomobile = new ecutomobile();
////                                                    thread_ecutomobile.start();
//                                                    timr_bt.start();
//                                                }
////                                                t1.start();
//                                            }
//                                        }
//        );




//TIMER======================================

//        timr = new CountDownTimer(30000, 200) {
//
//            public void onTick(long millisUntilFinished) {
//                displayText.setText("seconds remaining: " + millisUntilFinished / 1000);

//                int startOfLineIndex = recDataString.indexOf("*");
//                int endOfLineIndex = recDataString.indexOf("#");                    // determine the end-of-line
//
//                if (endOfLineIndex > 0) {                                          // make sure there data before ~
//
//                    try {
//
//                        String dataInPrint = recDataString.substring(startOfLineIndex, endOfLineIndex);    // extract string
//                        String tokens[] = dataInPrint.split("\\s*,\\s*");
//
//                        motor_speed_from_bt = tokens[1];
//                        pwm_data_from_bt = tokens[2];
//                        geo_ch_from_bt = tokens[7];
//                        geo_dh_from_bt = tokens[8];
//                        geo_cd_from_bt = tokens[9];
//
//                        displayText.setText(" SPEED = " + tokens[1] + "\n" + " PWM = " + tokens[2] + "\n" + " CH = " + tokens[7] + "\n" + " DH = " + tokens[8] + "\n" + " CD = " + tokens[9] + "\n");
////                        recDataString.delete(0, recDataString.length());                    //clear all string data
//                        recDataString.delete(startOfLineIndex, endOfLineIndex);
//                        dataInPrint = " ";
//
//                    } catch (Exception e) {
//                        Toast.makeText(MapsActivity.this, "timr_bt" + e.getMessage().toString(), Toast.LENGTH_SHORT).show();
//                    }
//
//                }
//                displayText.setText(" SPEED = " + motor_speed_from_bt + "\n" + " PWM = " + pwm_data_from_bt + "\n" + " CH = " + geo_ch_from_bt + "\n" + " DH = " + geo_dh_from_bt + "\n" + " CD = " + geo_cd_from_bt + "\n");
//            }

//            public void onFinish() {
////                displayText.setText("done!");
////                start();
//            }
//        };

//        timr_bt = new CountDownTimer(30000, 100) {
//
//            String message = " ";
//            int bytesRead;
////            byte[] buffer = new byte[256];
//
//            public void onTick(long millisUntilFinished) {

//                try {
//
//                    int bytes;
//                    byte[] buffer = new byte[4098];
//                    ByteArrayInputStream input = new ByteArrayInputStream(buffer);
//                    inputStream = BtSocket.getInputStream();
//                    bytes = inputStream.read(buffer);            //read bytes from input buffer
//                    String readMessage = new String(buffer, 0, bytes);
//                    recDataString.append(readMessage);                                      //keep appending to string until ~
//
//                    int startOfLineIndex = recDataString.indexOf("*");
//                    int endOfLineIndex = recDataString.indexOf("#");                    // determine the end-of-line
//
//                    if (endOfLineIndex > 0) {                                          // make sure there data before ~
//
//                        try {
//
//                            String dataInPrint = recDataString.substring(startOfLineIndex, endOfLineIndex);    // extract string
//                            String tokens[] = dataInPrint.split("\\s*,\\s*");
//
//                            motor_speed_from_bt = tokens[1];
//                            pwm_data_from_bt = tokens[2];
//                            geo_ch_from_bt = tokens[7];
//                            geo_dh_from_bt = tokens[8];
//                            geo_cd_from_bt = tokens[9];
//
//                            displayText.setText(" SPEED = " + tokens[1] + "\n" + " PWM = " + tokens[2] + "\n" + " CH = " + tokens[7] + "\n" + " DH = " + tokens[8] + "\n" + " CD = " + tokens[9] + "\n");
////                        recDataString.delete(0, recDataString.length());                    //clear all string data
//                            recDataString.delete(startOfLineIndex, endOfLineIndex);
//                            dataInPrint = " ";
//
//                        } catch (Exception e) {
//                            Toast.makeText(MapsActivity.this, "timr_bt" + e.getMessage().toString(), Toast.LENGTH_SHORT).show();
//                        }
//
//                    }
//
//                } catch (Exception e) {
//                            Toast.makeText(MapsActivity.this, "timr_bt" + e.getMessage().toString(), Toast.LENGTH_SHORT).show();
//                }

//            }
//
//            public void onFinish() {
////                displayText.setText("done!");
////                start();
//            }
//        };


//        handler = new Handler(Looper.getMainLooper()){
//
//            @Override
//            public void handleMessage(@NonNull Message msg) {
//
//                super.handleMessage(msg);
//
//                if (msg.what == handlerState) {
//                    updateLocationUI();
//                    msg.what = 0;
//                }
//                Toast.makeText(MapsActivity.this, "im here 7", Toast.LENGTH_SHORT).show();
//            }
//        };
//
//        final updateMap updateMap_T = new updateMap();





