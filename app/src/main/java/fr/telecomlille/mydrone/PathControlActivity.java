package fr.telecomlille.mydrone;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import java.util.List;

import fr.telecomlille.mydrone.drone.BebopDrone;
import fr.telecomlille.mydrone.view.DrawPathView;

public class PathControlActivity extends AppCompatActivity implements DrawPathView.PathListener, PathControlTask.TaskListener {

    private static final String TAG = "PathControlActivity";
    private int mRoomSizeX;
    private int mRoomSizeY;
    private float[] mInitialRealPos;
    private float mScreenWidth, mScreenHeight;
    private BebopDrone mDrone;
    private DrawPathView mPathView;
    private PathControlTask mPathControlTask;
    private ImageButton mTakeoffLandButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_control);
        mPathView = (DrawPathView) findViewById(R.id.drawPathView);
        mPathView.setPathListener(this);
        mInitialRealPos = new float[]{1, 1};
        Intent caller = getIntent();
        ARDiscoveryDeviceService deviceService = caller.getParcelableExtra(MainActivity.EXTRA_DEVICE_SERVICE);
        mDrone = new BebopDrone(this, deviceService);
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
    }

    @Override
    protected void onStart() {
        super.onStart();
        mDrone.connect();
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
        mPathControlTask = new PathControlTask(mDrone, mPathView, mInitialRealPos, pointsInPath, this);
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
        mInitialRealPos = initialCoordinates;
        Toast.makeText(this, "Path finished.", Toast.LENGTH_SHORT).show();
        mPathView.setDrawingEnabled(true);
    }
}
