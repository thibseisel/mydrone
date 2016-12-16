package fr.telecomlille.mydrone.recognition;

import android.os.AsyncTask;
import android.view.View;

import java.util.List;

import fr.telecomlille.mydrone.drone.BebopDrone;

/**
 * Created by Hélène on 15/12/2016.
 */

public class FaceFollowingTask extends AsyncTask<Void, Integer, Void>{

    private static final String TAG = "FaceFollowingTask";

    private BebopDrone mDrone;
    private float mScreenWidth, mScreenHeight;
    private int[] mFaceCenterCoordinates;

    public FaceFollowingTask(BebopDrone drone, View view, int[] faceCenterCoordinates) {
        mDrone = drone;
        mScreenHeight = view.getHeight();
        mScreenWidth = view.getWidth();
        mFaceCenterCoordinates = faceCenterCoordinates;
    }


    @Override
    protected Void doInBackground(Void... voids) {


        mDrone.setFlag((byte) 1);
        mDrone.setRoll(((int) (10 * (mFaceCenterCoordinates[0] - mScreenWidth/2 )/ Math.abs(mFaceCenterCoordinates[0] - mScreenWidth/2))));
        mDrone.setGaz(((int) (10 * (mScreenHeight/2- mFaceCenterCoordinates[1])/ Math.abs(mScreenHeight/2 - mFaceCenterCoordinates[1]))));

        if ((mFaceCenterCoordinates[0] < mScreenWidth /2 + 10) && (mFaceCenterCoordinates[0] > mScreenWidth/2 - 10)){
            mDrone.setRoll(0);
        }
        if ((mFaceCenterCoordinates[1] < mScreenHeight/2 + 10) && (mFaceCenterCoordinates[1] > mScreenHeight/2 - 10)){
            mDrone.setGaz(0);
        }

        return null;
    }
}
