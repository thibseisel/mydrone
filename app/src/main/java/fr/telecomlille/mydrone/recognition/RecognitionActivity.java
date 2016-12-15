package fr.telecomlille.mydrone.recognition;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

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


public class RecognitionActivity extends AppCompatActivity implements BebopDrone.Listener {

    private BebopVideoView mVideoView;
    private BebopDrone mDrone;
    private ProgressDialog mConnectionDialog;
    private ToggleButton mFollowMe;
    private ImageButton mTakeoffLandButton;
//    private opencv_objdetect.CascadeClassifier mClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);

//        mClassifier = new opencv_objdetect.CascadeClassifier("file:///android_asset/haarcascade_frontalface_default.xml");
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
        mVideoView = (BebopVideoView) findViewById(R.id.videoView);

        mFollowMe = (ToggleButton) findViewById(R.id.btn_followme);

        mTakeoffLandButton = (ImageButton) findViewById(R.id.btn_takeoff_land);
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
        mDrone.dispose();
        super.onDestroy();
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
}
