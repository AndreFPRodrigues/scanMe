package assistive.com.scanme;

import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by andre on 07-Aug-15.
 */
public class AccessibilityTree {

    // TODO hierarchical data structure
    private ArrayList<AccessibilityNodeInfoCompat> mTree;


    public AccessibilityTree(AccessibilityNodeInfoCompat root) {
        mTree = new ArrayList<>();
        buildTree(root);
    }

    private void buildTree(AccessibilityNodeInfoCompat node ) {
//        Log.d(ScanMe.TAG, "Father Node:" + node.getViewIdResourceName() + " " +node.getContentDescription() );
        if (isNavigableNode(node)) {
            mTree.add(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChild(i) != null) {

                if (node.getChild(i).getChildCount() > 0) {
                    buildTree(node.getChild(i));
                }else{
                    if (isNavigableNode(node.getChild(i))) {
                        mTree.add(node.getChild(i));
                    }else{
                        node.getChild(i).recycle();
                    }
                }
            }
        }


    }

    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if (other == null)
            return result;

        if (other instanceof AccessibilityTree) {
            AccessibilityTree tree = (AccessibilityTree) other;

            if (mTree.size() != tree.size())
                return false;

            for (int i = 0; i < mTree.size(); i++) {
//                Log.d(ScanMe.TAG, "View mTree:" + mTree.get(i).getViewIdResourceName() + " tree:" + tree.mTree.get(i).getViewIdResourceName());
//                Log.d(ScanMe.TAG, "View content:" + mTree.get(i).getContentDescription() + " tree:" + tree.mTree.get(i).getContentDescription());
//                Log.d(ScanMe.TAG, "View description:" + mTree.get(i).getText() + " tree:" + tree.mTree.get(i).getText());


                // are ids the same?
                result = (mTree.get(i).getViewIdResourceName() != null &&
                        mTree.get(i).getViewIdResourceName().equals(tree.mTree.get(i).getViewIdResourceName()))
                        || mTree.get(i).getViewIdResourceName() == tree.mTree.get(i).getViewIdResourceName();

                // are content descriptions the same?
                result = result && mTree.get(i).getContentDescription() != null &&
                        mTree.get(i).getContentDescription().equals(tree.mTree.get(i).getContentDescription())
                        || mTree.get(i).getContentDescription() == tree.mTree.get(i).getContentDescription();

                // are texts the same?
                //TODO in the future comparing node.text (for textviews) may not be needed
                result = result && mTree.get(i).getText() != null &&
                        mTree.get(i).getText().equals(tree.mTree.get(i).getText())
                        || mTree.get(i).getText() == tree.mTree.get(i).getText();


                // TODO careful, nodes might not be in the same order
                if (!result)
                    return result;
            }
        }
        return result;
    }

    public int size() {
        return mTree.size();
    }

    public AccessibilityNodeInfoCompat getNode(int index) {
        return mTree.get(index);
    }

    @Override
    public String toString() {
        String s="";
        for (int i =0; i<mTree.size();i++){
            Log.d(ScanMe.TAG, mTree.get(i) + "\n");

        }
        return s;
    }

    public boolean isNavigableNode(AccessibilityNodeInfoCompat node){

        return node.getText()!=null|| node.getContentDescription()!=null ||node.isClickable() ;
    }

    public void recycle(){
        for (int i =0;i < mTree.size();i++){
            mTree.get(i).recycle();
        }
        mTree.clear();
    }
}
