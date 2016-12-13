package fr.telecomlille.mydrone.recognition;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import fr.telecomlille.mydrone.MainActivity;
import fr.telecomlille.mydrone.R;
import fr.telecomlille.mydrone.drone.BebopDrone;
import fr.telecomlille.mydrone.view.BebopVideoView;

//import org.bytedeco.javacpp.opencv_objdetect;

public class RecognitionActivity extends AppCompatActivity {

    private BebopVideoView mVideoView;
    private BebopDrone mDrone;
//    private opencv_objdetect.CascadeClassifier mClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);

//        mClassifier = new opencv_objdetect.CascadeClassifier("file:///android_asset/haarcascade_frontalface_default.xml");
        ARDiscoveryDeviceService deviceService = getIntent()
                .getParcelableExtra(MainActivity.EXTRA_DEVICE_SERVICE);

        if (deviceService == null) {
            throw new IllegalStateException("Calling Activity must start this Activity with an " +
                    "ARDiscoveryDeviceService object passed as an extra (EXTRA_DEVICE_SERVICE).");
        }

        mDrone = new BebopDrone(this, deviceService);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mDrone != null && !mDrone.connect()) {
            Toast.makeText(this, "Error while connecting to the drone.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mDrone != null && !mDrone.disconnect()) {
            Toast.makeText(this, "error while disconnecting to the drone.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
//        mDrone.dispose();
        super.onDestroy();
    }


}
