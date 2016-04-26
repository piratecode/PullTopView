package lxfeng.pulltopview;

import android.content.Context;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.Scroller;

public class PullTopView extends FrameLayout {

    private static final String TAG = PullTopView.class.getSimpleName();
    private static final float SCROLL_PATIO = 0.8f;

    private View mTopView;
    private View mInnerScrollView;
    private Scroller mScroller;

    private int mTopViewHeight;

    private int mTouchSlop;
    private float mStartY;

    private VelocityTracker mVelocityTracker;

    public PullTopView(Context context) {
        super(context);
        init(context);
    }

    public PullTopView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PullTopView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mScroller = new Scroller(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 2) return;
        mTopView = getChildAt(0);
        mInnerScrollView = getChildAt(1);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mTopViewHeight = mTopView.getMeasuredHeight();
        mTopView.layout(0, 0, mTopView.getRight(), mTopViewHeight);
        mInnerScrollView.layout(0, mTopViewHeight, mInnerScrollView.getRight(),
                mTopViewHeight + mInnerScrollView.getBottom());
        Log.d(TAG, "TopViewHeight-->" + mTopViewHeight);
        Log.d(TAG, "Bottom-->" + (mTopViewHeight + mInnerScrollView.getBottom()));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d(TAG, "----onInterceptTouchEvent----");
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float preY = mStartY == 0 ? ev.getY() : mStartY;
                float nowY = ev.getY();
                int deltaY = (int) (nowY - preY);
                mStartY = nowY;
                Log.d(TAG, "deltaY-->" + deltaY);
                boolean isIntercept = true;
                if (getScrollY() >= mTopViewHeight && deltaY <= 0) {
                    isIntercept = false;
                }
                if (getScrollY() >= mTopViewHeight && !isInnerViewScrollToTop()) {
                    isIntercept = false;
                }
                if (getScrollY() <= 0 && deltaY >= 0) {
                    isIntercept = false;
                }
                Log.d(TAG,"isIntercept-->"+isIntercept);
                return isIntercept;
            case MotionEvent.ACTION_UP:

                break;

        }
        return super.onInterceptTouchEvent(ev);
    }

    public boolean isInnerViewScrollToTop() {
        if (Build.VERSION.SDK_INT < 14) {
            if (mInnerScrollView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mInnerScrollView;
                return !(absListView.getChildCount() > 0 && (absListView
                        .getFirstVisiblePosition() > 0 || absListView
                        .getChildAt(0).getTop() < absListView.getPaddingTop()));
            } else {
                return !(mInnerScrollView.getScrollY() > 0);
            }
        } else {
            return !ViewCompat.canScrollVertically(mInnerScrollView, -1);
        }
    }


    private void createVelocityTracker(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    private int getYVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000);
        return (int) mVelocityTracker.getYVelocity();
    }

    private void recyclerVelocity() {
        if (mVelocityTracker == null) return;
        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        createVelocityTracker(event);
        int curScrollY = getScrollY();
        Log.d(TAG, "curScrollY-->" + curScrollY);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float preY = mStartY == 0 ? event.getY() : mStartY;
                float nowY = event.getY();
                int deltaY = (int) (nowY - preY);
                mStartY = nowY;
                if (curScrollY - deltaY >= 0 && curScrollY - deltaY <= mTopViewHeight) {
                    scrollBy(0, -deltaY);
                } else if (curScrollY - deltaY < 0) {
                    scrollBy(0, -curScrollY);
                    onInterceptTouchEvent(event);
                } else if (curScrollY - deltaY > mTopViewHeight) {
                    scrollBy(0, mTopViewHeight - curScrollY);
                    onInterceptTouchEvent(event);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (getYVelocity() < 0) {
                    smoothScrollTo(0, mTopViewHeight);
                } else if (getYVelocity() > 0) {
                    smoothScrollTo(0, 0);
                }
                recyclerVelocity();
                break;
            case MotionEvent.ACTION_CANCEL:
                recyclerVelocity();
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        mTopView.scrollTo(0, -getScrollY() / 2);
        Log.d(TAG,"top-->"+t);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    private void smoothScrollTo(int fx, int fy) {
        int dx = fx - getScrollX();
        int dy = fy - getScrollY();
        smoothScrollBy(dx, dy);
    }

    private void smoothScrollBy(int dx, int dy) {
        mScroller.startScroll(getScrollX(), getScrollY(), dx, dy, (int) (Math.abs(dy) * 1.5f));
        invalidate();
    }

}
