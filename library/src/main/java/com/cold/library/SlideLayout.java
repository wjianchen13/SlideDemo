package com.cold.library;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.customview.widget.ViewDragHelper;

import java.lang.reflect.Field;

/**
 * name: SlideLayout
 * desc: 侧滑菜单
 * author:
 * date: 2019-09-04 15:10
 * remark:
 */
public class SlideLayout extends FrameLayout implements View.OnTouchListener {

    private static final String TAG = SlideLayout.class.getName();

    /**
     * 滑动速率，每秒dip
     */
    private static final int SLIDING_VELOCITY = 600;

    /**
     * debug
     */
    private boolean isDebug = true;

    /**
     * 最底层视图
     */
    private View mBaseView;

    /**
     * 清屏视图
     */
    private View mDrawerView;

    /**
     * 菜单视图
     */
    private View mMenuView;

    /**
     *
     */
    private Context mContext;

    /**
     * 抽屉视图是否可见
     */
    private boolean flag = true;

    /**
     * 页面状态 1:侧边栏可见，清屏可见 2:侧边栏不可见 清屏可见 3:侧边栏和清屏界面不可见，初始状态是都可见
     */
//    private int state = 1;

    /**
     * ViewDragHelper
     */
    private ViewDragHelper mHelper;

    /**
     * drawer显示出来的占自身的百分比
     */
    private float mLeftMenuOnScrren;

    /**
     * slideview 移动百分比
     */
    private float mSlideViewOnScreen;

    /**
     * 控制的view
     */
    private View controlView;

    /**
     * 是否计算完成
     */
    private boolean isCalculate = false;

    /**
     * 当前State
     */
    private int mDragState;

    /**
     * 侧边栏不随手势滑动
     */
    private int newLeft;

    /**
     * 侧边栏是否随手势滑动
     */
    private boolean isMenuGesture;

    /**
     * 进度监听
     */
    private ISlideListener slideListener;

    /**
     * 设置进度监听
     * @param
     * @return void
     */
    public void setSlideListener(ISlideListener slideListener) {
        this.slideListener = slideListener;
    }

