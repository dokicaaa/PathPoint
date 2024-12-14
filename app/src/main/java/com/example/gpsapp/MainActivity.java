package com.example.gpsapp;

import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.gpsapp.databinding.ActivityMapBinding;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapBinding binding;

    private static final float DEFAULT_ZOOM = 15;

    private int currentMapType = GoogleMap.MAP_TYPE_NORMAL;
    List<LatLng> markerPoints = new ArrayList<>();
    List<Marker> markers = new ArrayList<>();

    Polyline currentPolyline;
    private AppDatabase db;

    private static final String ACTION_USB_PERMISSION = "com.example.gpsapp.USB_PERMISSION";
    private UsbDevice device; // Declare device at the class level
    private UsbDeviceConnection connection; // Declare connection at the class level if needed
    private UsbManager usbManager;

    private UsbSerialPort arduinoPort = null;
    private UsbSerialPort gpsPort = null;


    //Replace VENDOR AND PRODUCT Id for you module
    private static final int GPS_VID = 0x1546; // Vendor ID for the GPS module ()
    private static final int GPS_PID = 0x01A9; // Product ID for the GPS module

    private static final int ARDUINO_VID = 0x1A86; // Vendor ID for the Arudino Nano
    private static final int ARDUINO_PID = 0x7523; // Product ID for the Arduino Nano


    //Your Google Maps API key here
    private static final String API_KEY = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Init
        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Button btnCreatePath = findViewById(R.id.btnPath);
        Button btnSavedPaths = findViewById(R.id.btnSavedPaths);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragement);
        mapFragment.getMapAsync(this);

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // Get info about the selected place and move the map camera
                Log.i("TAG", "Place: " + place.getName() + ", " + place.getId());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), DEFAULT_ZOOM));
            }

            @Override
            public void onError(Status status) {
                // Handle the error
                Log.i("TAG", "An error occurred: " + status);
            }
        });

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), API_KEY );
        }

        // Initialize the Room database
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "Paths")
                .allowMainThreadQueries()
                .build();

        //FUNCTIONS
       removePoint();

       //BUTTONS
       btnCreatePath.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               popUp();
           }
       });

       Button btnStartMotor = findViewById(R.id.btnDevices);
        btnStartMotor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendDataToArduino("MOVE\n");
            }
        });

       btnSavedPaths.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               openSaved();
           }
       });

        // Check if there's a path ID passed to this activity
        if (getIntent().hasExtra("PATH_ID")) {
            int pathId = getIntent().getIntExtra("PATH_ID", -1);
            if (pathId != -1) {
                // Use the pathId to load and display the path and its markers
                loadPathMarkers(pathId);
            }
        }

        //SERIAL COMMUNICATION
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbPermissionReceiver, filter);
        checkForConnectedDevices();
    }

    private void checkForConnectedDevices() {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                UsbDevice device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();

                Log.d("USB", "Found Device: VID=" + deviceVID + ", PID=" + devicePID);

                //Change the VID and PID adresses of each module

                //Arduino Nano
                if (deviceVID == ARDUINO_VID && devicePID == ARDUINO_PID) {
                    requestPermission(device);
                }
                //Ubloc F9P GPS module
                else if(deviceVID == GPS_VID && devicePID == GPS_PID){
                    requestPermission(device);
                }
            }
        }
        Log.d("USB", "No suitable device found.");
    }


    //Requesting Serial Permission
    private void requestPermission(UsbDevice device) {
        Log.d("USB", "Requesting permission for device: VID=" + device.getVendorId() + ", PID=" + device.getProductId());
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_UPDATE_CURRENT);
        usbManager.requestPermission(device, pi);
    }


    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Log.d("USB", "Permission granted for device " + device);
                            connectToDevice(device);
                        } else {
                            Log.d("USB", "Device is null in BroadcastReceiver");
                        }
                    } else {
                        Log.d("USB", "Permission denied for device " + device);
                    }
                }
            }
        }
    };

    private void connectToDevice(UsbDevice device) {
        UsbDeviceConnection connection = usbManager.openDevice(device);
        if (connection == null) {
            Log.d("USB", "Unable to open connection");
            return;
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if (driver != null) {
            UsbSerialPort port = driver.getPorts().get(0); // Assuming only one port per device
            try {
                port.open(connection);
                port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                if (device.getVendorId() == ARDUINO_VID && device.getProductId() == ARDUINO_PID) {
                    arduinoPort = port;
                    Log.d("USB", "Arduino Nano connected.");
                } else if (device.getVendorId() == GPS_VID && device.getProductId() == GPS_PID) {
                    gpsPort = port;
                    Log.d("USB", "GPS Module connected.");
                }

                // Optionally, start reading from the port here or elsewhere in your code
                // startReading(port);

            } catch (IOException e) {
                Log.e("USB", "Error setting up device: " + e.getMessage(), e);
            }
        } else {
            Log.d("USB", "No driver for device");
        }
    }


    private void sendDataToArduino(String data) {
        if (arduinoPort != null) {
            try {
                arduinoPort.write(data.getBytes(), 1000);
                Log.d("SendData", "Data sent to Arduino: " + data);
            } catch (IOException e) {
                Log.e("SendData", "Error sending data to Arduino: ", e);
            }
        } else {
            Log.d("SendData", "Arduino port is null");
        }
    }

    private void sendDataToGPS(String data) {
        if (gpsPort != null) {
            try {
                gpsPort.write(data.getBytes(), 1000);
                Log.d("SendData", "Data sent to GPS: " + data);
            } catch (IOException e) {
                Log.e("SendData", "Error sending data to GPS: ", e);
            }
        } else {
            Log.d("SendData", "GPS port is null");
        }
    }

    private void startReading(final UsbSerialPort port) {
        Thread thread = new Thread(() -> {
            byte[] buffer = new byte[64];
            int numBytesRead;
            try {
                while ((numBytesRead = port.read(buffer, 1000)) > 0) {
                    final String receivedData = new String(buffer, 0, numBytesRead);
                    Log.d("USB", "Received: " + receivedData);
                }
            } catch (IOException e) {
                Log.e("USB", "Error reading: " + e.toString());
            }
        });
        thread.start();
    }


    private void loadPathMarkers(int pathId) {
        AppDatabase db = AppDatabase.getInstance(this);
        // Observe the LiveData returned by the DAO
        db.pathDao().getWaypointsByPathId(pathId).observe(this, waypoints -> {
            // Check if waypoints are not null and not empty
            if (waypoints != null && !waypoints.isEmpty()) {
                // Clear previous markers if any
                mMap.clear();

                // Clear the current markerPoints to prepare for new data
                markerPoints.clear();

                for (Waypoint waypoint : waypoints) {
                    LatLng point = new LatLng(waypoint.getLatitude(), waypoint.getLongitude());
                    MarkerOptions markerOptions = new MarkerOptions().position(point).title("Point");
                    mMap.addMarker(markerOptions);

                    // Add this point to markerPoints for polyline drawing
                    markerPoints.add(point);
                }


                if (!markerPoints.isEmpty()) {
                    drawPolyline(); // Draw polyline based on the loaded markers
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(waypoints.get(0).getLatitude(), waypoints.get(0).getLongitude()), 20));
                }
            }
        });
    }
    @Override
    public void  onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Button for changing map type
        ImageButton changePath = findViewById(R.id.changeMap);
        changePath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check the current map type and toggle
                if (currentMapType == GoogleMap.MAP_TYPE_NORMAL) {
                    googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    currentMapType = GoogleMap.MAP_TYPE_SATELLITE;
                } else {
                    googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    currentMapType = GoogleMap.MAP_TYPE_NORMAL;
                }
            }
        });


        // Set a listener for map click events.
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                // Add a marker at the tapped location with a title and show the coordinates in the snippet
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(point)
                        .title("Point")
                        .snippet("Lat: " + point.latitude + ", Lng: " + point.longitude));

                // Add the new point to the list for polyline drawing
                markerPoints.add(point);
                markers.add(marker);
                drawPolyline(); // Draw or update the polyline


                //Prints the cordinates of the markes in logcat
                logMarkerCoordinates();

                setInfoWindowAdapter(); // Set a custom InfoWindowAdapter if needed
            }
        });

        // Add a custom click listener to the markers
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                // Toggle the InfoWindow visibility
                if (marker.isInfoWindowShown()) {
                    marker.hideInfoWindow();
                } else {
                    marker.showInfoWindow();
                }
            }
        });
    }

    private void drawPolyline() {
        // If a polyline already exists, remove it
        if (currentPolyline != null) {
            currentPolyline.remove();
        }

        // Create a polyline with all the marker points
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.addAll(markerPoints);
        polylineOptions.width(5); // Set the width of the polyline
        polylineOptions.color(Color.RED); // Set the color of the polyline

        // Add the polyline to the map
        currentPolyline = mMap.addPolyline(polylineOptions);
    }

    public void removePoint(){
        ImageButton redoMarkerButton = findViewById(R.id.redoMarker);
        redoMarkerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!markers.isEmpty() && !markerPoints.isEmpty()) {
                    // Get and remove the last marker added from the map and the list
                    Marker lastMarker = markers.get(markers.size() - 1);
                    lastMarker.remove();
                    markers.remove(markers.size() - 1);

                    // Also, remove the corresponding point from the markerPoints list
                    markerPoints.remove(markerPoints.size() - 1);

                    // Redraw the polyline to reflect the removal of the last point
                    drawPolyline();
                }
            }
        });
    }

    private void logMarkerCoordinates() {
        for (int i = 0; i < markers.size(); i++) {
            Marker marker = markers.get(i);
            LatLng position = marker.getPosition();
            Log.i("MarkerCoordinates", "Marker " + (i + 1) + ": Lat = " + position.latitude + ", Lng = " + position.longitude);
        }
    }

    public void setInfoWindowAdapter(){
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                // Inflate custom layout
                View view = getLayoutInflater().inflate(R.layout.custom_info_window, null);

                TextView title = view.findViewById(R.id.title);
                TextView snippet = view.findViewById(R.id.snippet);

                title.setText(marker.getTitle());
                snippet.setText(marker.getSnippet());

                return view;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Use default info contents if desired
                return null;
            }
        });
    }

    public void savePath(){
        final List<LatLng> waypoints = markerPoints;

        new Thread(new Runnable() {
            @Override
            public void run() {
                Path path = new Path();
                long pathId = db.pathDao().insertPath(path);

                // For each waypoint, create a Waypoint entity and insert it
                for (LatLng point : waypoints) {
                    Waypoint waypoint = new Waypoint(point.latitude, point.longitude, (int) pathId);
                    db.pathDao().insertWaypoint(waypoint);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Path saved successfully!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }
    public void openSaved(){
        Intent pathsIntent = new Intent(this, PathsActivity.class);
        startActivity(pathsIntent);
    }

    public void popUp(){
        Dialog myDialog = new Dialog(MainActivity.this);
        myDialog.setContentView(R.layout.save_popup);
        myDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        myDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        myDialog.setCancelable(true);
        myDialog.show();

        // Make sure to call findViewById on the dialog's view
        ImageButton btnConfirmSave = myDialog.findViewById(R.id.btnYes);
        ImageButton btnCancelSave = myDialog.findViewById(R.id.btnNo);

        btnConfirmSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePath();

                myDialog.dismiss();

                String dbPath = db.getOpenHelper().getWritableDatabase().getPath();
                Log.d("DatabasePath", "Database Path: " + dbPath);
            }
        });

        btnCancelSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myDialog.dismiss();
            }
        });
    }
}
