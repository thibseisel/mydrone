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
import android.widget.ImageView;
import android.widget.Toast;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import fr.telecomlille.mydrone.MainActivity;
import fr.telecomlille.mydrone.R;
import fr.telecomlille.mydrone.drone.BebopDrone;
import fr.telecomlille.mydrone.view.DrawPathView;

import static fr.telecomlille.mydrone.ControllerActivity.LEVEL_LAND;
import static fr.telecomlille.mydrone.ControllerActivity.LEVEL_TAKEOFF;


public class PathControlActivity extends AppCompatActivity
        implements DrawPathView.PathListener, BebopDrone.Listener {

    private static final String TAG = "PathControlActivity";
    private static final String KEY_ROOM_X = "RoomX";
    private static final String KEY_ROOM_Y = "RoomY";

    private BebopDrone mDrone;
    private DrawPathView mPathView;
    private float mScreenWidth, mScreenHeight;
    private float[] mInitialPosInRoom;
    private ImageView mBatteryIndicator;
    private float mRoomSizeX;
    private float mRoomSizeY;
    private ProgressDialog mConnectionDialog;
    private ImageButton mTakeoffLandButton;
    private Queue<float[]> mMoveQueue;
    private float mRealDistLeft;
    private float mRealDistRight;
    private float mRealDistFor;
    private float mRealDistBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_control);
        mPathView = (DrawPathView) findViewById(R.id.drawPathView);
        mPathView.setPathListener(this);

        Intent caller = getIntent();
        mScreenHeight = mPathView.getHeight();
        mScreenWidth = mPathView.getWidth();
        mMoveQueue = new LinkedList<>();
        ARDiscoveryDeviceService deviceService = caller.getParcelableExtra(MainActivity.EXTRA_DEVICE_SERVICE);
        mDrone = new BebopDrone(this, deviceService);
        mDrone.addListener(this);

        mBatteryIndicator = (ImageView) findViewById(R.id.battery_indicator);
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
                        mInitialPosInRoom = new float[]{mRoomSizeX / 2, mRoomSizeY / 2};
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
        mPathView.setDrawingEnabled(false);
        float previousX = pointsInPath.get(0)[0];
        float previousY = pointsInPath.get(0)[1];
        float actualX, actualY;
        float distX, distY;
        mRealDistLeft = mInitialPosInRoom[0];
        mRealDistRight = mRoomSizeX - mInitialPosInRoom[0];
        mRealDistFor = mInitialPosInRoom[1];
        mRealDistBack = mRoomSizeY - mInitialPosInRoom[1];
        for (int i = 1; i < pointsInPath.size(); i++) {
            actualX = pointsInPath.get(i)[0];
            actualY = pointsInPath.get(i)[1];
            if (actualX <= previousX) {
                distX = (actualX - previousX) * mRealDistLeft / previousX;
            } else {
                distX = (actualX - previousX) * mRealDistRight / (mScreenWidth - previousX);
            }
            if (actualY <= previousY) {
                distY = (previousY - actualY) * mRealDistFor / previousY;
            } else {
                distY = (previousY - actualY) * mRealDistBack / (mScreenHeight - previousY);
            }
            mMoveQueue.add(new float[]{distY, distX});
            previousX = actualX;
            previousY = actualY;
        }

        if (!mMoveQueue.isEmpty()) {
            float[] nextCoordinates = mMoveQueue.poll();
            mDrone.moveTowards(nextCoordinates[0], nextCoordinates[1], 0, 0);
        }
    }

    @Override
    public void onPathCanceled() {
        Log.d(TAG, "onPathCanceled: Cancel current path !");
        mDrone.moveTowards(0, 0, 0, 0);
        mPathView.setDrawingEnabled(true);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void onRelativeMoveFinished(float dX, float dY, float dZ, float dPsi, boolean isInterrupted) {
        mRealDistBack += dX;
        mRealDistFor -= dX;
        mRealDistLeft += dY;
        mRealDistRight -= dY;
        mInitialPosInRoom[0] = mRealDistLeft;
        mInitialPosInRoom[1] = mRealDistFor;
        if (!mMoveQueue.isEmpty()) {
            float[] nextCoordinates = mMoveQueue.poll();
            mDrone.moveTowards(nextCoordinates[0], nextCoordinates[1], 0, 0);
        } else {
            mPathView.setDrawingEnabled(true);
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
        mBatteryIndicator.setImageLevel(batteryPercentage);
    }

    @Override
    public void onPilotingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
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
        // On ne prend pas de photos avec ce mode de pilotage
    }

    @Override
    public void configureDecoder(ARControllerCodec codec) {
        // On n'affiche pas de vidéo en temps réel avec ce mode de pilotage
    }

    @Override
    public void onFrameReceived(ARFrame frame) {
        // On n'affiche pas de vidéo en temps réel avec ce mode de pilotage
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
