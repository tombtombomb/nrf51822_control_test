package com.example.administrator.nrf51822_control;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SettingActivity extends Activity {
    private Button btn_ok;
    private EditText pi_Ki_et,pi_Kp_et,pi_Kd_et;
    private EditText ro_Ki_et,ro_Kp_et,ro_Kd_et;
    private EditText ya_Ki_et,ya_Kp_et,ya_Kd_et;
    private EditText gx_Ki_et,gx_Kp_et,gx_Kd_et;
    private EditText gy_Ki_et,gy_Kp_et,gy_Kd_et;
    private EditText gz_Ki_et,gz_Kp_et,gz_Kd_et;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_layout);
        btn_ok=findViewById(R.id.set_parameters);
        pi_Ki_et=findViewById(R.id.pitch_Ki_et);
        pi_Kp_et=findViewById(R.id.pitch_Kp_et);
        pi_Kd_et=findViewById(R.id.pitch_Kd_et);
        ro_Ki_et=findViewById(R.id.roll_Ki_et);
        ro_Kp_et=findViewById(R.id.roll_Kp_et);
        ro_Kd_et=findViewById(R.id.roll_Kd_et);
        ya_Ki_et=findViewById(R.id.yaw_Ki_et);
        ya_Kp_et=findViewById(R.id.yaw_Kp_et);
        ya_Kd_et=findViewById(R.id.yaw_Kd_et);
        gx_Ki_et=findViewById(R.id.gyx_Ki_et);
        gx_Kp_et=findViewById(R.id.gyx_Kp_et);
        gx_Kd_et=findViewById(R.id.gyx_Kd_et);
        gy_Ki_et=findViewById(R.id.gyy_Ki_et);
        gy_Kp_et=findViewById(R.id.gyy_Kp_et);
        gy_Kd_et=findViewById(R.id.gyy_Kd_et);
        gz_Ki_et=findViewById(R.id.gyz_Ki_et);
        gz_Kp_et=findViewById(R.id.gyz_Kp_et);
        gz_Kd_et=findViewById(R.id.gyz_Kd_et);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                b.putString("pi_Ki", pi_Ki_et.getText().toString() );
                b.putString("pi_Kp", pi_Kp_et.getText().toString() );
                b.putString("pi_Kd", pi_Kd_et.getText().toString() );
                b.putString("ro_Ki", ro_Ki_et.getText().toString() );
                b.putString("ro_Kp", ro_Kp_et.getText().toString() );
                b.putString("ro_Kd", ro_Kd_et.getText().toString() );
                b.putString("ya_Ki", ya_Ki_et.getText().toString() );
                b.putString("ya_Kp", ya_Kp_et.getText().toString() );
                b.putString("ya_Kd", ya_Kd_et.getText().toString() );
                b.putString("gx_Ki", gx_Ki_et.getText().toString() );
                b.putString("gx_Kp", gx_Ki_et.getText().toString() );
                b.putString("gx_Kd", gx_Ki_et.getText().toString() );
                b.putString("gy_Ki", gy_Ki_et.getText().toString() );
                b.putString("gy_Kp", gy_Ki_et.getText().toString() );
                b.putString("gy_Kd", gy_Ki_et.getText().toString() );
                b.putString("gz_Ki", gz_Ki_et.getText().toString() );
                b.putString("gz_Kp", gz_Ki_et.getText().toString() );
                b.putString("gz_Kd", gz_Ki_et.getText().toString() );
                Intent result = new Intent();
                result.putExtras(b);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });
    }
}
