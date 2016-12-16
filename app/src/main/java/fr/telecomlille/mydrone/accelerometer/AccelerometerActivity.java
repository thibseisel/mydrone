package fr.telecomlille.mydrone.accelerometer;

import android.app.ProgressDialog;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import fr.telecomlille.mydrone.MainActivity;
import fr.telecomlille.mydrone.R;
import fr.telecomlille.mydrone.drone.BebopDrone;
import fr.telecomlille.mydrone.view.BebopVideoView;

public class AccelerometerActivity extends AppCompatActivity implements BebopDrone.Listener,
        SensorEventListener {

    public static final int SENSITIVITY = 20;

    private static final String TAG = "AccelerometerActivity";

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private BebopDrone mDrone;
    private BebopVideoView mVideoView;
    private ProgressBar mBatteryBar;
    private ImageView mBatteryIndicator;

    private ProgressDialog mConnectionDialog;

    // TODO Utiliser notre classe OrientationSensor ?

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accelerometer);

        ARDiscoveryDeviceService deviceService = getIntent()
                .getParcelableExtra(MainActivity.EXTRA_DEVICE_SERVICE);

        if (deviceService == null) {
            throw new IllegalStateException("Calling Activity must start this Activity with an " +
                    "ARDiscoveryDeviceService object passed as an extra (EXTRA_DEVICE_SERVICE).");
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);


        mDrone = new BebopDrone(this, deviceService);
        mDrone.addListener(this);

        initIHM();
    }

    private void initIHM() {
        mVideoView = (BebopVideoView) findViewById(R.id.videoView);
        mBatteryBar = (ProgressBar) findViewById(R.id.batteryLevel);
        mBatteryIndicator = (ImageView) findViewById(R.id.battery_indicator);

        // Décollage et atterrissage
        findViewById(R.id.btn_takeoff_land).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (mDrone.getFlyingState()) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                        mDrone.takeOff();
                        break;
                    // Atterir directement après le décollage ?
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_TAKINGOFF:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        mDrone.land();
                        break;
                }
            }
        });

        // Arrêt d'urgence
        findViewById(R.id.btn_emergency).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrone.emergency();
            }
        });

        // Monter en altitude
        findViewById(R.id.btn_gaz_up).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setPressed(true);
                        mDrone.setGaz(50);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setPressed(false);
                        mDrone.setGaz(0);
                        break;
                }
                return true;
            }
        });

        // Descendre en altitude
        findViewById(R.id.btn_gaz_down).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setPressed(true);
                        mDrone.setGaz(-50);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setPressed(false);
                        mDrone.setGaz(0);
                        break;
                }
                return true;
            }
        });

        // Pivoter sur la droite
        findViewById(R.id.btn_yaw_right).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setPressed(true);
                        mDrone.setYaw(50);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setPressed(false);
                        mDrone.setYaw(0);
                        break;
                }
                return true;
            }
        });

        // Pivoter sur la gauche
        findViewById(R.id.btn_yaw_left).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setPressed(true);
                        mDrone.setYaw(-50);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setPressed(false);
                        mDrone.setYaw(0);
                        break;
                }
                return true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);

        if (mDrone != null && !(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING
                .equals(mDrone.getConnectionState()))) {
            mConnectionDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionDialog.setIndeterminate(true);
            mConnectionDialog.setMessage(getString(R.string.connecting));
            mConnectionDialog.setCancelable(false);
            mConnectionDialog.show();

            if (!mDrone.connect()) {
                Toast.makeText(this, R.string.error_connecting, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this, mSensor);
    }

    @Override
    public void onBackPressed() {
        if (mDrone != null) {
            mConnectionDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionDialog.setIndeterminate(true);
            mConnectionDialog.setMessage(getString(R.string.disconnecting));
            mConnectionDialog.setCancelable(false);
            mConnectionDialog.show();

            if (!mDrone.disconnect()) {
                Toast.makeText(this, R.string.error_disconnecting, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        mDrone.dispose();
        super.onDestroy();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Prendre une photo avec le bouton "Volume bas"
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (mDrone != null) {
                mDrone.takePicture();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
        switch (state) {
            case ARCONTROLLER_DEVICE_STATE_RUNNING:
                mConnectionDialog.dismiss();
                break;
            case ARCONTROLLER_DEVICE_STATE_STOPPED:
                // Si la déconnexion est un succès, retour à l'activité précédente
                mConnectionDialog.dismiss();
                finish();
        }
    }

    @Override
    public void onBatteryChargeChanged(int batteryPercentage) {
        mBatteryBar.setProgress(batteryPercentage);
        mBatteryIndicator.setImageLevel(batteryPercentage);
    }

    @Override
    public void onPilotingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {

    }

    @Override
    public void onPictureTaken(ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {

    }

    @Override
    public void configureDecoder(ARControllerCodec codec) {
        mVideoView.configureDecoder(codec);
    }

    @Override
    public void onFrameReceived(ARFrame frame) {
        mVideoView.displayFrame(frame);
    }

    @Override
    public void onMatchingMediasFound(int nbMedias) {

    }

    @Override
    public void onDownloadProgressed(String mediaName, int progress) {

    }

    @Override
    public void onDownloadComplete(String mediaName) {

    }

    @Override
    public void onRelativeMoveFinished(float dX, float dY, float dZ, float dPsi, boolean isInterrupted) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] linearAcceleration = new float[3];
        linearAcceleration[0] = event.values[0];
        linearAcceleration[1] = event.values[1];
        linearAcceleration[2] = event.values[2];

        mDrone.setFlag(BebopDrone.FLAG_ENABLED);

        if ((linearAcceleration[0] >= 5) && (linearAcceleration[1] >= 1)) {
            mDrone.setPitch(-SENSITIVITY);
            mDrone.setRoll(SENSITIVITY);
        }
        //AVANT DROITE
        else if ((linearAcceleration[0] <= -0.5) && (linearAcceleration[1] >= 1)) {
            mDrone.setPitch(SENSITIVITY);
            mDrone.setRoll(SENSITIVITY);
        }
        //ARRIERE GAUCHE
        else if ((linearAcceleration[0] >= 5) && (linearAcceleration[1] <= -1)) {
            mDrone.setPitch(-SENSITIVITY);
            mDrone.setRoll(-SENSITIVITY);
        }
        //AVANT GAUCHE
        else if ((linearAcceleration[0] <= -0.5) && (linearAcceleration[1] <= -1)) {
            mDrone.setPitch(SENSITIVITY);
            mDrone.setRoll(-SENSITIVITY);
        }
        //ARRIERE
        else if (linearAcceleration[0] >= 5) {
            mDrone.setPitch(-SENSITIVITY);
            mDrone.setRoll(0);
        }
        //AVANT
        else if (linearAcceleration[0] <= -0.5) {
            mDrone.setPitch(SENSITIVITY);
            mDrone.setRoll(0);
        }
        //DROITE
        else if (linearAcceleration[1] >= 2) {
            mDrone.setPitch(0);
            mDrone.setRoll(SENSITIVITY);
        }
        //GAUCHE
        else if (linearAcceleration[1] <= -2) {
            mDrone.setPitch(0);
            mDrone.setRoll(-SENSITIVITY);
            // IMMOBILE
        } else {
            mDrone.setFlag(BebopDrone.FLAG_DISABLED);
            mDrone.setPitch(0);
            mDrone.setRoll(0);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int newAccuracy) {
        Log.d(TAG, "onAccuracyChanged() called with: sensor = [" + sensor
                + "], newAccuracy = [" + newAccuracy + "]");
    }
}
