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
    public static int offBatSpeed = 50;

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


    private static final byte PACKET_HDR = 0x21;

    private static final byte ENABLE_MSG_HDR = 0x00;
    private static final byte THROW_MSG_HDR = 0x01;
    private static final byte ANGLE_MSG_HDR = 0x02;
    private static final byte ELEV_MSG_HDR = 0x03;
    private static final byte SPEED_MSG_HDR = 0x04;
    private static final byte RIGHT_MSG_HDR = 0x05;
    private static final byte LEFT_MSG_HDR = 0x06;
    private static final byte UP_MSG_HDR = 0x07;
    private static final byte DOWN_MSG_HDR = 0x08;

    private static final int WHEEL_DIAMETER = 10; //inches




    public static byte[] angleMsg = {PACKET_HDR,ANGLE_MSG_HDR,0x00,0x00};
    public static byte[] elevationMsg = {PACKET_HDR,ELEV_MSG_HDR,0x00,0x00};
    public static byte[] speedMsg = {PACKET_HDR,SPEED_MSG_HDR,0x00,0x00};
    public static byte[] throwMsg = {PACKET_HDR,THROW_MSG_HDR,0x00,0x01};
    public static byte[] upMsg = {PACKET_HDR,UP_MSG_HDR,0x00,0x00};
    public static byte[] downMsg = {PACKET_HDR,DOWN_MSG_HDR,0x00,0x00};
    public static byte[] leftMsg = {PACKET_HDR,LEFT_MSG_HDR,0x00,0x00};
    public static byte[] rightMsg = {PACKET_HDR,RIGHT_MSG_HDR,0x00,0x00};


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


    public static void saveFieldDimensions(int rightFieldDistance, int centerFieldDistance,
                                      int leftFieldDistance, int mph)
    {
        fieldDimensions[0] = rightFieldDistance;
        fieldDimensions[1] = centerFieldDistance;
        fieldDimensions[2] = leftFieldDistance;
        offBatSpeed = mph;

        System.out.println("*****Field Dimensions are RF:" + fieldDimensions[0] + " CF:" +
                fieldDimensions[1] + " LF:" + fieldDimensions[2]);
    }

    public static void formatSpeedMsg()
    {
        short speedCount;

        speedCount = (byte) (((88*offBatSpeed*12)/(double)(Math.PI*WHEEL_DIAMETER)) * (4095/(double)3450));
        speedMsg[2] = (byte) ((speedCount & 0xFF00) >> 8);
        speedMsg[3] = (byte) (speedCount & 0x00FF);
    }

    public static void formatUpMsg(byte status)
    {
        upMsg[2] = status;
    }

    public static void formatDownMsg(byte status)
    {
        downMsg[2] = status;
    }

    public static void formatLeftMsg(byte status)
    {
        leftMsg[2] = status;
    }

    public static void formatRightMsg(byte status)
    {
        rightMsg[2] = status;
    }

    public static void formatMessages()
    {
        switch (currentPosition)
        {
            case R.id.p1bButton:

                angleMsg[2] = (byte) ((p1bAngle & 0x00000F00) >> 8);
                angleMsg[3] = (byte) (p1bAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[2] = (byte) ((p1bSettings[2] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (p1bSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[2] = (byte) ((p1bSettings[1] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (p1bSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[2] = (byte) ((p1bSettings[0] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (p1bSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.p2bButton:

                angleMsg[2] = (byte) ((p2bAngle & 0x00000F00) >> 8);
                angleMsg[3] = (byte) (p2bAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[2] = (byte) ((p2bSettings[2] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (p2bSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[2] = (byte) ((p2bSettings[1] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (p2bSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[2] = (byte) ((p2bSettings[0] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (p2bSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.p3bButton:

                angleMsg[2] = (byte) ((p3bAngle & 0x00000F00) >> 8);
                angleMsg[3] = (byte) (p3bAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[2] = (byte) ((p3bSettings[2] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (p3bSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[2] = (byte) ((p3bSettings[1] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (p3bSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[2] = (byte) ((p3bSettings[0] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (p3bSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.pssButton:

                angleMsg[2] = (byte) ((pssAngle & 0x00000F00) >> 8);
                angleMsg[3] = (byte) (pssAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[2] = (byte) ((pssSettings[2] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (pssSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[2] = (byte) ((pssSettings[1] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (pssSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[2] = (byte) ((pssSettings[0] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (pssSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.prlButton:

                angleMsg[2] = (byte) ((prlAngle & 0x00000F00) >> 8);
                angleMsg[3] = (byte) (prlAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[2] = (byte) ((prlSettings[2] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (prlSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[2] = (byte) ((prlSettings[1] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (prlSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[2] = (byte) ((prlSettings[0] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (prlSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.prButton:

                angleMsg[2] = (byte) ((prfAngle & 0x00000F00) >> 8);
                angleMsg[3] = (byte) (prfAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[2] = (byte) ((prfSettings[2] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (prfSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[2] = (byte) ((prfSettings[1] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (prfSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[2] = (byte) ((prfSettings[0] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (prfSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.prcButton:

                angleMsg[2] = (byte) ((prcAngle & 0x00000F00) >> 8);
                angleMsg[3] = (byte) (prcAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[2] = (byte) ((prcSettings[2] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (prcSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[2] = (byte) ((prcSettings[1] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (prcSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[2] = (byte) ((prcSettings[0] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (prcSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.pcButton:

                angleMsg[2] = (byte) ((pcfAngle & 0x00000F00) >> 8);
                angleMsg[3] = (byte) (pcfAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[2] = (byte) ((pcfSettings[2] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (pcfSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[2] = (byte) ((pcfSettings[1] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (pcfSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[2] = (byte) ((pcfSettings[0] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (pcfSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.plcButton:

                angleMsg[2] = (byte) ((plcAngle & 0x00000F00) >> 8);
                angleMsg[3] = (byte) (plcAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[2] = (byte) ((plcSettings[2] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (plcSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[2] = (byte) ((plcSettings[1] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (plcSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[2] = (byte) ((plcSettings[0] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (plcSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.plButton:

                angleMsg[2] = (byte) ((plfAngle & 0x00000F00) >> 8);
                angleMsg[3] = (byte) (plfAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[2] = (byte) ((plfSettings[2] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (plfSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[2] = (byte) ((plfSettings[1] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (plfSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[2] = (byte) ((plfSettings[0] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (plfSettings[0] & 0x000000FF);
                        break;
                }
                break;

            case R.id.pllButton:

                angleMsg[2] = (byte) ((pllAngle & 0x00000F00) >> 8);
                angleMsg[3] = (byte) (pllAngle & 0x000000FF);

                switch (currentElevation)
                {
                    case R.id.groundRadioButton:
                        elevationMsg[2] = (byte) ((pllSettings[2] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (pllSettings[2] & 0x000000FF);
                        break;
                    case R.id.lineRadioButton:
                        elevationMsg[2] = (byte) ((pllSettings[1] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (pllSettings[1] & 0x000000FF);
                        break;
                    case R.id.flyRadioButton:
                    default:
                        elevationMsg[2] = (byte) ((pllSettings[0] & 0x00000F00) >> 8);
                        elevationMsg[3] = (byte) (pllSettings[0] & 0x000000FF);
                        break;
                }
                break;

        }
    }
}



