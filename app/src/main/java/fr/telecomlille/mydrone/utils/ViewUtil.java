package fr.telecomlille.mydrone.utils;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * Classe utilitaire regroupant des fonctionnalités liées aux View.
 */
public final class ViewUtil {

    /**
     * Donne le nombre de pixels équivalent à un nombre de DIP (Density-Independant Pixel).
     * Cette unité permet d'obtenir des dimensions similaires sur des écrans de taille
     * et de densité différentes.
     * @param dp nombre de dp à convertir en pixels
     * @return nombre de pixels
     */
    public static int dipToPixels(Context context, float dp) {
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics));
    }

    /**
     * Récupère une couleur appartenant au thème de l'application, telle que "colorPrimary"
     * ou "colorAccent".
     * @param themeAttr attribut du thème pointant sur la couleur
     * @return nombre entier représentant cette couleur
     */
    @ColorInt
    public static int resolveThemeColor(Context context, @AttrRes int themeAttr) {
        TypedValue outValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(themeAttr, outValue, true);
        return outValue.data;
    }
}
