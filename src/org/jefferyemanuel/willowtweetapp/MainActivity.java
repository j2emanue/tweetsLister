package org.jefferyemanuel.willowtweetapp;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.jefferyemanuel.willowtweetapp.TaskFragment.TaskCallbacks;
import org.json.JSONException;

import twitter4j.User;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/*
 * the Main Activity.  Here we use twitter4j library to parse twitter JSON on a seperate thread and after successful
 * parse we set the page adapter and to each page we add a listview and set an adapter where we maniuplate each item
 * in the listview. The network processing only run on onCreate, we can move it to onresume to have it called everytime
 * we come back to the app. On my \\TODO list is to build a diskcache system  instead of LruCache for the http images or store them all in a map instead of
 * retrieving them each time from web.  
 * */

public class MainActivity extends FragmentActivity implements
		OnItemClickListener,TaskCallbacks{

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	private static SectionsPagerAdapter mSectionsPagerAdapter;

	private static Fragment mListfragment;
	TaskFragment mTaskFragment;
	
	private static NumberFormat nf;
	//private CommonsHttpOAuthConsumer httpOauthConsumer;
	//private CommonsHttpOAuthProvider httpOauthprovider;

	//contains each tweeters array of info we collected in background
	static ArrayList<ArrayList<HashMap<String, Object>>> mTweetersObj_map;
	static String[] mpageTitles;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (Consts.DEVELOPER_MODE)
			Log.v(Consts.TAG, "calling onCreate");

		mpageTitles = getResources().getStringArray(R.array.tweeters);//store the titles for each page view

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.

		/* set up twitter4j library with oAUTH keys, never call more the once */
		

		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		/*
		 * define our global map of tweeter's object. In this case it will
		 * be an array of many arrays of tweeter objects
		 */
		mTweetersObj_map = new ArrayList<ArrayList<HashMap<String, Object>>>();
		
		
		//this calls expensive so lets do it once as it has to fetch the system locale which can be intense
		nf = NumberFormat.getInstance();

		
		
		/*
		 * if the user is not on line warn about connection else start the
		 * processing twitter JSON feed
		 */
		if (!isOnline())
			createToast(getString(R.string.warning_no_connection));
		else{
			FragmentManager fm = getSupportFragmentManager();
			
			 mTaskFragment = (TaskFragment) fm.findFragmentByTag("task");
		 
		    // If the Fragment is non-null, then it is currently being
		    // retained across a configuration change.
		    if (mTaskFragment == null) {
		    	mTaskFragment = new TaskFragment();
		      fm.beginTransaction().add(mTaskFragment, "task").commit();
			
		}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	protected boolean isOnline() {
		final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected()) {
			return true;
		} else {
			return false;
		}
	}

	/* creates a custom toast message, gives our app flavor */
	public void createToast(String msg) {

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View layout = inflater.inflate(R.layout.toast_layout, null);

		TextView text = (TextView) layout.findViewById(R.id.text);
		text.setText(msg);

		Toast toast = new Toast(this);
		toast.setGravity(Gravity.BOTTOM, 0, 40);//TODO convert by display metric

		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(layout);
		toast.show();
	}

	/*
	 * background mechanism. Here we parse JSON twitter feed and update main UI
	 * Thread to beginning showing visuals
	 */


	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public static class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);

		}

				@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a listFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			mListfragment = TweetsListFragment.newInstance();
			
			
