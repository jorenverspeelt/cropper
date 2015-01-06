import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;

public class CropView extends ImageView {

    private Paint mPaint = new Paint();
    private Paint mCirclePaint = new Paint();
    /**
     * The starting size of the rectangle.
     */
    private int mInitialWidthSize;
    private int mInitialHeightSize;
    /**
     * The points of the cropping rectangle.
     */
    private Point mLeftTop;
    private Point mRightBottom;
    private Point mCenter;
    /**
     * Points to check if the cropping rectangle does not leave the image.
     */
    private Point mScreenCenter;
    private Point mPrevious;
    private DisplayMetrics mMetrics = getResources().getDisplayMetrics();
    /**
     * Size and touch variables for changing the rectangle.
     */
    private int mMinimumSize;
    private int mTouchBuffer;

    /**
     * The possible grabpoint of the rectangle.
     */
    private enum TouchAction {
        DRAG, LEFTTOP, RIGHTTOP, RIGHTBOTTOM, LEFTBOTTOM, LEFTSIDE, RIGHTSIDE, BOTTOMSIDE, TOPSIDE
    }

    /**
     * Variables to check if the cropping rectangle does not leave the image.
     */
    private int mImageScaledWidth, mImageScaledHeight;

    // adding parent class constructors
    public CropView(Context context) {
        super(context);
        initCropView();
    }