    /**
     * ViewDragHelper回调接口
     */
    ViewDragHelper.Callback cb = new ViewDragHelper.Callback() {
        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            log("clampViewPositionHorizontal");
            int drawerLeft = Math.min( Math.max(getWidth() - child.getWidth(), left), getWidth());
            if(!isMenuGesture && child == mMenuView) {
                log("clampViewPositionHorizontal newLeft: " + newLeft);
                return newLeft;
            }
            return drawerLeft;
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            log("tryCaptureView");
            return false;
        }

        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            log("onEdgeDragStarted");
            if(mDragState == ViewDragHelper.STATE_IDLE)
                mHelper.captureChildView(controlView, pointerId);
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            log("onViewPositionChanged");
            final int childWidth = changedView.getWidth();
//            float offset = (left - (getWidth() - childWidth))* 1.0f / childWidth;
            float offset = (float) left / childWidth;
            if(slideListener != null) {
                slideListener.onPositionChanged(offset);
            }
            if(changedView == mDrawerView) {
                mLeftMenuOnScrren = offset;
            } else if(changedView == mMenuView){
                mSlideViewOnScreen = offset;
            }
            changedView.setVisibility(offset == 1 ? View.INVISIBLE : View.VISIBLE);
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            log("onViewReleased");
            final int childWidth = releasedChild.getWidth();
            float offset = (float)(releasedChild.getLeft() - (getWidth() - childWidth)) / (float)childWidth;
            if(releasedChild == mDrawerView) {
                mLeftMenuOnScrren = offset;
            } else if(releasedChild == mMenuView){
                mSlideViewOnScreen = offset;
            }
            if(xvel < 0 || xvel == 0 && offset < 0.5f ) { // 目标向左移动
                if(releasedChild == mDrawerView) {
//                    state = 2;
                } else if(releasedChild == mMenuView){
//                    state = 1;
                    newLeft = getWidth() - childWidth;
                }
                flag = true;
                mHelper.settleCapturedViewAt(getWidth() - childWidth, releasedChild.getTop());
            } else { // 目标向右移动
                if(releasedChild == mDrawerView) {
//                    state = 3;
                } else if(releasedChild == mMenuView){
                    newLeft = getWidth();
//                    state = 2;
                }
                mHelper.settleCapturedViewAt(getWidth(), releasedChild.getTop());
            }
//            mHelper.settleCapturedViewAt(flag ? getWidth() - childWidth : getWidth(), releasedChild.getTop());
            invalidate();
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            log("getViewHorizontalDragRange");
            return controlView == child ? child.getWidth() : 0;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            log("onViewDragStateChanged state: " +  state);
            mDragState = state;
        }
    };

    public SlideLayout(Context context, AttributeSet attrs)  {
        super(context, attrs);
        this.mContext = context;

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.slidelayout);
        isMenuGesture = ta.getBoolean(R.styleable.slidelayout_menu_gesture, true);
        ta.recycle();

        final float density = getResources().getDisplayMetrics().density;
        final float minVel = SLIDING_VELOCITY * density;

        mHelper = ViewDragHelper.create(this, 1.0f, cb);
        mHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_RIGHT);
        mHelper.setMinVelocity(minVel);

        setDrawerLeftEdgeSize(context, mHelper, 1.0f);
        setOnTouchListener(this);

    }

    /**
     * 设置滑动范围
     * @param: activity
     * @param: dragHelper 设置范围的ViewDragHelper
     * @param: displayWidthPercentage 滑动因子，为 1 全屏滑动
     * @return: void
     */
    public void setDrawerLeftEdgeSize(Context activity, ViewDragHelper dragHelper, float displayWidthPercentage) {
        Field edgeSizeField;
        try {
            edgeSizeField = dragHelper.getClass().getDeclaredField("mEdgeSize");
            edgeSizeField.setAccessible(true);
            DisplayMetrics dm = new DisplayMetrics();
            ((Activity)activity).getWindowManager().getDefaultDisplay().getMetrics(dm);
            edgeSizeField.setInt(dragHelper, (int) (dm.widthPixels * displayWidthPercentage));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed,l, t, r,b);
        MarginLayoutParams lp = (MarginLayoutParams) mDrawerView.getLayoutParams();
        final int menuWidth = mDrawerView.getMeasuredWidth();
        int childLeft = (getWidth() - menuWidth) + (int)(menuWidth * mLeftMenuOnScrren);
        mDrawerView.layout(childLeft, lp.topMargin, childLeft + menuWidth, lp.topMargin + mDrawerView.getMeasuredHeight());

        MarginLayoutParams lp1 = (MarginLayoutParams) mMenuView.getLayoutParams();
        final int menuWidth1 = mMenuView.getMeasuredWidth();
        int childLeft1 = (getWidth() - menuWidth1) + (int)(menuWidth1 * mSlideViewOnScreen);
        mMenuView.layout(childLeft1, lp1.topMargin, childLeft1 + menuWidth1, lp1.topMargin + mMenuView.getMeasuredHeight());

        newLeft = mMenuView.getLeft();
    }

    /**
     * 获取容器内的控件引用
     * @param:
     * @return:
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int count =  getChildCount();
        log("count: " + count);
        if(count != 3){
            throw new IllegalArgumentException("child view count must equal 3");
        }

        if(count == 3) {
            mBaseView = getChildAt(0);
            mDrawerView = getChildAt(1);
            mMenuView = getChildAt(2);
        }
        controlView = mMenuView;
    }

    /**
     * 按下点X坐标
     */
    private float startX;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                mHelper.shouldInterceptTouchEvent(ev);
                return false;
            case MotionEvent.ACTION_MOVE:
