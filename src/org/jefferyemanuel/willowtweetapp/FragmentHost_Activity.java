package org.jefferyemanuel.willowtweetapp;

import static org.jefferyemanuel.willowtweetapp.Utils.createToast;
import static org.jefferyemanuel.willowtweetapp.Utils.printLog;

import java.util.ArrayList;
import java.util.HashMap;

import org.jefferyemanuel.listeners.TaskCallbacks;
import org.jefferyemanuel.listeners.TweeterListObserver;
import org.jefferyemanuel.listeners.TweeterSelectedListener;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

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
 */

public class FragmentHost_Activity extends FragmentActivity implements

TaskCallbacks, TweeterSelectedListener {

	private TweeterListObserver mFragmentCallback;
	private FragmentManager fm;
	private ViewGroup mModiftyTweetersLayout, mTweetslistLayout;
	private boolean isEditing, isFromSavedState;
	private ArrayList<ArrayList<HashMap<String, String>>> tweeters;
	private int restoredIndex = 0;

	
	/*make any fragment method calls after the fragments have been resumed*/
	@Override
	protected void onResumeFragments() {
		// TODO Auto-generated method stub
		super.onResumeFragments();

		if (isFromSavedState) {
			mFragmentCallback.changePage(restoredIndex);
			isFromSavedState = false;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		printLog(Consts.TAG, "onCreate: savedInstanceState "
				+ (savedInstanceState == null ? "==" : "!=") + " null");

		setContentView(R.layout.activity_main);
		fm = getSupportFragmentManager();
		// Restore state

		if (savedInstanceState != null) {
			// The fragment manager will handle restoring them if we are being
			// restored from a saved state
			isFromSavedState = true;
			mFragmentCallback = (TweeterListFragment) fm
					.findFragmentByTag(TweeterListFragment.class.getName());

			/* on orientation change restore the data */
			tweeters = (ArrayList<ArrayList<HashMap<String, String>>>) savedInstanceState
					.getSerializable(Consts.SAVED_INSTANCE_TWEETERS);
			restoredIndex = savedInstanceState
					.getInt(Consts.SAVED_INSTANCE_POSITION);
			if (tweeters != null)
				((TweeterListObserver) mFragmentCallback)
						.onConnectedToTwitterComplete(tweeters);
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

				mFragmentCallback = (TweeterListObserver) tweeterListFragment;

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

			ft.add(R.id.activity_tweetslist_container,
					new ModifyTweetersFragment(),
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

			mFragmentCallback.requestRefresh();
			break;

		case R.id.menu__delete_all_settings:

			deleteAllTweetListEntries();

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
		Fragment taskfragment = fm.findFragmentByTag(TaskFragment.class
				.getName());
		if (taskfragment != null)
			showProgressDialog(getString(R.string.notification_loading_items));
	}

	public void createWarningDialog(String msg) {

		MyDialogFragment dialogfragment = (MyDialogFragment) MyDialogFragment
				.newInstance(this, msg);
		dialogfragment.show(fm, "dialog2");
	}

	private void showProgressDialog(String msg) {

		dismissProgressDialog();
		ProgressDialogFragment dialog = ProgressDialogFragment.newInstance(msg);
		dialog.setCancelable(true);
		dialog.show(fm, "dialog");
	}

	private void dismissProgressDialog() {
		Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
		if (prev != null && prev.getActivity() != null) {
			DialogFragment df = (DialogFragment) prev;
			try {
				df.dismiss();
			} catch (IllegalStateException e) {
				// TODO: handle exception
			}
		}
	}

	/* locates our fragment via tag and updates the progress bar */
	private void updateProgressDialog(int value) {

		if (value == Consts.ERROR_CODE_RATE_LIMIT_EXCEEDED) {
			createWarningDialog(getString(R.string.warning_max_apiCalls_Reached));
		}

		else {
			ProgressDialogFragment theCurrentDialog = (ProgressDialogFragment) getSupportFragmentManager()
					.findFragmentByTag("dialog");
			if (theCurrentDialog != null) {
				theCurrentDialog.setProgressOfDialog(value);
			}

		}

	}

	/*
	 * called from the actionbar when user wants to wipe the entire list of
	 * tweeters
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
	public void onPreExecute() {
		Fragment modifyfragment = fm
				.findFragmentByTag(ModifyTweetersFragment.class.getName());
		/* dont show the progress if we are not on the viewpager screen */
		if (isTwoPane() || modifyfragment == null || !modifyfragment.isAdded())
			showProgressDialog(getString(R.string.notification_loading_items));
	}

	@Override
	public void onProgressUpdate(int value) {
		printLog(Consts.TAG, "calling onprogressUpdate");
		updateProgressDialog(value);
	}

	@Override
	public void onCancelled() {
		printLog(Consts.TAG, "calling onCancelled");
		dismissProgressDialog();
	}

	@Override
	public void onPostExecute(
			ArrayList<ArrayList<HashMap<String, String>>> result) {

		dismissProgressDialog();

		printLog(Consts.TAG, "calling onPostExecute from MainActivity:"
				+ result.size() + " items");
		
			/*
			 * all done looping for one specific tweeter, lets save all that
			 * users status's into our global map
			 */
			if (mFragmentCallback != null) {
				mFragmentCallback.onConnectedToTwitterComplete(result);
				tweeters = result;
			}

		if (result.size() <= 0)
			createToast(this, getString(R.string.warning_no_data_collected));
	}

	@Override
	public void selectListItem(String tweeterName) {

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
