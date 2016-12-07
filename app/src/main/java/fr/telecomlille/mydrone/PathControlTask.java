package fr.telecomlille.mydrone;

import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

import fr.telecomlille.mydrone.drone.BebopDrone;
import fr.telecomlille.mydrone.view.DrawPathView;

import static android.content.ContentValues.TAG;

/**
 * Tâche d'arrière plan permettant de faire suivre la trajectoire tracée à l'écran par le drône.
 * Etant donné que l'application doit attendre que le drône effectue son mouvement avant chaque
 * changement de direction, cette tâche est exécutée sur un autre thread pour éviter de bloquer
 * l'interface graphique.
 * @author Hélène
 */
public class PathControlTask extends AsyncTask<Void, Integer, float[]> {

    public static final String TAG = "PathControlTask";
    private BebopDrone mDrone;
    private float mScreenWidth, mScreenHeight;
    private float[] mInitialRealPos;
    private List<float[]> mPointsInPath;
    private TaskListener mTaskListener;

    public PathControlTask(BebopDrone drone, DrawPathView pathView, float[] initialRealPos,
                           List<float[]> pointsInPath, TaskListener taskListener) {
        mDrone = drone;
        mScreenHeight = pathView.getHeight();
        mScreenWidth = pathView.getWidth();
        mInitialRealPos = initialRealPos;
        mPointsInPath = pointsInPath;
        mTaskListener = taskListener;

    }

    @Override
    protected float[] doInBackground(Void... voids) {
        float previousX = mPointsInPath.get(0)[0];
        float previousY = mPointsInPath.get(0)[1];
        float actualX, actualY;
        float distX, distY;
        float realDistLeft = mInitialRealPos[0];
        float realDistRight = 2 - mInitialRealPos[0];
        float realDistFor = mInitialRealPos[1];
        float realDistBack = 2 - mInitialRealPos[1];
        float timeToMove;

        for (int i = 1; i < mPointsInPath.size(); i++) {
            if (isCancelled()) {
                return new float[]{realDistLeft, realDistFor};
            }
            actualX = mPointsInPath.get(i)[0];
            actualY = mPointsInPath.get(i)[1];
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
                timeToMove = (Math.abs(distX) / 2) * 1000;
                mDrone.setFlag((byte) 1);
                mDrone.setRoll((byte) Math.round(10 * (distX / Math.abs(distX))));
                mDrone.setPitch((byte) Math.round(distY / timeToMove));
            } else {
                timeToMove = (Math.abs(distY) / 2) * 1000;
                mDrone.setFlag((byte) 1);
                mDrone.setPitch((byte) Math.round(10 * (distY / Math.abs(distY))));
                mDrone.setRoll((byte) Math.round(distX / timeToMove));
            }

            try {
                Thread.sleep(Math.round(timeToMove));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mDrone.setPitch((byte) 0);
            mDrone.setRoll((byte) 0);
            mDrone.setFlag(BebopDrone.FLAG_DISABLED);
        }

        return new float[]{realDistLeft, realDistFor};
    }

    @Override
    protected void onPostExecute(float[] initialCoordinates) {
        mDrone.setFlag(BebopDrone.FLAG_DISABLED);
        mDrone.setPitch(0);
        mDrone.setRoll(0);
        if (mTaskListener != null) {
            mTaskListener.onPathExecuted(initialCoordinates, false);
        }
    }

    @Override
    protected void onCancelled() {
        Log.d(TAG, "onCancelled() called with no arguments.");
        mDrone.setFlag(BebopDrone.FLAG_DISABLED);
        mDrone.setPitch(0);
        mDrone.setRoll(0);

        if (mTaskListener != null) {
            mTaskListener.onPathExecuted(mInitialRealPos, true);
        }
    }

    @Override
    protected void onCancelled(float[] initialCoordinates) {
        Log.d(TAG, "onCancelled() with array as argument.");
        mDrone.setFlag(BebopDrone.FLAG_DISABLED);
        mDrone.setPitch(0);
        mDrone.setRoll(0);

        if (mTaskListener != null) {
            mTaskListener.onPathExecuted(initialCoordinates, true);
        }
    }

    public interface TaskListener {
        /**
         * Appelée lorsque le tracé dessiné à l'écran a été reproduit par le drône.
         * @param initialCoordinates nouvelle position du drône dans l'espace défini
         * @param interrupted si le déplacement a été interrompu par l'utilisateur
         */
        void onPathExecuted(float[] initialCoordinates, boolean interrupted);
    }
}
