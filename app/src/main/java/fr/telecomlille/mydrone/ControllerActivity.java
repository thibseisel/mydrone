package fr.telecomlille.mydrone;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

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
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryException;

import fr.telecomlille.mydrone.view.BebopVideoView;

public class ControllerActivity extends AppCompatActivity implements ARDeviceControllerListener,
        ARDeviceControllerStreamListener {

    private static final String TAG = "ControllerActivity";
    private ARDeviceController mDeviceController;
    private BebopVideoView mVideoView;
    private ProgressBar mBatteryLevel;
    private ImageView mBatteryIndicator;
    private ARCONTROLLER_DEVICE_STATE_ENUM mState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

        mVideoView = (BebopVideoView) findViewById(R.id.videoView);
        mBatteryLevel = (ProgressBar) findViewById(R.id.batteryLevel);
        mBatteryIndicator = (ImageView) findViewById(R.id.battery_indicator);

        Intent caller = getIntent();
        ARDiscoveryDeviceService deviceService = caller.getParcelableExtra(MainActivity.EXTRA_DEVICE_SERVICE);

        ARDiscoveryDevice device = createDiscoveryDevice(deviceService);
        Log.d(TAG, "My device: " + device);

        try {
            mDeviceController = new ARDeviceController(device);

        } catch (ARControllerException e) {
            e.printStackTrace();
        }

        ImageButton takeoffButton = (ImageButton) findViewById(R.id.btn_takeoff);
        takeoffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)
                        && ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED.equals(getPilotingState())) {
                    mDeviceController.getFeatureARDrone3().sendPilotingTakeOff();
                }
            }
        });
        ImageButton landButton = (ImageButton) findViewById(R.id.btn_land);
        landButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)
                        && (ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING.equals(getPilotingState())
                        || ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING.equals(getPilotingState()))) {
                    mDeviceController.getFeatureARDrone3().sendPilotingLanding();
                }
            }
        });
        Button emergencyButton = (Button) findViewById(R.id.btn_emergency);
        emergencyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDeviceController != null && mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)) {
                    mDeviceController.getFeatureARDrone3().sendPilotingEmergency();
                }
            }
        });

        findViewById(R.id.btn_gaz_up).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)
                        && (ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING.equals(getPilotingState())
                        || ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING.equals(getPilotingState()))) {
                    switch(motionEvent.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            view.setPressed(true);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDGaz((byte) 50);
                            break;
                        case MotionEvent.ACTION_UP:
                            view.setPressed(false);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDGaz((byte) 0);
                            break;
                    }
                }
                return true;
            }
        });

        findViewById(R.id.btn_gaz_down).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)
                        && (ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING.equals(getPilotingState())
                        || ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING.equals(getPilotingState()))) {
                    switch(motionEvent.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            view.setPressed(true);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDGaz((byte) -50);
                            break;
                        case MotionEvent.ACTION_UP:
                            view.setPressed(false);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDGaz((byte) 0);
                            break;
                    }
                }
                return true;
            }
        });

        findViewById(R.id.btn_yaw_right).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)
                        && (ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING.equals(getPilotingState())
                        || ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING.equals(getPilotingState()))) {
                    switch(motionEvent.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            view.setPressed(true);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDYaw((byte) 50);
                            break;
                        case MotionEvent.ACTION_UP:
                            view.setPressed(false);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDYaw((byte) 0);
                            break;
                    }
                }
                return true;
            }
        });

        findViewById(R.id.btn_yaw_left).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)
                        && (ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING.equals(getPilotingState())
                        || ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING.equals(getPilotingState()))) {
                    switch(motionEvent.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            view.setPressed(true);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDYaw((byte) -50);
                            break;
                        case MotionEvent.ACTION_UP:
                            view.setPressed(false);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDYaw((byte) 0);
                            break;
                    }
                }
                return true;
            }
        });

        findViewById(R.id.btn_forward).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)
                        && (ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING.equals(getPilotingState())
                        || ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING.equals(getPilotingState()))) {
                    switch(motionEvent.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            view.setPressed(true);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDPitch((byte) 50);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte) 1);
                            break;
                        case MotionEvent.ACTION_UP:
                            view.setPressed(false);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDPitch((byte) 0);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte) 0);
                            break;
                    }
                }
                return true;
            }
        });

        findViewById(R.id.btn_backward).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)
                        && (ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING.equals(getPilotingState())
                        || ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING.equals(getPilotingState()))) {
                    switch(motionEvent.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            view.setPressed(true);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDPitch((byte) -50);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte) 1);
                            break;
                        case MotionEvent.ACTION_UP:
                            view.setPressed(false);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDPitch((byte) 0);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte) 0);
                            break;
                    }
                }
                return true;
            }
        });

        findViewById(R.id.btn_roll_right).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)
                        && (ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING.equals(getPilotingState())
                        || ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING.equals(getPilotingState()))) {
                    switch(motionEvent.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            view.setPressed(true);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDRoll((byte) 50);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte) 1);
                            break;
                        case MotionEvent.ACTION_UP:
                            view.setPressed(false);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDRoll((byte) 0);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte) 0);
                            break;
                    }
                }
                return true;
            }
        });

        findViewById(R.id.btn_roll_left).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)
                        && (ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING.equals(getPilotingState())
                        || ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING.equals(getPilotingState()))) {
                    switch(motionEvent.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            view.setPressed(true);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDRoll((byte) -50);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte) 1);
                            break;
                        case MotionEvent.ACTION_UP:
                            view.setPressed(false);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDRoll((byte) 0);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte) 0);
                            break;
                    }
                }
                return true;
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        mDeviceController.addListener(this);
        mDeviceController.addStreamListener(this);
        ARCONTROLLER_ERROR_ENUM error = mDeviceController.start();
        Log.d(TAG, "onStart: error=" + error);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDeviceController.removeListener(this);
        mDeviceController.removeStreamListener(this);
        ARCONTROLLER_ERROR_ENUM error = mDeviceController.stop();
        Log.d(TAG, "onStop: error=" + error);
    }

    /**
     * Récupère une référence à un appareil à proximité à partir de ses informations de détection.
     */
    private ARDiscoveryDevice createDiscoveryDevice(ARDiscoveryDeviceService service) {
        ARDiscoveryDevice device = null;
        // On a un peu allégé ce if, en vrai.
        if (service != null) {
            try {
                device = new ARDiscoveryDevice();
                ARDiscoveryDeviceNetService netDeviceService = (ARDiscoveryDeviceNetService) service.getDevice();
                device.initWifi(ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_ARDRONE, netDeviceService.getName(),
                        netDeviceService.getIp(), netDeviceService.getPort());
            } catch (ARDiscoveryException e) {
                e.printStackTrace();
                Log.e(TAG, "Error: " + e.getError());
            }
        }
        return device;
    }

    private ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM getPilotingState() {
        ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM flyingState = ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.eARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_UNKNOWN_ENUM_VALUE;
        if (mDeviceController != null) {
            try {
                ARControllerDictionary dict = mDeviceController.getCommandElements(ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED);
                if (dict != null) {
                    ARControllerArgumentDictionary<Object> args = dict.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                    if (args != null) {
                        Integer flyingStateInt = (Integer) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE);
                        flyingState = ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.getFromValue(flyingStateInt);
                    }
                }
            } catch (ARControllerException e) {
                e.printStackTrace();
            }
        }
        return flyingState;
    }

    @Override
    public void onStateChanged(ARDeviceController deviceController,
                               ARCONTROLLER_DEVICE_STATE_ENUM newState,
                               ARCONTROLLER_ERROR_ENUM error) {
        mState = newState;
        switch (mState) {
            case ARCONTROLLER_DEVICE_STATE_RUNNING:
                Log.d(TAG, "State changed: RUNNING");
                deviceController.getFeatureARDrone3().sendMediaStreamingVideoEnable((byte) 1);
                break;
            case ARCONTROLLER_DEVICE_STATE_STOPPED:
                Log.d(TAG, "State changed: STOPPED");
                deviceController.getFeatureARDrone3().sendMediaStreamingVideoEnable((byte) 0);
                break;
            case ARCONTROLLER_DEVICE_STATE_STARTING:
                Log.d(TAG, "State changed: STARTING");
                break;
            case ARCONTROLLER_DEVICE_STATE_STOPPING:
                Log.d(TAG, "State changed: STOPPING");
                break;
            default:
                Log.e(TAG, "State changed: UNKNOWN");
                break;
        }
    }

    @Override
    public void onExtensionStateChanged(ARDeviceController deviceController,
                                        ARCONTROLLER_DEVICE_STATE_ENUM newState,
                                        ARDISCOVERY_PRODUCT_ENUM product, String name,
                                        ARCONTROLLER_ERROR_ENUM error) {
        Log.d(TAG, "onExtensionStateChanged() called with: deviceController = ["
                + deviceController + "], newState = [" + newState + "], product = ["
                + product + "], name = [" + name + "], error = [" + error + "]");
    }

    @Override
    public void onCommandReceived(ARDeviceController deviceController,
                                  ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey,
                                  ARControllerDictionary elementDictionary) {
        if (elementDictionary != null) {
            // if the command received is a battery state changed
            if (commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED) {
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
                if (args != null) {
                    int batValue = (int) args.get(ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED_PERCENT);

                    // do what you want with the battery level
                    mBatteryLevel.setProgress(batValue);
                    // mBatteryIndicator.setImageLevel(batValue);
                }
            }
        } else if (commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED) {
            ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
            if (args != null) {
                Integer flyingStateInt = (Integer) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE);
                ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM flyingState = ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.getFromValue(flyingStateInt);
            }
        } else {
            Log.e(TAG, "elementDictionary is null");
        }
    }

    @Override
    public ARCONTROLLER_ERROR_ENUM configureDecoder(ARDeviceController deviceController, ARControllerCodec codec) {
        Log.d(TAG, "configureDecoder() called with: codec = [" + codec + "]");

        try {
            mVideoView.configureDecoder(codec);
            return ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK;
        } catch (RuntimeException e) {
            Log.e(TAG, "configureDecoder: error while configuring VideoView decoder.", e);
            return ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_ERROR;
        }
    }

    @Override
    public ARCONTROLLER_ERROR_ENUM onFrameReceived(ARDeviceController deviceController, ARFrame frame) {
        Log.d(TAG, "onFrameReceived() called with: frame = [" + frame + "]");

        try {
            mVideoView.displayFrame(frame);
            return ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK;
        } catch (RuntimeException e) {
            Log.e(TAG, "onFrameReceived: error while displaying frame.", e);
            return ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_ERROR;
        }
    }

    @Override
    public void onFrameTimeout(ARDeviceController deviceController) {
        Log.d(TAG, "onFrameTimeout");
    }
}
