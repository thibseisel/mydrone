package fr.telecomlille.mydrone;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ListView;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements ARDiscoveryServicesDevicesListUpdatedReceiverDelegate {

    public static final String EXTRA_DEVICE_SERVICE = "DeviceService";
    private static final String TAG = "MainActivity";
    private ARDiscoveryService mArdiscoveryService;
    private ServiceConnection mArdiscoveryServiceConnection;
    private ARDiscoveryServicesDevicesListUpdatedReceiver mArdiscoveryServicesDevicesListUpdatedReceiver;

    private ListView mDeviceListview;
    private DeviceListAdapter mAdapter;
    private ContentLoadingProgressBar mProgress;
    private View mEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDeviceListview = (ListView) findViewById(android.R.id.list);
        mAdapter = new DeviceListAdapter(this);
        mDeviceListview.setAdapter(mAdapter);

        mProgress = (ContentLoadingProgressBar) findViewById(android.R.id.progress);

        mDeviceListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> view, View item, int pos, long id) {
                Intent controllerActivity = new Intent(MainActivity.this, ControllerActivity.class);
                controllerActivity.putExtra(EXTRA_DEVICE_SERVICE, mAdapter.getItem(pos));
                startActivity(controllerActivity);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Snackbar.make(findViewById(R.id.activity_main), "Wifi is disabled.", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Enable", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.d(TAG, "Enabling Wi-Fi...");
                            wifiManager.setWifiEnabled(true);
                        }
                    }).show();
        }

        initDiscoveryService();
        registerReceivers();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mProgress.hide();
        closeServices();
        unregisterReceivers();
    }

    /**
     * Démarre le service permettant de chercher des drônes à proximité.
     */
    private void initDiscoveryService() {
        // create the service connection
        if (mArdiscoveryServiceConnection == null) {
            mArdiscoveryServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mArdiscoveryService = ((ARDiscoveryService.LocalBinder) service).getService();
                    startDiscovery();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mArdiscoveryService = null;
                }
            };
        }

        if (mArdiscoveryService == null) {
            // if the discovery service doesn't exists, bind to it
            Intent i = new Intent(getApplicationContext(), ARDiscoveryService.class);
            getApplicationContext().bindService(i, mArdiscoveryServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            // if the discovery service already exists, start discovery
            startDiscovery();
        }
    }

    /**
     * Commence à chercher les drônes à proximité.
     */
    private void startDiscovery() {
        if (mArdiscoveryService != null) {
            mArdiscoveryService.start();
            mProgress.show();
        }
    }

    /**
     * Méthode de callback appelée lorsqu'on a terminé de rechercher des drônes à prixmité.
     * Cette méthode peuple la ListView avec des informations sur les appareils disponibles.
     */
    @Override
    public void onServicesDevicesListUpdated() {
        Log.d(TAG, "onServicesDevicesListUpdated ...");
        if (mArdiscoveryService != null) {
            List<ARDiscoveryDeviceService> deviceList = mArdiscoveryService.getDeviceServicesArray();

            Log.d(TAG, "List is empty or null ? " + (deviceList == null || deviceList.isEmpty()));

            // Do what you want with the device list
            mAdapter.clear();
            mAdapter.addAll(deviceList);
            mProgress.hide();
            showEmptyView(deviceList.isEmpty());
        }
    }

    /**
     * Enregistre un BroadcastReceiver local, pour recevoir une notification lorsque la liste des
     * appareils est disponible. La méthode {@link #onServicesDevicesListUpdated()} sera appelée.
     */
    private void registerReceivers() {
        mArdiscoveryServicesDevicesListUpdatedReceiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(this);
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.registerReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver,
                new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));
    }

    private void unregisterReceivers() {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.unregisterReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver);
    }

    private void closeServices() {
        Log.d(TAG, "closeServices ...");

        if (mArdiscoveryService != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mArdiscoveryService.stop();

                    getApplicationContext().unbindService(mArdiscoveryServiceConnection);
                    mArdiscoveryService = null;
                }
            }).start();
        }
    }

    private void showEmptyView(boolean shown) {
        if (mEmpty == null) {
            mEmpty = ((ViewStub) findViewById(android.R.id.empty)).inflate();
        }
        mEmpty.setVisibility(shown ? View.VISIBLE : View.GONE);
    }
}