//                float endX = ev.getX();
//                float distanceX = endX - startX;
//                if(mMenuView.getVisibility() == View.VISIBLE) {
//                    return false;
//                } else if(mMenuView.getVisibility() == View.GONE) {
//                    if ((flag && distanceX > 0 && isGreaterThanMinSize(endX, startX)) || (!flag && distanceX < 0 && isGreaterThanMinSize(endX, startX))) {
//                        return mHelper.shouldInterceptTouchEvent(ev);
//                    } else if ((flag && distanceX < 0) || (!flag && distanceX > 0)) {
//                        return false;
//                    }
//                }
                break;
            case MotionEvent.ACTION_UP:
                return mHelper.shouldInterceptTouchEvent(ev);
            default:
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * 滑动范围阈值，可以修改布局上存在滑动控件时的移动范围
     * @param:
     * @return:
     */
    public boolean isGreaterThanMinSize(float x1, float x2) {
        return Math.abs((int)(x1 - x2)) > 66;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                mHelper.processTouchEvent(ev);
                isCalculate = false;//计算开始
                break;
            case MotionEvent.ACTION_MOVE:
                float endX = ev.getX();
                float distanceX = endX - startX;
                if(!isCalculate) {
                    if ((mMenuView.getLeft() >= getWidth() - mMenuView.getWidth()) && (mMenuView.getLeft() <  getWidth() - mMenuView.getWidth() / 2)) { // 侧边栏显示
                        if(distanceX > 0) { // 向右滑动
                            controlView = mMenuView;
                            isCalculate = true;
                        } else { // 向左滑动，不叼他
                        }
                    } else if ((mMenuView.getLeft() >= getWidth() - mMenuView.getWidth() / 2) && (mDrawerView.getLeft() < getWidth() - mDrawerView.getWidth() / 2)) { // 侧边栏隐藏，清屏界面显示
                        if(distanceX > 0) { // 向右滑动，移动drawerview
                            controlView = mDrawerView;
                            isCalculate = true;
                        } else if(distanceX < 0) {// 向左滑动，移动slideview
                            controlView = mMenuView;
                            isCalculate = true;
                        }
                    } else if (mDrawerView.getLeft() >= getWidth() - mDrawerView.getWidth() / 2) { // 清屏界面隐藏
                        if(distanceX < 0) { // 移动移动drawerview
                            controlView = mDrawerView;
                            isCalculate = true;
                        } else { // 不管
                        }
                    }
                }
                if(isCalculate) {
                    mHelper.processTouchEvent(ev);
                }

                break;
            case MotionEvent.ACTION_UP:
                mHelper.processTouchEvent(ev);
                break;
            default:
                mHelper.processTouchEvent(ev);
                break;
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (mHelper.continueSettling(true)) {
            invalidate();
        }
    }

    /**
     * 恢复清屏内容
     * @param
     * @return
     */
    public void restoreContent() {
        if(mDrawerView != null) {
            mLeftMenuOnScrren = 0.0f;
            flag = true;
            mHelper.smoothSlideViewTo(mDrawerView, 0, mDrawerView.getTop());
            invalidate();
        }
    }

    /**
     * 关闭drawer，预留
     * @param:
     * @return: void
     */
    public void closeDrawer() {
        View menuView = mDrawerView;
        mHelper.smoothSlideViewTo(menuView, -menuView.getWidth(), menuView.getTop());
    }

    /**
     * 打开drawer，预留
     * @param:
     * @return: void
     */
    public void openDrawer() {
        View menuView = mDrawerView;
        mHelper.smoothSlideViewTo(menuView, 0, menuView.getTop());
    }

    /**
     * 关闭drawer，预留
     * @param:
     * @return: void
     */
    public void closeSlideMenu() {
        View menuView = mMenuView;
        mSlideViewOnScreen = 1.0f;
        newLeft = getWidth();
        mHelper.smoothSlideViewTo(menuView, getWidth(), menuView.getTop());
        invalidate();
    }

    /**
     * 打开drawer，预留
     * @param:
     * @return: void
     */
    public void openSlideMenu() {
        View menuView = mMenuView;
        mSlideViewOnScreen = 0.0f;
        newLeft = getWidth() - mMenuView.getWidth();
        mHelper.smoothSlideViewTo(menuView, getWidth() - mMenuView.getWidth(), menuView.getTop());
        invalidate();
    }

    /**
     * 判断侧滑菜单是否打开
     * @param:
     * @return:
     */
    public boolean isSlideMenuOpen() {
        return mMenuView.getLeft() >= getWidth() - mMenuView.getWidth() && mMenuView.getLeft() <  getWidth() - mMenuView.getWidth() / 2;
    }

    /**
     * 以下4个参数用来记录按下点的初始坐标和结束坐标
     */
    private float firstDownX = 0;
    private float lastDownX = 0;
    private float firstDownY = 0;
    private float lastDownY = 0;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                firstDownX = event.getX();
                firstDownY = event.getY();
                lastDownX = event.getX();
                lastDownY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                lastDownX = event.getX();
                lastDownY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                lastDownX = event.getX();
                lastDownY = event.getY();
                float deltaX = Math.abs(lastDownX - firstDownX);
                float deltaY = Math.abs(lastDownY - firstDownY);
                if (deltaX < 10 && deltaY < 10) {
                    if(isTouchCloseSlideArea(lastDownX, lastDownY)) {
                        if(isSlideMenuOpen()){
                            closeSlideMenu();
                            return true;
                        }
                    }
                }
                break;
        }
        return false;
    }

    /**
     * 判断是否点击在了关闭侧边栏的区域
     * @param: x 按下点x坐标
     * @param: y 按下点y坐标
     * @return: true：关闭 false：不关闭
     */
    private boolean isTouchCloseSlideArea(float x, float y) {
        int left = 0;
        int top = 0;
        int right = getWidth() - mMenuView.getWidth();
        int bottom = mMenuView.getHeight();
        if (x >= left && x <= right && y >= top && y <= bottom) {
            return true;
        }
        return false;
    }


    private void log(String msg) {
        if(isDebug)
            Log.e(TAG, "===================================> " + msg);
    }
}