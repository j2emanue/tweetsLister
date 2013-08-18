package org.jefferyemanuel.willowtweetapp;

import static org.jefferyemanuel.willowtweetapp.Utils.createToast;
import static org.jefferyemanuel.willowtweetapp.Utils.printLog;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.jefferyemanuel.listeners.TweeterListObserver;
import org.jefferyemanuel.willowtweetapp.TweeterListAdapter.Holder;

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
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/*
 * the Main Activity.  Here we use twitter4j library to parse twitter JSON on a seperate thread and after successful
 * parse we set the page adapter and to each page we add a listview and set an adapter where we maniuplate each item
 * in the listview. A dISKCACHE is used to store http images. two implementations of this class 1. TaskCallbacks is an 
 * interface from TaskFragment that reports when a  AsnychTask has completed parsing twitter JSON feed and other is 
 * for cursor loader which loads a cursor to content provider that points to list of tweeters that user entered.  
 * */

public class TweeterListFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor>, TweeterListObserver {

	private SectionsPagerAdapter mSectionsPagerAdapter;
	private static DiskLruImageCache imageDiskCache;
	private static NumberFormat nf;
	private boolean fromSavedState = false;

	FragmentManager fm;
	//contains each tweeters array of info we collected in background
	static ArrayList<ArrayList<HashMap<String, String>>> mTweetersObj_map;
	String[] mpageTitles = { "blank" };
	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		//getActivity().getSupportLoaderManager().destroyLoader(Consts.LOADER_ID);
		printLog(Consts.TAG, "calling onDestroy");

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		/*
		 * If onDestroyView() is called first, we can use the previously
		 * savedState but we can't call saveState() anymore
		 */
		/*
		 * If onSaveInstanceState() is called first, we don't have savedState,
		 * so we need to call saveState()
		 */
		/* => (?:) operator inevitable! */
		//    fromSavedState=true;
		printLog(Consts.TAG,
				"calling onSavedInstanceState from tweeterListFragment");
	}

	@Override
	public void onResume() {
		super.onResume();
		{

			TaskFragment task_fragment = (TaskFragment) fm
					.findFragmentByTag(TaskFragment.class.getName());
			printLog(Consts.TAG, "calling onResume:" + task_fragment);
			if (task_fragment != null)
				/*
				 * send message to handler (although on UI thread already, to
				 * begin showing dialog
				 */
				threadMsg(Consts.HANDLER_SHOW_DIALOG);
		}
		Utils.hideKeyboard(getActivity());

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		if (container == null) {
			// We have different layouts, and in one of them this
			// fragment's containing frame doesn't exist.  The fragment
			// may still be created from its saved state, but there is
			// no reason to try to create its view hierarchy because it
			// won't be displayed.  Note this is not needed -- we could
			// just run the code below, where we would create and return
			// the view hierarchy; it would just never be used.
			return null;
		}

		View rootView = inflater.inflate(R.layout.tweeterlist_fragment, null);

		mViewPager = (ViewPager) rootView.findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		return rootView;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Consts.DEVELOPER_MODE)
			LoaderManager.enableDebugLogging(true);

		if (savedInstanceState != null) {
			fromSavedState = true;
		}

		printLog(Consts.TAG, "calling onCreate from TweetListFragment:"
				+ (savedInstanceState != null ? "orientationChanged" : "null"));

		/* alloc fragment manager */
		fm = getActivity().getSupportFragmentManager();

		//this calls expensive so lets do it once as it has to fetch the system locale which can be intense
		nf = NumberFormat.getInstance();

		/* log into twitter using oAUTH */

		/* CREATE OUR DISK CACHE TO STORE IMAGES */
		if (Utils.isExternalStorageAvailable())
			imageDiskCache = new DiskLruImageCache(getActivity(), "diskcache",
					Consts.DISK_CACHE_SIZE, CompressFormat.PNG,
					Consts.COMPRESS_QUALITY);

		/* initialize the map of all our tweeter data sources for each listview */
		mTweetersObj_map = new ArrayList<ArrayList<HashMap<String, String>>>();

		mSectionsPagerAdapter = new SectionsPagerAdapter(fm);

		/* create cursor loader to get names from tweeters table */
		getActivity().getSupportLoaderManager().initLoader(Consts.LOADER_ID,
				null, this);

	}

		public void createWarningDialog(String msg) {

		MyDialogFragment dialogfragment = (MyDialogFragment) MyDialogFragment
				.newInstance(getActivity(), msg);
		dialogfragment.show(fm, "dialog2");

	}

	private void dismissProgressDialog() {
		Fragment prev = getActivity().getSupportFragmentManager()
				.findFragmentByTag("dialog");
		if (prev != null && prev.getActivity() != null) {
			DialogFragment df = (DialogFragment) prev;
			try {
				df.dismiss();
			} catch (IllegalStateException e) {
				// TODO: handle exception
			}
		}
	}

	private void showProgressDialog(String msg) {

		dismissProgressDialog();
		ProgressDialogFragment dialog = ProgressDialogFragment.newInstance(msg);
		dialog.setCancelable(true);
		dialog.show(fm, "dialog");
	}

	public void reStartLoader() {

		printLog(Consts.TAG, "beginning refresh");
		getActivity().getSupportLoaderManager().restartLoader(
				Consts.LOADER_ASYNCH_ID, null, this);
	}

	/* locates our fragment via tag and updates the progress bar */
	private void updateProgressDialog(int value) {

		if (value == Consts.ERROR_CODE_RATE_LIMIT_EXCEEDED) {
			createWarningDialog(getString(R.string.warning_max_apiCalls_Reached));
		}

		else {
			ProgressDialogFragment theCurrentDialog = (ProgressDialogFragment) getActivity()
					.getSupportFragmentManager().findFragmentByTag("dialog");
			if (theCurrentDialog != null) {
				theCurrentDialog.setProgressOfDialog(value);
			}

		}

	}

	/* confirms weather the user has a network connection */
	protected boolean isOnline() {
		final ConnectivityManager cm = (ConnectivityManager) getActivity()
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected()) {
			return true;
		} else {
			return false;
		}
	}

	public void goToPage(String name) {

		int index = Arrays.asList(mpageTitles).indexOf(name);
		mViewPager.setCurrentItem(index, true);
	}

	public void setupTitles(String[] userList) {
		mpageTitles = userList;
	}

	/**
	 * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages
	 * 
	 * this is a {@link android.support.v4.app.FragmentStatePagerAdapter}
	 * derivative, which will NOT keep every loaded fragment in memory. use
	 * {@link android.support.v4.app.FragmentPagerAdapter} if you want to load
	 * all fragments into memory.
	 */

	public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

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

	public static class TweetsListFragment extends ListFragment implements
			OnItemClickListener {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";

		int position;
		String key;
		ListView lv;
		TweeterListAdapter mAdapter;

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
			View rootView = inflater.inflate(
					R.layout.lists_viewpager_fragments_layout, null);
			lv = (ListView) rootView.findViewById(android.R.id.list);
			//lv=this.getListView();

			return rootView; //You must return your view here
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			// TODO Auto-generated method stub
			super.onActivityCreated(savedInstanceState);

			lv.setOnItemClickListener((OnItemClickListener) this);

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
				mAdapter = new TweeterListAdapter(
						(FragmentActivity) getActivity(),
						R.layout.list_item_child,
						mTweetersObj_map.get(position), imageDiskCache);

				addHeader();

				//finally set the adapter for each listview upon call
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

		//add header to listview, we could have made this stationary but we are providing more real estate for the 
		//listview this way
		public void addHeader() {

			String temp;
			/* get a specific tweeter's tweets from the map of all tweeters */
			ArrayList<HashMap<String, String>> aTweeter = mTweetersObj_map
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
				if (!(Boolean) Boolean.parseBoolean(aTweeter.get(i).get(
						Consts.KEY_IS_RETWEET))) {
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

					/* save header image to disk cache for quick re-use */
					if (imageDiskCache != null)
						imageDiskCache.getBitmap(avatarURL, headerImage);
					break;//break out of the loop as we have filled the header with valid data not from a retweet
				}
			}
			/* add the header to listView */
			lv.addHeaderView(header, null, false);

		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// TODO Auto-generated method stub

			/*
			 * A listView item click listener that on item click either goes to
			 * web twitter or if app is installed opens up users profile
			 */
			Intent intent = null;

			/*
			 * our listView adapter saved each users info each items views tag.
			 * We use this user info here.
			 */
			Holder twitterUser = (Holder) view.getTag();

			printLog(Consts.TAG, "list item clicked");

			try {
				intent = new Intent(Intent.ACTION_VIEW);
				intent.setClassName("com.twitter.android",
						"com.twitter.android.ProfileActivity");
				// Don't forget to put a long object as the extra.
				intent.putExtra("user_id", Long.parseLong(twitterUser.id));
				startActivity(intent);
			} catch (Exception e) {
				// no Twitter app, revert to browser
				intent = new Intent(Intent.ACTION_VIEW,
						Uri.parse("https://twitter.com/"
								+ twitterUser.screen_name));

				this.startActivity(intent);
			}
		}

		/*
		 * @Override public void onSaveInstanceState(Bundle outState) {
		 * 
		 * //cheap fix from stackoverflow if pageradapter does not refresh on
		 * orientation change. now using //statepageadapter which would resolve
		 * this conflict but leaving legacy code incase String tabTitle
		 * =(String) mSectionsPagerAdapter.getPageTitle(position);
		 * outState.putString("tab", tabTitle);
		 * super.onSaveInstanceState(outState);
		 * 
		 * }
		 */

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

			if (aResponse.equals(Consts.HANDLER_BEGIN_TASK)) {

				TaskFragment taskFragment = (TaskFragment) fm
						.findFragmentByTag(TaskFragment.class.getName());

				// If the Fragment is non-null, then it is currently being
				// retained across a configuration change.
				if (taskFragment == null) {

					printLog(TAG, "task fragment not found, invoking..");

					taskFragment = TaskFragment.newInstance(mpageTitles);

					fm.beginTransaction()
							.add(taskFragment, TaskFragment.class.getName())
							.commit();

				} else if (!fromSavedState) {
					taskFragment.CancelLongOperation();

					fm.beginTransaction().remove(taskFragment).commit();
					fm.beginTransaction()
							.add(TaskFragment.newInstance(mpageTitles),
									TaskFragment.class.getName()).commit();
				}
				fromSavedState = false;
			}

		}

	};

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {

		return new CursorLoader(getActivity(), Consts.CONTENT_URI,
				new String[] { Consts.COLUMN_USER }, null, null,
				Consts.COLUMN_USER + " COLLATE NOCASE ASC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {

		/*
		 * our cursor loader has observed a change in content and notified us of
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

			cursor.moveToNext();
		}

		/* store user names */
		setupTitles((String[]) mUsernamesList.toArray(new String[0]));

		/* notify our page adapter of the change */
		mSectionsPagerAdapter.notifyDataSetChanged();

		/*
		 * send a message to handler to start taskFragment which is really a
		 * asynchTask to contact Twitter.com for feeds
		 */
		if (isOnline())
			threadMsg(Consts.HANDLER_BEGIN_TASK);
		else
			createToast(getActivity(),
					getString(R.string.warning_no_connection));
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void onConnectedToTwitterComplete(
			ArrayList<ArrayList<HashMap<String, String>>> allUserTweetsMap) {
		// TODO Auto-generated method stub

		mTweetersObj_map = allUserTweetsMap;
		mSectionsPagerAdapter.notifyDataSetChanged();
	}

	@Override
	public void requestRefresh() {
		reStartLoader();
	}

}