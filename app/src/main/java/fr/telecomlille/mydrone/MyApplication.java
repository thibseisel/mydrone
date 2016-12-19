package fr.telecomlille.mydrone;

import android.app.Application;

import com.parrot.arsdk.ARSDK;

/**
 * Classe représentant une instance de l'application.
 * Permet de charger les libraries natives permettant le pilotage du drone dès le démarrage de l'application,
 * et ce pour toutes les Activity.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ARSDK.loadSDKLibs();
    }
}