//			mListfragment.getFragmentManager().beginTransaction().commitAllowingStateLoss();
			Bundle args = new Bundle();
			args.putInt(TweetsListFragment.ARG_SECTION_NUMBER, position);
			mListfragment.setArguments(args);
			return mListfragment;
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return mpageTitles.length;
		}

		@Override
		public CharSequence getPageTitle(int position) {

			return "@" + mpageTitles[position];

		}

		/**
		 * A reusable fragment representing a section of the app as a listview.
		 * It gets the page # (position from the activitys section adapter which
		 * passes it as a bundle. We can use this position to get which tweeter
		 * the end user is viewing.
		 */
	}

	public static class TweetsListFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";

		int position;
		String key;
		ListView lv;

		public static TweetsListFragment newInstance() {
			TweetsListFragment f = new TweetsListFragment();
			return f;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
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

			if(Consts.DEVELOPER_MODE)Log.v(Consts.TAG,position+"<---");
			Bundle args = this.getArguments();
			position = args.getInt(ARG_SECTION_NUMBER);

			//key = getActivity().getResources().getStringArray(R.array.tweeters)[position];

			if (!mTweetersObj_map.isEmpty()) {

				if (Consts.DEVELOPER_MODE)
					Log.v(Consts.TAG,
							"position being passed to fragment and adapter:"
									+ position);
				/*
				 * since we know the tweeter (by section fragment argument) lets
				 * get the array of tweets for that tweeter and pass it to our
				 * adapter as the datasource. So each time the position changes
				 * we re-use this fragment and get another tweeter based on
				 * section position and create an adapter for our listview
				 */
				TweeterJSONAdapter mAdapter = new TweeterJSONAdapter(
						getActivity(), R.layout.list_item_child,
						mTweetersObj_map.get(position));
				try {
					// add a header to each listview
					addHeader();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//finally set the adapter
				lv.setAdapter(mAdapter);

			}

		}

		//add header to listview
		public void addHeader() throws JSONException {

			String total_tweets = "";
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
					tv_total_followers.setText(aTweeter.get(i).get(
							Consts.KEY_FOLLOWERS)
							+ " Followers");
					total_tweets = (String) aTweeter.get(i).get(
							Consts.KEY_TWEET_COUNT);
					total_tweets = nf.format(Long.parseLong(total_tweets));// format the number
					tv_total_tweets.setText(total_tweets + " "
							+ getString(R.string.tweets));
					tv_total_friends.setText(aTweeter.get(i).get(
							Consts.KEY_FOLLOWING)
							+ " Following");

					String avatarURL = (String) aTweeter.get(i).get(
							Consts.KEY_AVATAR);

					/* make a network call to make bitmap from http image source */
					//new DownloadImageTask(headerImage).execute(avatarURL);
					//new DownloadImageTask().fetchDrawableOnThread(avatarURL,headerImage);
					DownloadImageTask.getInstance(getActivity()).loadBitmap(avatarURL,
							headerImage);
				}
			}
			/* add the header to listView */
			lv.addHeaderView(header, null, false);

		}

		@Override
		public void onSaveInstanceState(Bundle outState) 
        {

			
			/*cheap fix not working ...leave it for now*/
		String tabTitle=(String) mSectionsPagerAdapter.getPageTitle(position)	;
    	    outState.putString("tab", tabTitle);
    		
            super.onSaveInstanceState(outState);
            

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

	@Override
	public void onPreExecute() {
		// TODO Auto-generated method stub
		mTweetersObj_map.clear();
	}

	@Override
	public void onProgressUpdate(ArrayList<HashMap<String, Object>> UsersTweet) {
		/*
		 * all done looping for one specific tweeter, lets save all
		 * that users status's into our global map
		 */
		
		
	mTweetersObj_map.add(UsersTweet);	
	}

	@Override
	public void onCancelled() {
	}


	
	@Override
	public void onPostExecute(boolean status) {
		// TODO Auto-generated method stub
	
		if (status == true) {
			// Set up the ViewPager with the sections adapter.

			mViewPager = (ViewPager) findViewById(R.id.pager);
			mViewPager.setAdapter(mSectionsPagerAdapter); //show the viewpager finally
		} else
			createToast(getString(R.string.warning_no_data_collected));
		
		 this.getSupportFragmentManager().beginTransaction().remove(mTaskFragment).commit();
	}
	

}