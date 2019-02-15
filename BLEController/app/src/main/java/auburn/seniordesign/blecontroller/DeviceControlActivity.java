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

import java.lang.reflect.Field;
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

        ((EditText) (findViewById(R.id.LFTextEntry))).setText(Integer.toString(FieldCalculations.fieldDimensions[0]));
        ((EditText) (findViewById(R.id.CFTextEntry))).setText(Integer.toString(FieldCalculations.fieldDimensions[1]));
        ((EditText) (findViewById(R.id.RFTextEntry))).setText(Integer.toString(FieldCalculations.fieldDimensions[2]));
        ((EditText) (findViewById(R.id.OffBatSpeedEntry))).setText(Integer.toString(FieldCalculations.offBatSpeed));

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

    public void onClickWrite(View v){
        if(mBluetoothLeService != null) {
            byte[] msg = new byte[1];
            msg[0] = (byte) 0xAA;
            mBluetoothLeService.writeCustomCharacteristic(msg);
        }
    }

    public void onClickRead(View v){
        if(mBluetoothLeService != null) {
            byte[] msg;
            msg = mBluetoothLeService.readCustomCharacteristic();
            System.out.println("Received: " + msg.toString());
        }
    }

    public boolean sendBLEMessage(byte[] msg){
        if(mBluetoothLeService != null) {
            return mBluetoothLeService.writeCustomCharacteristic(msg);
        }
        else
        {
            return false;
        }
    }

    public byte[] recvBLEMessage(){
        if(mBluetoothLeService != null) {
            byte[] msg;
            msg = mBluetoothLeService.readCustomCharacteristic();
            return msg;
        }
        else
        {
            return null;
        }
    }

    public void saveFieldDimensions(View button) {

        byte[] speedMsg = {0x00,0x00};

        FieldCalculations.fieldDimensions[0] = Integer.parseInt(((EditText) (findViewById(R.id.LFTextEntry))).getText().toString());
        FieldCalculations.fieldDimensions[1] = Integer.parseInt(((EditText) (findViewById(R.id.CFTextEntry))).getText().toString());
        FieldCalculations.fieldDimensions[2] = Integer.parseInt(((EditText) (findViewById(R.id.RFTextEntry))).getText().toString());

        FieldCalculations.offBatSpeed = Integer.parseInt(((EditText) (findViewById(R.id.OffBatSpeedEntry))).getText().toString());

        speedMsg[0] = (byte) ((FieldCalculations.offBatSpeed & 0x00000F00) >> 8);
        speedMsg[0] = (byte) (speedMsg[0] | 0x40);
        speedMsg[1] = (byte) (FieldCalculations.offBatSpeed & 0x000000FF);

        sendBLEMessage(speedMsg);

        System.out.println("*****Field Dimensions are RF:" + FieldCalculations.fieldDimensions[0] + " CF:" +
                FieldCalculations.fieldDimensions[1] + " LF:" + FieldCalculations.fieldDimensions[2]);

        /**********************************************************************************
         * Once dimensions are entered, then we must switch view and create touch listeners
         *********************************************************************************/

        RadioGroup radioGroup = findViewById(R.id.modeSelection);
        int radioButtonID = radioGroup.getCheckedRadioButtonId();

        switch (radioButtonID) {
            //case R.id.p2pRadioButton:
            //    p2p = new FieldActivity.Point2Point();
            //    useP2P = true;
            //    break;

            case R.id.pitchRadioButton:
                setContentView(R.layout.pitching_main);
                FieldCalculations.useP2P = false;
                break;

            default:
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

                FieldCalculations.useP2P = false;
                break;
        }
    }

    public void backToDimensions(View view)
    {
        setContentView(R.layout.dimensions);

        ((EditText) (findViewById(R.id.LFTextEntry))).setText(Integer.toString(FieldCalculations.fieldDimensions[0]));
        ((EditText) (findViewById(R.id.CFTextEntry))).setText(Integer.toString(FieldCalculations.fieldDimensions[1]));
        ((EditText) (findViewById(R.id.RFTextEntry))).setText(Integer.toString(FieldCalculations.fieldDimensions[2]));
        ((EditText) (findViewById(R.id.OffBatSpeedEntry))).setText(Integer.toString(FieldCalculations.offBatSpeed));
    }

    public void sendThrowMsg(View view)
    {
        byte[] msg = {0x00,0x00};

        msg[0] = (byte) (msg[0] | 0x80);

        sendBLEMessage(msg);
    }

    public void throwPitch(View view)
    {
        byte[] msg = new byte[2];

        msg[0] = (byte) 0b10000000;
        msg[1] = 0b00000000;

        sendBLEMessage(msg);
    }


    //TODO: change this to press and hold messages
    public void adjustPitchSettings(View view)
    {
        byte[] msg = {0x00,0x00};

        switch(view.getId()) {
            case R.id.pitchSettingsDown:
                FieldCalculations.pitchSettings[1] = (short) (FieldCalculations.pitchSettings[1] - 1);
                msg[0] = (byte) ((FieldCalculations.pitchSettings[1] & 0x00000F00) >> 8);
                msg[0] = (byte) (msg[0] | 0b00010000);
                msg[1] = (byte) (FieldCalculations.pitchSettings[1] & 0x000000FF);
                break;

            case R.id.pitchSettingsLeft:
                FieldCalculations.pitchSettings[0] = (short) (FieldCalculations.pitchSettings[0] - 1);
                msg[0] = (byte) ((FieldCalculations.pitchSettings[0] & 0x00000F00) >> 8);
                msg[0] = (byte) (msg[0] | 0b00100000);
                msg[1] = (byte) (FieldCalculations.pitchSettings[0] & 0x000000FF);
                break;

            case R.id.pitchSettingsRight:
                FieldCalculations.pitchSettings[0] = (short) (FieldCalculations.pitchSettings[0] + 1);
                msg[0] = (byte) ((FieldCalculations.pitchSettings[0] & 0x00000F00) >> 8);
                msg[0] = (byte) (msg[0] | 0b00100000);
                msg[1] = (byte) (FieldCalculations.pitchSettings[0] & 0x000000FF);
                break;

            case R.id.pitchSettingsUp:
            default:
                FieldCalculations.pitchSettings[1] = (short) (FieldCalculations.pitchSettings[1] + 1);
                msg[0] = (byte) ((FieldCalculations.pitchSettings[1] & 0x00000F00) >> 8);
                msg[0] = (byte) (msg[0] | 0b00010000);
                msg[1] = (byte) (FieldCalculations.pitchSettings[1] & 0x000000FF);
                break;

        }

        sendBLEMessage(msg);
    }

    public void goToSettingsPage(View view)
    {
        setContentView(R.layout.settings);

        ((EditText)(findViewById(R.id.pllPop1))).setText(Integer.toString(FieldCalculations.pllSettings[0]));
        ((EditText)(findViewById(R.id.pllPop2))).setText(Integer.toString(FieldCalculations.pllAngle));
        ((EditText)(findViewById(R.id.pllLine1))).setText(Integer.toString(FieldCalculations.pllSettings[1]));
        ((EditText)(findViewById(R.id.pllGround1))).setText(Integer.toString(FieldCalculations.pllSettings[2]));
        ((EditText)(findViewById(R.id.plfPop1))).setText(Integer.toString(FieldCalculations.plfSettings[0]));
        ((EditText)(findViewById(R.id.plfPop2))).setText(Integer.toString(FieldCalculations.plfAngle));
        ((EditText)(findViewById(R.id.plfLine1))).setText(Integer.toString(FieldCalculations.plfSettings[1]));
        ((EditText)(findViewById(R.id.plfGround1))).setText(Integer.toString(FieldCalculations.plfSettings[2]));
        ((EditText)(findViewById(R.id.plcPop1))).setText(Integer.toString(FieldCalculations.plcSettings[0]));
        ((EditText)(findViewById(R.id.plcPop2))).setText(Integer.toString(FieldCalculations.plcAngle));
        ((EditText)(findViewById(R.id.plcLine1))).setText(Integer.toString(FieldCalculations.plcSettings[1]));
        ((EditText)(findViewById(R.id.plcGround1))).setText(Integer.toString(FieldCalculations.plcSettings[2]));
        ((EditText)(findViewById(R.id.pcfPop1))).setText(Integer.toString(FieldCalculations.pcfSettings[0]));
        ((EditText)(findViewById(R.id.pcfPop2))).setText(Integer.toString(FieldCalculations.pcfAngle));
        ((EditText)(findViewById(R.id.pcfLine1))).setText(Integer.toString(FieldCalculations.pcfSettings[1]));
        ((EditText)(findViewById(R.id.pcfGround1))).setText(Integer.toString(FieldCalculations.pcfSettings[2]));
        ((EditText)(findViewById(R.id.prcPop1))).setText(Integer.toString(FieldCalculations.prcSettings[0]));
        ((EditText)(findViewById(R.id.prcPop2))).setText(Integer.toString(FieldCalculations.prcAngle));
        ((EditText)(findViewById(R.id.prcLine1))).setText(Integer.toString(FieldCalculations.prcSettings[1]));
        ((EditText)(findViewById(R.id.prcGround1))).setText(Integer.toString(FieldCalculations.prcSettings[2]));
        ((EditText)(findViewById(R.id.prfPop1))).setText(Integer.toString(FieldCalculations.prfSettings[0]));
        ((EditText)(findViewById(R.id.prfPop2))).setText(Integer.toString(FieldCalculations.prfAngle));
        ((EditText)(findViewById(R.id.prfLine1))).setText(Integer.toString(FieldCalculations.prfSettings[1]));
        ((EditText)(findViewById(R.id.prfGround1))).setText(Integer.toString(FieldCalculations.prfSettings[2]));
        ((EditText)(findViewById(R.id.prlPop1))).setText(Integer.toString(FieldCalculations.prlSettings[0]));
        ((EditText)(findViewById(R.id.prlPop2))).setText(Integer.toString(FieldCalculations.prlAngle));
        ((EditText)(findViewById(R.id.prlLine1))).setText(Integer.toString(FieldCalculations.prlSettings[1]));
        ((EditText)(findViewById(R.id.prlGround1))).setText(Integer.toString(FieldCalculations.prlSettings[2]));
        ((EditText)(findViewById(R.id.p1bPop1))).setText(Integer.toString(FieldCalculations.p1bSettings[0]));
        ((EditText)(findViewById(R.id.p1bPop2))).setText(Integer.toString(FieldCalculations.p1bAngle));
        ((EditText)(findViewById(R.id.p1bLine1))).setText(Integer.toString(FieldCalculations.p1bSettings[1]));
        ((EditText)(findViewById(R.id.p1bGround1))).setText(Integer.toString(FieldCalculations.p1bSettings[2]));
        ((EditText)(findViewById(R.id.p2bPop1))).setText(Integer.toString(FieldCalculations.p2bSettings[0]));
        ((EditText)(findViewById(R.id.p2bPop2))).setText(Integer.toString(FieldCalculations.p2bAngle));
        ((EditText)(findViewById(R.id.p2bLine1))).setText(Integer.toString(FieldCalculations.p2bSettings[1]));
        ((EditText)(findViewById(R.id.p2bGround1))).setText(Integer.toString(FieldCalculations.p2bSettings[2]));
        ((EditText)(findViewById(R.id.p3bPop1))).setText(Integer.toString(FieldCalculations.p3bSettings[0]));
        ((EditText)(findViewById(R.id.p3bPop2))).setText(Integer.toString(FieldCalculations.p3bAngle));
        ((EditText)(findViewById(R.id.p3bLine1))).setText(Integer.toString(FieldCalculations.p3bSettings[1]));
        ((EditText)(findViewById(R.id.p3bGround1))).setText(Integer.toString(FieldCalculations.p3bSettings[2]));
        ((EditText)(findViewById(R.id.pssPop1))).setText(Integer.toString(FieldCalculations.pssSettings[0]));
        ((EditText)(findViewById(R.id.pssPop2))).setText(Integer.toString(FieldCalculations.pssAngle));
        ((EditText)(findViewById(R.id.pssLine1))).setText(Integer.toString(FieldCalculations.pssSettings[1]));
        ((EditText)(findViewById(R.id.pssGround1))).setText(Integer.toString(FieldCalculations.pssSettings[2]));
    }

    public void saveSettings(View view)
    {
        FieldCalculations.pllSettings[0] = Short.parseShort(((EditText)(findViewById(R.id.pllPop1))).getText().toString());
        FieldCalculations.pllAngle = Short.parseShort(((EditText)(findViewById(R.id.pllPop2))).getText().toString());
        FieldCalculations.pllSettings[1] = Short.parseShort(((EditText)(findViewById(R.id.pllLine1))).getText().toString());
        FieldCalculations.pllSettings[2] = Short.parseShort(((EditText)(findViewById(R.id.pllGround1))).getText().toString());
        FieldCalculations.plfSettings[0] = Short.parseShort(((EditText)(findViewById(R.id.plfPop1))).getText().toString());
        FieldCalculations.plfAngle = Short.parseShort(((EditText)(findViewById(R.id.plfPop2))).getText().toString());
        FieldCalculations.plfSettings[1] = Short.parseShort(((EditText)(findViewById(R.id.plfLine1))).getText().toString());
        FieldCalculations.plfSettings[2] = Short.parseShort(((EditText)(findViewById(R.id.plfGround1))).getText().toString());
        FieldCalculations.plcSettings[0] = Short.parseShort(((EditText)(findViewById(R.id.plcPop1))).getText().toString());
        FieldCalculations.plcAngle = Short.parseShort(((EditText)(findViewById(R.id.plcPop2))).getText().toString());
        FieldCalculations.plcSettings[1] = Short.parseShort(((EditText)(findViewById(R.id.plcLine1))).getText().toString());
        FieldCalculations.plcSettings[2] = Short.parseShort(((EditText)(findViewById(R.id.plcGround1))).getText().toString());
        FieldCalculations.pcfSettings[0] = Short.parseShort(((EditText)(findViewById(R.id.pcfPop1))).getText().toString());
        FieldCalculations.pcfAngle = Short.parseShort(((EditText)(findViewById(R.id.pcfPop2))).getText().toString());
        FieldCalculations.pcfSettings[1] = Short.parseShort(((EditText)(findViewById(R.id.pcfLine1))).getText().toString());
        FieldCalculations.pcfSettings[2] = Short.parseShort(((EditText)(findViewById(R.id.pcfGround1))).getText().toString());
        FieldCalculations.prcSettings[0] = Short.parseShort(((EditText)(findViewById(R.id.prcPop1))).getText().toString());
        FieldCalculations.prcAngle = Short.parseShort(((EditText)(findViewById(R.id.prcPop2))).getText().toString());
        FieldCalculations.prcSettings[1] = Short.parseShort(((EditText)(findViewById(R.id.prcLine1))).getText().toString());
        FieldCalculations.prcSettings[2] = Short.parseShort(((EditText)(findViewById(R.id.prcGround1))).getText().toString());
        FieldCalculations.prfSettings[0] = Short.parseShort(((EditText)(findViewById(R.id.prfPop1))).getText().toString());
        FieldCalculations.prfAngle = Short.parseShort(((EditText)(findViewById(R.id.prfPop2))).getText().toString());
        FieldCalculations.prfSettings[1] = Short.parseShort(((EditText)(findViewById(R.id.prfLine1))).getText().toString());
        FieldCalculations.prfSettings[2] = Short.parseShort(((EditText)(findViewById(R.id.prfGround1))).getText().toString());
        FieldCalculations.prlSettings[0] = Short.parseShort(((EditText)(findViewById(R.id.prlPop1))).getText().toString());
        FieldCalculations.prlAngle = Short.parseShort(((EditText)(findViewById(R.id.prlPop2))).getText().toString());
        FieldCalculations.prlSettings[1] = Short.parseShort(((EditText)(findViewById(R.id.prlLine1))).getText().toString());
        FieldCalculations.prlSettings[2] = Short.parseShort(((EditText)(findViewById(R.id.prlGround1))).getText().toString());
        FieldCalculations.p1bSettings[0] = Short.parseShort(((EditText)(findViewById(R.id.p1bPop1))).getText().toString());
        FieldCalculations.p1bAngle = Short.parseShort(((EditText)(findViewById(R.id.p1bPop2))).getText().toString());
        FieldCalculations.p1bSettings[1] = Short.parseShort(((EditText)(findViewById(R.id.p1bLine1))).getText().toString());
        FieldCalculations.p1bSettings[2] = Short.parseShort(((EditText)(findViewById(R.id.p1bGround1))).getText().toString());
        FieldCalculations.p2bSettings[0] = Short.parseShort(((EditText)(findViewById(R.id.p2bPop1))).getText().toString());
        FieldCalculations.p2bAngle = Short.parseShort(((EditText)(findViewById(R.id.p2bPop2))).getText().toString());
        FieldCalculations.p2bSettings[1] = Short.parseShort(((EditText)(findViewById(R.id.p2bLine1))).getText().toString());
        FieldCalculations.p2bSettings[2] = Short.parseShort(((EditText)(findViewById(R.id.p2bGround1))).getText().toString());
        FieldCalculations.p3bSettings[0] = Short.parseShort(((EditText)(findViewById(R.id.p3bPop1))).getText().toString());
        FieldCalculations.p3bAngle = Short.parseShort(((EditText)(findViewById(R.id.p3bPop2))).getText().toString());
        FieldCalculations.p3bSettings[1] = Short.parseShort(((EditText)(findViewById(R.id.p3bLine1))).getText().toString());
        FieldCalculations.p3bSettings[2] = Short.parseShort(((EditText)(findViewById(R.id.p3bGround1))).getText().toString());
        FieldCalculations.pssSettings[0] = Short.parseShort(((EditText)(findViewById(R.id.pssPop1))).getText().toString());
        FieldCalculations.pssAngle = Short.parseShort(((EditText)(findViewById(R.id.pssPop2))).getText().toString());
        FieldCalculations.pssSettings[1] = Short.parseShort(((EditText)(findViewById(R.id.pssLine1))).getText().toString());
        FieldCalculations.pssSettings[2] = Short.parseShort(((EditText)(findViewById(R.id.pssGround1))).getText().toString());

        setContentView(R.layout.dimensions);
    }

    public void positionButtonClicked(View view) {


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

        FieldCalculations.currentPosition = view.getId();
        view.setBackgroundColor(Color.RED);

        FieldCalculations.formatMessages();

        sendBLEMessage(FieldCalculations.angleMsg);
        sendBLEMessage(FieldCalculations.elevationMsg);
    }

    public void elevationButtonClicked(View view)
    {

        RadioGroup radioGroup = findViewById(R.id.elevationSelection);
        FieldCalculations.currentElevation = radioGroup.getCheckedRadioButtonId();

        FieldCalculations.formatMessages();

        sendBLEMessage(FieldCalculations.angleMsg);
        sendBLEMessage(FieldCalculations.elevationMsg);
    }





    /******************************************************************************************
     * BETA POINT2POINT CODE BELOW IN THIS CLASS
     *****************************************************************************************/

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
                actualDistance2FenceAtThrowingAngle = (int) (FieldCalculations.fieldDimensions[1] - (((FieldCalculations.fieldDimensions[1]-FieldCalculations.fieldDimensions[2])/(Math.PI/4)) * touchAngleFromPlate));
            }
            else if (touchAngleFromPlate < 0)
            {
                actualDistance2FenceAtThrowingAngle = (int) (FieldCalculations.fieldDimensions[1] - (((FieldCalculations.fieldDimensions[1]-FieldCalculations.fieldDimensions[0])/(Math.PI/4)) * touchAngleFromPlate));
            }
            else
            {
                actualDistance2FenceAtThrowingAngle = FieldCalculations.fieldDimensions[1];
            }

            if (machineAngleFromPlate > 0)
            {
                actualDistance2FenceAtMachineAngle = (int) (FieldCalculations.fieldDimensions[1] - (((FieldCalculations.fieldDimensions[1]-FieldCalculations.fieldDimensions[2])/(Math.PI/4)) * Math.abs(machineAngleFromPlate)));
            }
            else if (machineAngleFromPlate < 0)
            {
                actualDistance2FenceAtMachineAngle = (int) (FieldCalculations.fieldDimensions[1] - (((FieldCalculations.fieldDimensions[1]-FieldCalculations.fieldDimensions[0])/(Math.PI/4)) * Math.abs(machineAngleFromPlate)));
            }
            else
            {
                actualDistance2FenceAtThrowingAngle = FieldCalculations.fieldDimensions[1];
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
