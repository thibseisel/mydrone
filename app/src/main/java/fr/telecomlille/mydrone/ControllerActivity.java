package fr.telecomlille.mydrone;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
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

public class ControllerActivity extends AppCompatActivity implements BebopDrone.Listener {

    public static final int LEVEL_LAND = 1;
    public static final int LEVEL_TAKEOFF = 0;

    private static final String TAG = "ControllerActivity";
    private BebopVideoView mVideoView;
    private BebopDrone mDrone;
    private ImageView mBatteryIndicator;
    private ProgressBar mBatteryBar;
    private ImageButton mTakeoffLandButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

        ARDiscoveryDeviceService deviceService = getIntent()
                .getParcelableExtra(MainActivity.EXTRA_DEVICE_SERVICE);

        mDrone = new BebopDrone(this, deviceService);
        mDrone.addListener(this);

        initIHM();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mDrone != null && !ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING
                .equals(mDrone.getConnectionState())) {
            if (!mDrone.connect()) {
                Toast.makeText(this, "Error while connecting to the drone.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mDrone != null) {
            if (!mDrone.disconnect()) {
                Toast.makeText(this, "error while disconnecting to the drone.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        mDrone.dispose();
        super.onDestroy();
    }

    /**
     * Définit une action lorsqu'on relâche le bouton pour diminuer le volume.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            mDrone.takePicture();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Initialise les View depuis XML, et attribue un comportement aux boutons de pilotage.
     */
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
        // Monter en altitude
        findViewById(R.id.btn_gaz_up).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setPressed(true);
                        mDrone.setGaz((byte) 50);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setPressed(false);
                        mDrone.setGaz((byte) 0);
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
                        mDrone.setGaz((byte) -50);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setPressed(false);
                        mDrone.setGaz((byte) 0);
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
                        mDrone.setYaw((byte) 50);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setPressed(false);
                        mDrone.setYaw((byte) 0);
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
                        mDrone.setYaw((byte) -50);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setPressed(false);
                        mDrone.setYaw((byte) 0);
                        break;
                }
                return true;
            }
        });
        // Avancer
        findViewById(R.id.btn_forward).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setPressed(true);
                        mDrone.setPitch((byte) 50);
                        mDrone.setFlag((byte) 1);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setPressed(false);
                        mDrone.setPitch((byte) 0);
                        mDrone.setFlag((byte) 0);
                        break;
                }
                return true;
            }
        });
        // Reculer
        findViewById(R.id.btn_back).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setPressed(true);
                        mDrone.setPitch((byte) -50);
                        mDrone.setFlag((byte) 1);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setPressed(false);
                        mDrone.setPitch((byte) 0);
                        mDrone.setFlag((byte) 0);
                        break;
                }
                return true;
            }
        });
        // Aller à droite
        findViewById(R.id.btn_roll_right).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setPressed(true);
                        mDrone.setRoll((byte) 50);
                        mDrone.setFlag((byte) 1);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setPressed(false);
                        mDrone.setRoll((byte) 0);
                        mDrone.setFlag((byte) 0);
                        break;
                }
                return true;
            }
        });
        // Aller à gauche
        findViewById(R.id.btn_roll_left).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setPressed(true);
                        mDrone.setRoll((byte) -50);
                        mDrone.setFlag((byte) 1);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setPressed(false);
                        mDrone.setRoll((byte) 0);
                        mDrone.setFlag((byte) 0);
                        break;
                }
                return true;
            }
        });
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
        Toast.makeText(this, R.string.picture_saved, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void configureDecoder(ARControllerCodec codec) {
        //noinspection WrongThread
        mVideoView.configureDecoder(codec);
    }

    @Override
    public void onFrameReceived(ARFrame frame) {
        //noinspection WrongThread
        mVideoView.displayFrame(frame);
    }

    @Override
    public void onMatchingMediasFound(int nbMedias) {
        Log.d(TAG, "onMatchingMediasFound() called with: nbMedias = [" + nbMedias + "]");
    }

    @Override
    public void onDownloadProgressed(String mediaName, int progress) {
        Log.d(TAG, "onDownloadProgressed() called with: mediaName = [" + mediaName + "], progress = [" + progress + "]");
    }

    @Override
    public void onDownloadComplete(String mediaName) {
        Log.d(TAG, "onDownloadComplete() called with: mediaName = [" + mediaName + "]");
    }
}
