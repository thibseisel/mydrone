package fr.telecomlille.mydrone.path;

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

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import java.util.List;

import fr.telecomlille.mydrone.MainActivity;
import fr.telecomlille.mydrone.R;
import fr.telecomlille.mydrone.drone.BebopDrone;
import fr.telecomlille.mydrone.view.DrawPathView;

public class PathControlActivity extends AppCompatActivity implements DrawPathView.PathListener, PathControlTask.TaskListener {

    private static final String TAG = "PathControlActivity";
    private BebopDrone mDrone;
    private DrawPathView mPathView;
    private PathControlTask mPathControlTask;
    private ImageButton mTakeoffLandButton;
    private float[] mInitialPosInRoom;
    private float mRoomSizeX;
    private float mRoomSizeY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_control);
        mPathView = (DrawPathView) findViewById(R.id.drawPathView);
        mPathView.setPathListener(this);
        Intent caller = getIntent();
        ARDiscoveryDeviceService deviceService = caller.getParcelableExtra(MainActivity.EXTRA_DEVICE_SERVICE);
        mDrone = new BebopDrone(this, deviceService);
        mTakeoffLandButton = (ImageButton) findViewById(R.id.btn_takeoff_land);
        showDimensionDialog();
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
    }

    @Override
    protected void onStart() {
        super.onStart();
        mDrone.connect();
    }

    private void showDimensionDialog() {
        final View dialogView = getLayoutInflater().inflate(R.layout.room_size_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Dimensions de la pièce")
                .setMessage("Placez le drône au centre de la pièce.")
                .setView(dialogView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
    protected void onStop() {
        super.onStop();
        mDrone.disconnect();
    }

    /*@Override
    protected void onDestroy() {
        mDrone.dispose();
        super.onDestroy();
    }*/

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
        Log.d(TAG, "onPathExecuted: path finished.Interrupted=" + interrupted);
        mInitialPosInRoom = initialCoordinates;
        Toast.makeText(this, "Path finished.", Toast.LENGTH_SHORT).show();
        mPathView.setDrawingEnabled(true);
    }
}
