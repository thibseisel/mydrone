package fr.telecomlille.mydrone;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class AccelerometerActivity extends AppCompatActivity implements SensorEventListener {

    private TextView infoText;
    private TextView moveText;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private StringBuilder mDirection;

    private int mFrameCount;
    private float[] linear_acceleration = new float[3];
    private float[] gravity = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accelerometer);
        infoText = (TextView) findViewById(R.id.txt_acceleroInfo);
        moveText = (TextView) findViewById(R.id.txt_acceleroMove);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        mDirection = new StringBuilder();

    }

    @Override
    protected void onStart() {
        super.onStart();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this, mSensor);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        final float alpha =  0.8f;
        mDirection.delete(0, mDirection.length());

        if (++mFrameCount % 5 == 0) {
/*
            // Isolate the force of gravity with the low-pass filter.
            gravity[0] = alpha * gravity[0] + (1 - alpha) * sensorEvent.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * sensorEvent.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * sensorEvent.values[2];

            // Remove the gravity contribution with the high-pass filter.
            linear_acceleration[0] = sensorEvent.values[0] - gravity[0];
            linear_acceleration[1] = sensorEvent.values[1] - gravity[1];
            linear_acceleration[2] = sensorEvent.values[2] - gravity[2]; */


            linear_acceleration[0] = sensorEvent.values[0];
            linear_acceleration[1] = sensorEvent.values[1];
            linear_acceleration[2] = sensorEvent.values[2];

            String infos = "axe x : " + linear_acceleration[0] + "  \naxe y : " + linear_acceleration[1] + "  \naxe z : "
                    + linear_acceleration[2];

            infoText.setText(infos);

        }

//        if (linear_acceleration[2] < 8.5){

 //       }

//        if (Math.abs(linear_acceleration[0]) + Math.abs(linear_acceleration[1]) > 3){
            if ((linear_acceleration[0] >= 5) && (linear_acceleration[1] >= 1)){
                mDirection.append(" ARRIERE DROITE");
            }
            else if ((linear_acceleration[0] <= -0.5) && (linear_acceleration[1] >= 1)){
                mDirection.append(" AVANT DROITE ");
            }
            else if ((linear_acceleration[0] >= 5) && (linear_acceleration[1] <= -1)){
                mDirection.append(" ARRIERE GAUCHE ");
            }
            else if ((linear_acceleration[0] <= -0.5) && (linear_acceleration[1] <= -1)){
                mDirection.append(" AVANT GAUCHE ");
            }
            else if (linear_acceleration[0] >= 5){
                mDirection.append(" ARRIERE ");
            }
            else if (linear_acceleration[0] <= -0.5){
                mDirection.append(" AVANT ");
            }
            else if (linear_acceleration[1] >= 2){
                mDirection.append(" DROITE ");
            }
            else if (linear_acceleration[1] <= -2){
                mDirection.append(" GAUCHE ");
            }
//        }



        moveText.setText(mDirection);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
