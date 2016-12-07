package fr.telecomlille.mydrone;

import android.content.BroadcastReceiver;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ListView;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;

import java.util.List;

import fr.telecomlille.mydrone.utils.PermissionUtil;

/**
 * Activité principale permettant d'initier la connexion à distance avec le drône Bebop.
 * L'activité affiche une liste des différents appareils détectés.
 * Lorsqu'on clique sur l'un d'eux, une boite de dialogue s'ouvre pour choisir le mode de pilotage souhaité.
 */
public class MainActivity extends AppCompatActivity
        implements ARDiscoveryServicesDevicesListUpdatedReceiverDelegate,
        PilotingModeFragment.OnPilotingModeSelectedListener {

    /**
     * Clé pour {@link Intent#putExtra(String, android.os.Parcelable)} permettant de passer
     * les informations concernant un drône auquel on peut se connecter à une autre Activity.
     * L'objet transmis doit être du type {@link ARDiscoveryDeviceService}.
     */
    public static final String EXTRA_DEVICE_SERVICE = "DeviceService";
    private static final String TAG = "MainActivity";

    private ARDiscoveryService mArdiscoveryService;
    private ServiceConnection mArdiscoveryServiceConnection;
    private ARDiscoveryServicesDevicesListUpdatedReceiver mArdiscoveryServicesDevicesListUpdatedReceiver;

    private DeviceListAdapter mAdapter;
    private ContentLoadingProgressBar mProgress;
    private View mEmptyView;
    private WifiManager mWifiManager;
    private WifiStateChangedReceiver mWifiStateReceiver;
    private ARDiscoveryDeviceService mSelectedDrone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiStateReceiver = new WifiStateChangedReceiver();

        ListView deviceListview = (ListView) findViewById(android.R.id.list);
        mAdapter = new DeviceListAdapter(this);
        deviceListview.setAdapter(mAdapter);

        mProgress = (ContentLoadingProgressBar) findViewById(android.R.id.progress);

        deviceListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> view, View item, int pos, long id) {
                mSelectedDrone = mAdapter.getItem(pos);
                new PilotingModeFragment().show(getSupportFragmentManager(), "DIALOG");
            }
        });

        PermissionUtil.requestExternalStoragePermission(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!mWifiManager.isWifiEnabled()) {
            showWifiDisabledMessage();
        }

        registerReceiver(mWifiStateReceiver,
                new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        initDiscoveryService();
        registerReceivers();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mProgress.hide();
        unregisterReceiver(mWifiStateReceiver);
        closeServices();
        unregisterReceivers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.action_drawpath == item.getItemId()) {
            startActivity(new Intent(this, PathControlActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Démarre le service permettant de chercher des drônes à proximité.
     * Etant donné que le DiscoveryService s'appuie sur un BoundService,
     * il doit être connecté dans {@link #onStart()} et déconnecté dans {@link #onStop()}.
     */
    private void initDiscoveryService() {
        // Initialise la connexion au service
        if (mArdiscoveryServiceConnection == null) {
            mArdiscoveryServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mArdiscoveryService = ((ARDiscoveryService.LocalBinder) service).getService();
                    Log.d(TAG, "onServiceConnected: discovery service is now bound.");
                    startDiscovery();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mArdiscoveryService = null;
                }
            };
        }

        if (mArdiscoveryService == null) {
            // Si le service n'existe pas, le créer et l'attacher à l'Activity
            Intent discoveryService = new Intent(getApplicationContext(), ARDiscoveryService.class);
            getApplicationContext().bindService(discoveryService, mArdiscoveryServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            // Si le service est déjà créé, commencer la recherche.
            startDiscovery();
        }
    }

    /**
     * Si le service est démarré et attaché à l'Activity,
     * commencer à chercher les appareils à proximité via WiFi, Bluetooth Low Energy et USB.
     * Le WiFi ou le Bluetooth doivent être activés pour que cela fonctionne.
     */
    private void startDiscovery() {
        Log.d(TAG, "Starting discovery...");
        if (mArdiscoveryService != null) {
            mArdiscoveryService.start();
            mProgress.show();
        }
    }

    /**
     * Méthode de callback appelée lorsqu'on a terminé de rechercher des appareils à proximité.
     */
    @Override
    public void onServicesDevicesListUpdated() {
        Log.d(TAG, "onServicesDevicesListUpdated");
        if (mArdiscoveryService != null) {

            // Récupère la liste des appareils détectés
            List<ARDiscoveryDeviceService> deviceList = mArdiscoveryService.getDeviceServicesArray();
            Log.d(TAG, "onServicesDevicesListUpdated: found " + deviceList.size() + " devices.");

            // Met à jour la ListView
            mAdapter.clear();
            mAdapter.addAll(deviceList);
            mProgress.hide();

            showEmptyView(deviceList.isEmpty());
        }
    }

    /**
     * Enregistre un BroadcastReceiver pour recevoir une notification lorsque la liste des
     * appareils est disponible. La méthode {@link #onServicesDevicesListUpdated()} sera appelée.
     */
    private void registerReceivers() {
        mArdiscoveryServicesDevicesListUpdatedReceiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(this);
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.registerReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver,
                new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));
    }

    /**
     * Arrêter l'écoute des notifications envoyées lorsque la liste des appareils est disponible.
     * La méthode {@link #onServicesDevicesListUpdated()} ne sera plus appelée.
     */
    private void unregisterReceivers() {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.unregisterReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver);
    }

    /**
     * Arrête le service de recherche des appareils à proximité.
     * Le service devra être démarré à nouveau par {@link #initDiscoveryService()}.
     */
    private void closeServices() {
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

    /**
     * Affiche/masque un message indiquant à l'utilisateur qu'aucun appareil n'a été détecté.
     * @param shown true pour afficher, false pour masquer
     */
    private void showEmptyView(boolean shown) {
        if (mEmptyView == null) {
            mEmptyView = ((ViewStub) findViewById(android.R.id.empty)).inflate();
        }
        mEmptyView.setVisibility(shown ? View.VISIBLE : View.GONE);
    }

    /**
     * Affiche un message en bas de l'écran pour indiquer à l'utilisateur que le WiFi est désactivé.
     * Une action permet d'activer automatiquement le WiFi.
     */
    private void showWifiDisabledMessage() {
        Snackbar.make(findViewById(R.id.activity_main), R.string.wifi_disabled, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.enable, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.v(TAG, "Enabling Wi-Fi...");
                        mWifiManager.setWifiEnabled(true);
                    }
                }).show();
    }

    @Override
    public void onPilotingModeSelected(Class targetActivity) {
        Intent pilotingActivity = new Intent(MainActivity.this, targetActivity);
        pilotingActivity.putExtra(EXTRA_DEVICE_SERVICE, mSelectedDrone);
        startActivity(pilotingActivity);
    }

    /**
     * Un {@link BroadcastReceiver} qui écoute les changements d'états du WiFi.
     */
    public class WifiStateChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
                    && mWifiManager.isWifiEnabled()) {
                startDiscovery();
            }
        }
    }
}