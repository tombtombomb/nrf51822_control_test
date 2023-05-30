package com.example.administrator.nrf51822_control;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import com.example.administrator.nrf51822_control.UartService;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int PID_PARAMETER_SETTING = 3;
//    private static final int UART_PROFILE_READY = 10;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int FLY_INIT =10;
    private static final int FLY_STARTUP =11;
    private static final int FLY_LOCK =12;
    public static final String TAG = "nRF51822_controlor";
    private int mState = UART_PROFILE_DISCONNECTED;
    private int fly_state=0;
    private byte DOWN_BYTE1 = (byte) 0xAA;
    private byte DOWN_BYTE2 = (byte) 0xAF;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private Button btnConnectDisconnect,btnLock,btnSetting,btnStartup;
    private TextView etAccX,etAccY,etAccZ,etGyroX,etGyroY,etGyroZ;
    private RockerView rockerLeft;
    private RockerView rockerRight;
    //设置油门， yaw，pit，rol的初始值，美国手
    private volatile int VAL_THR = 0;
    private volatile int VAL_YAW = 0, VAL_ROL = 0, VAL_PIT = 0;

    //加速度计、陀螺仪、姿态角的数据
    public volatile static int VAL_ACC_X = 0;
    public volatile static int VAL_ACC_Y = 0;
    public volatile static int VAL_ACC_Z = 0;
    public volatile static int VAL_GYR_X = 0;
    public volatile static int VAL_GYR_Y = 0;
    public volatile static int VAL_GYR_Z = 0;
    private Timer timer;

    //执行更新任务
    TimerTask updataTask=new TimerTask() {
        @Override
        public void run() {
            //记录摇杆数据
            rockerData();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                  更新控件
                    etAccX.setText(String.valueOf(VAL_ACC_X));
                    etAccY.setText(String.valueOf(VAL_ACC_Y));
                    etAccZ.setText(String.valueOf(VAL_ACC_Z));
                    etGyroX.setText(String.valueOf(VAL_GYR_X));
                    etGyroY.setText(String.valueOf(VAL_GYR_Y));
                    etGyroZ.setText(String.valueOf(VAL_GYR_Z));
                    /*etAccX.setText(String.valueOf(VAL_YAW));
                    etAccY.setText(String.valueOf(VAL_THR));
                    etGyroX.setText(String.valueOf(VAL_ROL));
                    etGyroY.setText(String.valueOf(VAL_PIT));*/
                }
            });
        }
    };

    TimerTask sendTask=new TimerTask()
    {
        byte[] msg=new byte[25];
        @Override
        public void run() {
            if(fly_state==FLY_STARTUP)
            {
                //send data to service
                byte sum=0;
                msg[0]=DOWN_BYTE1;
                msg[1]=DOWN_BYTE2;
                msg[2]=0x03;
                msg[3]=0x14;
                msg[4]= (byte) ((VAL_THR>>8)&0xff);
                msg[5]= (byte) ((VAL_THR)&0xff);
                msg[6]= (byte) ((VAL_YAW>>8)&0xff);
                msg[7]= (byte) ((VAL_YAW)&0xff);
                msg[8]= (byte) ((VAL_ROL>>8)&0xff);
                msg[9]= (byte) ((VAL_ROL)&0xff);
                msg[10]= (byte) ((VAL_PIT>>8)&0xff);
                msg[11]= (byte) ((VAL_PIT)&0xff);
                msg[12]=0x01;
                msg[13]=0x00;
                msg[14]=0x00;
                msg[15]=0x00;
                msg[16]=0x00;
                msg[17]=0x00;
                msg[18]=0x00;
                msg[19]=0x00;
                msg[20]=0x00;
                msg[21]=0x00;
                msg[22]=0x00;
                msg[23]=0x00;
                for(int i=0;i<24;i++)
                    sum += msg[i];
                msg[24] = sum;
                mService.writeRXCharacteristic(msg);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controler_main);
        //保持屏幕长亮
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //横屏模式
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        btnConnectDisconnect=(Button) findViewById(R.id.controler_connect_bt);
        btnLock=(Button) findViewById(R.id.controler_lock_bt);
        btnSetting=findViewById(R.id.setting_bt);
        btnStartup=findViewById(R.id.startup_bt);
        etAccX= (TextView) findViewById(R.id.acceler_x_editview);
        etAccY= (TextView) findViewById(R.id.acceler_y_editview);
        etAccZ= (TextView) findViewById(R.id.acceler_z_editview);
        etGyroX= (TextView) findViewById(R.id.gyroscope_x_editview);
        etGyroY= (TextView) findViewById(R.id.gyroscope_y_editview);
        etGyroZ= (TextView) findViewById(R.id.gyroscope_z_editview);
        rockerLeft=findViewById(R.id.rockerView_left);
        rockerRight=findViewById(R.id.rockerView_right);
        timer=new Timer();
        service_init();
        // Handler Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else {
                    if (btnConnectDisconnect.getText().equals("Connect")){
                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice!=null)
                        {
                            mService.disconnect();
                        }
                    }
                }
            }
        });
        // Handler Lock button之前的send按钮改动的，没改万完
        btnLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

