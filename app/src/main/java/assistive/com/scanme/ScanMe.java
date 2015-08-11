package assistive.com.scanme;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;

import android.os.Handler;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import assistive.com.scanme.com.googlecode.eyesfree.utils.AccessibilityNodeInfoUtils;

/**
 * Created by andre on 30-Jul-15.
 */
public class ScanMe extends AccessibilityService {
    public static String TAG = "ScanMe";

    private Context mService;

    // navigation variables
    private AccessibilityTree mTree;
    private int mCurrentIndex = 0;
    private long mLastTreeUpdate = 0;
    private Handler mHandler;

    // overlay - focus
    private Handler mHightlightHandler;
    private WindowManager mWindowManager;
    private RelativeLayout mOverlay;
    private WindowManager.LayoutParams mHighlightParams;
    private boolean mScrolled = false;

    private Runnable mAutoNavigate = new Runnable() {
        @Override
        public void run() {

            if (mCurrentIndex < mTree.size()) {
                AccessibilityNodeInfoCompat node = mTree.getNode(mCurrentIndex);

                if (!(mScrolled = performScroll(node))) {
                    // no need for scroll, continue navigation to next item

                    Log.d(TAG, "Text:" + node.getText() + " content:" + node.getContentDescription());

                    // draw overlay / highlight
                    Rect bounds = new Rect();
                    node.getBoundsInScreen(bounds);
                    addHighlight(bounds.top, bounds.left, (float) 0.6, bounds.width(), bounds.height(), Color.CYAN);

                    mCurrentIndex++;

                    startAutoNavigation();
                } else {
                    // we remove overlay and wait for scroll event update
                    mOverlay.removeAllViews();
                }
            }
        }
    };

    /**
     * Extract the child nodes from the given root and adds them to the supplied
     * list of nodes.
     *
     * @param root  The root node.
     * @param nodes The list of child nodes.
     * @return The mask of supported all granularities supported by the root and
     * child nodes.
     */
    private static int extractNavigableNodes(Context context, AccessibilityNodeInfoCompat root,
                                             ArrayList<AccessibilityNodeInfoCompat> nodes) {
        if (root == null) {
            return 0;
        }

        if (nodes != null) {
            AccessibilityNodeInfoCompat ac = AccessibilityNodeInfoCompat.obtain(root);
            Log.d(TAG, "text:" + ac.getText() + " content:" + ac.getContentDescription() + " view:" + ac.getViewIdResourceName());
            nodes.add(ac);
        }

        int supportedGranularities = root.getMovementGranularities();

        // Don pull children from nodes with content descriptions.
        if (!TextUtils.isEmpty(root.getContentDescription())) {
            return supportedGranularities;
        }

        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final AccessibilityNodeInfoCompat child = root.getChild(i);
            if (child == null) {
                continue;
            }

            child.performAction(AccessibilityNodeInfoCompat.ACTION_SET_SELECTION, null);

            // Only extract nodes that aren't reachable by traversal.
            if (!AccessibilityNodeInfoUtils.shouldFocusNode(context, child)) {
                supportedGranularities |= extractNavigableNodes(context, child, nodes);
            }

            child.recycle();
        }

        return supportedGranularities;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, AccessibilityEvent.eventTypeToString(event.getEventType()));

        // check whether event is newer
        if (event.getEventTime() < mLastTreeUpdate) {
            return;
        }


        // get root
        AccessibilityNodeInfo root = getRootInActiveWindow();//getRoot(source);
        if (root == null) {
            Log.d(TAG, "Root null");
            return;
        }

        // update current tree
        if (updateTree(new AccessibilityTree(new AccessibilityNodeInfoCompat(root)))) {

            // stop current navigation
            if (mHandler != null)
                mHandler.removeCallbacks(mAutoNavigate);

            // if new tree, start navigation
            Log.d(TAG, "-----------------------------------------------------");
            Log.d(TAG, "Updated tree, start auto navigation");
            Log.d(TAG, "Size:" + mTree.size());
            Log.d(TAG, mTree.toString());
            Log.d(TAG, "-----------------------------------------------------");


            mCurrentIndex = 0;
            startAutoNavigation();
        } else if (mScrolled) { // we were waiting for this event to re-start navigation with updated tree

            // stop current navigation
            if (mHandler != null)
                mHandler.removeCallbacks(mAutoNavigate);

            startAutoNavigation();
        }
        root.recycle();

    }

    @Override
    public void onInterrupt() {
        mOverlay.removeAllViews();
        mWindowManager.removeView(mOverlay);
    }

    @Override
    public void onDestroy() {
        mOverlay.removeAllViews();
        mWindowManager.removeView(mOverlay);
        super.onDestroy();
    }


    @Override
    public void onServiceConnected() {
        mService = this;

        // configure overlay
        mHightlightHandler = new Handler();
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mHighlightParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.FILL_PARENT,
                WindowManager.LayoutParams.FILL_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        mOverlay = new RelativeLayout(this);
        mWindowManager.addView(mOverlay, mHighlightParams);
    }

    private synchronized boolean updateTree(AccessibilityTree accessibilityTree) {
        if (!accessibilityTree.equals(mTree)) {
//            if (mTree != null)
//                mTree.recycle();
            mTree = accessibilityTree;
            return true;
        } else {
//            if (mTree != null)
//                mTree.recycle();
            mTree = accessibilityTree;
            return false;
        }


    }

    private void startAutoNavigation() {
        mHandler = new Handler();
        mHandler.postDelayed(mAutoNavigate, 800);
    }


    public void addHighlight(final int marginTop, final int marginLeft,
                             final float alpha, final int width, final int height,
                             final int color) {
        // Log.d(LT, "ADD OVERLAY");

        mHightlightHandler.post(new Runnable() {
            public synchronized void run() {

                // clear last highlight
                if (mOverlay != null) {
                    mOverlay.removeAllViews();
                }

                // draw focus
                ImageView tv2 = new ImageView(mService);
                tv2.setBackgroundColor(color);
                tv2.setAlpha(alpha);
                RelativeLayout.LayoutParams parms1 = new RelativeLayout.LayoutParams(0, 0);
                parms1.height = height;
                parms1.width = width;
                parms1.leftMargin = marginLeft;
                parms1.topMargin = marginTop - getStatusBarHeight();
                tv2.setLayoutParams(parms1);

                // add to view
                if (mOverlay != null) {
                    mOverlay.addView(tv2, parms1);
                }

                // refresh view
                mWindowManager.updateViewLayout(mOverlay, mHighlightParams);
            }
        });
    }

    // set focus in node
    private boolean performScroll(AccessibilityNodeInfoCompat node) {
        if (!node.isVisibleToUser()) {
            Log.d(TAG, "Not visible");

            // find scroll direction
            int action = getScrollDirection(node);

            // get scrollable node
            AccessibilityNodeInfoCompat scrollableNode = getScrollable(node);

            // perform scrollable action
            return scrollableNode.performAction(action);
        }
        return false;
    }

    private int getScrollDirection(AccessibilityNodeInfoCompat node) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.centerX() < 0 || bounds.centerY() < 0) {
            Log.v(TAG, "Scroll backwards");
            return AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD;
        } else {
            Log.v(TAG, "Scroll forward");
            return AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD;
        }
    }

    private AccessibilityNodeInfoCompat getScrollable(AccessibilityNodeInfoCompat node) {
        AccessibilityNodeInfoCompat result = node;
        while (result != null && !result.isScrollable())
            result = node.getParent();
        return result;
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

}
