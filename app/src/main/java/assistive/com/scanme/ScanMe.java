package assistive.com.scanme;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;

import android.os.Handler;

import assistive.com.scanme.com.googlecode.eyesfree.utils.AccessibilityNodeInfoUtils;

/**
 * Created by andre on 30-Jul-15.
 */
public class ScanMe extends AccessibilityService {
    /**
     * Represents navigation to next element.
     */
    private static final int NAVIGATION_DIRECTION_NEXT = 1;
    /**
     * Represents navigation to previous element.
     */
    private static final int NAVIGATION_DIRECTION_PREVIOUS = -1;
    private static String TAG = "ScanMe";
    private static boolean navigating = false;
    // Parent node to the current screen content
    AccessibilityNodeInfo currentParent = null;
    private  int index = 0;
    private  ArrayList<AccessibilityNodeInfoCompat> mNavigableNodes = new ArrayList<AccessibilityNodeInfoCompat>();
    private Handler handler;
    private  AccessibilityService mService;
    private Runnable run = new Runnable() {
        @Override
        public void run() {

            if (index < mNavigableNodes.size()) {

                //TODO if result false no DELAY
                boolean result = mNavigableNodes.get(index).performAction(AccessibilityNodeInfoCompat.ACTION_FOCUS);
            //    Log.d(TAG, "ViewResourceName:" + mNavigableNodes.get(index).getViewIdResourceName());
                Log.d(TAG, "Text:" + mNavigableNodes.get(index).getText());
               // Log.d(TAG, "View:" + mNavigableNodes.get(index).getWindowId());
               // Log.d(TAG, "content:" + mNavigableNodes.get(index).getContentDescription());
                Log.d(TAG, "index:" + index + " size:"  +mNavigableNodes.size());

                index++;

                scrollNodes();
            }
        }
    };



    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        mService=this;
        if (handler != null)
            handler.removeCallbacks(run);

        AccessibilityNodeInfo source = event.getSource();
        if (source == null) {
            return;
        }
        source = getRootInActiveWindow();

        if (source == null) {
            return;
        }
        Log.d(TAG, AccessibilityEvent.eventTypeToString(event.getEventType()));

        currentParent = source;
        index=0;
        Log.d(TAG, "-----------------------------------------------------index:" + index);

        mNavigableNodes = new ArrayList<AccessibilityNodeInfoCompat>();
        extractNavigableNodes(mService, getCursor(), mNavigableNodes);

        Log.d(TAG, "-----------------------------------------------------SIZE:" + mNavigableNodes.size());
        scrollNodes();

       /*
        Log.d(TAG, "Navigate:" + navigate(AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, getCursor()));
    */
    }

    private void scrollNodes() {
        handler = new Handler();
        handler.postDelayed(run, 800);


    }

    @Override
    public void onInterrupt() {

    }

    /**
     * Returns the node in the active window that has accessibility focus. If no
     * node has focus, or if the focused node is invisible, returns the root
     * node.
     * <p/>
     * The client is responsible for recycling the resulting node.
     *
     * @return The node in the active window that has accessibility focus.
     */
    public AccessibilityNodeInfoCompat getCursor() {
        final AccessibilityNodeInfo activeRoot = getRootInActiveWindow();
        if (activeRoot == null) {
            return null;
        }

        final AccessibilityNodeInfoCompat compatRoot = new AccessibilityNodeInfoCompat(activeRoot);
       return compatRoot;

       /* final AccessibilityNodeInfoCompat focusedNode = compatRoot.findFocus(
                AccessibilityNodeInfoCompat.FOCUS_ACCESSIBILITY);

        // TODO: If there's no focused node, we should either mimic following
        // focus from new window or try to be smart for things like list views.
        if (focusedNode == null) {
            return compatRoot;
        }

        // TODO: We should treat non-focusable nodes (e.g. invisible or fail the
        // heuristic we're using elsewhere) then we should try to find a
        // focusable node.
        if (focusedNode.isVisibleToUser()) {
            focusedNode.recycle();
            return compatRoot;
        }

        return focusedNode;*/
    }

    /**
     * Get root parent from node source
     *
     * @param source
     * @return
     */
    private AccessibilityNodeInfo getRootParent(AccessibilityNodeInfo source) {
        AccessibilityNodeInfo current = source;
        while (current.getParent() != null) {
            AccessibilityNodeInfo oldCurrent = current;
            current = current.getParent();
            oldCurrent.recycle();
        }
        return current;
    }
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
            AccessibilityNodeInfoCompat ac =AccessibilityNodeInfoCompat.obtain(root);
            Log.d(TAG, "text:" +ac.getText() + " content:" + ac.getContentDescription() + " view:" +ac.getViewIdResourceName());
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

}
