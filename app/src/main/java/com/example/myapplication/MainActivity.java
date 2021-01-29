package com.example.myapplication;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.danavi.danavibluetoothmanager.bluetooth.DanaviBluetoothManager;
import com.danavi.danavibluetoothmanager.data.Constants;
import com.danavi.danavibluetoothmanager.data.EmittedEvent;
import com.danavi.danavibluetoothmanager.data.OximeterMeasurement;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private DanaviBluetoothManager danaviBluetoothManager;
    private ArrayList<String> discoveredMac = new ArrayList<String>();
    private final String TAG = "MAIN_ACTIVITY";

    // UI
    TextView btStatusTextView;
    Button scanButton;
    RadioButton oximeterRadioButton;
    TextView bloodOxygenTextView;
    TextView heartRateTextView;
    ListView deviceListView;
    TextView deviceStatusTextView;
    private String selectedDevice = "";

    private void handleUI() {
        btStatusTextView = (TextView) findViewById(R.id.bt_status_tv);
        scanButton = (Button) findViewById(R.id.scan_btn);
        oximeterRadioButton = (RadioButton) findViewById(R.id.oximeter_radio_btn);
        bloodOxygenTextView = (TextView) findViewById(R.id.ox_blood_oxygen_tv);
        heartRateTextView = (TextView) findViewById(R.id.ox_heart_rate_tv);
        deviceListView = findViewById(R.id.scan_device_list_div);
        deviceStatusTextView = findViewById(R.id.device_status_tv);

        // Set bluetooth status
        int bluetoothStatus = danaviBluetoothManager.getState();
        if (bluetoothStatus == BluetoothAdapter.STATE_ON) {
            btStatusTextView.setText("ON");
        } else {
            btStatusTextView.setText("OFF");
        }

        // Scan
        scanButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                scanButton.setClickable(false);
                scanButton.setBackgroundColor(Color.parseColor("#a89c9b"));
                danaviBluetoothManager.scanDevices(Constants.DANAVI_OXIMETER_TYPE, 10.0);
                deviceStatusTextView.setText("Status: Scanning for device...");
            }
        });

        // Device selection
        oximeterRadioButton.setChecked(true);
        oximeterRadioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedDevice = Constants.DANAVI_OXIMETER_TYPE;
            }
        });

        // Adapter
        @SuppressLint("ResourceType") final ArrayAdapter adapter = new ArrayAdapter(this, R.id.scan_device_list_div, discoveredMac);
        deviceListView.setAdapter(adapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String mac = (String) parent.getItemAtPosition(position);
                danaviBluetoothManager.connectDevice(mac);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // REGISTER RECEIVER
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.EVENT_BLUETOOTH_DANAVI);
        intentFilter.addAction(Constants.ACTION_SCAN_FINISHED);
        intentFilter.addAction(Constants.ACTION_DEVICE_DISCOVERED);
        intentFilter.addAction(Constants.ACTION_DEVICE_CONNECTED);
        intentFilter.addAction(Constants.ACTION_DEVICE_DISCONNECTED);
        intentFilter.addAction(Constants.ACTION_DEVICE_FAILED_TO_CONNECT);
        intentFilter.addAction(Constants.ACTION_DEVICE_READY_TO_TAKE_MEASUREMENT);
        intentFilter.addAction(Constants.ACTION_OXIMETER_LIVE_DATA);
        intentFilter.addAction(Constants.ACTION_OXIMETER_MEASUREMENT_DONE);
        intentFilter.addAction(Constants.ACTION_OXIMETER_TIMER);
        intentFilter.addAction(Constants.ACTION_DANAVI_THERMOMETER_MEASUREMENT);
        this.registerReceiver(broadcastReceiver, intentFilter);
        Intent intent = new Intent();

        // INIT BLUETOOTH MANAGER
        this.danaviBluetoothManager = new DanaviBluetoothManager(this, intent, this);

        // CHECK BLUETOOTH STATE