//        PID参数设置
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fly_state==FLY_INIT)
                {
                    Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                    startActivityForResult(intent,PID_PARAMETER_SETTING);
                }
            }
        });

        btnStartup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnStartup.getText().equals("Startup")){
                    fly_state=FLY_STARTUP;
                    btnStartup.setText("Down");
                }
                else
                {
                    fly_state=FLY_INIT;
                }
            }
        });
        // Set initial UI state
        timer.schedule(updataTask,1000,10);
    }
    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }
        public void onServiceDisconnected(ComponentName classname) {
            //     mService.disconnect(mDevice);
            mService = null;
        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final Intent mIntent = intent;
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
//                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");
                        fly_state=FLY_INIT;
                        timer.schedule(sendTask,1000,20);//必须放在这里
                        btnLock.setEnabled(true);
                        btnSetting.setEnabled(true);
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }

            //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
//                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        sendTask.cancel();
                        btnLock.setEnabled(false);
                        mState = UART_PROFILE_DISCONNECTED;
                        fly_state=FLY_INIT;
                        mService.close();
                    }
                });
            }

            //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }
            //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            parseData(txValue);//处理接受的数据
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }
            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }
        }
    };

    private void service_init() {
        /**
         * service:该参数通过Intent指定需要启动的service。
         mServiceConnection:该参数是ServiceConnnection对象，当绑定成功后，系统将调用serviceConnnection的onServiceConnected ()方法，
         当绑定意外断开后，系统将调用ServiceConnnection中的onServiceDisconnected方法。
         flags:该参数指定绑定时是否自动创建Service。如果指定为BIND_AUTO_CREATE，则自动创建，指定为0，则不自动创建。
         */
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        timer.cancel();
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

   @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    mService.connect(deviceAddress);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case PID_PARAMETER_SETTING:
                {
                    final byte[] msg=new byte[23];
                    final byte[] msg2=new byte[23];
                    byte sum=0;
//                    解析PID参数并通过蓝牙发送PID参数
                    if (resultCode == Activity.RESULT_OK && data != null)
                    {
                        Toast.makeText(this, "pi_Kp is "+data.getStringExtra("pi_Kp"), Toast.LENGTH_SHORT).show();

                        msg[0]=DOWN_BYTE1;
                        msg[1]=DOWN_BYTE2;
                        msg[2]=0x10;
                        msg[3]=0x12;
                        msg[4]= (byte) ((Short.parseShort(data.getStringExtra("pi_Kp"))>>8)&0xff);
                        msg[5]= (byte) (Short.parseShort(data.getStringExtra("pi_Kp"))&0xff);
                        msg[6]= (byte) ((Short.parseShort(data.getStringExtra("pi_Ki"))>>8)&0xff);
                        msg[7]= (byte) (Short.parseShort(data.getStringExtra("pi_Ki"))&0xff);
                        msg[8]= (byte) ((Short.parseShort(data.getStringExtra("pi_Kd"))>>8)&0xff);
                        msg[9]= (byte) (Short.parseShort(data.getStringExtra("pi_Kd"))&0xff);
                        msg[10]= (byte) ((Short.parseShort(data.getStringExtra("ro_Kp"))>>8)&0xff);
                        msg[11]= (byte) (Short.parseShort(data.getStringExtra("ro_Kp"))&0xff);
                        msg[12]=(byte) ((Short.parseShort(data.getStringExtra("ro_Ki"))>>8)&0xff);
                        msg[13]=(byte) (Short.parseShort(data.getStringExtra("ro_Ki"))&0xff);
                        msg[14]=(byte) ((Short.parseShort(data.getStringExtra("ro_Kd"))>>8)&0xff);
                        msg[15]=(byte) (Short.parseShort(data.getStringExtra("ro_Kd"))&0xff);
                        msg[16]=(byte) ((Short.parseShort(data.getStringExtra("ya_Kp"))>>8)&0xff);
                        msg[17]=(byte) (Short.parseShort(data.getStringExtra("ya_Kp"))&0xff);
                        msg[18]=(byte) ((Short.parseShort(data.getStringExtra("ya_Ki"))>>8)&0xff);
                        msg[19]=(byte) (Short.parseShort(data.getStringExtra("ya_Ki"))&0xff);
                        msg[20]=(byte) ((Short.parseShort(data.getStringExtra("ya_Kd"))>>8)&0xff);
                        msg[21]=(byte) (Short.parseShort(data.getStringExtra("ya_Kd"))&0xff);
                        for(int i=0;i<22;i++)
                            sum += msg[i];
                        msg[22] = sum;

                        sum=0;
                        msg2[0]=DOWN_BYTE1;
                        msg2[1]=DOWN_BYTE2;
                        msg2[2]=0x11;
                        msg2[3]=0x12;
                        msg2[4]= (byte) ((Short.parseShort(data.getStringExtra("gx_Kp"))>>8)&0xff);
                        msg2[5]= (byte) (Short.parseShort(data.getStringExtra("gx_Kp"))&0xff);
                        msg2[6]= (byte) ((Short.parseShort(data.getStringExtra("gx_Ki"))>>8)&0xff);
                        msg2[7]= (byte) (Short.parseShort(data.getStringExtra("gx_Ki"))&0xff);
                        msg2[8]= (byte) ((Short.parseShort(data.getStringExtra("gx_Kd"))>>8)&0xff);
                        msg2[9]= (byte) (Short.parseShort(data.getStringExtra("gx_Kd"))&0xff);
                        msg2[10]= (byte) ((Short.parseShort(data.getStringExtra("gy_Kp"))>>8)&0xff);
                        msg2[11]= (byte) (Short.parseShort(data.getStringExtra("gy_Kp"))&0xff);
                        msg2[12]=(byte) ((Short.parseShort(data.getStringExtra("gy_Ki"))>>8)&0xff);
                        msg2[13]=(byte) (Short.parseShort(data.getStringExtra("gy_Ki"))&0xff);
                        msg2[14]=(byte) ((Short.parseShort(data.getStringExtra("gy_Kd"))>>8)&0xff);
                        msg2[15]=(byte) (Short.parseShort(data.getStringExtra("gy_Kd"))&0xff);
                        msg2[16]=(byte) ((Short.parseShort(data.getStringExtra("gz_Kp"))>>8)&0xff);
                        msg2[17]=(byte) (Short.parseShort(data.getStringExtra("gz_Kp"))&0xff);
                        msg2[18]=(byte) ((Short.parseShort(data.getStringExtra("gz_Ki"))>>8)&0xff);
                        msg2[19]=(byte) (Short.parseShort(data.getStringExtra("gz_Ki"))&0xff);
                        msg2[20]=(byte) ((Short.parseShort(data.getStringExtra("gz_Kd"))>>8)&0xff);
                        msg2[21]=(byte) (Short.parseShort(data.getStringExtra("gz_Kd"))&0xff);
                        for(int i=0;i<22;i++)
                            sum += msg2[i];
                        msg2[22] = sum;

                        new Thread(){
                            @Override
                            public void run() {
                                super.run();
                                mService.writeRXCharacteristic(msg);
                                try {
                                    Thread.sleep(20);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                mService.writeRXCharacteristic(msg2);
                            }
                        }.start();
                    }
                 }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        }
        else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.popup_title)
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.popup_no, null)
                    .show();
        }
    }
    //   数据解析函数
    private void parseData(byte[] data){
        //飞控发来的数据
        int msgID;
        msgID=data[0];
        switch (msgID)
        {
            case ANOProtocol.HARDWARE_INFO:
                break;
            case ANOProtocol.CONTRAL_DATA:
                break;
            case ANOProtocol.D_DISTANCE:
                break;
            case ANOProtocol.D_LOCATION:
                break;
            case ANOProtocol.DISTANCE:
                break;
            case ANOProtocol.FLY_MODE:
                break;
            case ANOProtocol.FP_NUMBER:
                break;
            case ANOProtocol.GPS_INFO:
                break;
            case ANOProtocol.LOCATION:
                break;
            case ANOProtocol.LOCATION_SET:
                break;
            case ANOProtocol.LOCATION_SET2:
                break;
            case ANOProtocol.PID_INFO_1:
                break;
            case ANOProtocol.PID_INFO_2:
                break;
            case ANOProtocol.PID_INFO_3:
                break;
            case ANOProtocol.PID_INFO_4:
                break;
            case ANOProtocol.PID_INFO_5:
                break;
            case ANOProtocol.PID_INFO_6:
                break;
            case ANOProtocol.POSE_INFO:
                break;
            case ANOProtocol.POWER_INFO:
                break;
            case ANOProtocol.PWM_MOTOR_INFO:
                break;
            case ANOProtocol.RADIO_LINK_SET:
                break;
            case ANOProtocol.SENSOR_DATA://加速度计和陀螺仪数据
            {
                VAL_ACC_X=bytetoUshort(data[2],(data[3]));
                VAL_ACC_Y=bytetoUshort(data[4],(data[5]));
                VAL_ACC_Z=bytetoUshort(data[6],(data[7]));
                VAL_GYR_X=bytetoUshort(data[8],(data[9]));
                VAL_GYR_Y=bytetoUshort(data[10],(data[11]));
                VAL_GYR_Z=bytetoUshort(data[12],(data[13]));
            }
            break;
            case ANOProtocol.SONIC_ALTITUDE_INFO:
                break;
            case ANOProtocol.CHECK:

                break;
            case ANOProtocol.MSG_INFO:
                break;
            default:
                break;
        }
    }

    static short bytetoUshort(byte b1,byte b2)
    {
        //将两个byte转换成一个short
        short r = 0;
        r <<= 8;  //r左移8位
        r |= (b1 & 0x00ff);
        r <<= 8;
        r |= (b2 & 0x00ff);
        return r;
    }

    private void rockerData()
    {
        VAL_YAW=rockerLeft.getXposition();
        VAL_THR=rockerLeft.getYposition();
        VAL_PIT=rockerRight.getYposition();
        VAL_ROL=rockerRight.getXposition();
    }

}
