/*
 * Copyright (C) 2013 The Android Open Source Project
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

package auburn.seniordesign.blecontroller;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.RadioGroup;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    View.OnTouchListener pitchLayoutTouchListener =
            new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    adjustPitchSettings(v, event);

                    return false;
                }
            };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
    };

    private void clearUI() {
//        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TODO: Add entry point to dimensions layout here
        setContentView(R.layout.dimensions);

        ((EditText) (findViewById(R.id.LFTextEntry))).setText(Integer.toString(FieldersChoice.fieldDimensions[0]));
        ((EditText) (findViewById(R.id.CFTextEntry))).setText(Integer.toString(FieldersChoice.fieldDimensions[1]));
        ((EditText) (findViewById(R.id.RFTextEntry))).setText(Integer.toString(FieldersChoice.fieldDimensions[2]));
        ((EditText) (findViewById(R.id.OffBatSpeedEntry))).setText(Integer.toString(FieldersChoice.offBatSpeed));

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
/*
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
*/
        mDataField = (TextView) findViewById(R.id.data_value);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public boolean sendBLEMessage(byte[] msg){
        if(mBluetoothLeService != null)
        {
            System.out.println("Sending BLE Message: [" + String.format("%02X",msg[0]) + "," + String.format("%02X",msg[1]) + "," + String.format("%02X",msg[2]) + "]");
            return mBluetoothLeService.writeCustomCharacteristic(msg);
        }
        else
        {
            System.err.println("Failed to send BLE Message: [" +String.format("%02X", msg[0]) + "," + String.format("%02X",msg[1]) + "," + String.format("%02X",msg[2]) + "]");
            return false;
        }
    }

    public byte[] recvBLEMessage(){
        if(mBluetoothLeService != null) {
            byte[] msg;

            msg = mBluetoothLeService.readCustomCharacteristic();
            System.out.print("Receiving BLE Message: [");
            if (msg.length > 0)
                System.out.print(String.format("%02x",msg[0]));
            for (int i = 1; i < msg.length; i++)
            {
                System.out.print("," + String.format("%02x",msg[i]));
            }
            System.out.print("]\n");

            return msg;
        }
        else
        {
            System.err.println("Failed to receive BLE Message");
            return null;
        }
    }

    public void saveFieldDimensions(View button) {
        try {
            FieldersChoice.saveFieldDimensions(
                    Integer.parseInt(((EditText) (findViewById(R.id.LFTextEntry))).getText().toString()),
                    Integer.parseInt(((EditText) (findViewById(R.id.CFTextEntry))).getText().toString()),
                    Integer.parseInt(((EditText) (findViewById(R.id.RFTextEntry))).getText().toString()),
                    Integer.parseInt(((EditText) (findViewById(R.id.OffBatSpeedEntry))).getText().toString()));
        }
        catch (NumberFormatException e)
        {
            System.err.println(e.getMessage());
            ((EditText) (findViewById(R.id.LFTextEntry))).setText(Integer.toString(FieldersChoice.fieldDimensions[0]));
            ((EditText) (findViewById(R.id.CFTextEntry))).setText(Integer.toString(FieldersChoice.fieldDimensions[1]));
            ((EditText) (findViewById(R.id.RFTextEntry))).setText(Integer.toString(FieldersChoice.fieldDimensions[2]));
            ((EditText) (findViewById(R.id.OffBatSpeedEntry))).setText(Integer.toString(FieldersChoice.offBatSpeed));
        }

        FieldersChoice.formatSpeedMsg();

        sendBLEMessage(FieldersChoice.speedMsg);

        RadioGroup radioGroup = findViewById(R.id.modeSelection);

        switch (radioGroup.getCheckedRadioButtonId()) {
            //case R.id.p2pRadioButton:
            //    p2p = new FieldActivity.Point2Point();
            //    useP2P = true;
            //    break;

            case R.id.pitchRadioButton:
                setContentView(R.layout.pitching_main);
                findViewById(R.id.pitchSettingsUp).setOnTouchListener(pitchLayoutTouchListener);
                findViewById(R.id.pitchSettingsDown).setOnTouchListener(pitchLayoutTouchListener);
                findViewById(R.id.pitchSettingsLeft).setOnTouchListener(pitchLayoutTouchListener);
                findViewById(R.id.pitchSettingsRight).setOnTouchListener(pitchLayoutTouchListener);
                break;

            case R.id.fieldRadioButton:
                setContentView(R.layout.field_layout);
                findViewById(R.id.p1bButton).setBackgroundResource(android.R.drawable.btn_default);
                findViewById(R.id.p2bButton).setBackgroundResource(android.R.drawable.btn_default);
                findViewById(R.id.p3bButton).setBackgroundResource(android.R.drawable.btn_default);
                findViewById(R.id.pssButton).setBackgroundResource(android.R.drawable.btn_default);
                findViewById(R.id.prlButton).setBackgroundResource(android.R.drawable.btn_default);
                findViewById(R.id.prButton).setBackgroundResource(android.R.drawable.btn_default);
                findViewById(R.id.prcButton).setBackgroundResource(android.R.drawable.btn_default);
                findViewById(R.id.pcButton).setBackgroundResource(android.R.drawable.btn_default);
                findViewById(R.id.plcButton).setBackgroundResource(android.R.drawable.btn_default);
                findViewById(R.id.plButton).setBackgroundResource(android.R.drawable.btn_default);
                findViewById(R.id.pllButton).setBackgroundResource(android.R.drawable.btn_default);
                break;
        }
    }

    public void backToDimensions(View view)
    {
        setContentView(R.layout.dimensions);

        ((EditText) (findViewById(R.id.LFTextEntry))).setText(Integer.toString(FieldersChoice.fieldDimensions[0]));
        ((EditText) (findViewById(R.id.CFTextEntry))).setText(Integer.toString(FieldersChoice.fieldDimensions[1]));
        ((EditText) (findViewById(R.id.RFTextEntry))).setText(Integer.toString(FieldersChoice.fieldDimensions[2]));
        ((EditText) (findViewById(R.id.OffBatSpeedEntry))).setText(Integer.toString(FieldersChoice.offBatSpeed));
    }

    public void sendThrowMsg(View view)
    {
        sendBLEMessage(FieldersChoice.throwMsg);
    }

    public void sendEnableMsg(View view)
    {
        FieldersChoice.formatEnableMsg(((ToggleButton)view).isChecked()?(byte)0x01:(byte)0x00);
        sendBLEMessage(FieldersChoice.enableMsg);
    }

    public void goToHomePosition(View view)
    {
        sendBLEMessage(FieldersChoice.homeMsg);

        findViewById(R.id.p1bButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.p2bButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.p3bButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.pssButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.prlButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.prButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.prcButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.pcButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.plcButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.plButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.pllButton).setBackgroundResource(android.R.drawable.btn_default);
    }

    public void sendWheelEnableMsg(View view)
    {
        FieldersChoice.formatWheelEnableMsg(((ToggleButton)view).isChecked()?(byte)0x01:(byte)0x00);
        sendBLEMessage(FieldersChoice.wheelEnableMsg);
    }

    public void adjustPitchSettings(View view, MotionEvent event)
    {
        switch(view.getId()) {
            case R.id.pitchSettingsDown:
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
                {
                    FieldersChoice.formatDownMsg((byte) 1);
                }
                else if (event.getActionMasked() == MotionEvent.ACTION_UP)
                {
                    FieldersChoice.formatDownMsg((byte) 0);
                }

                sendBLEMessage(FieldersChoice.downMsg);
                break;

            case R.id.pitchSettingsLeft:
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
                {
                    FieldersChoice.formatLeftMsg((byte) 1);
                }
                else if (event.getActionMasked() == MotionEvent.ACTION_UP)
                {
                    FieldersChoice.formatLeftMsg((byte) 0);
                }

                sendBLEMessage(FieldersChoice.leftMsg);
                break;

            case R.id.pitchSettingsRight:
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
                {
                    FieldersChoice.formatRightMsg((byte) 1);
                }
                else if (event.getActionMasked() == MotionEvent.ACTION_UP)
                {
                    FieldersChoice.formatRightMsg((byte) 0);
                }

                sendBLEMessage(FieldersChoice.rightMsg);
                break;

            case R.id.pitchSettingsUp:
            default:
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
                {
                    FieldersChoice.formatUpMsg((byte) 1);
                }
                else if (event.getActionMasked() == MotionEvent.ACTION_UP)
                {
                    FieldersChoice.formatUpMsg((byte) 0);
                }

                sendBLEMessage(FieldersChoice.upMsg);
                break;
        }
    }

    public void goToSettingsPage(View view)
    {
        setContentView(R.layout.settings);

        ((EditText)(findViewById(R.id.pllPop1))).setText(Integer.toString(FieldersChoice.pllSettings[0]));
        ((EditText)(findViewById(R.id.pllAngle))).setText(Integer.toString(FieldersChoice.pllAngle));
        ((EditText)(findViewById(R.id.pllLine1))).setText(Integer.toString(FieldersChoice.pllSettings[1]));
        ((EditText)(findViewById(R.id.pllGround1))).setText(Integer.toString(FieldersChoice.pllSettings[2]));
        ((EditText)(findViewById(R.id.plfPop1))).setText(Integer.toString(FieldersChoice.plfSettings[0]));
        ((EditText)(findViewById(R.id.plfAngle))).setText(Integer.toString(FieldersChoice.plfAngle));
        ((EditText)(findViewById(R.id.plfLine1))).setText(Integer.toString(FieldersChoice.plfSettings[1]));
        ((EditText)(findViewById(R.id.plfGround1))).setText(Integer.toString(FieldersChoice.plfSettings[2]));
        ((EditText)(findViewById(R.id.plcPop1))).setText(Integer.toString(FieldersChoice.plcSettings[0]));
        ((EditText)(findViewById(R.id.plcAngle))).setText(Integer.toString(FieldersChoice.plcAngle));
        ((EditText)(findViewById(R.id.plcLine1))).setText(Integer.toString(FieldersChoice.plcSettings[1]));
        ((EditText)(findViewById(R.id.plcGround1))).setText(Integer.toString(FieldersChoice.plcSettings[2]));
        ((EditText)(findViewById(R.id.pcfPop1))).setText(Integer.toString(FieldersChoice.pcfSettings[0]));
        ((EditText)(findViewById(R.id.pcfAngle))).setText(Integer.toString(FieldersChoice.pcfAngle));
        ((EditText)(findViewById(R.id.pcfLine1))).setText(Integer.toString(FieldersChoice.pcfSettings[1]));
        ((EditText)(findViewById(R.id.pcfGround1))).setText(Integer.toString(FieldersChoice.pcfSettings[2]));
        ((EditText)(findViewById(R.id.prcPop1))).setText(Integer.toString(FieldersChoice.prcSettings[0]));
        ((EditText)(findViewById(R.id.prcAngle))).setText(Integer.toString(FieldersChoice.prcAngle));
        ((EditText)(findViewById(R.id.prcLine1))).setText(Integer.toString(FieldersChoice.prcSettings[1]));
        ((EditText)(findViewById(R.id.prcGround1))).setText(Integer.toString(FieldersChoice.prcSettings[2]));
        ((EditText)(findViewById(R.id.prfPop1))).setText(Integer.toString(FieldersChoice.prfSettings[0]));
        ((EditText)(findViewById(R.id.prfAngle))).setText(Integer.toString(FieldersChoice.prfAngle));
        ((EditText)(findViewById(R.id.prfLine1))).setText(Integer.toString(FieldersChoice.prfSettings[1]));
        ((EditText)(findViewById(R.id.prfGround1))).setText(Integer.toString(FieldersChoice.prfSettings[2]));
        ((EditText)(findViewById(R.id.prlPop1))).setText(Integer.toString(FieldersChoice.prlSettings[0]));
        ((EditText)(findViewById(R.id.prlAngle))).setText(Integer.toString(FieldersChoice.prlAngle));
        ((EditText)(findViewById(R.id.prlLine1))).setText(Integer.toString(FieldersChoice.prlSettings[1]));
        ((EditText)(findViewById(R.id.prlGround1))).setText(Integer.toString(FieldersChoice.prlSettings[2]));
        ((EditText)(findViewById(R.id.p1bPop1))).setText(Integer.toString(FieldersChoice.p1bSettings[0]));
        ((EditText)(findViewById(R.id.p1bAngle))).setText(Integer.toString(FieldersChoice.p1bAngle));
        ((EditText)(findViewById(R.id.p1bLine1))).setText(Integer.toString(FieldersChoice.p1bSettings[1]));
        ((EditText)(findViewById(R.id.p1bGround1))).setText(Integer.toString(FieldersChoice.p1bSettings[2]));
        ((EditText)(findViewById(R.id.p2bPop1))).setText(Integer.toString(FieldersChoice.p2bSettings[0]));
        ((EditText)(findViewById(R.id.p2bAngle))).setText(Integer.toString(FieldersChoice.p2bAngle));
        ((EditText)(findViewById(R.id.p2bLine1))).setText(Integer.toString(FieldersChoice.p2bSettings[1]));
        ((EditText)(findViewById(R.id.p2bGround1))).setText(Integer.toString(FieldersChoice.p2bSettings[2]));
        ((EditText)(findViewById(R.id.p3bPop1))).setText(Integer.toString(FieldersChoice.p3bSettings[0]));
        ((EditText)(findViewById(R.id.p3bAngle))).setText(Integer.toString(FieldersChoice.p3bAngle));
        ((EditText)(findViewById(R.id.p3bLine1))).setText(Integer.toString(FieldersChoice.p3bSettings[1]));
        ((EditText)(findViewById(R.id.p3bGround1))).setText(Integer.toString(FieldersChoice.p3bSettings[2]));
        ((EditText)(findViewById(R.id.pssPop1))).setText(Integer.toString(FieldersChoice.pssSettings[0]));
        ((EditText)(findViewById(R.id.pssAngle))).setText(Integer.toString(FieldersChoice.pssAngle));
        ((EditText)(findViewById(R.id.pssLine1))).setText(Integer.toString(FieldersChoice.pssSettings[1]));
        ((EditText)(findViewById(R.id.pssGround1))).setText(Integer.toString(FieldersChoice.pssSettings[2]));
    }

    public void saveSettings(View view)
    {
        try {
            FieldersChoice.pllSettings[0] = Short.parseShort(((EditText) (findViewById(R.id.pllPop1))).getText().toString());
            FieldersChoice.pllAngle = Short.parseShort(((EditText) (findViewById(R.id.pllAngle))).getText().toString());
            FieldersChoice.pllSettings[1] = Short.parseShort(((EditText) (findViewById(R.id.pllLine1))).getText().toString());
            FieldersChoice.pllSettings[2] = Short.parseShort(((EditText) (findViewById(R.id.pllGround1))).getText().toString());
            FieldersChoice.plfSettings[0] = Short.parseShort(((EditText) (findViewById(R.id.plfPop1))).getText().toString());
            FieldersChoice.plfAngle = Short.parseShort(((EditText) (findViewById(R.id.plfAngle))).getText().toString());
            FieldersChoice.plfSettings[1] = Short.parseShort(((EditText) (findViewById(R.id.plfLine1))).getText().toString());
            FieldersChoice.plfSettings[2] = Short.parseShort(((EditText) (findViewById(R.id.plfGround1))).getText().toString());
            FieldersChoice.plcSettings[0] = Short.parseShort(((EditText) (findViewById(R.id.plcPop1))).getText().toString());
            FieldersChoice.plcAngle = Short.parseShort(((EditText) (findViewById(R.id.plcAngle))).getText().toString());
            FieldersChoice.plcSettings[1] = Short.parseShort(((EditText) (findViewById(R.id.plcLine1))).getText().toString());
            FieldersChoice.plcSettings[2] = Short.parseShort(((EditText) (findViewById(R.id.plcGround1))).getText().toString());
            FieldersChoice.pcfSettings[0] = Short.parseShort(((EditText) (findViewById(R.id.pcfPop1))).getText().toString());
            FieldersChoice.pcfAngle = Short.parseShort(((EditText) (findViewById(R.id.pcfAngle))).getText().toString());
            FieldersChoice.pcfSettings[1] = Short.parseShort(((EditText) (findViewById(R.id.pcfLine1))).getText().toString());
            FieldersChoice.pcfSettings[2] = Short.parseShort(((EditText) (findViewById(R.id.pcfGround1))).getText().toString());
            FieldersChoice.prcSettings[0] = Short.parseShort(((EditText) (findViewById(R.id.prcPop1))).getText().toString());
            FieldersChoice.prcAngle = Short.parseShort(((EditText) (findViewById(R.id.prcAngle))).getText().toString());
            FieldersChoice.prcSettings[1] = Short.parseShort(((EditText) (findViewById(R.id.prcLine1))).getText().toString());
            FieldersChoice.prcSettings[2] = Short.parseShort(((EditText) (findViewById(R.id.prcGround1))).getText().toString());
            FieldersChoice.prfSettings[0] = Short.parseShort(((EditText) (findViewById(R.id.prfPop1))).getText().toString());
            FieldersChoice.prfAngle = Short.parseShort(((EditText) (findViewById(R.id.prfAngle))).getText().toString());
            FieldersChoice.prfSettings[1] = Short.parseShort(((EditText) (findViewById(R.id.prfLine1))).getText().toString());
            FieldersChoice.prfSettings[2] = Short.parseShort(((EditText) (findViewById(R.id.prfGround1))).getText().toString());
            FieldersChoice.prlSettings[0] = Short.parseShort(((EditText) (findViewById(R.id.prlPop1))).getText().toString());
            FieldersChoice.prlAngle = Short.parseShort(((EditText) (findViewById(R.id.prlAngle))).getText().toString());
            FieldersChoice.prlSettings[1] = Short.parseShort(((EditText) (findViewById(R.id.prlLine1))).getText().toString());
            FieldersChoice.prlSettings[2] = Short.parseShort(((EditText) (findViewById(R.id.prlGround1))).getText().toString());
            FieldersChoice.p1bSettings[0] = Short.parseShort(((EditText) (findViewById(R.id.p1bPop1))).getText().toString());
            FieldersChoice.p1bAngle = Short.parseShort(((EditText) (findViewById(R.id.p1bAngle))).getText().toString());
            FieldersChoice.p1bSettings[1] = Short.parseShort(((EditText) (findViewById(R.id.p1bLine1))).getText().toString());
            FieldersChoice.p1bSettings[2] = Short.parseShort(((EditText) (findViewById(R.id.p1bGround1))).getText().toString());
            FieldersChoice.p2bSettings[0] = Short.parseShort(((EditText) (findViewById(R.id.p2bPop1))).getText().toString());
            FieldersChoice.p2bAngle = Short.parseShort(((EditText) (findViewById(R.id.p2bAngle))).getText().toString());
            FieldersChoice.p2bSettings[1] = Short.parseShort(((EditText) (findViewById(R.id.p2bLine1))).getText().toString());
            FieldersChoice.p2bSettings[2] = Short.parseShort(((EditText) (findViewById(R.id.p2bGround1))).getText().toString());
            FieldersChoice.p3bSettings[0] = Short.parseShort(((EditText) (findViewById(R.id.p3bPop1))).getText().toString());
            FieldersChoice.p3bAngle = Short.parseShort(((EditText) (findViewById(R.id.p3bAngle))).getText().toString());
            FieldersChoice.p3bSettings[1] = Short.parseShort(((EditText) (findViewById(R.id.p3bLine1))).getText().toString());
            FieldersChoice.p3bSettings[2] = Short.parseShort(((EditText) (findViewById(R.id.p3bGround1))).getText().toString());
            FieldersChoice.pssSettings[0] = Short.parseShort(((EditText) (findViewById(R.id.pssPop1))).getText().toString());
            FieldersChoice.pssAngle = Short.parseShort(((EditText) (findViewById(R.id.pssAngle))).getText().toString());
            FieldersChoice.pssSettings[1] = Short.parseShort(((EditText) (findViewById(R.id.pssLine1))).getText().toString());
            FieldersChoice.pssSettings[2] = Short.parseShort(((EditText) (findViewById(R.id.pssGround1))).getText().toString());
        }
        catch (NumberFormatException e)
        {
            System.err.println(e.getMessage());

        }
        backToDimensions(null);
    }

    public void positionButtonClicked(View view)
    {
        findViewById(R.id.p1bButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.p2bButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.p3bButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.pssButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.prlButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.prButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.prcButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.pcButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.plcButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.plButton).setBackgroundResource(android.R.drawable.btn_default);
        findViewById(R.id.pllButton).setBackgroundResource(android.R.drawable.btn_default);

        FieldersChoice.currentPosition = view.getId();
        view.setBackgroundColor(Color.RED);

        FieldersChoice.formatMessages();

        sendBLEMessage(FieldersChoice.angleMsg);
        // I believe this should send both messages, but the hardware currently does not support it
        //sendBLEMessage(FieldersChoice.elevationMsg);
    }

    public void elevationButtonClicked(View view)
    {
        FieldersChoice.currentElevation = ((RadioGroup)(findViewById(R.id.elevationSelection))).getCheckedRadioButtonId();

        FieldersChoice.formatMessages();

        sendBLEMessage(FieldersChoice.elevationMsg);
        // I believe this should send both messages, but the hardware currently does not support it
        //sendBLEMessage(FieldersChoice.angleMsg);
    }

    /******************************************************************************************
     * BETA POINT2POINT CODE BELOW IN THIS CLASS
     *****************************************************************************************/
    /* This code could potentially be used to place the machine anywhere on the field and throw
     * to anywhere on the field. THIS HAS NOT BEEN TESTED
     */

    private class Point2Point {


        private boolean screenCalibrationComplete;
        private int[] touchCoordinates = new int[2];
        private int[] homePlateCoordinates = new int[2];
        private int[] centerfieldCoordinates = new int[2];
        private int[] machineCoordinates = new int[2];
        private int throwingAngle;
        private int throwingDistance;


        View.OnTouchListener mainTouchListener =
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                            touchCoordinates[0] = (int) event.getX();
                            touchCoordinates[1] = (int) event.getY();

                            System.out.println("Touch: (" + touchCoordinates[0] + "," + touchCoordinates[1] + ")");
                        }

                        return false;
                    }
                };

        View.OnClickListener mainClickListener =
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        calculatePolarCoordinates();
                    }
                };


        View.OnTouchListener plateTouchListener =
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                            homePlateCoordinates[0] = (int) event.getX();
                            homePlateCoordinates[1] = (int) event.getY();

                            System.out.println("HomePlate: (" + homePlateCoordinates[0] + "," + homePlateCoordinates[1] + ")");
                        }

                        return false;
                    }
                };
        View.OnClickListener plateClickListener =
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setContentView(R.layout.touch_centerfield);

                        View touchCenterfieldLayout = findViewById(R.id.touchCenterfieldLayout);
                        touchCenterfieldLayout.setOnTouchListener(centerfieldTouchListener);
                        touchCenterfieldLayout.setOnClickListener(centerfieldClickListener);
                    }
                };


        View.OnTouchListener centerfieldTouchListener =
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                            centerfieldCoordinates[0] = (int) event.getX();
                            centerfieldCoordinates[1] = (int) event.getY();

                            System.out.println("Centerfield: (" + centerfieldCoordinates[0] + "," + centerfieldCoordinates[1] + ")");
                        }

                        return false;
                    }
                };

        View.OnClickListener centerfieldClickListener =
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setContentView(R.layout.touch_machine);

                        View touchMachineLayout = findViewById(R.id.touchMachineLayout);
                        touchMachineLayout.setOnTouchListener(machineTouchListener);
                        touchMachineLayout.setOnClickListener(machineClickListener);
                    }
                };


        View.OnTouchListener machineTouchListener =
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                            machineCoordinates[0] = (int) event.getX();
                            machineCoordinates[1] = (int) event.getY();

                            System.out.println("Machine: (" + machineCoordinates[0] + "," + machineCoordinates[1] + ")");
                        }

                        return false;
                    }
                };

        View.OnClickListener machineClickListener =
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setContentView(R.layout.point2point);

                        screenCalibrationComplete = true;
                        View mainLayout = findViewById(R.id.point2pointLayout);
                        mainLayout.setOnTouchListener(mainTouchListener);
                        mainLayout.setOnClickListener(mainClickListener);
                    }
                };


        private void calibrateScreen()
        {
            if (!screenCalibrationComplete) {
                setContentView(R.layout.touch_home_plate);
                View touchPlateLayout = findViewById(R.id.touchHomeLayout);
                touchPlateLayout.setOnTouchListener(plateTouchListener);
                touchPlateLayout.setOnClickListener(plateClickListener);
            } else {
                setContentView(R.layout.point2point);
            }
        }

        private void calculatePolarCoordinates()
        {
            int pixelDistance2Centerfield = 0;
            int pixelDistance2Foulpole = 0;
            double touchAngleFromPlate = 0.0;
            int pixelDistance2Touch = 0;
            int pixelDistance2FenceAtThrowingAngle = 0;
            int actualDistance2FenceAtThrowingAngle = 0;
            int touchDistanceFromPlate = 0;
            double machineAngleFromPlate = 0.0;
            int pixelDistance2Machine = 0;
            int pixelDistance2FenceAtMachineAngle = 0;
            int actualDistance2FenceAtMachineAngle = 0;
            int machineDistanceFromPlate = 0;
            double machine2TouchAngle = 0.0;
            double machine2CenterfieldAngle = 0.0;
            int throwingVectorX = 0;
            int throwingVectorY = 0;


            // Calculate polar coordinates from home plate
            // (for stretching due to different field dimensions)

            touchAngleFromPlate = Math.atan((double) (homePlateCoordinates[0] - touchCoordinates[0]) / (homePlateCoordinates[1] - touchCoordinates[1])) * -1;

            machineAngleFromPlate = Math.atan((double) (homePlateCoordinates[0] - machineCoordinates[0]) / (homePlateCoordinates[1] - machineCoordinates[1])) * -1;

            // find unstretched distance
            pixelDistance2Centerfield = homePlateCoordinates[1] - centerfieldCoordinates[1];
            pixelDistance2Foulpole = (int) Math.sqrt(Math.pow(((double)((homePlateCoordinates[1] - centerfieldCoordinates[1]) * 13) / 20),2.0) +
                    Math.pow(homePlateCoordinates[0],2.0));

            pixelDistance2FenceAtThrowingAngle = (int) (pixelDistance2Centerfield - (Math.abs(touchAngleFromPlate) * ((double)(pixelDistance2Centerfield - pixelDistance2Foulpole) / (Math.PI / 4))));

            pixelDistance2FenceAtMachineAngle = (int) (pixelDistance2Centerfield - (Math.abs(machineAngleFromPlate) * ((double)(pixelDistance2Centerfield - pixelDistance2Foulpole) / (Math.PI / 4))));

            pixelDistance2Touch = (int) Math.sqrt((Math.pow(homePlateCoordinates[0]-touchCoordinates[0],2.0) + Math.pow(homePlateCoordinates[1]-touchCoordinates[1],2.0)));

            pixelDistance2Machine = (int) Math.sqrt((Math.pow(homePlateCoordinates[0]-machineCoordinates[0],2.0) + Math.pow(homePlateCoordinates[1] - machineCoordinates[1],2.0)));

            if (touchAngleFromPlate > 0)
            {
                actualDistance2FenceAtThrowingAngle = (int) (FieldersChoice.fieldDimensions[1] - (((FieldersChoice.fieldDimensions[1]- FieldersChoice.fieldDimensions[2])/(Math.PI/4)) * touchAngleFromPlate));
            }
            else if (touchAngleFromPlate < 0)
            {
                actualDistance2FenceAtThrowingAngle = (int) (FieldersChoice.fieldDimensions[1] - (((FieldersChoice.fieldDimensions[1]- FieldersChoice.fieldDimensions[0])/(Math.PI/4)) * touchAngleFromPlate));
            }
            else
            {
                actualDistance2FenceAtThrowingAngle = FieldersChoice.fieldDimensions[1];
            }

            if (machineAngleFromPlate > 0)
            {
                actualDistance2FenceAtMachineAngle = (int) (FieldersChoice.fieldDimensions[1] - (((FieldersChoice.fieldDimensions[1]- FieldersChoice.fieldDimensions[2])/(Math.PI/4)) * Math.abs(machineAngleFromPlate)));
            }
            else if (machineAngleFromPlate < 0)
            {
                actualDistance2FenceAtMachineAngle = (int) (FieldersChoice.fieldDimensions[1] - (((FieldersChoice.fieldDimensions[1]- FieldersChoice.fieldDimensions[0])/(Math.PI/4)) * Math.abs(machineAngleFromPlate)));
            }
            else
            {
                actualDistance2FenceAtThrowingAngle = FieldersChoice.fieldDimensions[1];
            }

            touchDistanceFromPlate = (int) (actualDistance2FenceAtThrowingAngle * ((double)pixelDistance2Touch/pixelDistance2FenceAtThrowingAngle));

            machineDistanceFromPlate = (int) (actualDistance2FenceAtMachineAngle * ((double) pixelDistance2Machine/pixelDistance2FenceAtMachineAngle));

            // Calculate polar coordinates from machine

            // Distance can be calculated by vector math
            //TODO: thowingVextorX & thowingVectorY are wrong
            throwingVectorY = (int) (touchDistanceFromPlate*Math.cos(touchAngleFromPlate)-machineDistanceFromPlate*Math.cos(machineAngleFromPlate));
            throwingVectorX = (int) (touchDistanceFromPlate*Math.sin(touchAngleFromPlate)-machineDistanceFromPlate*Math.sin(machineAngleFromPlate));
            throwingDistance = (int) (Math.sqrt(Math.pow(throwingVectorX,2.0) + Math.pow(throwingVectorY,2.0)));
            machine2TouchAngle = Math.atan((double) throwingVectorX / throwingVectorY);
            machine2CenterfieldAngle = Math.atan((double) (centerfieldCoordinates[0] - machineCoordinates[0]) / (centerfieldCoordinates[1] - machineCoordinates[1]));

            throwingAngle = (int) ((machine2TouchAngle - machine2CenterfieldAngle) * (180 / Math.PI));

            //System.out.println("pixelDistance2Centerfield: " + pixelDistance2Centerfield);
            //System.out.println("pixelDistance2FoulPole: " + pixelDistance2Foulpole);
            //System.out.println("touchAngleFromPlate: " + (touchAngleFromPlate * 180 / Math.PI));
            //System.out.println("pixelDistance2Touch: " + pixelDistance2Touch);
            //System.out.println("pixelDistance2FenceAtThrowingAngle: " + pixelDistance2FenceAtThrowingAngle);
            //System.out.println("actualDistance2FenceAtThrowingAngle: " + actualDistance2FenceAtThrowingAngle);
            //System.out.println("touchDistanceFromPlate: " + touchDistanceFromPlate);
            //System.out.println("machineAngleFromPlate: " + (machineAngleFromPlate * 180 / Math.PI));
            //System.out.println("pixelDistance2Machine: " + pixelDistance2Machine);
            //System.out.println("pixelDistance2FenceAtMachineAngle: " + pixelDistance2FenceAtMachineAngle);
            //System.out.println("actualDistance2FenceAtMachineAngle: " + actualDistance2FenceAtMachineAngle);
            //System.out.println("machineDistanceFromPlate: " + machineDistanceFromPlate);
            //System.out.println("machine2TouchAngle: " + (machine2TouchAngle * 180 / Math.PI));
            //System.out.println("machine2CenterfieldAngle: " + (machine2CenterfieldAngle * 180 / Math.PI));
            //System.out.println("throwingVectorX: " + throwingVectorX);
            //System.out.println("throwingVectorY: " + throwingVectorY);

            System.out.println("*****Throw " + throwingDistance + " ft @ " + throwingAngle + " degrees");
        }

    }
}
