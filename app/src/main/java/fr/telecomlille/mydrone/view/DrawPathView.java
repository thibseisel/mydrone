package fr.telecomlille.mydrone.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hélène on 05/12/2016.
 */

public class DrawPathView extends View{

    private Paint mPathPaint;
    private Path mPath = new Path();
    private PathListener mListener;
    private ArrayList<float[]> mPointsInPath = new ArrayList<>();

    public DrawPathView(Context context) {
        this(context, null, 0);
    }

    public DrawPathView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawPathView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPathPaint = new Paint();
        mPointsInPath = new ArrayList<>();
        mPathPaint.setColor(Color.RED);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int i = 0 ; i < mPointsInPath.size() - 1 ; i ++){
            canvas.drawLine(mPointsInPath.get(i)[0], mPointsInPath.get(i)[1], mPointsInPath.get(i + 1)[0],
                    mPointsInPath.get(i + 1)[1], mPathPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN :
                mPointsInPath.clear();
                mPath.reset();
                mPath.moveTo(event.getX(), event.getY());
                mPointsInPath.add(new float[]{getX(), getY()});
                break;
            case MotionEvent.ACTION_MOVE :
                mPath.lineTo(event.getX(), event.getY());
                mPointsInPath.add(new float[]{getX(), getY()});
                break;
            case MotionEvent.ACTION_UP :
                if (mListener != null) {
                    mListener.onPathFinished(mPointsInPath);
                }
                break;
        }
        invalidate();
        return true;
    }

    public void setPathListener(PathListener listener){
        this.mListener = listener;
    }


    public interface PathListener {
        public void onPathFinished(ArrayList<float[]> pointsInPath);
    }
}
