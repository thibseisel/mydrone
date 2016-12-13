package fr.telecomlille.mydrone;

import android.app.ProgressDialog;
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
    private ProgressDialog mConnectionDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

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

    @Override
    protected void onStart() {
        super.onStart();
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
        mDrone.removeListener(this);
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
            return false;
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
        // Avancer
        findViewById(R.id.btn_forward).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setPressed(true);
                        mDrone.setPitch(50);
                        mDrone.setFlag(BebopDrone.FLAG_ENABLED);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setPressed(false);
                        mDrone.setPitch(0);
                        mDrone.setFlag(BebopDrone.FLAG_DISABLED);
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
                        mDrone.setPitch(-50);
                        mDrone.setFlag(BebopDrone.FLAG_ENABLED);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setPressed(false);
                        mDrone.setPitch(0);
                        mDrone.setFlag(BebopDrone.FLAG_DISABLED);
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
                        mDrone.setRoll(50);
                        mDrone.setFlag(BebopDrone.FLAG_ENABLED);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setPressed(false);
                        mDrone.setRoll(0);
                        mDrone.setFlag(BebopDrone.FLAG_DISABLED);
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
                        mDrone.setRoll(-50);
                        mDrone.setFlag(BebopDrone.FLAG_ENABLED);
                        break;
                    case MotionEvent.ACTION_UP:
                        view.setPressed(false);
                        mDrone.setRoll(0);
                        mDrone.setFlag(BebopDrone.FLAG_DISABLED);
                        break;
                }
                return true;
            }
        });
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
        mDrone.getLastFlightMedias();
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
        Log.d(TAG, "onDownloadProgressed() called with: mediaName = [" + mediaName + "], progress = [" + progress + "]");
    }

    @Override
    public void onDownloadComplete(String mediaName) {
        Log.d(TAG, "onDownloadComplete() called with: mediaName = [" + mediaName + "]");
        Toast.makeText(this, "Téléchargement terminé", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRelativeMoveFinished(float dX, float dY, float dZ, float dPsi, boolean isInterrupted) {

    }
}
