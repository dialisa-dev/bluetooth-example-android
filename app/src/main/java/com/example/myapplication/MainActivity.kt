package com.hge.copd.activities

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import com.danavi.danavibluetoothmanager.bluetooth.DanaviBluetoothManager
import com.danavi.danavibluetoothmanager.data.Constants
import com.danavi.danavibluetoothmanager.data.EmittedEvent
import com.danavi.danavibluetoothmanager.data.OximeterMeasurement
import com.hge.copd.R
import com.hge.copd.helpers.DeviceClient
import com.hge.copd.models.Responce
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.reflect.Constructor
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : BaseActivity() {
    private var danaviBluetoothManager: DanaviBluetoothManager? = null
    private var discoveredMac = ArrayList<String>()
    private val TAG = "MAIN_ACTIVITY"
    private lateinit var btStartButton: AppCompatButton
    private lateinit var tvBluthStatus: AppCompatTextView
    private lateinit var lltop:LinearLayout
    private var isSaved=false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btStartButton = findViewById(R.id.btStartButton)
        tvBluthStatus = findViewById(R.id.tvBluthStatus)
        lltop=findViewById(R.id.lltop);
        // REGISTER RECEIVER
        initLoader()
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.EVENT_BLUETOOTH_DANAVI)
        intentFilter.addAction(Constants.ACTION_SCAN_FINISHED)
        intentFilter.addAction(Constants.ACTION_DEVICE_DISCOVERED)
        intentFilter.addAction(Constants.ACTION_DEVICE_CONNECTED)
        intentFilter.addAction(Constants.ACTION_DEVICE_DISCONNECTED)
        intentFilter.addAction(Constants.ACTION_DEVICE_FAILED_TO_CONNECT)
        intentFilter.addAction(Constants.ACTION_DEVICE_READY_TO_TAKE_MEASUREMENT)
        intentFilter.addAction(Constants.ACTION_OXIMETER_LIVE_DATA)
        intentFilter.addAction(Constants.ACTION_OXIMETER_MEASUREMENT_DONE)
        intentFilter.addAction(Constants.ACTION_OXIMETER_TIMER)
        intentFilter.addAction(Constants.ACTION_DANAVI_THERMOMETER_MEASUREMENT)
        this.registerReceiver(broadcastReceiver, intentFilter)

