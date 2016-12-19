package fr.telecomlille.mydrone.path;

import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

import fr.telecomlille.mydrone.drone.BebopDrone;
import fr.telecomlille.mydrone.view.DrawPathView;

/**
 * Tâche d'arrière plan permettant de faire suivre la trajectoire tracée à l'écran par le drône.
 * Etant donné que l'application doit attendre que le drône effectue son mouvement avant chaque
 * changement de direction, cette tâche est exécutée sur un autre thread pour éviter de bloquer
 * l'interface graphique.
 */
public class PathControlTask extends AsyncTask<Void, Integer, float[]> {

    private static final String TAG = "PathControlTask";

    private BebopDrone mDrone;
    private float mRoomSizeX;
    private float mRoomSizeY;
    private float mScreenWidth, mScreenHeight;
    private float[] mInitialPosInRoom;
    private List<float[]> mPointsInPath;
    private TaskListener mTaskListener;

    /**
     * Construit une nouvelle instance de cette Task.
     * Ceci doit être effectué pour chaque nouveau tracé.
     * @param drone objet permettant le pilotage du drone
     * @param pathView vue réalisant le tracé
     * @param initialRealPos position réelle du drne
     * @param roomSize dimensions de la pièce selon x (roomSize[0]) et y (roomSize[1])
     * @param pointsInPath liste des coordonnées des points du tracé réalisé sur l'écran
     * @param taskListener listener permettant de recevoir des évènements liés au déplacement
     */
    public PathControlTask(BebopDrone drone, DrawPathView pathView, float[] initialRealPos, float[] roomSize,
                           List<float[]> pointsInPath, TaskListener taskListener) {
        mDrone = drone;
        mScreenHeight = pathView.getHeight();
        mScreenWidth = pathView.getWidth();
        mInitialPosInRoom = initialRealPos;
        mPointsInPath = pointsInPath;
        mTaskListener = taskListener;
        mRoomSizeX = roomSize[0];
        mRoomSizeY = roomSize[1];

    }

    @Override
    protected float[] doInBackground(Void... voids) {
        float previousX = mPointsInPath.get(0)[0];
        float previousY = mPointsInPath.get(0)[1];
        float actualX, actualY;
        float distX, distY;
        float realDistLeft = mInitialPosInRoom[0];
        float realDistRight = mRoomSizeX - mInitialPosInRoom[0];
        float realDistFor = mInitialPosInRoom[1];
        float realDistBack = mRoomSizeY - mInitialPosInRoom[1];
        float timeToMove;
        try {
            for (int i = 1; i < mPointsInPath.size(); i++) {
                if (isCancelled()) {
                    return new float[]{realDistLeft, realDistFor};
                }
                actualX = mPointsInPath.get(i)[0];
                actualY = mPointsInPath.get(i)[1];
                if (actualX <= previousX) {
                    distX = (actualX - previousX) * realDistLeft / previousX;
                    realDistRight += Math.abs(distX);
                    realDistLeft -= Math.abs(distX);
                } else {
                    distX = (actualX - previousX) * realDistRight / (mScreenWidth - previousX);
                    realDistRight -= Math.abs(distX);
                    realDistLeft += Math.abs(distX);
                }
                if (actualY <= previousY) {
                    distY = (previousY - actualY) * realDistFor / previousY;
                    realDistFor -= Math.abs(distY);
                    realDistBack += Math.abs(distY);
                } else {
                    distY = (previousY - actualY) * realDistBack / (mScreenHeight - previousY);
                    realDistFor += Math.abs(distY);
                    realDistBack -= Math.abs(distY);
                }
                previousX = actualX;
                previousY = actualY;
                if (Math.abs(distX) >= Math.abs(distY)) {
                    timeToMove = (Math.abs(distX) / 1.3f) * 1000;
                    mDrone.setFlag(BebopDrone.FLAG_ENABLED);
                    mDrone.setRoll(Math.round(10 * (distX / Math.abs(distX))));
                    mDrone.setPitch(Math.round(distY / timeToMove));
                } else {
                    timeToMove = (Math.abs(distY) / 1.3f) * 1000;
                    mDrone.setFlag(BebopDrone.FLAG_ENABLED);
                    mDrone.setPitch(Math.round(10 * (distY / Math.abs(distY))));
                    mDrone.setRoll(Math.round(distX / timeToMove));
                }

                Thread.sleep(Math.round(timeToMove));

                mDrone.setPitch(0);
                mDrone.setRoll(0);
                mDrone.setFlag(BebopDrone.FLAG_DISABLED);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            mTaskListener.onPathExecuted(mInitialPosInRoom, true);
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

    /**
     * Interface de callback permettant de notifier le client lorsque le drone a fini de se déplacer.
     */
    public interface TaskListener {
        /**
         * Appelée lorsque le tracé dessiné à l'écran a été reproduit par le drône.
         *
         * @param initialCoordinates nouvelle position du drône dans l'espace défini
         * @param interrupted        si le déplacement a été interrompu par l'utilisateur
         */
        void onPathExecuted(float[] initialCoordinates, boolean interrupted);
    }
}
