package fr.telecomlille.mydrone.drone;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.IntRange;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.util.Log;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGEVENT_MOVEBYEND_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARControllerException;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.arcontroller.ARDeviceControllerStreamListener;
import com.parrot.arsdk.arcontroller.ARFeatureARDrone3;
import com.parrot.arsdk.arcontroller.ARFeatureCommon;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_FAMILY_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryException;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.arutils.ARUtilsException;
import com.parrot.arsdk.arutils.ARUtilsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Cette classe permet de contrôler un drône de type Bebop.
 * <p>
 * Avant toute chose, il faut se connecter au drône via {@link #connect()}. De la même manière,
 * {@link #disconnect()} met fin à la connexion.
 * </p><p>
 * Pour recevoir les évènements liés aux changements d'état du drône
 * (tels que le niveau de batterie, l'état du pilotage et le flux vidéo en temps réel),
 * il est nécessaire d'limplémenter {@link BebopDrone.Listener}, puis de souscrire aux évènements
 * via {@link #addListener(Listener)}.
 * </p><p>
 * Une fois l'utilisation de cet objet terminée, il faut appeler {@link #dispose()}
 * pour libérer les ressources.
 * </p>
 */
public class BebopDrone {

    /**
     * Constante pour {@link #setFlag(byte)}.
     * Active les mouvements "pitch" et "roll" du drône dans le plan (X,Z).
     */
    public static final byte FLAG_ENABLED = 1;
    /**
     * Constante pour {@link #setFlag(byte)}.
     * Désactive les mouvements "pitch" et "roll" du drône dans le plan (X,Z).
     */
    public static final byte FLAG_DISABLED = 0;
    private static final String TAG = "BebopDrone";
    private static final int DEVICE_PORT = 21;
    private final List<Listener> mListeners;
    private final Handler mHandler;

    /**
     * Relaie les évènements liés au stockage des photos et des vidéos prises par le drône.
     */
    private final SDCardModule.Listener mSDCardModuleListener = new SDCardModule.Listener() {
        @Override
        public void onMatchingMediasFound(final int nbMedias) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyMatchingMediasFound(nbMedias);
                }
            });
        }

        @Override
        public void onDownloadProgressed(final String mediaName, final int progress) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyDownloadProgressed(mediaName, progress);
                }
            });
        }

        @Override
        public void onDownloadComplete(final String mediaName) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyDownloadComplete(mediaName);
                }
            });
        }
    };

    /**
     * Relaie les évènements liés au flux média émis par le drône (i.e. vidéo en temps réel).
     */
    private final ARDeviceControllerStreamListener mStreamListener = new ARDeviceControllerStreamListener() {
        @Override
        public ARCONTROLLER_ERROR_ENUM configureDecoder(ARDeviceController deviceController, final ARControllerCodec codec) {
            notifyConfigureDecoder(codec);
            return ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK;
        }

        @Override
        public ARCONTROLLER_ERROR_ENUM onFrameReceived(ARDeviceController deviceController, final ARFrame frame) {
            notifyFrameReceived(frame);
            return ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK;
        }

        @Override
        public void onFrameTimeout(ARDeviceController deviceController) {
        }
    };

    private ARDeviceController mDeviceController;
    private SDCardModule mSDCardModule;
    private ARCONTROLLER_DEVICE_STATE_ENUM mState;
    private ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM mFlyingState;
    private String mCurrentRunId;

    /**
     * Relaie les évènements liés au pilotage du drône (changements d'état, réception de commandes).
     */
    private final ARDeviceControllerListener mDeviceControllerListener = new ARDeviceControllerListener() {
        @Override
        public void onStateChanged(ARDeviceController deviceController,
                                   ARCONTROLLER_DEVICE_STATE_ENUM newState, ARCONTROLLER_ERROR_ENUM error) {
            mState = newState;
            if (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mState)) {
                mDeviceController.getFeatureARDrone3().sendMediaStreamingVideoEnable((byte) 1);
            } else if (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED.equals(mState)) {
                mSDCardModule.cancelGetFlightMedias();
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyConnectionChanged(mState);
                }
            });
        }

        @Override
        public void onExtensionStateChanged(ARDeviceController deviceController,
                                            ARCONTROLLER_DEVICE_STATE_ENUM newState,
                                            ARDISCOVERY_PRODUCT_ENUM product, String name,
                                            ARCONTROLLER_ERROR_ENUM error) {
            Log.d(TAG, "onExtensionStateChanged");
        }

        @Override
        public void onCommandReceived(ARDeviceController deviceController,
                                      ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey,
                                      ARControllerDictionary elementDictionary) {
            // if event received is the battery update
            if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED) && (elementDictionary != null)) {
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                if (args != null) {
                    final int battery = (Integer) args.get(ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED_PERCENT);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyBatteryChanged(battery);
                        }
                    });
                }
            }
            // if event received is the flying state update
            else if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED) && (elementDictionary != null)) {
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                if (args != null) {
                    final ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state = ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.getFromValue((Integer) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE));

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mFlyingState = state;
                            notifyPilotingStateChanged(state);
                        }
                    });
                }
            }
            // if event received is the picture notification
            else if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED) && (elementDictionary != null)) {
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                if (args != null) {
                    final ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error = ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM.getFromValue((Integer) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR));
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyPictureTaken(error);
                        }
                    });
                }
            }
            // if event received is the run id
            else if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_RUNSTATE_RUNIDCHANGED) && (elementDictionary != null)) {
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                if (args != null) {
                    final String runID = (String) args.get(ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_RUNSTATE_RUNIDCHANGED_RUNID);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCurrentRunId = runID;
                        }
                    });
                }
            } else if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGEVENT_MOVEBYEND) && (elementDictionary != null)) {
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                if (args != null) {
                    Log.d(TAG, "onCommandReceived: moveByFinished");
                    final float dX = (float) ((Double) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGEVENT_MOVEBYEND_DX)).doubleValue();
                    final float dY = (float) ((Double) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGEVENT_MOVEBYEND_DY)).doubleValue();
                    final float dZ = (float) ((Double) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGEVENT_MOVEBYEND_DZ)).doubleValue();
                    final float dPsi = (float) ((Double) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGEVENT_MOVEBYEND_DPSI)).doubleValue();
                    final ARCOMMANDS_ARDRONE3_PILOTINGEVENT_MOVEBYEND_ERROR_ENUM error = ARCOMMANDS_ARDRONE3_PILOTINGEVENT_MOVEBYEND_ERROR_ENUM.getFromValue((Integer) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGEVENT_MOVEBYEND_ERROR));
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyRelativeMoveFinished(dX, dY, dZ, dPsi,
                                    error.equals(ARCOMMANDS_ARDRONE3_PILOTINGEVENT_MOVEBYEND_ERROR_ENUM
                                            .ARCOMMANDS_ARDRONE3_PILOTINGEVENT_MOVEBYEND_ERROR_INTERRUPTED));
                        }
                    });
                }
            }

            if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_SPEEDCHANGED) && (elementDictionary != null)) {
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                if (args != null) {
                    float speedX = (float) ((Double) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_SPEEDCHANGED_SPEEDX)).doubleValue();
                    float speedY = (float) ((Double) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_SPEEDCHANGED_SPEEDY)).doubleValue();
                    float speedZ = (float) ((Double) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_SPEEDCHANGED_SPEEDZ)).doubleValue();
                    Log.d(TAG, String.format("onCommandReceived: speed=[%f, %f, %f]", speedX, speedY, speedZ));
                }
            }
        }
    };

    /**
     * Construit un nouvelle instance d'un objet permettant de contrôler un drône Bebop à distance.
     * Pour établir la connexion, il est nécessaire d'appeler {@link #connect()},
     * puis éventuellement de souscrire aux évènements produits par le drône via
     * {@link #addListener(Listener)}.
     *
     * @param context       contexte courant
     * @param deviceService les données associées au drône lors de sa découverte.
     *                      Il doit s'agit d'un drône Bebop.
     */
    public BebopDrone(@NonNull Context context, @NonNull ARDiscoveryDeviceService deviceService) {

        mListeners = new ArrayList<>();

        // Needed because some callbacks will be called on the main thread
        mHandler = new Handler(context.getMainLooper());

        mState = ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED;

        // If the product type of the deviceService match with the types supported
        ARDISCOVERY_PRODUCT_ENUM productType = ARDiscoveryService.getProductFromProductID(deviceService.getProductID());
        ARDISCOVERY_PRODUCT_FAMILY_ENUM family = ARDiscoveryService.getProductFamily(productType);
        if (ARDISCOVERY_PRODUCT_FAMILY_ENUM.ARDISCOVERY_PRODUCT_FAMILY_ARDRONE.equals(family)) {

            ARDiscoveryDevice discoveryDevice = createDiscoveryDevice(deviceService, productType);
            if (discoveryDevice != null) {
                mDeviceController = createDeviceController(discoveryDevice);
                discoveryDevice.dispose();
            }

            try {
                String productIP = ((ARDiscoveryDeviceNetService) (deviceService.getDevice())).getIp();

                ARUtilsManager ftpListManager = new ARUtilsManager();
                ARUtilsManager ftpQueueManager = new ARUtilsManager();

                ftpListManager.initWifiFtp(productIP, DEVICE_PORT, ARUtilsManager.FTP_ANONYMOUS, "");
                ftpQueueManager.initWifiFtp(productIP, DEVICE_PORT, ARUtilsManager.FTP_ANONYMOUS, "");

                mSDCardModule = new SDCardModule(ftpListManager, ftpQueueManager);
                mSDCardModule.addListener(mSDCardModuleListener);
            } catch (ARUtilsException e) {
                Log.e(TAG, "Exception", e);
            }

        } else {
            Log.e(TAG, "DeviceService type is not supported by BebopDrone");
        }
    }

    /**
     * Libère les ressources utilisées par cet objet.
     * Cette méthode doit être appelée lorsque l'objet n'est plus utilisé.
     */
    public void dispose() {
        if (mDeviceController != null)
            mDeviceController.dispose();
    }

    /**
     * Souscrit aux évènements émis par le drône, tels que les changements d'état de la connexion,
     * du pilotage, du niveau de batterie, ainsi que la réception du flux vidéo en temps réel.
     *
     * @param listener objet client souhaitant recevoir les évènements du drône
     */
    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    /**
     * Se désabonne des évènements émis le drône.
     *
     * @param listener objet client souhaitant ne plus recevoir les évènements du drône
     */
    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Etablit la connexion distante avec le drône.
     *
     * @return true si cette opération s'est déroulée correctement.
     * La valeur de retour true n'indique par forcément que la connexion a été établie avec succès.
     * Pour connaitre l'état actuel de la connexion avec le drône,
     * se référer à la méthode {@link Listener#onDroneConnectionChanged}.
     */
    public boolean connect() {
        boolean success = false;
        if ((mDeviceController != null) && (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED.equals(mState))) {
            ARCONTROLLER_ERROR_ENUM error = mDeviceController.start();
            if (error == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                success = true;
            }
        }
        return success;
    }

    /**
     * Se déconnecte du drône.
     *
     * @return true si cette opération s'est déroulée correctement.
     * La valeur de retour true n'indique par forcément que la connexion a fermée avec succès.
     * Pour connaitre l'état actuel de la connexion avec le drône,
     * se référer à la méthode {@link Listener#onDroneConnectionChanged}.
     */
    public boolean disconnect() {
        boolean success = false;
        if ((mDeviceController != null) && (ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mState))) {
            // Bloque les mouvements du drône et le fait atterir avant déconnexion par sécurité
            setFlag(FLAG_DISABLED);
            land();
            ARCONTROLLER_ERROR_ENUM error = mDeviceController.stop();
            if (error == ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                success = true;
            }
        }
        return success;
    }

    /**
     * @return l'état courant de la connexion avec le drône
     */
    public ARCONTROLLER_DEVICE_STATE_ENUM getConnectionState() {
        return mState;
    }

    /**
     * @return l'état courant du pilotage du drône
     */
    public ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM getFlyingState() {
        return mFlyingState;
    }

    /**
     * Demande le décollage du drône.
     * Cette méthode n'a d'effet que si la connexion avec le drône est établie.
     */
    public void takeOff() {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingTakeOff();
        }
    }

    /**
     * Demande l'atterrissage du drône.
     * Cette méthode n'a d'effet que si la connexion avec le drône est établie.
     */
    public void land() {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingLanding();
        }
    }

    /**
     * Coupe immédiatement les moteurs. Le drône va tomber.
     * Cette commande prévaut sur toutes les autres et doit être appelée uniquement en cas d'urgence
     * pour prévenir les dommages matériels.
     */
    public void emergency() {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingEmergency();
        }
    }

    /**
     * Demande la capture d'une photo, et l'enregistre sur le stockage interne du drône.
     */
    public void takePicture() {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendMediaRecordPictureV2();
        }
    }

    /**
     * Set the forward/backward angle of the drone
     * Note that {@link BebopDrone#setFlag(byte)} should be set to 1 in order to take in account the pitch value
     *
     * @param pitch value in percentage from -100 to 100
     */
    public void setPitch(@IntRange(from = -100, to = 100) int pitch) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMDPitch((byte) pitch);
        }
    }

    /**
     * Set the side angle of the drone
     * Note that {@link BebopDrone#setFlag(byte)} should be set to 1 in order to take in account the roll value
     *
     * @param roll value in percentage from -100 to 100
     */
    public void setRoll(@IntRange(from = -100, to = 100) int roll) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMDRoll((byte) roll);
        }
    }

    /**
     * Fait pivoter le drône vers la gauche (valeur négative) ou vers la droite (valeur positive).
     * Plus la valeur est élevée, puis le drône pivotera rapidement.
     * Le drône continuera à pivoter tant que cette méthode n'est pas appelée à nouveau avec la valeur 0.
     *
     * @param yaw valeur en pourcent de -100 à 100
     */
    public void setYaw(@IntRange(from = -100, to = 100) int yaw) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMDYaw((byte) yaw);
        }
    }

    /**
     * Règle l'altitude du drône. Une valeur positive le faut s'élever dans les airs,
     * tandis qu'une valeur négative le fait descendre.
     * Plus la valeur est élevée, plus le drône changera rapidement d'altitude.
     * L'altitude du drône changera tant que cette méthode n'est pas appelée à nouveau avec la valeur 0.
     *
     * @param gaz valeur en pourcent de -100 à 100
     */
    public void setGaz(@IntRange(from = -100, to = 100) int gaz) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMDGaz((byte) gaz);
        }
    }

    /**
     * Prend en compte ou non les commandes {@link #setPitch(int)} et {@link #setRoll(int)}.
     *
     * @param flag {@link #FLAG_ENABLED} si Pitch et Roll doivent être pris en compte,
     *             {@link #FLAG_DISABLED} sinon
     */
    public void setFlag(byte flag) {
        if ((mDeviceController != null) && (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMDFlag(flag);
        }
    }

    /**
     * Fait se déplacer le drône selon une trajectoire rectiligne.
     * La méthode {@link Listener#onRelativeMoveFinished} est appelée lorsque le mouvement est terminé.
     * Si on appelle à nouveau cette méthode alors que le déplacement du drône n'est pas terminé,
     * alors le déplacement est interrompu et {@link Listener#onRelativeMoveFinished}
     * est appelée immédiatement.
     *
     * @param dX   déplacement en mètres selon l'axe x (avant->arrière)
     * @param dY   déplacement en mètres selon l'axe y (gauche->droite)
     * @param dZ   déplacement en mètres selon l'axe z (haut->bas)
     * @param dPsi angle de rotation en radians.
     *             La modification de l'angle n'influence pas la trajectoire du drône.
     */
    public void moveBy(float dX, float dY, float dZ, float dPsi) {
        mDeviceController.getFeatureARDrone3().sendPilotingMoveBy(dX, dY, dZ, dPsi);
    }

    /**
     * Download the last flight medias
     * Uses the run id to download all medias related to the last flight
     * If no run id is available, download all medias of the day
     */
    public void getLastFlightMedias() {
        String runId = mCurrentRunId;
        if ((runId != null) && !runId.isEmpty()) {
            mSDCardModule.getFlightMedias(runId);
        } else {
            Log.e(TAG, "RunID not available, fallback to the day's medias");
            mSDCardModule.getTodaysFlightMedias();
        }
    }

    public void cancelGetLastFlightMedias() {
        mSDCardModule.cancelGetFlightMedias();
    }

    private ARDiscoveryDevice createDiscoveryDevice(@NonNull ARDiscoveryDeviceService service, ARDISCOVERY_PRODUCT_ENUM productType) {
        ARDiscoveryDevice device = null;
        try {
            device = new ARDiscoveryDevice();

            ARDiscoveryDeviceNetService netDeviceService = (ARDiscoveryDeviceNetService) service.getDevice();
            device.initWifi(productType, netDeviceService.getName(), netDeviceService.getIp(), netDeviceService.getPort());

        } catch (ARDiscoveryException e) {
            Log.e(TAG, "Exception", e);
            Log.e(TAG, "Error: " + e.getError());
        }

        return device;
    }

    private ARDeviceController createDeviceController(@NonNull ARDiscoveryDevice discoveryDevice) {
        ARDeviceController deviceController = null;
        try {
            deviceController = new ARDeviceController(discoveryDevice);

            deviceController.addListener(mDeviceControllerListener);
            deviceController.addStreamListener(mStreamListener);
        } catch (ARControllerException e) {
            Log.e(TAG, "Exception", e);
        }

        return deviceController;
    }

    //region notify listener block
    private void notifyConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onDroneConnectionChanged(state);
        }
    }

    private void notifyBatteryChanged(int battery) {
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onBatteryChargeChanged(battery);
        }
    }

    private void notifyPilotingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onPilotingStateChanged(state);
        }
    }

    private void notifyPictureTaken(ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onPictureTaken(error);
        }
    }

    private void notifyConfigureDecoder(ARControllerCodec codec) {
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.configureDecoder(codec);
        }
    }

    private void notifyFrameReceived(ARFrame frame) {
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onFrameReceived(frame);
        }
    }

    private void notifyMatchingMediasFound(int nbMedias) {
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onMatchingMediasFound(nbMedias);
        }
    }

    private void notifyDownloadProgressed(String mediaName, int progress) {
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onDownloadProgressed(mediaName, progress);
        }
    }

    private void notifyDownloadComplete(String mediaName) {
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onDownloadComplete(mediaName);
        }
    }

    private void notifyRelativeMoveFinished(float dX, float dY, float dZ, float dPsi, boolean isInterrupted) {
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onRelativeMoveFinished(dX, dY, dZ, dPsi, isInterrupted);
        }
    }
    //endregion notify listener block

    public interface Listener {
        /**
         * Appelé quand l'état de connexion au drône a changé.
         * Cette méthode est appelée sur le Thread principal.
         *
         * @param state le nouvel état de connexion avec le drône
         */
        @MainThread
        void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state);

        /**
         * Appelé lorsque le niveau de charge de la batterie du drône a changé.
         * Cette méthode est appelée sur le Thread principal.
         *
         * @param batteryPercentage le pourcentage de batterie restant
         */
        @MainThread
        void onBatteryChargeChanged(int batteryPercentage);

        /**
         * Appelée lorsque l'état de pilotage du drône a changé.
         * Cette méthode est appelée sur le Thread principal.
         *
         * @param state l'état de pilotage du drône
         */
        @MainThread
        void onPilotingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state);

        /**
         * Appelé lorsqu'une photo a été prise par le drône.
         * Cette méthode est appelée depuis un autre Thread.
         *
         * @param error ERROR_OK si la photo a été prise avec succès,
         *              sinon une autre valeur décrivant l'erreur qui s'est produite.
         */
        void onPictureTaken(ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error);

        /**
         * Appelé lorsque le décodeur vidéo doit être configuré.
         * Cette méthode est appelée depuis un autre Thread.
         *
         * @param codec le codec avec lequel le décodeur doit être configuré.
         */
        void configureDecoder(ARControllerCodec codec);

        /**
         * Appelé quand une frame est reçue depuis le drône lors de la prise de vue en temps réel.
         * Cette frame doit d'abord être décodée pour être interprétée comme image.
         * Cette méthode est appelée depuis un autre Thread.
         *
         * @param frame une frame vidéo
         */
        void onFrameReceived(ARFrame frame);

        /**
         * Appelé avant le téléchargement de fichiers médias, tels que les photos et vidéos prises
         * par le drône pendant le vol.
         * Cette méthode est appelée sur le Thread principal.
         *
         * @param nbMedias le nombre de médias qui va être téléchargé
         */
        @MainThread
        void onMatchingMediasFound(int nbMedias);

        /**
         * Appelée à chaque fois que la progression du téléchargement des fichiers médias évolue.
         * Cette méthode est appelée sur le Thread principal.
         *
         * @param mediaName le nom du fichier média téléchargé
         * @param progress  pourcentage de progression du téléchargement
         */
        @MainThread
        void onDownloadProgressed(String mediaName, int progress);

        /**
         * Appelé lorsque le téléchargement d'un fichier média vient de se terminer.
         * Cette méthode est appelée sur le Thread principal.
         *
         * @param mediaName le nom du fichier média téléchargé
         */
        @MainThread
        void onDownloadComplete(String mediaName);

        /**
         * Appelé lorsque le drône a fini de se déplacer suite à l'appel à {@link #moveBy}.
         *
         * @param dX            déplacement en mètres selon l'axe x (avant/arrière)
         * @param dY            déplacement en mètres selon l'axe y (gauche/droite)
         * @param dZ            déplacement en mètres selon l'axe z (bas/haut)
         * @param dPsi          angle de rotation du drône en radians
         * @param isInterrupted indique si le mouvement a été interrompu
         */
        @MainThread
        void onRelativeMoveFinished(float dX, float dY, float dZ, float dPsi, boolean isInterrupted);
    }
}
