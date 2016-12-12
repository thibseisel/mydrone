package fr.telecomlille.mydrone.path;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import java.util.List;

import fr.telecomlille.mydrone.MainActivity;
import fr.telecomlille.mydrone.R;
import fr.telecomlille.mydrone.drone.BebopDrone;
import fr.telecomlille.mydrone.view.DrawPathView;

public class PathControlActivity extends AppCompatActivity
        implements DrawPathView.PathListener, PathControlTask.TaskListener, BebopDrone.Listener {

    private static final String TAG = "PathControlActivity";
    private static final String KEY_ROOM_X = "RoomX";
    private static final String KEY_ROOM_Y = "RoomY";

    private BebopDrone mDrone;
    private DrawPathView mPathView;
    private PathControlTask mPathControlTask;
    private float[] mInitialPosInRoom;
    private float mRoomSizeX;
    private float mRoomSizeY;
    private ProgressDialog mConnectionDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_control);
        mPathView = (DrawPathView) findViewById(R.id.drawPathView);
        mPathView.setPathListener(this);

        Intent caller = getIntent();
        ARDiscoveryDeviceService deviceService = caller.getParcelableExtra(MainActivity.EXTRA_DEVICE_SERVICE);
        mDrone = new BebopDrone(this, deviceService);
        mDrone.addListener(this);

        ImageButton mTakeoffLandButton = (ImageButton) findViewById(R.id.btn_takeoff_land);
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
        findViewById(R.id.btn_emergency).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrone.emergency();
            }
        });

        if (savedInstanceState != null) {
            mRoomSizeX = savedInstanceState.getInt(KEY_ROOM_X);
            mRoomSizeY = savedInstanceState.getInt(KEY_ROOM_Y);
        } else {
            showDimensionDialog();
        }
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

    @SuppressLint("InflateParams")
    private void showDimensionDialog() {
        final View dialogView = getLayoutInflater().inflate(R.layout.room_size_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.room_dimensions)
                .setMessage(R.string.path_control_explanation)
                .setCancelable(false)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText textRoomX = (EditText) dialogView.findViewById(R.id.txtRoomDimX);
                        mRoomSizeX = Float.parseFloat(textRoomX.getText().toString());
                        EditText textRoomY = (EditText) dialogView.findViewById(R.id.txtRoomDimY);
                        mRoomSizeY = Float.parseFloat(textRoomY.getText().toString());
                        mInitialPosInRoom = new float[]{mRoomSizeX/2, mRoomSizeY/2};
                        dialog.dismiss();
                    }
                }).create().show();
    }

    @Override
    protected void onDestroy() {
        mDrone.dispose();
        super.onDestroy();
    }

    @Override
    public void onPathFinished(final List<float[]> pointsInPath) {
        Log.d(TAG, "onPathFinished: received new path of size: " + pointsInPath.size());
        mPathControlTask = new PathControlTask(mDrone, mPathView, mInitialPosInRoom,
                new float[]{mRoomSizeX, mRoomSizeY}, pointsInPath, this);
        mPathControlTask.execute();
        mPathView.setDrawingEnabled(false);
    }

    @Override
    public void onPathCanceled() {
        Log.d(TAG, "onPathCanceled: Cancel current path !");
        mPathControlTask.cancel(true);
        mDrone.setFlag(BebopDrone.FLAG_DISABLED);
        mDrone.setPitch(0);
        mDrone.setRoll(0);
        mPathView.setDrawingEnabled(true);
    }

    @Override
    public void onPathExecuted(float[] initialCoordinates, boolean interrupted) {
        Log.d(TAG, "onPathExecuted: path finished. Interrupted=" + interrupted);
        mInitialPosInRoom = initialCoordinates;
        mPathView.setDrawingEnabled(true);
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
}
