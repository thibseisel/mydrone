package fr.telecomlille.mydrone.path;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import java.util.Locale;

import fr.telecomlille.mydrone.MainActivity;
import fr.telecomlille.mydrone.R;
import fr.telecomlille.mydrone.drone.BebopDrone;

public class MoveByActivity extends AppCompatActivity implements BebopDrone.Listener, View.OnClickListener {

    private static final String TAG = "MoveByActivity";
    private BebopDrone mDrone;
    private ImageButton mTakeoffLandButton;
    private SeekBar mMetersBar;
    private TextView mMetersText;
    private ViewGroup mParent;
    private ProgressDialog mConnectionDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move_by);

        ARDiscoveryDeviceService deviceService =
                getIntent().getParcelableExtra(MainActivity.EXTRA_DEVICE_SERVICE);

        if (deviceService == null)
            throw new IllegalStateException("Calling activity must pass EXTRA_DEVICE_SERVICE");

        mDrone = new BebopDrone(this, deviceService);
        mDrone.addListener(this);

        initIHM();
    }

    private void initIHM() {
        findViewById(R.id.btn_forward).setOnClickListener(this);
        findViewById(R.id.btn_backward).setOnClickListener(this);
        findViewById(R.id.btn_left).setOnClickListener(this);
        findViewById(R.id.btn_right).setOnClickListener(this);
        findViewById(R.id.btn_emergency).setOnClickListener(this);
        findViewById(R.id.btn_stop).setOnClickListener(this);

        mMetersText = (TextView) findViewById(R.id.meters);
        mParent = (ViewGroup) findViewById(R.id.activity_move_by);

        mTakeoffLandButton = (ImageButton) findViewById(R.id.btn_takeoff_land);
        mTakeoffLandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (mDrone.getFlyingState()) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                        mDrone.takeOff();
                        break;
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_TAKINGOFF:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        mDrone.land();
                        break;
                }
            }
        });

        mMetersBar = (SeekBar) findViewById(R.id.seekbar);
        mMetersBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar view, int progress, boolean fromUser) {
                mMetersText.setText(String.format(Locale.FRANCE, "%fm", progress / 100f));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
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
        Log.d(TAG, "onPilotingStateChanged() called with: state = [" + state + "]");
        switch (state) {
            case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                mTakeoffLandButton.setImageLevel(0);
                break;
            case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
            case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                mTakeoffLandButton.setImageLevel(1);
                break;
        }
    }

    @Override
    public void onPictureTaken(ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {
    }

    @Override
    public void configureDecoder(ARControllerCodec codec) {
    }

    @Override
    public void onFrameReceived(ARFrame frame) {
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
        Toast.makeText(this, "Finished", Toast.LENGTH_SHORT).show();
        mParent.setBackgroundColor(0xffffffff);
    }

    @Override
    public void onClick(View view) {
        if (mDrone != null) {
            float dist = mMetersBar.getProgress() / 100f;
            switch (view.getId()) {
                case R.id.btn_forward:
                    mDrone.moveBy(dist, 0, 0, 0);
                    mParent.setBackgroundColor(0xff000000);
                    break;
                case R.id.btn_backward:
                    mDrone.moveBy(-dist, 0, 0, 0);
                    mParent.setBackgroundColor(0xff000000);
                    break;
                case R.id.btn_left:
                    mDrone.moveBy(0, -dist, 0, 0);
                    mParent.setBackgroundColor(0xff000000);
                    break;
                case R.id.btn_right:
                    mDrone.moveBy(0, dist, 0, 0);
                    mParent.setBackgroundColor(0xff000000);
                    break;
                case R.id.btn_stop:
                    mDrone.moveBy(0, 0, 0, 0);
                    mParent.setBackgroundColor(0xffffffff);
                    break;
                case R.id.btn_emergency:
                    mDrone.emergency();
                    break;
            }
        }
    }
}
