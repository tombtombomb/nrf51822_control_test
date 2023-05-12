package com.example.administrator.nrf51822_control;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class RockerView extends View{
    private static final int scale=4096;
    private Paint outerCirclePaint;
    private Paint innerCirclePaint;
    /** 内圆中心x坐标 */
    private double innerCenterX;
    /** 内圆中心y坐标 */
    private double innerCenterY;
    /** view中心点x坐标 */
    private float viewCenterX;
    /** view中心点y左边 */
    private float viewCenterY;
    /** view宽高大小，设定宽高相等 */
    private int size;
    /** 外圆半径 */
    private int outerCircleRadius;
    /** 内圆半径 */
    private int innerCircleRadius;

    private int innerLimit;



    public RockerView(Context context) {
        super(context);
        init();
    }
//在自定义View的时候，需要两个构造函数。否则在编译的时候会报异常：Binary XML file line Error inflating class.
// 原因是：Android在根据xml文件夹创建View对象的时候会调用View的双参构造方法，即public SquareView(Context context， AttributeSetattrs)，
// 所以如果没有写全的话就会报错。
    public RockerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        outerCirclePaint = new Paint();
        outerCirclePaint.setColor(getResources().getColor(R.color.dimgray));
        outerCirclePaint.setAntiAlias(true);
        outerCirclePaint.setMaskFilter(new BlurMaskFilter(50, BlurMaskFilter.Blur.INNER));
        innerCirclePaint = new Paint();
        innerCirclePaint.setAlpha(130);
        innerCirclePaint.setColor(getResources().getColor(R.color.orangered));
        innerCirclePaint.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        size = getMeasuredWidth();
        setMeasuredDimension(size, size);
        innerCenterX = size/2;
        innerCenterY = size/2;
        viewCenterX = size/2;
        viewCenterY = size/2;
        outerCircleRadius = size/2;
        innerCircleRadius = size/5;
        innerLimit=2*(outerCircleRadius-innerCircleRadius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(viewCenterX, viewCenterY, outerCircleRadius, outerCirclePaint);
        canvas.drawCircle((float) innerCenterX, (float) innerCenterY, innerCircleRadius, innerCirclePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                handleEvent(event);
                break;
            case MotionEvent.ACTION_UP:
                restorePosition();
                break;
        }
        return true;
    }

    /**
     * 处理手势事件
     */
    private void handleEvent(MotionEvent event) {
        double distance = Math.sqrt(Math.pow(event.getX()-viewCenterX, 2) + Math.pow(event.getY()-viewCenterY, 2)); //触摸点与view中心距离
        if (distance < outerCircleRadius-innerCircleRadius) {
            //在自由域之内，触摸点实时作为内圆圆心
            innerCenterX = event.getX();
            innerCenterY = event.getY();
            invalidate();
        } else {
            //在自由域之外，内圆圆心在触摸点与外圆圆心的线段上
            updateInnerCircelCenter(event);
        }
    }

    /**
     * 在自由域外更新内圆中心坐标
     */
    private void updateInnerCircelCenter(MotionEvent event) {
        double distance = Math.sqrt(Math.pow(event.getX()-viewCenterX, 2) + Math.pow(event.getY()-viewCenterY, 2));  //当前触摸点到圆心的距离
        int innerDistance = outerCircleRadius-innerCircleRadius;  //内圆圆心到中心点距离
        //相似三角形的性质，两个相似三角形各边比例相等得到等式
        innerCenterX = (event.getX()-viewCenterX)*innerDistance/distance + viewCenterX;
        innerCenterY = (event.getY()-viewCenterY)*innerDistance/distance + viewCenterY;
        invalidate();
    }

    /**
     * 恢复内圆到view中心位置
     */
    private void restorePosition() {
        innerCenterX = viewCenterX;
        innerCenterY = viewCenterY;
        invalidate();
    }

    public int getXposition()
    {
        return (int) (((innerCenterX-innerCircleRadius)/innerLimit)*scale);
    }

    public int getYposition()
    {
        return (int) ((1-(innerCenterY-innerCircleRadius)/innerLimit)*scale);
    }
}
