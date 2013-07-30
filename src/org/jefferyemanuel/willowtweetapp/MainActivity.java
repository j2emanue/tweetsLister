package org.jefferyemanuel.willowtweetapp;

import static org.jefferyemanuel.willowtweetapp.Utils.createToast;
import static org.jefferyemanuel.willowtweetapp.Utils.printLog;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.jefferyemanuel.willowtweetapp.TaskFragment.TaskCallbacks;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import twitter4j.User;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap.CompressFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/*
 * the Main Activity.  Here we use twitter4j library to parse twitter JSON on a seperate thread and after successful
 * parse we set the page adapter and to each page we add a listview and set an adapter where we maniuplate each item
 * in the listview. A dISKCACHE is used to store http images. two implementations of this class 1. TaskCallbacks is an 
 * interface from TaskFragment that reports when a  AsnychTask has completed parsing twitter JSON feed and other is 
 * for cursor loader which loads a cursor to content provider that points to list of tweeters that user entered.  
 * */

public class MainActivity extends FragmentActivity implements
		OnItemClickListener, TaskCallbacks,
		LoaderManager.LoaderCallbacks<Cursor> {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	private static SectionsPagerAdapter mSectionsPagerAdapter;
	private static DiskLruImageCache imageDiskCache;
	private static NumberFormat nf;
	private Configuration mTwitterConfig;
	FragmentManager fm;
	//contains each tweeters array of info we collected in background
	static ArrayList<ArrayList<HashMap<String, Object>>> mTweetersObj_map;
	static String[] mpageTitles = {};
	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		printLog(Consts.TAG, "calling onDestroy");
		//	threadMsg(Consts.HANDLER_REMOVE_TASK);
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		printLog(Consts.TAG, "calling onStop");
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		printLog(Consts.TAG, "calling onPause");
		threadMsg(Consts.HANDLER_HIDE_DIALOG);
	}

	@Override
	protected void onResumeFragments() {
		// TODO Auto-generated method stub
		super.onResumeFragments();
		TaskFragment task_fragment = (TaskFragment) fm
				.findFragmentByTag(Consts.TASK_FRAGMENT_ID);
		printLog(Consts.TAG, "calling onResumeFragments:" + task_fragment);

	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!isOnline())
			createToast(this, getString(R.string.warning_no_connection));
		else {
			TaskFragment task_fragment = (TaskFragment) fm
					.findFragmentByTag(Consts.TASK_FRAGMENT_ID);
			printLog(Consts.TAG, "calling onResume:" + task_fragment);
			if (task_fragment != null)
				threadMsg(Consts.HANDLER_SHOW_DIALOG);
		}
		Utils.hideKeyboard(this);

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (Consts.DEVELOPER_MODE)
			LoaderManager.enableDebugLogging(true);

		printLog(Consts.TAG, "calling onCreate");

		//this calls expensive so lets do it once as it has to fetch the system locale which can be intense
		nf = NumberFormat.getInstance();

		mViewPager = (ViewPager) findViewById(R.id.pager);

		/* log into twitter using oAUTH */

		/* CREATE OUR DISK CACHE TO STORE IMAGES */
		imageDiskCache = new DiskLruImageCache(this, "diskcache",
				Consts.DISK_CACHE_SIZE, CompressFormat.PNG,
				Consts.COMPRESS_QUALITY);

		/*
		 * send message to handler (although on UI thread already to begin
		 * showing dialog
		 */
		//threadMsg(Consts.HANDLER_SHOW_DIALOG);

		/* alloc fragment manager */
		fm = getSupportFragmentManager();
		/* create cursor loader to get names from tweeters table */
		getSupportLoaderManager().initLoader(Consts.LOADER_ID, null, this);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void dismissDialog() {
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

	private void showDialog(String msg) {

		dismissDialog();
		ProgressDialogFragment dialog = ProgressDialogFragment.newInstance(msg);
		dialog.setCancelable(false);

		//-----
		/*
		 * FragmentTransaction ft = fm.beginTransaction();
		 * ft.add(dialog,"dialog"); ft.commitAllowingStateLoss();
		 */
		//dialog.show(ft,"dialog");

		//----

		dialog.show(fm, "dialog");

	}

	/* locates our fragment via tag and updates the progress bar */
	private void updateProgressDialog(int value) {
		ProgressDialogFragment theCurrentDialog = (ProgressDialogFragment) getSupportFragmentManager()
				.findFragmentByTag("dialog");
		if (theCurrentDialog != null) {
			theCurrentDialog.setProgressOfDialog(value);
		}

	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_additionbtn:
			/*
			 * start the activity that allows users to enter in new twitter user
			 * accounts
			 */
			Intent i = new Intent(this, ModifyTweetersActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(i);
			break;

		case R.id.menu_refresh_btn:
			/*
			 * user may press refresh button, force the loader to get new data
			 * in this case
			 */
			printLog(Consts.TAG, "beginning refresh");
			getSupportLoaderManager().restartLoader(Consts.LOADER_ASYNCH_ID,
					null, this);

			break;

		case R.id.action_settings:
			break;

		default:
			return true;
		}
		return false;
	}

	/* confirms wheather the user has a network connection */
	protected boolean isOnline() {
		final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public static class SectionsPagerAdapter extends FragmentStatePagerAdapter {

		//FragmentManager fm;

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a listFragment (defined as a static inner class
			// below) with the page number as its lone argument.

			TweetsListFragment mListfragment = TweetsListFragment
					.newInstance(position);

			return mListfragment;
		}

		@Override
		public int getItemPosition(Object object) {
			printLog(Consts.TAG, "getting item position from pageview adapter");
			return PagerAdapter.POSITION_NONE;
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return mpageTitles.length;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return mpageTitles[position];

		}

		/**
		 * A reusable fragment representing a section of the app as a listview.
		 * It gets the page # (position from the activitys section adapter which
		 * passes it as a bundle. We can use this position to get which tweeter
		 * the end user is viewing.
		 */
	}

	public static class TweetsListFragment extends ListFragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";

		int position;
		String key;
		ListView lv;
		TweeterJSONAdapter mAdapter;

		public static TweetsListFragment newInstance(int position) {
			Bundle args = new Bundle();
			args.putInt(TweetsListFragment.ARG_SECTION_NUMBER, position);
			TweetsListFragment f = new TweetsListFragment();
			f.setArguments(args);
			return f;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {

			printLog(Consts.TAG_FRAGMENT, "OnCreateView called");

			/* lets get a handle on our listview and set a click listener */
			View rootView = inflater
					.inflate(R.layout.listfragment_layout, null);
			lv = (ListView) rootView.findViewById(android.R.id.list);
			lv.setOnItemClickListener((OnItemClickListener) getActivity());
			return rootView; //You must return your view here
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			// TODO Auto-generated method stub
			super.onActivityCreated(savedInstanceState);

			printLog(Consts.TAG_FRAGMENT, "OnActivityCreated called");

			Bundle args = this.getArguments();
			position = args.getInt(ARG_SECTION_NUMBER);

			if (!mTweetersObj_map.isEmpty()) {

				/*
				 * since we know the tweeter (by section fragment argument) lets
				 * get the array of tweets for that tweeter and pass it to our
				 * adapter as the datasource. So each time the position changes
				 * we re-use this fragment and get another tweeter based on
				 * section position and create an adapter for our listview
				 */

				//if(!mTweetersObj_map.get(position).isEmpty())
				mAdapter = new TweeterJSONAdapter(
						(FragmentActivity) getActivity(),
						R.layout.list_item_child,
						mTweetersObj_map.get(position), imageDiskCache);

				addHeader();

				//finally set the adapter
				lv.setAdapter(mAdapter);

			}

		}

		@Override
		public void onResume() {
			// TODO Auto-generated method stub
			super.onResume();

			printLog(Consts.TAG_FRAGMENT, "OnResume called:" + position);

		}

		@Override
		public void onAttach(Activity activity) {
			// TODO Auto-generated method stub
			super.onAttach(activity);

			printLog(Consts.TAG_FRAGMENT, "OnAttach called");

		}

		//add header to listview
		public void addHeader() {

			String temp;
			ArrayList<HashMap<String, Object>> aTweeter = mTweetersObj_map
					.get(position);

			int size = aTweeter.size();

			View header = View.inflate(getActivity(), R.layout.list_header,
					null);

			TextView tv_total_tweets = (TextView) header
					.findViewById(R.id.tv_total_tweets);
			TextView tv_total_followers = (TextView) header
					.findViewById(R.id.tv_followers);
			TextView tv_total_friends = (TextView) header
					.findViewById(R.id.tv_following);
			ImageView headerImage = (ImageView) header
					.findViewById(R.id.avatar_header);

			for (int i = 0; i < size; i++) {

				if (!(Boolean) aTweeter.get(i).get(Consts.KEY_IS_RETWEET)) {
					//since its not a retweet we know the info is for the sectioned user 
					temp = null;
					/* set up total followers format */
					temp = aTweeter.get(i).get(Consts.KEY_FOLLOWERS) + "";
					temp = nf.format(Long.parseLong(temp));

					tv_total_followers.setText(temp + " Followers");

					/* set up total tweets format */

					temp = (String) aTweeter.get(i).get(Consts.KEY_TWEET_COUNT);

					temp = nf.format(Long.parseLong(temp));// format the number

					tv_total_tweets.setText(temp + " "
							+ getString(R.string.tweets));

					/* set up total following format */
					temp = (String) (aTweeter.get(i).get(Consts.KEY_FOLLOWING) + "");
					tv_total_friends.setText(nf.format(Long.parseLong(temp))
							+ " Following");

					String avatarURL = (String) aTweeter.get(i).get(
							Consts.KEY_AVATAR);

					//DownloadImageTask.getInstance(getActivity()).loadBitmap(
					//	avatarURL, headerImage);

					/* save header image to disk cache for quick re-use */
					imageDiskCache.getBitmap(avatarURL, headerImage);
				}
			}
			/* add the header to listView */
			lv.addHeaderView(header, null, false);

		}

		/*
		 * @Override public void onSaveInstanceState(Bundle outState) {
		 * 
		 * //cheap fix not working ...leave it for now String tabTitle =
		 * (String) mSectionsPagerAdapter .getPageTitle(position);
		 * outState.putString("tab", tabTitle);
		 * 
		 * super.onSaveInstanceState(outState);
		 * 
		 * }
		 */

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub

		/*
		 * A listView item click listener that on item click either goes to web
		 * twitter or if app is installed opens up users profile
		 */
		Intent intent = null;

		/*
		 * our listView adapter saved each users info each items views tag. We
		 * use this user info here.
		 */
		User twitterUser = (User) view.getTag();

		try {
			intent = new Intent(Intent.ACTION_VIEW);
			intent.setClassName("com.twitter.android",
					"com.twitter.android.ProfileActivity");
			// Don't forget to put the "L" at the end of the id.
			intent.putExtra("user_id", twitterUser.getId());
			startActivity(intent);
		} catch (Exception e) {
			// no Twitter app, revert to browser
			intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse("https://twitter.com/"
							+ twitterUser.getScreenName()));
		}
		this.startActivity(intent);

	}

	/* sends a message to handler on UI thread */
	private void threadMsg(String msg) {

		if (!msg.equals(null) && !msg.equals("")) {
			Message msgObj = handler.obtainMessage();
			Bundle b = new Bundle();
			b.putString("message", msg);
			msgObj.setData(b);
			handler.sendMessage(msgObj);
		}
	}

	/*
	 * when the cursor completes loading the framework will not allow modifying
	 * fragments on onloadFinished, so i've set up a handler where we can send a
	 * message to the UI handler to update the UI
	 */
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {

			String TAG = "willow_handler";

			String aResponse = msg.getData().getString("message");

			printLog(TAG, aResponse);

			if (aResponse.equals(Consts.HANDLER_SHOW_DIALOG))
				showDialog(getString(R.string.notification_loading_items));

			if (aResponse.equals(Consts.HANDLER_HIDE_DIALOG)) {
				dismissDialog();
			}

			if (aResponse.equals(Consts.HANDLER_BEGIN_TASK)) {

				TaskFragment taskFragment = (TaskFragment) fm
						.findFragmentByTag(Consts.TASK_FRAGMENT_ID);

				// If the Fragment is non-null, then it is currently being
				// retained across a configuration change.
				if (taskFragment == null) {

					printLog(TAG, "task fragment not found, invoking..");

					taskFragment = TaskFragment.newInstance(mpageTitles);
					//taskFragment.setTwitterCoonfiguration(mTwitterConfig);
					fm.beginTransaction()
							.add(taskFragment, Consts.TASK_FRAGMENT_ID)
							.commit();

				}
			}

			if (aResponse.equals(Consts.HANDLER_REMOVE_TASK)) {

				/*
				 * pop the fragment off the fragment manager as the task has
				 * completed or has been cancelled
				 */
				TaskFragment task_fragment = (TaskFragment) getSupportFragmentManager()
						.findFragmentByTag(Consts.TASK_FRAGMENT_ID);

				if (task_fragment != null
						&& task_fragment.getActivity() != null
						&& !task_fragment.getActivity().isFinishing())
				//	try
				{
					getSupportFragmentManager().beginTransaction()
							.remove(task_fragment).commitAllowingStateLoss();
				}
				//catch (IllegalStateException e) {
				//incase the activity has already been destroyed we catch the exception

				//}
			}

		}

	};

	@Override
	public void onPreExecute() {
		threadMsg(Consts.HANDLER_SHOW_DIALOG);
	}

	@Override
	public void onProgressUpdate(int value) {
		printLog(Consts.TAG, "calling onprogressUpdate");
		updateProgressDialog(value);
	}

	@Override
	public void onCancelled() {
		printLog(Consts.TAG, "calling onCancelled");
		threadMsg(Consts.HANDLER_HIDE_DIALOG);
	}

	@Override
	public void onPostExecute(
			ArrayList<ArrayList<HashMap<String, Object>>> result) {

		threadMsg(Consts.HANDLER_HIDE_DIALOG);

		printLog(Consts.TAG, "calling onPostExecute from MainActivity:"
				+ result.size() + " items");

		if (result.size() > 0) {

			/*
			 * all done looping for one specific tweeter, lets save all that
			 * users status's into our global map
			 */

			mTweetersObj_map = result;
			//mpageTitles=tempTitles;

			mSectionsPagerAdapter.notifyDataSetChanged();

			mViewPager.setAdapter(mSectionsPagerAdapter); //show the viewpager finally
		} else
			createToast(this, getString(R.string.warning_no_data_collected));

		threadMsg(Consts.HANDLER_REMOVE_TASK);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {

		return new CursorLoader(this, Consts.CONTENT_URI,
				new String[] { Consts.COLUMN_USER }, null, null,
				Consts.COLUMN_USER + " COLLATE NOCASE ASC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {

		/*
		 * our cursor loader has observed a change in content and notfied us of
		 * user adding new tweets to the tab, lets contact twitter.com to get
		 * tweets for these users
		 */
		ArrayList<String> mUsernamesList = new ArrayList<String>();

		int index = cursor.getColumnIndex(Consts.COLUMN_USER);
		cursor.moveToFirst();
		String username;
		/* loop through user column from cursor and gather all user names */
		while (!cursor.isAfterLast()) {
			username = cursor.getString(index);
			mUsernamesList.add(username);

			printLog("jeff", username);
			cursor.moveToNext();
		}

		/* store user names */
		setupTitles((String[]) mUsernamesList.toArray(new String[0]));

		mSectionsPagerAdapter = new SectionsPagerAdapter(fm);
		/* notify our page adapter of the change */
		mSectionsPagerAdapter.notifyDataSetChanged();

		/*
		 * Since we are not allowed to create fragments in a loader, we create a
		 * mainUI handle and send a message
		 */
		threadMsg(Consts.HANDLER_HIDE_DIALOG);
		/*
		 * send a message to handler to start taskFragment which is really a
		 * asynchTask to contact Twitter.com for feeds
		 */
		if (isOnline())
			threadMsg(Consts.HANDLER_BEGIN_TASK);
		else
			createToast(this, getString(R.string.warning_no_connection));
	}

	public void setupTitles(String[] userList) {
		mpageTitles = userList;

	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub	
	}

	
}