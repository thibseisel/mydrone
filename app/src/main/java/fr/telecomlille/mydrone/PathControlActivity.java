package fr.telecomlille.mydrone;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.List;

import fr.telecomlille.mydrone.drone.BebopDrone;
import fr.telecomlille.mydrone.view.DrawPathView;

public class PathControlActivity extends AppCompatActivity implements DrawPathView.PathListener {

    private int mRoomSizeX;
    private int mRoomSizeY;
    private float mInitialRealPosX, mInitialRealPosY;
    private float mScreenWidth, mScreenHeight;
    private BebopDrone mDrone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_control);
        DrawPathView pathView = (DrawPathView) findViewById(R.id.drawPathView);
//        mPathView.setPathListener(this);
        mInitialRealPosX = 1;
        mInitialRealPosY = 1;
        mScreenHeight = pathView.getHeight();
        mScreenWidth = pathView.getWidth();
        /*Intent caller = getIntent();
        ARDiscoveryDeviceService deviceService = caller.getParcelableExtra(MainActivity.EXTRA_DEVICE_SERVICE);
        mDrone = new BebopDrone(this, deviceService);*/
    }

    @Override
    protected void onStart() {
        super.onStart();
//        mDrone.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
//        mDrone.disconnect();
    }

    @Override
    public void onPathFinished(final List<float[]> pointsInPath) {
        float previousX = pointsInPath.get(0)[0];
        float previousY = pointsInPath.get(0)[1];
        float actualX, actualY;
        float distX, distY;
        float realDistLeft = mInitialRealPosX;
        float realDistRight = mScreenWidth - mInitialRealPosX;
        float realDistFor = mInitialRealPosY;
        float realDistBack = mScreenHeight - mInitialRealPosY;
        float timeToMove;

        for (int i = 1; i < pointsInPath.size(); i++) {
            actualX = pointsInPath.get(i)[0];
            actualY = pointsInPath.get(i)[1];
            if (actualX <= previousX) {
                distX = (actualX - previousX) * realDistLeft / actualX;
                realDistRight += Math.abs(distX);
                realDistLeft -= Math.abs(distX);
            } else {
                distX = (actualX - previousX) * realDistRight / (mScreenWidth - actualX);
                realDistRight -= Math.abs(distX);
                realDistLeft += Math.abs(distX);
            }
            if (actualY <= previousY) {
                distY = (previousY - actualY) * realDistFor / actualY;
                realDistFor -= Math.abs(distY);
                realDistBack += Math.abs(distY);
            } else {
                distY = (previousY - actualY) * realDistBack / (mScreenHeight - actualY);
                realDistFor += Math.abs(distY);
                realDistBack -= Math.abs(distY);
            }
            previousX = actualX;
            previousY = actualY;
            if (Math.abs(distX) >= Math.abs(distY)) {
                timeToMove = (distX / 9) * 1000;
                mDrone.setFlag((byte) 1);
                mDrone.setRoll((byte) Math.round(50 * (distX / Math.abs(distX))));
                mDrone.setPitch((byte) Math.round(distY / timeToMove));
            } else {
                timeToMove = (distY / 9) * 1000;
                mDrone.setFlag((byte) 1);
                mDrone.setPitch((byte) Math.round(50 * (distY / Math.abs(distY))));
                mDrone.setRoll((byte) Math.round(distX / timeToMove));
            }

            // SLEEP FOR TIME = timeToMove
            // TODO Déporter les mouvements et l'attente sur un autre thread
            try {
                Thread.sleep(Math.round(timeToMove));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mDrone.setPitch((byte) 0);
            mDrone.setRoll((byte) 0);
            mDrone.setFlag((byte) 0);
        }

        mInitialRealPosX = realDistLeft;
        mInitialRealPosY = realDistFor;
    }
}