//        int bluetoothState = this.danaviBluetoothManager.getState();
//        if (bluetoothState == BluetoothAdapter.STATE_ON) {
//            // START DISCOVERY
//            this.danaviBluetoothManager.scanDevices(Constants.DANAVI_OXIMETER_TYPE, 5.0);
//        } else {
//            Log.d(TAG, "Bluetooth state is not on! Prompt user to turn on the Bluetooth.");
//        }

        handleUI();
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String intentAction = intent.getAction();
            try {
                if (intentAction.equals(Constants.EVENT_BLUETOOTH_DANAVI)) {
                    EmittedEvent e = intent.getExtras().getParcelable(Constants.EVENT_BLUETOOTH_DANAVI);

                    if (e == null) {
                        Log.d(TAG, "null event");
                    } else {
                        String action = e.action;
                        if (action.equals(Constants.ACTION_DEVICE_DISCOVERED)) {
                            onDeviceDiscovered(e);
                        } else if (action.equals(Constants.ACTION_SCAN_FINISHED)) {
                            onScanFinished(e);
                        } else if (action.equals(Constants.ACTION_DEVICE_CONNECTED)) {
                            onDeviceConnected(e);
                        } else if (action.equals(Constants.ACTION_DEVICE_DISCONNECTED)) {
                            onDeviceDisconnected(e);
                        } else if (action.equals(Constants.ACTION_DEVICE_FAILED_TO_CONNECT)) {
                            onDeviceFailedToConnect(e);
                        } else if (action.equals(Constants.ACTION_DEVICE_READY_TO_TAKE_MEASUREMENT)) {
                            onReadyTakeMeasurement(e);
                        } else if (action.equals(Constants.ACTION_OXIMETER_LIVE_DATA)) {
                            handleReceiveOximeterMeasurement(e);
                        } else if (action.equals(Constants.ACTION_OXIMETER_TIMER)) {
                            handleOximeterTimer(e);
                        } else if (action.equals(Constants.ACTION_OXIMETER_MEASUREMENT_DONE)) {
                            handleOximeterMeasurementDone(e);
                        } else if (action.equals(Constants.ACTION_DANAVI_THERMOMETER_MEASUREMENT)) {
                            handleReceiveThermometerMeasurement(e);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, String.valueOf(e));
            }
        }
    };

    private void onDeviceDiscovered(EmittedEvent e) {
        discoveredMac.add(e.mac);
        Log.d(TAG, e.toString());
    }

    @SuppressLint("SetTextI18n")
    private void onScanFinished(EmittedEvent e) {
        Log.d(TAG, e.toString());
        // CHECK DISCOVERED DEVICE LIST
        Log.d(TAG, String.valueOf(discoveredMac));

        if (discoveredMac.size() == 1) {
            // ONLY 1 DEVICE FOUND, ATTEMPT TO CONNECT RIGHT AWAY
            danaviBluetoothManager.connectDevice(discoveredMac.get(0));
        } else if (discoveredMac.size() == 0) {
            // UPDATE UI: NO DEVICE FOUND
        } else {
            // UPDATE UI: MORE THAN 1 DEVICE FOUND, PROMPT USER TO CHOOSE DEVICE
            deviceStatusTextView.setText("Status: Not found");
        }

        // Add device to list
    }

    @SuppressLint("SetTextI18n")
    private void onDeviceConnected(EmittedEvent e) {
        Log.d(TAG, e.toString());
        // IF DEVICE IS CONNECTED, THEN START MEASUREMENT
        if (e.deviceType.equals(Constants.DANAVI_EAR_THERMOMETER_TYPE)) {
            danaviBluetoothManager.startDanaviEarThermometerMeasurement();
        } else if (e.deviceType.equals(Constants.DANAVI_OXIMETER_TYPE)) {
            danaviBluetoothManager.startDanaviOximeterMeasurement(true, false, 10, 20);
        }
        deviceStatusTextView.setText("Status: CONNECTED");
    }

    @SuppressLint("SetTextI18n")
    private void onDeviceDisconnected(EmittedEvent e) {
        // UPDATE UI: PROMPT USER THAT DEVICE IS DISCONNECTED
        Log.d(TAG, e.toString());
        deviceStatusTextView.setText("Status: DISCONNECTED");
        scanButton.setClickable(true);
        scanButton.setBackgroundResource(android.R.drawable.btn_default);
    }

    private void onDeviceFailedToConnect(EmittedEvent e) {
        // UPDATE UI: PROMPT USER THAT CONNECTION ATTEMPT TO THE DEVICE HAS FAILED
        Log.d(TAG, e.toString());
    }

    private void onReadyTakeMeasurement(EmittedEvent e) {
        // UPDATE UI: PRINT INSTRUCTION FOR MEASUREMENT
        Log.d(TAG, e.toString());
    }

    @SuppressLint("DefaultLocale")
    private void handleReceiveOximeterMeasurement(EmittedEvent e) {
        // UPDATE UI: SHOW OXIMETER MEASUREMENT
        Log.d(TAG, e.toString());
        OximeterMeasurement measurement = (OximeterMeasurement) e.value;
        bloodOxygenTextView.setText(String.format("Blood Oxygen Level: %d%%", measurement.bloodOxygen));
        heartRateTextView.setText(String.format("Heart Rate: %d BPM", measurement.heartRate));
        //Log.d(TAG, String.format("Blood Oxygen: %d, Heart Rate: %d, Pi: %d", measurement.bloodOxygen, measurement.heartRate, measurement.pi));
    }

    private void handleOximeterTimer(EmittedEvent e) {
        // UPDATE UI: SHOW REMAINING TIME
        Log.d(TAG, e.toString());
    }

    private void handleOximeterMeasurementDone(EmittedEvent e) {
        // UPDATE UI: INFORM USER THAT THEY HAVE FINISHED THE MEASUREMENT
        Log.d(TAG, e.toString());

        // RETRIEVE THE OXIMETER MEASUREMENT FROM TEMP FILE
        String data = danaviBluetoothManager.retrieveOximeterData();
        Log.d(TAG, data);
    }

    private void handleReceiveThermometerMeasurement(EmittedEvent e) {
        // UPDATE UI: SHOW TEMPERATURE MEASUREMENT
        Log.d(TAG, e.toString());
    }
}