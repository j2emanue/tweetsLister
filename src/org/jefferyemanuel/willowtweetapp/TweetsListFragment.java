package org.jefferyemanuel.willowtweetapp;

import static org.jefferyemanuel.willowtweetapp.Utils.printLog;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.jefferyemanuel.listeners.TaskCallbacks;
import org.jefferyemanuel.willowtweetapp.TweeterListAdapter.Holder;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.sothree.multiitemrowlistadapter.MultiItemRowListAdapter;

public class TweetsListFragment extends ListFragment implements
		OnItemClickListener, TaskCallbacks {
	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	public static final String ARG_SECTION_NUMBER = "section_number";
	public static final String ARG_MULTICOLUMN = "shouldColumn";
	public static final String ARG_USERNAME = "username";
	private static boolean indicatorOn = false;

	int position;
	String key;
	ListView lv;

	/* initialize the map of all our tweeter data sources for each listview */
	ArrayList<ArrayList<HashMap<String, String>>> mTweetersObj_map;
	TweeterListAdapter mAdapter;
	boolean multiColumn;
	String userName;
	static NumberFormat nf;

	public static TweetsListFragment newInstance(int position,
			boolean multiColumn, NumberFormat numformat, String username) {
		nf = numformat;

		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, position);
		args.putBoolean(ARG_MULTICOLUMN, multiColumn);
		args.putString(ARG_USERNAME, username);
		TweetsListFragment f = new TweetsListFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		printLog(Consts.TAG_FRAGMENT, "OnCreateView called");

		mTweetersObj_map = new ArrayList<ArrayList<HashMap<String, String>>>();

		/* lets get a handle on our listview and set a click listener */
		View rootView = inflater.inflate(
				R.layout.lists_viewpager_fragments_layout, null);
		lv = (ListView) rootView.findViewById(android.R.id.list);

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
		userName = args.getString(ARG_USERNAME);
		multiColumn = args.getBoolean(ARG_MULTICOLUMN);

		/*
		 * create a taskFragment to contact twitter passing it our user names
		 * supplied from cursor loader
		 */
		TaskFragment taskFragment = TaskFragment
				.newInstance(new String[] { userName });
		taskFragment.setTaskListener(this);
		getActivity().getSupportFragmentManager().beginTransaction()
				.add(taskFragment, TaskFragment.class.getName())
				.commitAllowingStateLoss();

	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		printLog(Consts.TAG_FRAGMENT, "OnResume called:" + position);

	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		printLog(Consts.TAG, "calling onDestroy of a single listfragment");
		FragmentManager cfm = getActivity().getSupportFragmentManager();
		TaskFragment taskFragment = (TaskFragment) cfm
				.findFragmentByTag(TaskFragment.class.getName());

		/* cancel any task still running by chance */
		if (taskFragment != null) {
			printLog(Consts.TAG,
					"calling cancelLongOperation on fragment at position"
							+ position);
			taskFragment.CancelLongOperation();
		}

	}

	@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);

		printLog(Consts.TAG_FRAGMENT, "OnAttach called");

	}

	/*
	 * add header to listview, we could have made this stationary but we are
	 * providing more real estate for the listview this way. We could have made
	 * this a stationary header but user experience would be affected with less
	 * room to scroll
	 */

	public void addHeader(ArrayList<ArrayList<HashMap<String, String>>> map) {

		String temp;
		/* get a specific tweeter's tweets from the map of all tweeters */
		ArrayList<HashMap<String, String>> aTweeter = map.get(0);

		int size = aTweeter.size();

		View header = View.inflate(getActivity(), R.layout.list_header, null);

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

				tv_total_tweets
						.setText(temp + " " + getString(R.string.tweets));

				/* set up total following format */
				temp = (String) (aTweeter.get(i).get(Consts.KEY_FOLLOWING) + "");
				tv_total_friends.setText(nf.format(Long.parseLong(temp))
						+ " Following");

				String avatarURL = (String) aTweeter.get(i).get(
						Consts.KEY_AVATAR);

				/* save header image to disk cache for quick re-use */
				if (FragmentHost_Activity.imageDiskCache != null)
					FragmentHost_Activity.imageDiskCache.getBitmap(avatarURL,
							headerImage);
				break;//break out of the loop as we have filled the header with valid data not from a retweet
			}
		}
		/* add the header to listView */
		lv.addHeaderView(header, null, false);

	}

	/*
	 * purpose: fills the listview of this fragment with data and adjust for
	 * multiple columns. The adapter is deinfed here
	 */
	private void fillListView(ArrayList<ArrayList<HashMap<String, String>>> data) {

		if (data != null && !data.isEmpty())

		{

			/*
			 * since we know the tweeter (by section fragment argument) lets get
			 * the array of tweets for that tweeter and pass it to our adapter
			 * as the datasource. So each time the position changes we re-use
			 * this fragment and get another tweeter based on section position
			 * and create an adapter for our listview
			 */

			int spacing = (int) getResources().getDimension(
					R.dimen.item_spacing);
			int itemsPerRow = getResources()
					.getInteger(R.integer.items_per_row);

			addHeader(data);

			/* pass in our datasource from taskfragment and the image cache */
			mAdapter = new TweeterListAdapter((FragmentActivity) getActivity(),
					R.layout.list_item_child, data.get(0),
					FragmentHost_Activity.imageDiskCache);

			/*
			 * if user request multi columned then wrap our adapter in
			 * MultiItemRow adapter this is a 3rd party library
			 */
			if (multiColumn) {
				MultiItemRowListAdapter wrapperAdapter = new MultiItemRowListAdapter(
						getActivity(), mAdapter, itemsPerRow, spacing);
				lv.setAdapter(wrapperAdapter);
			} else

				//finally set the adapter for fragments listview 
				lv.setAdapter(mAdapter);
		}

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
					Uri.parse("https://twitter.com/" + twitterUser.screen_name));

			this.startActivity(intent);
		}
	}

	/*
	 * @Override public void onSaveInstanceState(Bundle outState) {
	 * 
	 * //cheap fix from stackoverflow if pageradapter does not refresh on
	 * orientation change. now using //statepageadapter which would resolve this
	 * conflict but leaving legacy code incase String tabTitle =(String)
	 * mSectionsPagerAdapter.getPageTitle(position); outState.putString("tab",
	 * tabTitle); super.onSaveInstanceState(outState);
	 * 
	 * }
	 */

	/* show a network call indicator on actionbar when asynchTask begins */
	private void setIndicator(boolean set) {

		if (getActivity() != null)
			getActivity().setProgressBarIndeterminateVisibility(set);

	}

	@Override
	public void onPreExecute() {
		setIndicator(true);
	}

	@Override
	public void onProgressUpdate(int value) {
		printLog(Consts.TAG, "calling onprogressUpdate");
		if (value == Consts.ERROR_CODE_RATE_LIMIT_EXCEEDED)
			createeWarningDialog(getResources().getString(
					R.string.warning_max_apiCalls_Reached));
	}

	@Override
	public void onCancelled() {
		printLog(Consts.TAG, "calling onCancelled");
		setIndicator(false);
	}

	@Override
	public void onPostExecute(
			ArrayList<ArrayList<HashMap<String, String>>> result) {

		printLog(Consts.TAG, "calling onPostExecute from MainActivity:"
				+ result.size() + " items");

		setIndicator(false);
		/*
		 * all done looping for one specific tweeter, lets save all that users
		 * status's into our global map
		 */
		if (getActivity() != null) {

			;

			mTweetersObj_map = result;

			fillListView(mTweetersObj_map);
		}
	}

	public void createeWarningDialog(String msg) {

		MyDialogFragment dialogfragment = (MyDialogFragment) MyDialogFragment
				.newInstance(getActivity(), msg);
		dialogfragment.show(getActivity().getSupportFragmentManager(),
				"dialog2");
	}

}
