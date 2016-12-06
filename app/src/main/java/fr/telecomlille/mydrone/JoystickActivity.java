package fr.telecomlille.mydrone;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import fr.telecomlille.mydrone.drone.BebopDrone;
import fr.telecomlille.mydrone.view.BebopVideoView;
import fr.telecomlille.mydrone.view.JoystickView;

/**
 * Permet le pilotage du drône avec un ensemble de 2 Joysticks, placés sur les bords de l'écran.
 */
public class JoystickActivity extends AppCompatActivity implements BebopDrone.Listener {

    private static final String TAG = "JoystickActivity";

    /**
     * Sensibilité du joystick pour les déplacements. Plus la valeur est élevée, plus le drône
     * se déplace rapidement.
     * Cette valeur doit être comprise entre 0 (insensible) et 100 (très sensible).
     */
    private static final int SENSITIVITY = 50;

    private static final int LEVEL_LAND = 1;
    private static final int LEVEL_TAKEOFF = 0;

    private BebopVideoView mVideoView;
    private BebopDrone mDrone;
    /**
     * Callback appelé lorsque la position du doigt sur le Joystick gauche a changé.
     * Ce joystick contrôle les déplacements du drône dans le plan (X,Z).
     * Cela correspond à des déplacements "Pitch" (avant/arrière) et "Roll" (gauche/droite).
     */
    private final JoystickView.Listener mPitchRollListener = new JoystickView.Listener() {
        @Override
        public void onThumbPositionChanged(float angle, float strength) {
            double pitch = strength * SENSITIVITY * Math.sin(angle);
            double roll = strength * SENSITIVITY * Math.cos(angle);

            mDrone.setFlag(strength > 0 ? BebopDrone.FLAG_ENABLED : BebopDrone.FLAG_DISABLED);
            mDrone.setPitch((int) Math.round(pitch));
            mDrone.setRoll((int) Math.round(roll));
        }
    };
    /**
     * Callback appelé lorsque la position du doigt sur le Joystick droit a changé.
     * Ce joystick contrôle le "Yaw" (rotation gauche/droite) et le "Gaz" (altitude du drône).
     */
    private final JoystickView.Listener mYawGazListener = new JoystickView.Listener() {
        @Override
        public void onThumbPositionChanged(float angle, float strength) {
            double yaw = strength * SENSITIVITY * Math.cos(angle);
            double gaz = strength * SENSITIVITY * Math.sin(angle);

            mDrone.setFlag(strength > 0 ? BebopDrone.FLAG_ENABLED : BebopDrone.FLAG_DISABLED);
            mDrone.setYaw((int) Math.round(yaw));
            mDrone.setGaz((int) Math.round(gaz));
        }
    };
    private ImageView mBatteryIndicator;
    private ProgressBar mBatteryBar;
    private ImageButton mTakeoffLandButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joystick);

        ARDiscoveryDeviceService deviceService = getIntent()
                .getParcelableExtra(MainActivity.EXTRA_DEVICE_SERVICE);

        if (deviceService == null) {
            throw new IllegalStateException("Calling Activity must start this Activity with an " +
                    "ARDiscoveryDeviceService object passed as an extra (EXTRA_DEVICE_SERVICE).");
        }

        mDrone = new BebopDrone(this, deviceService);
        mDrone.addListener(this);

        initIHM();
    }

    private void initIHM() {
        mTakeoffLandButton = (ImageButton) findViewById(R.id.btn_takeoff_land);
        mVideoView = (BebopVideoView) findViewById(R.id.videoView);
        mBatteryIndicator = (ImageView) findViewById(R.id.battery_indicator);
        mBatteryBar = (ProgressBar) findViewById(R.id.batteryLevel);

        // Décollage et atterrissage
        mTakeoffLandButton.setOnClickListener(new View.OnClickListener() {
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
        // Atterrissage d'urgence
        findViewById(R.id.btn_emergency).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrone.emergency();
            }
        });

        ((JoystickView) findViewById(R.id.joystick_pitch_roll)).setJoystickListener(mPitchRollListener);
        ((JoystickView) findViewById(R.id.joystick_yawgaz)).setJoystickListener(mYawGazListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mDrone != null && !mDrone.connect()) {
            Toast.makeText(this, "Error while connecting to the drone.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mDrone != null && !mDrone.disconnect()) {
            Toast.makeText(this, "error while disconnecting to the drone.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        mDrone.dispose();
        super.onDestroy();
    }

    /**
     * Définit une action lorsqu'on relâche le bouton pour diminuer le volume.
     * Sert à prendre une photo.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            mDrone.takePicture();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
        Log.d(TAG, "onDroneConnectionChanged() called with: state = [" + state + "]");
    }

    @Override
    public void onBatteryChargeChanged(int batteryPercentage) {
        mBatteryBar.setProgress(batteryPercentage);
        mBatteryIndicator.setImageLevel(batteryPercentage);
    }

    @Override
    public void onPilotingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
        Log.d(TAG, "onPilotingStateChanged() called with: state = [" + state + "]");
        switch (state) {
            case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                mTakeoffLandButton.setImageLevel(LEVEL_TAKEOFF);
                break;
            case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
            case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                mTakeoffLandButton.setImageLevel(LEVEL_LAND);
                break;
        }
    }

    @Override
    public void onPictureTaken(ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {
        Log.d(TAG, "onPictureTaken() called with: error = [" + error + "]");
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
        Log.d(TAG, "onMatchingMediasFound() called with: nbMedias = [" + nbMedias + "]");
    }

    @Override
    public void onDownloadProgressed(String mediaName, int progress) {
        Log.d(TAG, "onDownloadProgressed() called");
    }

    @Override
    public void onDownloadComplete(String mediaName) {
        Log.d(TAG, "onDownloadComplete() called with: mediaName = [" + mediaName + "]");
    }
}