//        checkLocationPermission();
        btStartButton.setOnClickListener(View.OnClickListener {
            val intent = Intent()
            danaviBluetoothManager = DanaviBluetoothManager(this@MainActivity, intent, this@MainActivity)
            discoveredMac = ArrayList()
            val bluetoothState = danaviBluetoothManager!!.state
            if (bluetoothState == BluetoothAdapter.STATE_ON) {
                danaviBluetoothManager!!.scanDevices(Constants.DANAVI_OXIMETER_TYPE, 5.0)
                isSaved=false;
                tvBluthStatus!!.text = "Scaning Device"
            } else {
                Log.d(TAG, "Bluetooth state is not on! Prompt user to turn on the Bluetooth.")
            }
        })
    }

    /*    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

        }
    }*/
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val intentAction = intent.action
            try {
                if (intentAction == Constants.EVENT_BLUETOOTH_DANAVI) {
                    val e: EmittedEvent? = intent.extras!!.getParcelable(Constants.EVENT_BLUETOOTH_DANAVI)
                    if (e == null) {
                        Log.d(TAG, "null event")
                    } else {
                        val action = e.action
                        if (action == Constants.ACTION_DEVICE_DISCOVERED) {
                            onDeviceDiscovered(e)
                        } else if (action == Constants.ACTION_SCAN_FINISHED) {
                            onScanFinished(e)
                        } else if (action == Constants.ACTION_DEVICE_CONNECTED) {
                            onDeviceConnected(e)
                        } else if (action == Constants.ACTION_DEVICE_DISCONNECTED) {
                            onDeviceDisconnected(e)
                        } else if (action == Constants.ACTION_DEVICE_FAILED_TO_CONNECT) {
                            onDeviceFailedToConnect(e)
                        } else if (action == Constants.ACTION_DEVICE_READY_TO_TAKE_MEASUREMENT) {
                            onReadyTakeMeasurement(e)
                        } else if (action == Constants.ACTION_OXIMETER_LIVE_DATA) {
                            handleReceiveOximeterMeasurement(e)
                        } else if (action == Constants.ACTION_OXIMETER_TIMER) {
                            handleOximeterTimer(e)
                        } else if (action == Constants.ACTION_OXIMETER_MEASUREMENT_DONE) {
                            handleOximeterMeasurementDone(e)
                        } else if (action == Constants.ACTION_DANAVI_THERMOMETER_MEASUREMENT) {
                            handleReceiveThermometerMeasurement(e)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d(TAG, e.toString())
            }
        }
    }

    private fun onDeviceDiscovered(e: EmittedEvent) {
        discoveredMac.add(e.mac)
        Log.d(TAG, e.toString())
        tvBluthStatus!!.text = "Device Discovered"
    }

    private fun onScanFinished(e: EmittedEvent) {
        Log.d(TAG, e.toString())
        // CHECK DISCOVERED DEVICE LIST
        Log.d(TAG, discoveredMac.toString())
        if (discoveredMac.size == 1) {
            // ONLY 1 DEVICE FOUND, ATTEMPT TO CONNECT RIGHT AWAY
            danaviBluetoothManager!!.connectDevice(discoveredMac[0])
            tvBluthStatus!!.text = discoveredMac[0]
        } else if (discoveredMac.size == 0) {
            tvBluthStatus!!.text = "NO DEVICE FOUND"
            // UPDATE UI: NO DEVICE FOUND
        } else {
            tvBluthStatus!!.text = "MULTIPLE DEVICE FOUND"
        }
    }

    private fun onDeviceConnected(e: EmittedEvent) {
        Log.d(TAG, e.toString())
        // IF DEVICE IS CONNECTED, THEN START MEASUREMENT
        tvBluthStatus!!.text = "Device Connected"
        if (e.deviceType == Constants.DANAVI_EAR_THERMOMETER_TYPE) {
            danaviBluetoothManager!!.startDanaviEarThermometerMeasurement()
        } else if (e.deviceType == Constants.DANAVI_OXIMETER_TYPE) {
            danaviBluetoothManager!!.startDanaviOximeterMeasurement(true, true, 10, 20)
        }
    }

    private fun onDeviceDisconnected(e: EmittedEvent) {
        // UPDATE UI: PROMPT USER THAT DEVICE IS DISCONNECTED
//        tvBluthStatus.setText("DEVICE IS DISCONNECTED");
        Log.d(TAG, e.toString())

        val data = danaviBluetoothManager!!.retrieveOximeterData()
        if(!isSaved&&!data.contains("-1")&&data.length>6){
            sendData()
        }

    }
    private fun sendData(){
        val data = danaviBluetoothManager!!.retrieveOximeterData()
        if(data.length==0||data.contains("-1")) return
        var tempData=(data.toString()).substring(1)
        var myData=(tempData).split("\n")
        var myJsonArra= ArrayList<JSONObject>()
        for (groupData in myData){
            var jData=groupData.split(",")
            if (jData.size<4) continue
            var obj=JSONObject()
            obj.put("bloodOxygen",jData[0])
            obj.put("heartRate",jData[1])
            obj.put("pi",jData[2])
            obj.put("timestamp",jData[3])
            myJsonArra.add(obj)

        }
        tvBluthStatus!!.text = myJsonArra.toString()
        val map = HashMap<String?, String?>()
        map["do"] = "devices"
        map["user_agent"] =getUserAgent(this@MainActivity)
        map["device_data"] = myJsonArra.toString()
        map["device_mac"] =discoveredMac[0]
        deviceClient.setParams(map)
        deviceClient.makeRequest()
        tvBluthStatus!!.text = "Device Discovered"
    }
    private fun onDeviceFailedToConnect(e: EmittedEvent) {
        // UPDATE UI: PROMPT USER THAT CONNECTION ATTEMPT TO THE DEVICE HAS FAILED
        Log.d(TAG, e.toString())
        tvBluthStatus!!.text = "CONNECTION ATTEMPT TO THE DEVICE HAS FAILED"
    }

    private fun onReadyTakeMeasurement(e: EmittedEvent) {
        // UPDATE UI: PRINT INSTRUCTION FOR MEASUREMENT
        tvBluthStatus!!.text = e.toString()
        Log.d(TAG, e.toString())
    }

    private fun handleReceiveOximeterMeasurement(e: EmittedEvent) {
        // UPDATE UI: SHOW OXIMETER MEASUREMENT
        Log.d(TAG, e.toString())
        val measurement = e.value as OximeterMeasurement
        tvBluthStatus!!.text = measurement.toString()
        //Log.d(TAG, String.format("Blood Oxygen: %d, Heart Rate: %d, Pi: %d", measurement.bloodOxygen, measurement.heartRate, measurement.pi));
    }

    private fun handleOximeterTimer(e: EmittedEvent) {
        // UPDATE UI: SHOW REMAINING TIME
        Log.d(TAG, e.toString())
        tvBluthStatus!!.text = e.toString()
    }

    @Throws(JSONException::class)
    private fun handleOximeterMeasurementDone(e: EmittedEvent) {
        // UPDATE UI: INFORM USER THAT THEY HAVE FINISHED THE MEASUREMENT
        Log.d(TAG, e.toString())
        val data = danaviBluetoothManager!!.retrieveOximeterData()
        if(!isSaved&&!data.contains("-1")&&data.length>6){
            sendData()
        }
        // RETRIEVE THE OXIMETER MEASUREMENT FROM TEMP FILE

//       Log.d(TAG, data)
    }

    private fun handleReceiveThermometerMeasurement(e: EmittedEvent) {
        // UPDATE UI: SHOW TEMPERATURE MEASUREMENT
        tvBluthStatus!!.text = e.toString()
        Log.d(TAG, e.toString())
    }
    private val deviceClient: DeviceClient = object : DeviceClient(this){
        override fun onStart() {
            layoutLoading.visibility=View.VISIBLE
            lltop.visibility=View.GONE
        }

        override fun onError(type: String?) {
            layoutLoading.visibility=View.GONE
            lltop.visibility=View.VISIBLE
            danaviBluetoothManager!!.deleteOximeterData()
            isSaved=false
        }

        override fun onSuccess(appSession: String?) {
            layoutLoading.visibility=View.GONE
            lltop.visibility=View.VISIBLE
            danaviBluetoothManager!!.deleteOximeterData()
            tvBluthStatus!!.text = appSession
            isSaved=false
            Responce.parse(appSession)
        }

    }

    fun getUserAgent(mContext: Context): String? {
        var mUserAgent:String=""
        return try {
            val constructor: Constructor<WebSettings> = WebSettings::class.java.getDeclaredConstructor(Context::class.java, WebView::class.java)
            constructor.setAccessible(true)
            try {
                val settings: WebSettings = constructor.newInstance(mContext, null)
                settings.userAgentString
            } finally {
                constructor.setAccessible(false)
            }
        } catch (e: Exception) {
            val ua: String
            if (Thread.currentThread().name.equals("main", ignoreCase = true)) {
                val m_webview = WebView(mContext)
                ua = m_webview.settings.userAgentString
            } else {
                (mContext as Activity).runOnUiThread {
                    val webview = WebView(mContext)
                    mUserAgent = webview.settings.userAgentString
                }
                return mUserAgent
            }
            ua
        }
    }

}