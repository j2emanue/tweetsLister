package org.jefferyemanuel.willowtweetapp;



import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.LruCache;

/*a RETAINED fragment used to keep a reference to the LruCache map.  In an orientation change event this fragment will
 * retain its instance and well be able to get back the LruCache map thus preserving our cache on Activity changes */

class RetainFragment extends Fragment {
    private static final String TAG = "RetainFragment";
    public static  LruCache<String, Bitmap> mRetainedCache;

    public RetainFragment() {}

    public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
        RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
        if (fragment == null) {
            fragment = new RetainFragment();
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);/*---> force fragment to retain itself on activity changes/events. making our LRU
        cache more persistent across orientation changes for example*/
    }
}

