package fr.telecomlille.mydrone.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import fr.telecomlille.mydrone.R;
import fr.telecomlille.mydrone.utils.ViewUtil;

/**
 * Un View qui se comporte comme un joystick.
 */
public class JoystickView extends View {

    private static final String TAG = "JoystickView";
    private boolean mIsPressed;
    private int mTrackColor;
    private int mThumbColor;
    private int mThumbColorPressed;
    private Paint mTrackPaint;
    private Paint mThumbPaint;
    private float mTrackCx;
    private float mTrackCy;
    private float mTrackRadius;
    private float mThumbCx;
    private float mThumbCy;
    private float mThumbRadius;
    private Listener mListener;

    /**
     * Construit une nouvelle instance de JoystickView.
     *
     * @param context contexte courant
     */
    public JoystickView(Context context) {
        this(context, null);
    }

    /**
     * Construit une nouvelle instance de JoystickView avec les attributs spécifiés par XML.
     *
     * @param context contexte courant
     * @param attrs   attributs spécifiés par XML
     */
    public JoystickView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Construit une nouvelle instance de JoystickView avec les attributs spécifiés par XML
     * et un style par défaut.
     *
     * @param context      contexte courant
     * @param attrs        attributs spécifiés par XML
     * @param defStyleAttr identifiant d'une ressource du thème courant pointant vers le style par
     *                     défaut à utiliser pour ce View. 0 pour ne pas utiliser de style par défaut.
     */
    public JoystickView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Récupération des couleurs du thème
        mTrackColor = ViewUtil.resolveThemeColor(context, R.attr.colorButtonNormal);
        mThumbColor = ViewUtil.resolveThemeColor(context, R.attr.colorControlHighlight);
        mThumbColorPressed = ViewUtil.resolveThemeColor(context, R.attr.colorControlActivated);

        // Récupération des attributs personnalisés
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.JoystickView,
                defStyleAttr, 0);
        a.recycle();
        init();
    }

    private void init() {
        mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTrackPaint.setColor(mTrackColor);
        mTrackPaint.setStyle(Paint.Style.STROKE);

        mThumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mThumbPaint.setColor(mThumbColor);
        mThumbPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minw = getPaddingLeft() + getSuggestedMinimumWidth() + getPaddingRight();
        int w = resolveSize(minw, widthMeasureSpec);

        int minh = getPaddingTop() + getSuggestedMinimumHeight() + getPaddingBottom();
        int h = resolveSize(minh, heightMeasureSpec);

        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float xpad = (float) (getPaddingLeft() + getPaddingRight());
        float ypad = (float) (getPaddingTop() + getPaddingBottom());

        float ww = (float) w - xpad;
        float hh = (float) h - ypad;

        mTrackCx = w / 2;
        mTrackCy = h / 2;

        float maxRadius = Math.min(ww, hh) / 2;
        mTrackPaint.setStrokeWidth(maxRadius / 10);
        mTrackRadius = maxRadius * 2 / 3;

        mThumbCx = w / 2;
        mThumbCy = h / 2;
        mThumbRadius = maxRadius / 3;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(mTrackCx, mTrackCy, mTrackRadius, mTrackPaint);
        canvas.drawCircle(mThumbCx, mThumbCy, mThumbRadius, mThumbPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float distance = getDistanceFromOrigin(event.getX(), event.getY());
        float strength = distance / mTrackRadius;
        float angle = getAngle(event.getX(), event.getY());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Le doigt se déplace sur le joystick
                setThumbPressed(true);
            case MotionEvent.ACTION_MOVE:
                // Le doigt vient de se poser sur le joystick
                if (strength <= 1.0) {
                    mThumbCx = event.getX();
                    mThumbCy = event.getY();
                } else {
                    mThumbCx = (float) (mTrackCx + mTrackRadius * Math.cos(angle));
                    mThumbCy = (float) (mTrackCy - mTrackRadius * Math.sin(angle));
                }
                break;
            case MotionEvent.ACTION_UP:
                // Le doigt vient de relâcher le joystick, retour en position neutre
                setThumbPressed(false);
                mThumbCx = mTrackCx;
                mThumbCy = mTrackCy;
                strength = 0;
                angle = 0;
                break;
        }
        invalidate();
        notifyPositionChanged(angle, (float) Math.min(strength, 1.0));
        return true;
    }

    /**
     * Calcule la distance euclidienne entre le point de placement du joystick
     * et le point de position neutre.
     *
     * @param x abscisse de la position de la partie amovible du joystick dans le repère
     * @param y ordonnées de la position de la partie amovible du joystick dans le repère
     * @return distance du point avec le centre de la View
     */
    private float getDistanceFromOrigin(float x, float y) {
        return (float) Math.hypot(mTrackCx - x, mTrackCy - y);
    }

    private void notifyPositionChanged(float angle, float strength) {
        if (mListener != null) {
            mListener.onThumbPositionChanged(angle, strength);
        }
    }

    private float getAngle(float x, float y) {
        if (x > mTrackCx) {
            if (y < mTrackCy) {
                return (float) Math.atan((y - mTrackCy) / (mTrackCx - x));
            } else {
                return (float) (2 * Math.PI - Math.atan((mTrackCy - y) / (mTrackCx - x)));
            }
        } else if (x < mTrackCx) {
            return (float) (Math.PI - Math.atan((mTrackCy - y) / (mTrackCx - x)));
        } else {
            if (y < mTrackCy) return (float) (Math.PI / 2);
            else return (float) (3 * Math.PI / 2);
        }
    }

    public boolean isPressed() {
        return mIsPressed;
    }

    private void setThumbPressed(boolean isPressed) {
        mThumbPaint.setColor(isPressed ? mThumbColorPressed : mThumbColor);
        mIsPressed = isPressed;
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return ViewUtil.dipToPixels(getContext(), 192);
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return ViewUtil.dipToPixels(getContext(), 192);
    }

    public void setJoystickListener(Listener listener) {
        mListener = listener;
    }

    /**
     * Interface permettant d'écouter les évènements liés aux mouvements d'un {@link JoystickView}.
     */
    public interface Listener {
        /**
         * Appelé lorsque le bouton amovible du joystick change de position suite à son toucher.
         *
         * @param angle    direction dans laquelle le joystick est orienté, en radians.
         *                 Arbitrairement, l'angle à 0 rad pointe vers la droite.
         * @param strength distance d'éloignement du bouton amovible avec sa position neutre.
         *                 Les valeurs vont de 0.0 (position neutre) à 1.0 (maximum d'inclinaison).
         */
        void onThumbPositionChanged(float angle, float strength);
    }
}
