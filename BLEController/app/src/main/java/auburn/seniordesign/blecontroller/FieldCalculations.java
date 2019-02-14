package auburn.seniordesign.blecontroller;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.graphics.Color;

import java.io.OutputStream;
import java.io.InputStream;
import java.util.UUID;

public class FieldCalculations {

    public static int[] fieldDimensions = {300,300,300}; // [RF,CF,LF]
    public static boolean useP2P = false;
    public static int offBatSpeed;

    public static short[] pitchSettings = {2048,2048};

    public static short[] pllSettings = {2048,2048,2048};
    public static short[] plfSettings = {2048,2048,2048};
    public static short[] plcSettings = {2048,2048,2048};
    public static short[] pcfSettings = {2048,2048,2048};
    public static short[] prcSettings = {2048,2048,2048};
    public static short[] prfSettings = {2048,2048,2048};
    public static short[] prlSettings = {2048,2048,2048};
    public static short[] p1bSettings = {2048,2048,2048};
    public static short[] p2bSettings = {2048,2048,2048};
    public static short[] p3bSettings = {2048,2048,2048};
    public static short[] pssSettings = {2048,2048,2048};
    public static short pllAngle = 2048;
    public static short plfAngle = 2048;
    public static short plcAngle = 2048;
    public static short pcfAngle = 2048;
    public static short prcAngle = 2048;
    public static short prfAngle = 2048;
    public static short prlAngle = 2048;
    public static short p1bAngle = 2048;
    public static short p2bAngle = 2048;
    public static short p3bAngle = 2048;
    public static short pssAngle = 2048;

    public static int currentPosition;
    public static int currentElevation;


    public static byte[] angleMsg = {0x10,0x00};
    public static byte[] elevationMsg = {0x20,0x00};


    private OutputStream outputStream;
    private InputStream inputStream;
    //private boolean bluetoothConnected;
    private UUID uuid = UUID.fromString("00001532-1212-EFDE-1523-785FEABCD123");

    //public BluetoothAdapter btAdapter;
    //private BluetoothSocket btSocket;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");




    // Used to load the 'native-lib' library on application startup.
    //static {
    //    System.loadLibrary("native-lib");
   // }

    //@Override
    //public void onCreate(Bundle savedInstanceState) {
    //    super.onCreate(savedInstanceState);

        //BluetoothDevice device = null;

        //setContentView(R.layout.dimensions);


    //}






/*
    private void sendMessage(byte[] msg)
    {
        int ack = -1;
        boolean sent = false;
        boolean ackReceived = false;

        sent = super.sendBLEMessage(msg);

        if (!sent)
        {
            System.err.println("Failed to send: [" + Integer.toHexString(msg[0]) + "," + Integer.toHexString(msg[1]) + "]");
        }

    }

    */

















    public static void formatMessages()
    {


        switch (currentPosition)
        {
            case R.id.p1bButton:

                angleMsg[0] = (byte) ((p1bAngle & 0x00001F00) >> 8);
                angleMsg[1] = (byte) (p1bAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((p1bSettings[2] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (p1bSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((p1bSettings[1] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (p1bSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[0] = (byte) (0b00100000 | ((p1bSettings[0] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (p1bSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.p2bButton:

                angleMsg[0] = (byte) ((p2bAngle & 0x00001F00) >> 8);
                angleMsg[1] = (byte) (p2bAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((p2bSettings[2] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (p2bSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((p2bSettings[1] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (p2bSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[0] = (byte) (0b00100000 | ((p2bSettings[0] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (p2bSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.p3bButton:

                angleMsg[0] = (byte) ((p3bAngle & 0x00001F00) >> 8);
                angleMsg[1] = (byte) (p3bAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((p3bSettings[2] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (p3bSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((p3bSettings[1] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (p3bSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[0] = (byte) (0b00100000 | ((p3bSettings[0] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (p3bSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.pssButton:

                angleMsg[0] = (byte) ((pssAngle & 0x00001F00) >> 8);
                angleMsg[1] = (byte) (pssAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((pssSettings[2] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (pssSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((pssSettings[1] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (pssSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[0] = (byte) (0b00100000 | ((pssSettings[0] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (pssSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.prlButton:

                angleMsg[0] = (byte) ((prlAngle & 0x00001F00) >> 8);
                angleMsg[1] = (byte) (prlAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((prlSettings[2] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (prlSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((prlSettings[1] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (prlSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[0] = (byte) (0b00100000 | ((prlSettings[0] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (prlSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.prButton:

                angleMsg[0] = (byte) ((prfAngle & 0x00001F00) >> 8);
                angleMsg[1] = (byte) (prfAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((prfSettings[2] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (prfSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((prfSettings[1] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (prfSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[0] = (byte) (0b00100000 | ((prfSettings[0] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (prfSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.prcButton:

                angleMsg[0] = (byte) ((prcAngle & 0x00001F00) >> 8);
                angleMsg[1] = (byte) (prcAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((prcSettings[2] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (prcSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((prcSettings[1] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (prcSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[0] = (byte) (0b00100000 | ((prcSettings[0] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (prcSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.pcButton:

                angleMsg[0] = (byte) ((pcfAngle & 0x00001F00) >> 8);
                angleMsg[1] = (byte) (pcfAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((pcfSettings[2] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (pcfSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((pcfSettings[1] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (pcfSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[0] = (byte) (0b00100000 | ((pcfSettings[0] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (pcfSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.plcButton:

                angleMsg[0] = (byte) ((plcAngle & 0x00001F00) >> 8);
                angleMsg[1] = (byte) (plcAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((plcSettings[2] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (plcSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((plcSettings[1] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (plcSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[0] = (byte) (0b00100000 | ((plcSettings[0] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (plcSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.plButton:

                angleMsg[0] = (byte) ((plfAngle & 0x00001F00) >> 8);
                angleMsg[1] = (byte) (plfAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((plfSettings[2] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (plfSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((plfSettings[1] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (plfSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[0] = (byte) (0b00100000 | ((plfSettings[0] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (plfSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.pllButton:

                angleMsg[0] = (byte) ((pllAngle & 0x00001F00) >> 8);
                angleMsg[1] = (byte) (pllAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((pllSettings[2] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (pllSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[0] = (byte) (0b00100000 | ((pllSettings[1] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (pllSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[0] = (byte) (0b00100000 | ((pllSettings[0] & 0x00002F00) >> 8));
                        elevationMsg[1] = (byte) (pllSettings[0] & 0x000000FF);
                        break;
                }
                break;

        }
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();



}



