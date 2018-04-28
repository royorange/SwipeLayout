/**
 * Copyright 2017 Roy Shi
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.royorange.lib.swipeopetionlib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PointF;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

import java.util.ArrayList;

public class SwipeOptionLayout extends ViewGroup {
    private static final Interpolator defaultInterpolator = new FastOutLinearInInterpolator();
    private static final int SpeedSlop = 1000;
    private static final int MOVE_SLOP = 10;
    private static final int MOVE_OPPOSITE_DIRECTION_SLOP = 100;
    private static final int EDGE_WIDTH = 100;
    private int mOptionSize;
    private View mContentView;
    private int mHeight;
    private VelocityTracker mVelocityTracker;
    private boolean mIsHorizontal = true;
    private boolean isSwipeToLeft = true;
    private boolean isSwipeToTop = true;
    private boolean isSwipeAble = true;
    private final ArrayList<View> mMatchParentChildren = new ArrayList<>(1);
    private boolean isAutoRecovery = true;
    private PointF mInitPointer = new PointF();
    private PointF mLastPointer = new PointF();
    private SwipeListener mListener;
    private Interpolator mInterpolator = defaultInterpolator;
    private int mDuration = 300;
    private int mScrollSlop;
    private ValueAnimator mAnimator;
    private boolean mIsExpanded;
    private int mPointerId;
    private int mMaxVelocity;
    private boolean mIsMoving;
    private boolean mIsCancelOutside = true;

    public interface SwipeListener{
        void onSwipe(float percent);
        void onExpanded();
        void onCollapsed();
    }


    public SwipeOptionLayout(Context context) {
        super(context);
    }

    public SwipeOptionLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs,0);
    }

    public SwipeOptionLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs,defStyleAttr);
    }

    private void init(Context context,AttributeSet attrs,int defStyleAttr){
        mMaxVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
    }

    public SwipeListener getListener() {
        return mListener;
    }

    public void setListener(SwipeListener listener) {
        this.mListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setClickable(true);

        mOptionSize = 0;
        mHeight = 0;
        int contentWidth = 0;
        int childCount = getChildCount();

        final boolean measureMatchParentChildren = mIsHorizontal ?MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY:
                MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY;
        mMatchParentChildren.clear();
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            childView.setClickable(true);
            if (childView.getVisibility() != GONE) {
                measureChildWithMargins(childView, widthMeasureSpec, 0, heightMeasureSpec, 0);
                final LayoutParams lp = childView.getLayoutParams();
                mHeight = Math.max(mHeight, childView.getMeasuredHeight());
                if (measureMatchParentChildren) {
                    if(mIsHorizontal &&lp.height == LayoutParams.MATCH_PARENT||
                            !mIsHorizontal &&lp.width == LayoutParams.MATCH_PARENT){
                        mMatchParentChildren.add(childView);
                    }
                }
                if (i > 0) {//第一个布局是Left item，从第二个开始才是RightMenu
                    mOptionSize += childView.getMeasuredWidth();
                } else {
                    mContentView = childView;
                    contentWidth = childView.getMeasuredWidth();
                }
            }
        }
        setMeasuredDimension(getPaddingLeft() + getPaddingRight() + contentWidth,
                mHeight + getPaddingTop() + getPaddingBottom());
        //最大可滑动值
        mScrollSlop = mOptionSize /3;
        int count = mMatchParentChildren.size();
        if (count > 0) {
            for(int i=0;i<count;i++){
                final int childWidthMeasureSpec;
                final int childHeightMeasureSpec;
                final View child = mMatchParentChildren.get(i);
                LayoutParams lp =  child.getLayoutParams();
                if(mIsHorizontal){
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.width,
                            MeasureSpec.EXACTLY);
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(),
                            MeasureSpec.EXACTLY);
                }else {
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(),
                            MeasureSpec.EXACTLY);
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(lp.height,
                            MeasureSpec.EXACTLY);

                }
                measureChildWithMargins(child, childWidthMeasureSpec, 0, childHeightMeasureSpec, 0);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(mIsHorizontal){
            layoutChildrenHorizontal();
        }else {
            layoutChildrenVertically();
        }
    }

    private void layoutChildrenVertically(){
        int childCount = getChildCount();
        int top = getPaddingTop();
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            if (childView.getVisibility() != GONE) {
                //take the first view as the default content in the layout
                if (i == 0) {
                    childView.layout(getPaddingLeft(), top, getPaddingLeft() + childView.getMeasuredWidth(), getPaddingTop() + childView.getMeasuredHeight());
                    top += childView.getMeasuredHeight();
                } else {
                    if (isSwipeToTop) {
                        childView.layout(getPaddingLeft(), top , getPaddingLeft() + childView.getMeasuredWidth(), top + childView.getMeasuredHeight());
                        top += childView.getMeasuredHeight();
                    } else {
                        childView.layout(getPaddingLeft() , top - childView.getMeasuredHeight()
                                , getPaddingLeft() + childView.getMeasuredWidth(), top);
                        top = top - childView.getMeasuredHeight();
                    }

                }
            }
        }
    }

    private void layoutChildrenHorizontal(){
        int childCount = getChildCount();
        int left = getPaddingLeft();
        int right = getPaddingLeft();
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            if (childView.getVisibility() != GONE) {
                //take the first view as the default content in the layout
                if (i == 0) {
                    childView.layout(left, getPaddingTop(), left + childView.getMeasuredWidth(), getPaddingTop() + childView.getMeasuredHeight());
                    left = left + childView.getMeasuredWidth();
                } else {
                    if (isSwipeToLeft) {
                        childView.layout(left, getPaddingTop(), left + childView.getMeasuredWidth(), getPaddingTop() + childView.getMeasuredHeight());
                        left = left + childView.getMeasuredWidth();
                    } else {
                        childView.layout(right - childView.getMeasuredWidth(), getPaddingTop(), right, getPaddingTop() + childView.getMeasuredHeight());
                        right = right - childView.getMeasuredWidth();
                    }

                }
            }
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(!isSwipeAble ||!isEnabled()){
            return super.dispatchTouchEvent(ev);
        }
        if(mVelocityTracker == null){
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPointerId = ev.getPointerId(0);
                mInitPointer.set(ev.getRawX(), ev.getRawY());
                mLastPointer.set(ev.getRawX(), ev.getRawY());
                Log.i("shijc","ACTION_DOWN:"+ev.getRawX()+","+ev.getRawY());
                break;
            case MotionEvent.ACTION_MOVE:
                float distance = mIsHorizontal ?mLastPointer.x - ev.getRawX():mLastPointer.y - ev.getRawY();
                Log.i("shijc","ACTION_MOVE:"+ev.getRawX()+","+ev.getRawY()+"，distance:"+distance);
                if(!checkIsMoving(ev)){
                    mLastPointer.set(ev.getRawX(), ev.getRawY());
                    break;
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                if(mIsHorizontal){
                    scrollBy((int)distance, 0);
                    if (isSwipeToLeft) {
                        if (getScrollX() < 0) {
                            scrollTo(0, 0);
                            notifyCollapseCallback();
                        }else if (getScrollX() > mOptionSize) {
                            scrollTo(mOptionSize, 0);
                            notifyExpandCallback();
                        }else {
                            notifySwipingCallback();
                        }
                    } else {
                        if (getScrollX() < -mOptionSize) {
                            scrollTo(-mOptionSize, 0);
                            notifyExpandCallback();
                        }else if (getScrollX() > 0) {
                            scrollTo(0, 0);
                            notifyCollapseCallback();
                        }else {
                            notifySwipingCallback();
                        }
                    }
                }else {
                    scrollBy(0, (int)distance);
                    if(isSwipeToTop){
                        if(getScrollY() < 0){
                            scrollTo(0, 0);
                            notifyCollapseCallback();
                        }else if (getScrollY() > mOptionSize) {
                            scrollTo(0, mOptionSize);
                            notifyExpandCallback();
                        }else {
                            notifySwipingCallback();
                        }
                    }else {
                        if (getScrollY() < -mOptionSize) {
                            scrollTo(0, -mOptionSize);
                            notifyExpandCallback();
                        }else if (getScrollY() > 0) {
                            scrollTo(0, 0);
                            notifyCollapseCallback();
                        }else {
                            notifySwipingCallback();
                        }
                    }
                }
                mLastPointer.set(ev.getRawX(), ev.getRawY());
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(!mIsMoving){
                    releaseVelocityTracker();
                    if(mIsCancelOutside){
                        if(mIsHorizontal){
                            if(isSwipeToLeft){
                                if(ev.getRawX()<getWidth()-mOptionSize){
                                    collapse();
                                }
                            }else {
                                if(ev.getRawX()<getWidth()-mOptionSize){
                                    collapse();
                                }
                            }
                        }

                    }
                }else {
                    mVelocityTracker.computeCurrentVelocity(1000,mMaxVelocity);
                    final float velocity = mIsHorizontal?mVelocityTracker.getXVelocity(mPointerId):mVelocityTracker.getYVelocity(mPointerId);
                    Log.i("shijc","finish speed:"+velocity);
                    if(velocity == 0){
                        if(mIsHorizontal){
                            if(Math.abs(getScrollX())<mScrollSlop){
                                collapse();
                            }else {
                                expand();
                            }
                        }else {
                            if(Math.abs(getScrollY())<mScrollSlop){
                                collapse();
                            }else {
                                expand();
                            }
                        }
                        break;
                    }
                    if(Math.abs(velocity)>SpeedSlop){
                        //执行操作
                        if(velocity>0){
                            if(isSwipeToLeft||isSwipeToTop){
                                collapse();
                            }else {
                                expand();
                            }
                        }else {
                            if(isSwipeToLeft||isSwipeToTop){
                                expand();
                            }else {
                                collapse();
                            }
                        }
                    }else {
                        //取消操作
                        if(velocity>0){
                            if(isSwipeToLeft||isSwipeToTop){
                                expand();
                            }else {
                                collapse();
                            }
                        }else {
                            if(isSwipeToLeft||isSwipeToTop){
                                collapse();
                            }else {
                                expand();
                            }
                        }

                    }
                }
                mIsMoving = false;
                releaseVelocityTracker();
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(!isSwipeAble){
            return super.onInterceptTouchEvent(ev);
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if(mIsMoving){
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:

                mIsMoving = false;
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private void releaseVelocityTracker() {
        if (null != mVelocityTracker) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private boolean checkIsMoving(MotionEvent ev){
        if(!mIsMoving){
            //avoid touch by mistake
            if(mIsHorizontal){
                //如果向右滑动并且是左划状态，若在左侧边缘滑动时则认为是无效滑动
                if(isSwipeToLeft && ev.getRawX()>mInitPointer.x){
                    //is hit left edge
                    if(mInitPointer.x < EDGE_WIDTH){
                        return mIsMoving;
                    }
                }else if(!isSwipeToLeft&&ev.getRawX()< mInitPointer.x){
                    //is hit right edge
                    if(mInitPointer.x > getWidth() - EDGE_WIDTH){
                        return mIsMoving;
                    }
                }
                if(Math.abs(mLastPointer.x - mInitPointer.x)>MOVE_SLOP
                        &&Math.abs(mLastPointer.y - mInitPointer.y)<MOVE_OPPOSITE_DIRECTION_SLOP){
                    mIsMoving = true;
                }
            }else {
                if(isSwipeToTop && ev.getRawY()>mInitPointer.y){
                    //is hit top edge
                    if(mInitPointer.y < EDGE_WIDTH){
                        return mIsMoving;
                    }
                }else if(!isSwipeToTop&&ev.getRawY()<mInitPointer.y){
                    //is hit bottom edge
                    if(mInitPointer.y > getHeight() - EDGE_WIDTH){
                        return mIsMoving;
                    }
                }
                if(Math.abs(mLastPointer.y - mInitPointer.y)>MOVE_SLOP
                        &&Math.abs(mLastPointer.x - mInitPointer.x)<MOVE_OPPOSITE_DIRECTION_SLOP){
                    mIsMoving = true;
                }
            }
        }
        return mIsMoving;
    }

    public void expand(){
        expand(true);
    }

    public void expand(boolean isAnimate){
        cancelAnimate();
        if(isAnimate){
            if(mIsHorizontal){
                //已经展开
                if(Math.abs(getScrollX()) == mOptionSize){
                    mIsExpanded = true;
                    if(isAutoRecovery){
                        collapse();
                    }
                    return;
                }
                mAnimator = ValueAnimator.ofInt(getScrollX(),
                        isSwipeToLeft ?mOptionSize:-mOptionSize);
            }else {
                if(Math.abs(getScrollY()) == mOptionSize){
                    mIsExpanded = true;
                    if(isAutoRecovery){
                        collapse();
                    }
                    return;
                }
                mAnimator = ValueAnimator.ofInt(getScrollY(),
                        isSwipeToTop ?mOptionSize:-mOptionSize);
            }
            mAnimator.setDuration(mDuration)
                    .setInterpolator(mInterpolator);
            mAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mIsExpanded = true;
                    notifyExpandCallback();
                    if(isAutoRecovery){
                        collapse();
                    }
                }
            });
            mAnimator.addUpdateListener(animation -> {
                if(mIsHorizontal){
                    scrollTo((Integer)(animation.getAnimatedValue()),0);
                }else {
                    scrollTo(0,(Integer)(animation.getAnimatedValue()));
                }
                notifySwipingCallback();
            });
            mAnimator.start();
        }else {
            if(mIsHorizontal){
                if (isSwipeToLeft) {
                    scrollTo(mOptionSize,0);
                }else {
                    scrollTo(-mOptionSize,0);
                }
            }else {
                if(isSwipeToTop){
                    scrollTo(0,mOptionSize);
                }else {
                    scrollTo(0,-mOptionSize);
                }
            }
            mIsExpanded = true;
            notifyExpandCallback();
        }
    }

    public void collapse(){
        collapse(true);
    }

    public void collapse(boolean isAnimate){
        cancelAnimate();
        if(isAnimate){
            mAnimator = ValueAnimator.ofInt(mIsHorizontal?getScrollX():getScrollY(),0);
            mAnimator.setDuration(mDuration)
                    .setInterpolator(mInterpolator);
            mAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mIsExpanded = false;
                    notifyCollapseCallback();
                }
            });
            mAnimator.addUpdateListener(animation -> {
                if(mIsHorizontal){
                    scrollTo((Integer)(animation.getAnimatedValue()),0);
                }else {
                    scrollTo(0,(Integer)(animation.getAnimatedValue()));
                }
                notifySwipingCallback();
            });
            mAnimator.start();
        }else {
            scrollTo(0,0);
            mIsExpanded = false;
            notifyCollapseCallback();
        }
    }

    public void cancelAnimate(){
        if(mAnimator!=null&&mAnimator.isRunning()){
            mAnimator.cancel();
        }
    }

    private void notifyExpandCallback(){
        if(mListener!=null){
            mListener.onExpanded();
        }
    }

    private void notifyCollapseCallback(){
        if(mListener!=null){
            mListener.onCollapsed();
        }
    }

    private void notifySwipingCallback(){
        if(mListener!=null){
            float percent = (mIsHorizontal?Math.abs(getScrollX()):Math.abs(getScrollY()))/(mOptionSize*1f);
            mListener.onSwipe(percent);
        }
    }

    public View getContentView(){
        return mContentView;
    }

    public boolean isExpand(){
        return mIsExpanded;
    }
}
