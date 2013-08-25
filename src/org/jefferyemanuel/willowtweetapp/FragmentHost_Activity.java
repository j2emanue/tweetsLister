package org.jefferyemanuel.willowtweetapp;

import static org.jefferyemanuel.willowtweetapp.Utils.createToast;
import static org.jefferyemanuel.willowtweetapp.Utils.printLog;

import java.util.ArrayList;
import java.util.HashMap;

import org.jefferyemanuel.listeners.TweeterListObserver;
import org.jefferyemanuel.listeners.TweeterSelectedListener;

import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;

/* Host activity for all fragments.  in onCreate we check if we are in two pane or single and attach fragments accordingly
 * this optimises tablet layouts.  on a two pane mode both modify list and tweeter list are shown on the same screen
 * and user can select from the list to move the viewpager to another tweeter.
 * Each fragment is loosely coupled with each other.  The communication occurs through interfaces found in "Listeners" package
 * we could have also communicated with broadcast receivers.  Each fragment communicates with the fragmentHost_Activity
 * which in turn sends a message to the respective fragment through an interface.  
 * This is also true for the host sending a message to the fragment. 
 */
/*TODO make each fragment have a seperate asynchTask instead of 
 * trying to get all tweeters at the same time. This can especially work since we are using a 
 * FragmentStatePagerAdapter which loads 3 fragments into memory at once.  Also,when user deletes from the list only, 
 * there should not be a network call, just delete the item and notify the adapter of data set changed
 
 *UPDATE: FINALLY MADE THE DESIGN WHERE EACH LIST FRAGMENT INSIDE OF THE VIEWPAGER LOADS ITS OWN TASKFRAGMENT AND THUS
 *ASYNCHTASK TO RETRIEVE THE DATA FROM TWITTER.  THIS IS HIGHLY EFFICIENTLY NETWORK CALLS AND SPAWN ONLY WHEN USER REQUIRES
 *THE VIEW
 */

