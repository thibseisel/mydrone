package fr.telecomlille.mydrone.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import fr.telecomlille.mydrone.R;
import fr.telecomlille.mydrone.utils.ViewUtil;

/**
 * Une View transparente permettant de dessiner des tracés avec le doigt.
 * Le tracé précédent est effacé lorsqu'un nouveau tracé doit être dessiné.
 */
public class DrawPathView extends View {

    public static final int TOUCH_TOLERANCE = 4;
    private static final String TAG = "DrawPathView";
    private Paint mPathPaint;
    private Path mPath;
    private PathListener mListener;
    private List<float[]> mPointsInPath = new ArrayList<>();

    private boolean mDrawingEnabled;

    public DrawPathView(Context context) {
        this(context, null, 0);
    }

    public DrawPathView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawPathView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mPointsInPath = new ArrayList<>();
        mPath = new Path();

        mPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPathPaint.setStyle(Paint.Style.STROKE);
        mPathPaint.setStrokeCap(Paint.Cap.ROUND);
        mPathPaint.setStrokeJoin(Paint.Join.ROUND);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.DrawPathView, defStyleAttr, 0);
        try {
            int defaultColor = ViewUtil.resolveThemeColor(context, R.attr.colorAccent);
            mPathPaint.setColor(a.getColor(R.styleable.DrawPathView_pathColor, defaultColor));
            mPathPaint.setStrokeWidth(a.getDimensionPixelSize(R.styleable.DrawPathView_pathSize, 12));
            mDrawingEnabled = a.getBoolean(R.styleable.DrawPathView_drawingEnabled, true);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mPointsInPath.size() > 1) {
            for (int i = 1; i < mPointsInPath.size(); i++) {
                float[] from = mPointsInPath.get(i - 1);
                float[] to = mPointsInPath.get(i);
                canvas.drawLine(from[0], from[1], to[0], to[1], mPathPaint);
            }
        }
    }

    private void onTouchDown(float x, float y) {
        mPath.reset();
        mPointsInPath.clear();
        mPath.moveTo(x, y);
        mPointsInPath.add(new float[]{x, y});
        invalidate();
    }

    private void onTouchMove(float x, float y) {
        float[] lastPoint = mPointsInPath.get(mPointsInPath.size() - 1);
        float dx = Math.abs(x - lastPoint[0]);
        float dy = Math.abs(y - lastPoint[1]);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(lastPoint[0], lastPoint[1],
                    (x + lastPoint[0] / 2), (y + lastPoint[1] / 2));
            mPointsInPath.add(new float[]{x, y});
            invalidate();

            if (mListener != null) {
                mListener.onPathFinished(mPointsInPath);
            }
        }
    }

    private void onTouchUp(float x, float y) {
        float[] lastPoint = mPointsInPath.get(mPointsInPath.size() - 1);
        mPath.lineTo(x, y);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onTouchDown(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
                onTouchUp(event.getX(), event.getY());
                return true;
            default:
                return false;
        }
    }

    /**
     * Active ou désactive l'enregistrement et le dessin des tracés.
     * Si le mode tracé est activé, ils sont dessinés à l'écran et notifiés quand le tracé est fini.
     * Si le mode tracé est désactivé, une notification est envoyée quand la View est touchée.
     *
     * @param enabled si le mode tracé est activé ou non
     */
    private void setDrawingEnabled(boolean enabled) {
        mDrawingEnabled = enabled;
    }

    /**
     * Enregistre un listener qui va recevoir les tracés effectués sur cette View.
     */
    public void setPathListener(PathListener listener) {
        mListener = listener;
    }


    public interface PathListener {
        /**
         * Indique qu'un tracé a été complété.
         * @param pointsInPath ensemble des points qui constituent le tracé
         */
        void onPathFinished(List<float[]> pointsInPath);

        /**
         * Indique que la zone de dessin a été touchée alors que le mode tracé est désactivé.
         * Cette méthode permet (typiquement) d'annuler une action liée à un tracé précédent.
         */
        void onPathCanceled();
    }
}
