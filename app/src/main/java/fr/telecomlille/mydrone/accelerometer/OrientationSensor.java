package fr.telecomlille.mydrone.accelerometer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * Classe permettant de mesurer à tout instant l'orientation de l'appareil dans le repère Monde.
 * Les mesures s'appuient sur celles de l'accéléromètre et du magnétomètre.
 * <p>
 * Pour obtenir périodiquement les valeurs de l'inclinaison, il est nécessaire d'enregistrer un
 * listener via {@link #registerListener(Listener)}, et de démarrer les mesures via {@link #start()}.
 * Une fois que cette objet n'est plus utilisée, on peut arrêter de mesurer l'orientation
 * en appelant la méthode {@link #stop()}.
 */
public class OrientationSensor {

    private static final String TAG = "OrientationSensor";

    private final SensorManager mSensorManager;
    private final Set<Listener> mSubscribers;
    private final Handler mUpdateHandler;
    private final HandlerThread mCalculateThread;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];
    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];
    private final int mFrequency;
    /**
     * Récupère périodiquement les mesures de l'accéléromètre et du magnétomètre.
     */
    private final SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                synchronized (mAccelerometerReading) {
                    System.arraycopy(event.values, 0, mAccelerometerReading,
                            0, mAccelerometerReading.length);
                }
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                synchronized (mMagnetometerReading) {
                    System.arraycopy(event.values, 0, mMagnetometerReading,
                            0, mMagnetometerReading.length);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int newAccuracy) {
            Log.w(TAG, "onAccuracyChanged() called with: sensor = ["
                    + sensor + "], newAccuracy = [" + newAccuracy + "]");
        }
    };
    private Handler mCalculateHandler;
    /**
     * Publie périodiquement les valeurs de l'orientation de l'appareil sur le Thread principal.
     */
    private final Runnable mPostRunnable = new Runnable() {
        @Override
        public void run() {
            // Re-schedule execution of the calculate task
            mCalculateHandler.postDelayed(mCalculateRunnable, mFrequency);
            notifySensorChanged(mOrientationAngles);
        }
    };
    /**
     * Effectue les calculs de l'orientation à partir des valeurs fournies par l'accéléromètre
     * et le magnétomètre.
     * Le calcul est effectué sur un autre Thread pour éviter de bloquer le Thread principal.
     */
    private final Runnable mCalculateRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (mAccelerometerReading) {
                synchronized (mMagnetometerReading) {
                    // Update rotation matrix, which is needed to update orientation angles.
                    SensorManager.getRotationMatrix(mRotationMatrix, null,
                            mAccelerometerReading, mMagnetometerReading);
                }
            }
            // Calculate the orientation angles from the rotation matrix
            SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

            // Deliver the result to the MainThread
            mUpdateHandler.post(mPostRunnable);
        }
    };

    /**
     * Construit un nouvelle instance du capteur d'orientation.
     * Pour démarrer les mesures, il est nécessaire de le démarrer avec {@link #start()}.
     *
     * @param updateFreqMillis délai entre 2 mises à jour de l'orientation
     */
    public OrientationSensor(@NonNull Context context, @IntRange(from = 1) int updateFreqMillis) {
        mUpdateHandler = new Handler();
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSubscribers = new HashSet<>();

        mCalculateThread = new HandlerThread("OrientationSensorCalculation");
        mCalculateThread.start();
        mCalculateHandler = new Handler(mCalculateThread.getLooper());

        if (updateFreqMillis <= 0) {
            throw new IllegalArgumentException("updateFrequency must be a positive number.");
        }

        mFrequency = updateFreqMillis;
    }

    /**
     * Démarre le calcul de l'orientation.
     * Cette méthode démarre l'accéléromètre et le magnétomètre, et doit être appelée (typiquement)
     * dans {@link android.app.Activity#onStart}.
     */
    public void start() {
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        mCalculateHandler.post(mCalculateRunnable);
    }

    /**
     * Arrête le calcul de l'orientation.
     * Cette méthode arrête l'accéléromètre et le magnétomètre, et doit être appelée (typiquement)
     * dans {@link android.app.Activity#onStop}.
     */
    public void stop() {
        mSensorManager.unregisterListener(mSensorListener);
        mCalculateHandler.removeCallbacks(mCalculateRunnable);
        mUpdateHandler.removeCallbacks(mPostRunnable);
    }

    /**
     * Enregistre un listener pour recevoir périodiquement les valeurs d'orientation de l'appareil.
     */
    public void registerListener(Listener listener) {
        mSubscribers.add(listener);
    }

    /**
     * Demande à arrêter de recevoir périoriquement les valeurs de l'orientation de l'appareil.
     */
    public void unregisterListener(Listener listener) {
        mSubscribers.remove(listener);
    }

    private void notifySensorChanged(float[] newValue) {
        for (Listener sub : mSubscribers) {
            sub.onSensorChanged(newValue);
        }
    }

    interface Listener {
        /**
         * Indique que l'orientation de l'appareil dans le repère monde a changé.
         *
         * @param values nouvelle orientation de l'appareil. Les valeurs sont les suivantes :
         *               <ul><li>
         *               values[0]: <i>Azimuth</i>, angle de rotation suivant l'axe -z.<br/>
         *               Cette valeur représente l'angle entre l'axe y de l'appareil et le pôle nord magnétique.
         *               En faisant face au nord cet angle est de 0 ; au sud il est de π.
         *               De la même manière, il est de π/2 à l'est et de -π/2 à l'ouest.
         *               Les valeurs vont de -π à π.
         *               </li><li>
         *               values[1]: <i>Pitch</i>, angle de rotation suivant l'axe x.<br/>
         *               Cette valeur représente l'angle entre un plan parallèle à l'écran de l'appareil
         *               et un plan parallèle au sol. En considérant que le téléphone est orienté
         *               en portrait et que l'écran est vers le haut, pencher le téléphone vers l'avant
         *               produit un angle de pitch positif.
         *               Les valeurs vont de -π à π.
         *               </li><li>
         *               values[2]: <i>Roll</i>, angle de rotation suivant l'axe y.<br/>
         *               Cette valeur représente l'angle entre un plan perpendiculaire à l'écran de l'appareil
         *               et un plan perpendiculaire au sol. En considérant que le téléphone est orienté
         *               en portrait et que l'écran est vers le haut, pencher le téléphone vers la gauche
         *               produit un angle de roll positif.
         *               Les valeurs vont de -π/2 à π/2.
         *               </li></ul>
         */
        void onSensorChanged(float[] values);
    }
}
