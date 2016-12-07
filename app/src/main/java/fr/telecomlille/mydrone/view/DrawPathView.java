package fr.telecomlille.mydrone.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DrawPathView extends View {

    public static final int TOUCH_TOLERANCE = 4;
    private static final String TAG = "DrawPathView";
    private Paint mPathPaint;
    private Path mPath;
    private PathListener mListener;
    private List<float[]> mPointsInPath = new ArrayList<>();

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
        mPathPaint.setColor(Color.RED);
        mPathPaint.setStrokeWidth(12);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mPointsInPath.size() > 1) {
            for (int i = 1; i < mPointsInPath.size(); i++) {
                float[] from = mPointsInPath.get(i - 1);
                float[] to = mPointsInPath.get(i);
                Log.d(TAG, String.format("Drawing lineto (%f.2;%f.2)", to[0], to[1]));
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
        mPath.lineTo(lastPoint[0], lastPoint[1]);
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

    public void setPathListener(PathListener listener) {
        mListener = listener;
    }


    public interface PathListener {
        void onPathFinished(List<float[]> pointsInPath);
    }
}