    public CropView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        initCropView();
    }

    public CropView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initCropView();
    }

    /**
     * Initializes the CropView with predefined settigns.
     */
    protected void initCropView() {
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeWidth(5);
        mCirclePaint.setStyle(Style.FILL);
        mCirclePaint.setColor(Color.WHITE);
        mCirclePaint.setStrokeWidth(5);
        mLeftTop = new Point();
        mRightBottom = new Point();
        mCenter = new Point();
        mScreenCenter = new Point();
        mPrevious = new Point();

        mPaint.setColor(Color.WHITE);
        mInitialWidthSize = mMetrics.widthPixels / 4;
        mInitialHeightSize = mMetrics.heightPixels / 4;
        mMinimumSize = mMetrics.densityDpi;
        mTouchBuffer = mMetrics.densityDpi / 3;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mLeftTop.equals(0, 0))
            resetPoints();
        // draw the points on the screen; one in every corner and one in the center
        int radius = mMetrics.densityDpi / 12;

        canvas.drawRect(mLeftTop.x, mLeftTop.y, mRightBottom.x, mRightBottom.y, mPaint);
        canvas.drawArc(new RectF(mLeftTop.x - radius, mLeftTop.y - radius, mLeftTop.x + radius,
                mLeftTop.y + radius), 90, 270, true, mCirclePaint);
        canvas.drawArc(new RectF(mRightBottom.x - radius, mLeftTop.y - radius, mRightBottom.x
                + radius, mLeftTop.y + radius), -180, 270, true, mCirclePaint);
        canvas.drawArc(new RectF(mRightBottom.x - radius, mRightBottom.y - radius, mRightBottom.x
                + radius, mRightBottom.y + radius), -90, 270, true, mCirclePaint);
        canvas.drawArc(new RectF(mLeftTop.x - radius, mRightBottom.y - radius, mLeftTop.x + radius,
                mRightBottom.y + radius), 0, 270, true, mCirclePaint);
        canvas.drawCircle(mCenter.x, mCenter.y, mMetrics.densityDpi / 22, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();
        switch (eventaction) {
        // set the touch point
            case MotionEvent.ACTION_DOWN:
                mPrevious.set((int) event.getX(), (int) event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                // check if the touch was inside the rectangle
                if (isActionInsideRectangle(event.getX(), event.getY())) {
                    // get where the touchevent happened
                    switch (getTouchAction(event.getX(), event.getY())) {
                        case LEFTTOP:
                            adjustRectangle((int) event.getX(), (int) event.getY(),
                                    TouchAction.LEFTTOP);
                            break;
                        case RIGHTTOP:
                            adjustRectangle((int) event.getX(), (int) event.getY(),
                                    TouchAction.RIGHTTOP);
                            break;
                        case LEFTBOTTOM:
                            adjustRectangle((int) event.getX(), (int) event.getY(),
                                    TouchAction.LEFTBOTTOM);
                            break;
                        case RIGHTBOTTOM:
                            adjustRectangle((int) event.getX(), (int) event.getY(),
                                    TouchAction.RIGHTBOTTOM);
                            break;
                        case BOTTOMSIDE:// bottom
                            adjustRectangle(mRightBottom.x, (int) event.getY(),
                                    TouchAction.BOTTOMSIDE);
                            break;
                        case TOPSIDE:// top
                            adjustRectangle(mLeftTop.x, (int) event.getY(), TouchAction.TOPSIDE);
                            break;
                        case LEFTSIDE:// left
                            adjustRectangle((int) event.getX(), mLeftTop.y, TouchAction.LEFTSIDE);
                            break;
                        case RIGHTSIDE:// right
                            adjustRectangle((int) event.getX(), mRightBottom.y,
                                    TouchAction.RIGHTSIDE);
                            break;
                        case DRAG:// drag
                            adjustRectangle((int) event.getX(), (int) event.getY(),
                                    TouchAction.DRAG);
                            break;

                    }
                    invalidate(); // redraw rectangle
                    mPrevious.set((int) event.getX(), (int) event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
                mPrevious = new Point();
                break;
        }

        return true;
    }

    /**
     * Method to detect which part of the rectangle is touched.
     * <p>
     * This method takes the coordinates of the touchevent and determines where and if the user
     * touched the square.
     * <p>
     * 
     * @param x
     *            coordinate from the touchevent
     * 
     * @param y
     *            coordinate from the touchevent
     * 
     * @return TouchAction for the side or point of the square
     */
    protected TouchAction getTouchAction(float x, float y) {
        // checks if the rectangle has to be dragged
        if (mCenter.x - mTouchBuffer / 2 < x && mCenter.x + mTouchBuffer / 2 > x
                && mCenter.y - mTouchBuffer / 2 < y && mCenter.y + mTouchBuffer > y) {
            return TouchAction.DRAG;
        }

        // checks for the top line
        if (mLeftTop.y - mTouchBuffer < y && mLeftTop.y + mTouchBuffer > y
                && x > mLeftTop.x + mTouchBuffer && mRightBottom.x - mTouchBuffer > x) {

            return TouchAction.TOPSIDE;
        }

        // checks for the right line
        if (mRightBottom.x - mTouchBuffer < x && mRightBottom.x + mTouchBuffer > x
                && y > mLeftTop.y + mTouchBuffer && mRightBottom.y - mTouchBuffer > y) {

            return TouchAction.RIGHTSIDE;
        }

        // checks for the bottom line
        if (mRightBottom.y - mTouchBuffer < y && mRightBottom.y + mTouchBuffer > y
                && x > mLeftTop.x + mTouchBuffer && mRightBottom.x - mTouchBuffer > x) {

            return TouchAction.BOTTOMSIDE;
        }

        // checks for the left line
        if (mLeftTop.x - mTouchBuffer < x && mLeftTop.x + mTouchBuffer > x
                && y > mLeftTop.y + mTouchBuffer && mRightBottom.y - mTouchBuffer > y) {

            return TouchAction.LEFTSIDE;
        }

        // checks for the left top corner
        if (mLeftTop.x - mTouchBuffer < x && mLeftTop.x + mTouchBuffer > x
                && mLeftTop.y - mTouchBuffer < y && mLeftTop.y + mTouchBuffer > y) {

            return TouchAction.LEFTTOP;
        }

        // checks for the right top corner
        if (mRightBottom.x - mTouchBuffer < x && mRightBottom.x + mTouchBuffer > x
                && mLeftTop.y - mTouchBuffer < y && mLeftTop.y + mTouchBuffer > y) {

            return TouchAction.RIGHTTOP;
        }

        // checks for the right bottom corner
        if (mRightBottom.x - mTouchBuffer < x && mRightBottom.x + mTouchBuffer > x
                && mRightBottom.y - mTouchBuffer < y && mRightBottom.y + mTouchBuffer > y) {

            return TouchAction.RIGHTBOTTOM;
        }

        // checks for the left bottom corner
        if (mLeftTop.x - mTouchBuffer < x && mLeftTop.x + mTouchBuffer > x
                && mRightBottom.y - mTouchBuffer < y && mRightBottom.y + mTouchBuffer > y) {

            return TouchAction.LEFTBOTTOM;
        }
        // if it was something else, return drag
        return TouchAction.DRAG;
    }

    /**
     * This method sets the coordinates of the points.
     */
    public void resetPoints() {
        mCenter.set(getWidth() / 2, getHeight() / 2);
        mScreenCenter.set(getWidth() / 2, getHeight() / 2);
        mLeftTop.set((getWidth() - mInitialWidthSize) / 2, (getHeight() - mInitialHeightSize) / 2);
        mRightBottom.set(mLeftTop.x + mInitialWidthSize, mLeftTop.y + mInitialHeightSize);
    }

    /**
     * This method checks if the touchaction happened inside the rectangle.
     * 
     * @param x
     *            The X coordinate of the touch.
     * @param y
     *            The Y coordinate of the touch.
     * @return A boolean to indicate the touchaction was inside the rectangle or not.
     */
    protected boolean isActionInsideRectangle(float x, float y) {
        if (x >= (mLeftTop.x - mTouchBuffer) && x <= (mRightBottom.x + mTouchBuffer)
                && y >= (mLeftTop.y - mTouchBuffer) && y <= (mRightBottom.y + mTouchBuffer)) {

            return true;

        } else {
            return false;
        }
    }

    /**
     * Checks if the movement of the rectangle will force it out of the image.
     * 
     * @param point
     *            The new rightBottom Point.
     * @return A boolean to indicate if the rectangle is still in the image or not.
     */
    protected boolean isInImageRange(PointF point) {
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        getImageMatrix().getValues(f);

        // Calculate the scaled dimensions
        mImageScaledWidth = Math.round(getDrawable().getIntrinsicWidth() * f[Matrix.MSCALE_X]);
        mImageScaledHeight = Math.round(getDrawable().getIntrinsicHeight() * f[Matrix.MSCALE_Y]);

        return (point.x >= (mScreenCenter.x - (mImageScaledWidth / 2))
                && point.x <= (mScreenCenter.x + (mImageScaledWidth / 2))
                && point.y >= (mScreenCenter.y - (mImageScaledHeight / 2)) && point.y <= (mScreenCenter.y + (mImageScaledHeight / 2))) ? true
                : false;
    }

    /**
     * Changes the coordinates of sLeftTop and sBottomRight
     * <p>
     * In every case statement of the switch will be checked first if changing the size of the
     * rectangle will force the rectangle out of the picture. If it does, the coordinates will not
     * be changed. After that, there is checked or changing the size of the rectangle will force the
     * rectangle to be smaller than it's defined minimum size. If it does, the coordinates will not
     * be changed.
     * </p>
     * 
     * @param x
     *            The X coordinate of the change.
     * @param y
     *            The Y coordinate of the change.
     * @param action
     *            The place where the rectangle changed.
     */
    protected void adjustRectangle(int x, int y, TouchAction action) {
        int movement;

        switch (action) {
            case LEFTSIDE:
                movement = x - mLeftTop.x;
                if (isInImageRange(new PointF(mLeftTop.x + movement, mLeftTop.y))) {
                    if (mLeftTop.x + movement < mRightBottom.x - mMinimumSize) {
                        mLeftTop.set(mLeftTop.x + movement, mLeftTop.y);
                    }
                }
                break;
            case RIGHTSIDE:
                movement = x - mRightBottom.x;
                if (isInImageRange(new PointF(mRightBottom.x + movement, mRightBottom.y))) {
                    if (mRightBottom.x + movement > mLeftTop.x + mMinimumSize) {
                        mRightBottom.set(mRightBottom.x + movement, mRightBottom.y);
                    }
                }
                break;
            case BOTTOMSIDE:
                movement = y - mRightBottom.y;
                if (isInImageRange(new PointF(mRightBottom.x, mRightBottom.y + movement))) {
                    if (mRightBottom.y + movement > mLeftTop.y + mMinimumSize) {
                        mRightBottom.set(mRightBottom.x, mRightBottom.y + movement);
                    }
                }
                break;
            case TOPSIDE:
                movement = y - mLeftTop.y;
                if (isInImageRange(new PointF(mLeftTop.x, mLeftTop.y + movement))) {
                    if (mLeftTop.y + movement < mRightBottom.y - mMinimumSize) {
                        mLeftTop.set(mLeftTop.x, mLeftTop.y + movement);
                    }
                }
                break;
            case LEFTTOP:
                movement = x - mLeftTop.x;
                if (isInImageRange(new PointF(mLeftTop.x + movement, mLeftTop.y + movement))) {
                    if (mLeftTop.y + movement < mRightBottom.y - mMinimumSize
                            && mLeftTop.x + movement < mRightBottom.x - mMinimumSize) {
                        mLeftTop.set(mLeftTop.x + movement, mLeftTop.y + movement);
                    }
                }
                break;
            case RIGHTTOP:
                movement = x - mRightBottom.x;
                if (isInImageRange(new PointF(mRightBottom.x + movement, mLeftTop.y + movement))) {
                    if (mRightBottom.x + movement > mLeftTop.x + mMinimumSize
                            && mLeftTop.y - movement < mRightBottom.y - mMinimumSize) {
                        mRightBottom.set(mRightBottom.x + movement, mRightBottom.y);
                        mLeftTop.set(mLeftTop.x, mLeftTop.y - movement);
                    }
                }
                break;
            case RIGHTBOTTOM:
                movement = x - mRightBottom.x;
                if (isInImageRange(new PointF(mRightBottom.x + movement, mRightBottom.y + movement))) {
                    if (mRightBottom.y + movement > mLeftTop.y + mMinimumSize
                            && mRightBottom.x + movement > mLeftTop.x + mMinimumSize) {
                        mRightBottom.set(mRightBottom.x + movement, mRightBottom.y + movement);
                    }
                }
                break;
            case LEFTBOTTOM:
                movement = x - mLeftTop.x;
                if (isInImageRange(new PointF(mLeftTop.x + movement, mRightBottom.y + movement))) {
                    if (mRightBottom.y - movement > mLeftTop.y + mMinimumSize
                            && mLeftTop.x + movement < mRightBottom.x - mMinimumSize) {
                        mRightBottom.set(mRightBottom.x, mRightBottom.y - movement);
                        mLeftTop.set(mLeftTop.x + movement, mLeftTop.y);
                    }
                }
                break;
            case DRAG:
                movement = x - mPrevious.x;
                int movementY = y - mPrevious.y;
                if (isInImageRange(new PointF(mLeftTop.x + movement, mLeftTop.y + movementY))
                        && isInImageRange(new PointF(mRightBottom.x + movement, mRightBottom.y
                                + movementY))) {
                    mLeftTop.set(mLeftTop.x + movement, mLeftTop.y + movementY);
                    mRightBottom.set(mRightBottom.x + movement, mRightBottom.y + movementY);
                }
                break;
            default:
                break;

        }
        mCenter.set((mRightBottom.x + mLeftTop.x) / 2, (mRightBottom.y + mLeftTop.y) / 2);

    }

    /**
     * Crops the image to the size of the cropping rectangle.
     * 
     * @param bitmapDrawable
     *            The original bitmap from which the cropped image will be created.
     * @param viewWidth
     *            The width of the View element, needed to calculate rescaling.
     * @param viewHeight
     *            The height of the View element, needed to calculate rescaling.
     * @return A Byte array of the cropped bitmap.
     */
    public byte[] getCroppedImage(BitmapDrawable bitmapDrawable, int viewWidth, int viewHeight) {
        // calculate the scaling for the image to the screen

        // int correctWidth = this.getWidth() - viewWidth;
        float scaleWidth = (float) bitmapDrawable.getBitmap().getWidth() / (float) viewWidth;
        float scaleHeight = (float) bitmapDrawable.getBitmap().getHeight() / (float) viewHeight;

        // calculate the points of the cropped image
        int startWidth = (int) ((bitmapDrawable.getBitmap().getWidth() - (bitmapDrawable
                .getBitmap().getWidth() - mLeftTop.x)) * scaleWidth);
        int startHeight = (int) ((bitmapDrawable.getBitmap().getHeight() - (bitmapDrawable
                .getBitmap().getHeight() - mLeftTop.y)) * scaleHeight);
        int endWidth = (int) ((bitmapDrawable.getBitmap().getWidth() - (bitmapDrawable.getBitmap()
                .getWidth() - mRightBottom.x)) * scaleWidth);
        int endHeight = (int) ((bitmapDrawable.getBitmap().getHeight() - (bitmapDrawable
                .getBitmap().getHeight() - mRightBottom.y)) * scaleHeight);

        // create the cropped bitmap
        Bitmap cropped = Bitmap.createBitmap(bitmapDrawable.getBitmap(), startWidth, startHeight,
                endWidth - startWidth, endHeight - startHeight);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        cropped.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

}