public class FragmentHost_Activity extends FragmentActivity implements

 TweeterSelectedListener {

	private TweeterListObserver mFragmentListCallback;
	private FragmentManager fm;
	private ViewGroup mModiftyTweetersLayout, mTweetslistLayout;
	private boolean isEditing, isFromSavedState, mMultiColumn;
	private ArrayList<ArrayList<HashMap<String, String>>> tweeters;
	private int restoredIndex = 0;
	public static DiskLruImageCache imageDiskCache;
	
	/* make any fragment method calls after the fragments have been resumed */
	@Override
	protected void onResumeFragments() {
		// TODO Auto-generated method stub
		super.onResumeFragments();

		if (isFromSavedState) {
			mFragmentListCallback.changePage(restoredIndex);
			isFromSavedState = false;
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();

		/*
		 * if(Consts.DEVELOPER_MODE) Debug.stopMethodTracing();
		 */
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		printLog(Consts.TAG, "onCreate: savedInstanceState "
				+ (savedInstanceState == null ? "==" : "!=") + " null");

		// This has to be called before setContentView and you must use the 
		// class in android.support.v4.view and NOT android.view

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.activity_main);

		/*
		 * if(Consts.DEVELOPER_MODE) Debug.startMethodTracing();
		 */
		fm = getSupportFragmentManager();
	
		if(Consts.DEVELOPER_MODE)
			FragmentManager.enableDebugLogging(true);
		
		
		/* CREATE OUR DISK CACHE TO STORE IMAGES */
		if (Utils.isExternalStorageAvailable())
			imageDiskCache = new DiskLruImageCache(this, "diskcache",
					Consts.DISK_CACHE_SIZE, CompressFormat.PNG,
					Consts.COMPRESS_QUALITY);
		
		// Restore state

		if (savedInstanceState != null) {
			// The fragment manager will handle restoring them if we are being
			// restored from a saved state
			/* restore our call backs after config change */
			isFromSavedState = true;
			/* restore our call backs after config change */
			mFragmentListCallback = (TweeterListFragment) fm
					.findFragmentByTag(TweeterListFragment.class.getName());

				/* on orientation change restore the data */
			tweeters = (ArrayList<ArrayList<HashMap<String, String>>>) savedInstanceState
					.getSerializable(Consts.SAVED_INSTANCE_TWEETERS);
			restoredIndex = savedInstanceState
					.getInt(Consts.SAVED_INSTANCE_POSITION);

			isEditing = savedInstanceState.getBoolean(Consts.SAVED_INSTANCE_EDIT_MODE);
			
			
		}
		//If this is the first creation of the activity, add fragments to it
		else {

			/*
			 * If our layout has a container for the modifytweeters fragment,
			 * create and add it. btw if this exist, we know we are in two pane
			 * mode
			 */
			mModiftyTweetersLayout = (ViewGroup) findViewById(R.id.activity_modifytweeters_container);
			if (mModiftyTweetersLayout != null) {
				printLog(Consts.TAG,
						"onCreate: adding modifytweeters Fragment to MainActivity");

				// Add modify tweeters fragment to the activity's container layout
				ModifyTweetersFragment modifyTweetersFragment = new ModifyTweetersFragment();
				FragmentTransaction fragmentTransaction = getSupportFragmentManager()
						.beginTransaction();
				fragmentTransaction.replace(mModiftyTweetersLayout.getId(),
						modifyTweetersFragment,
						ModifyTweetersFragment.class.getName());
				
				// Commit the transaction
				fragmentTransaction.commit();
			}

			// If our layout has a container for the tweetslist fragment, create
			// it and add it
			mTweetslistLayout = (ViewGroup) findViewById(R.id.activity_tweetslist_container);
			if (mTweetslistLayout != null) {
				printLog(Consts.TAG,
						"onCreate: adding tweetslist Fragment to MainActivity");

				// Add tweeter list fragment to the activity's container layout
				TweeterListFragment tweeterListFragment = new TweeterListFragment();
				FragmentTransaction fragmentTransaction = getSupportFragmentManager()
						.beginTransaction();
				fragmentTransaction.replace(mTweetslistLayout.getId(),
						tweeterListFragment,
						TweeterListFragment.class.getName());

				mFragmentListCallback = (TweeterListObserver) tweeterListFragment;

				// Commit the transaction
				fragmentTransaction.commit();
			}
		}

	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		/*
		 * on any configuration change such as orientation, lets save all of our
		 * tweeters instead of making another inefficient network call
		 */
		state.putSerializable(Consts.SAVED_INSTANCE_TWEETERS, tweeters);
		state.putBoolean(Consts.SAVED_INSTANCE_EDIT_MODE, isEditing);
		/*
		 * get the current page from the tweetlist fragment and save it
		 * onConfiguration change
		 */
		TweeterListFragment fragment = (TweeterListFragment) fm
				.findFragmentByTag(TweeterListFragment.class.getName());
		if (fragment != null) {
			state.putInt(Consts.SAVED_INSTANCE_POSITION,
					fragment.getcurrentPage());
		}

	}

	/* if modify list exist we are in two pane mode clearly */
	private boolean isTwoPane() {
		return findViewById(R.id.activity_modifytweeters_container) != null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		/*
		 * we have two menus. if we are in two pane mode the menu is more
		 * restricted with less options
		 */
		if (!isTwoPane())
			getMenuInflater().inflate(R.menu.main, menu);
		else
			getMenuInflater().inflate(R.menu.twopane_menu, menu);
		return true;
	}

	/*
	 * this is called every time we invalidate the options menu. we use this to
	 * either hide or show the editing button
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (!isTwoPane()) {
			menu.findItem(R.id.menu_load_tweeter_btn).setVisible(isEditing);
			menu.findItem(R.id.menu_additionbtn).setVisible(!isEditing);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		FragmentTransaction ft = fm.beginTransaction();
		switch (item.getItemId()) {

		case R.id.menu_additionbtn:

			/*
			 * start the activity that allows users to enter in new twitter user
			 * accounts
			 */
			ModifyTweetersFragment frag = new ModifyTweetersFragment();

			ft.add(R.id.activity_tweetslist_container, frag,
					ModifyTweetersFragment.class.getName());
			ft.addToBackStack(null);//adding to the backstack allows us to revert the last transaction and thus return to tweeter list afterwards
			ft.commit();

			isEditing = true;
			invalidateOptionsMenu();
			break;

		case R.id.menu_load_tweeter_btn:

			onBackPressed();
			invalidateOptionsMenu();
			break;
		case R.id.menu_refresh_btn:
		case R.id.menu_refreshlisting_settings:

			/*
			 * user may press refresh button, force the loader to get new data
			 * in this case
			 */

			mFragmentListCallback.requestRefresh();
			break;

		case R.id.menu__delete_all_settings:

			deleteAllTweetListEntries();

			break;

		case R.id.menu_quickread_settings:

			mMultiColumn = !mMultiColumn;
			mFragmentListCallback.requestMultiColumn(mMultiColumn);
			break;
		default:
			return true;
		}
		return false;
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();

		isEditing = false;
		invalidateOptionsMenu();

		/*
		 * on returning to the tweeter list fragment, if the taskfragment exist
		 * it means there is a network call occuring, show the proress dialoag
		 */
		/*Fragment taskfragment = fm.findFragmentByTag(TaskFragment.class
				.getName());
		if (taskfragment != null)
			showProgressDialog(getString(R.string.notification_loading_items));*/
	}

	public void createWarningDialog(String msg) {

		MyDialogFragment dialogfragment = (MyDialogFragment) MyDialogFragment
				.newInstance(this, msg);
		dialogfragment.show(fm, "dialog2");
	}



	/*
	 * called from the actionbar when user wants to wipe the entire list of
	 * tweeters if there is a single item to delete we use callbacks to delete
	 * the entries instead of making any network calls
	 */
	public void deleteAllTweetListEntries() {

		TaskFragment task = (TaskFragment) fm
				.findFragmentByTag(TaskFragment.class.getName());
		if (task != null)
			task.CancelLongOperation();

		int numrows = getContentResolver().delete(Consts.CONTENT_URI, null,
				null);
		if (numrows > 0)
			createToast(this, getString(R.string.warning_item_deleted));
		
	}

	
	@Override
	public void requestListItem(String tweeterName) {

		/*
		 * if we are in two pane mode this listener will execute a interface
		 * method to change the viewpager page
		 */

		TweeterListFragment listFragment = (TweeterListFragment) getSupportFragmentManager()
				.findFragmentByTag(TweeterListFragment.class.getName());
		if (listFragment != null) {
			listFragment.goToPage(tweeterName);
		}

	}

	
}
