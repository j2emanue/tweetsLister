package org.jefferyemanuel.willowtweetapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This Fragment manages a single background task and retains itself across
 * configuration changes.
 */
public class TaskFragment extends Fragment {
	private ProgressDialogFragment pdialog;

	private static ConfigurationBuilder mTwitterConfigBuilder;

	/**
	 * Callback interface through which the fragment will report the task's
	 * progress and results back to the Activity.
	 */
	static interface TaskCallbacks {
		void onPreExecute();

		void onProgressUpdate(int value);

		void onCancelled();

		void onPostExecute(
				ArrayList<ArrayList<HashMap<String, Object>>> allUserTweetsMap);
	}

	public static TaskFragment newInstance() {

		TaskFragment f = new TaskFragment();
		return f;
	}

	private TaskCallbacks mCallbacks;
	private LongOperation mTask;

	/**
	 * Hold a reference to the parent Activity so we can report the task's
	 * current progress and results. The Android framework will pass us a
	 * reference to the newly created Activity after each configuration change.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mCallbacks = (TaskCallbacks) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement TaskCallBacks Listener");
		}
	}

	/**
	 * This method will only be called once when the retained Fragment is first
	 * created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mTwitterConfigBuilder = new ConfigurationBuilder();
		mTwitterConfigBuilder.setOAuthConsumerKey(Consts.CONSUMER_KEY);
		mTwitterConfigBuilder
				.setOAuthConsumerSecret(Consts.CONSUMER_SECRET_KEY);
		mTwitterConfigBuilder.setOAuthAccessToken(Consts.ACCESS_TOKEN);
		mTwitterConfigBuilder
				.setOAuthAccessTokenSecret(Consts.ACCESS_TOKEN_SECRET);

		// Retain this fragment across configuration changes.
		setRetainInstance(true);

		// Create and execute the background task.
		mTask = new LongOperation(mTwitterConfigBuilder);
		mTask.execute();
	}

	/**
	 * Set the callback to null so we don't accidentally leak the Activity
	 * instance.
	 */
	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = null;
	}

	/**
	 * /* background mechanism. Here we parse JSON twitter feed and update main
	 * UI Thread to beginning showing visuals
	 * 
	 * 
	 * Note that we need to check if the callbacks are null in each method in
	 * case they are invoked after the Activity's and Fragment's onDestroy()
	 * method have been called.
	 */
	private class LongOperation
			extends
			AsyncTask<Void, Integer, ArrayList<ArrayList<HashMap<String, Object>>>> {

		ConfigurationBuilder cb;
		/* alloc a twitter object to make read and write calls */
		Twitter twitter;

		/*
		 * define our global map of tweeter's object. In this case it will be an
		 * array of many arrays of tweeter objects
		 */
		ArrayList<ArrayList<HashMap<String, Object>>> mTweetersObj_map = new ArrayList<ArrayList<HashMap<String, Object>>>();

		public LongOperation(ConfigurationBuilder twitterConfigBuilder) {

			this.cb = twitterConfigBuilder;
			/* alloc a twitter object to make read and write calls */
			twitter = new TwitterFactory(cb.build()).getInstance();

		}

		@Override
		protected void onPreExecute() {

			if (mCallbacks != null) {
				mCallbacks.onPreExecute();
			}
			
			pdialog=ProgressDialogFragment.newInstance("Loading...");
			pdialog.show(getActivity().getSupportFragmentManager(), "dialogprogress");
			
		}

		/**
		 * Note that we do NOT call the callback object's methods directly from
		 * the background thread, as this could result in a race condition.
		 */
		@Override
		protected ArrayList<ArrayList<HashMap<String, Object>>> doInBackground(
				Void... ignore) {

			String[] tweeters = getResources().getStringArray(R.array.tweeters);

			/*
			 * our array to hold tweet statuses we retrieve as we parse each
			 * tweeters page
			 */
			List<twitter4j.Status> statuses = new ArrayList<twitter4j.Status>();

			/* define our loop variables outside the loop to avoid massive GC */
			ArrayList<HashMap<String, Object>> tweeterInfo;
			HashMap<String, Object> object;
			User user;
			String avatar;
			int usercount = 0;
			/* define how many pages we will retrieve from twitter time line */
			Paging pagination = new Paging(1, Consts.NUMBER_OF_STATUSES);

			/*
			 * loop through and for each tweeters name get his/her timeline and
			 * save each status's info into a hashmap
			 */

			for (String tweeter : tweeters) {
				usercount++;
				try {
					//grab xNumber of tweets from 1st page for each tweeter
					statuses = twitter.getUserTimeline(tweeter, pagination);

				} catch (TwitterException e) {
					Log.e(Consts.TAG, "Error logging into Twitter");
					Log.e(Consts.TAG, e.getMessage());
				}

				/*
				 * save all user specific info into an array ofhashmap object
				 * called tweeterInfo.
				 */
				tweeterInfo = new ArrayList<HashMap<String, Object>>();

				if (statuses.size() > 0) {

					for (twitter4j.Status s : statuses) {

						/*
						 * loop through each status(tweet) and save the
						 * characters of that tweet to an object
						 */
						object = new HashMap<String, Object>();

						//handle retweets
						object.put(Consts.KEY_IS_RETWEET, s.isRetweet());
						if (s.isRetweet()) {

							s = s.getRetweetedStatus();
						}
						avatar = s.getUser().getProfileImageURL();

						user = s.getUser();

						object.put(Consts.KEY_TWEET_MSG, s.getText());
						String timePosted = s.getCreatedAt().toString();//TODO check format of date
						object.put(Consts.KEY_TWEETDATE, timePosted);
						object.put(Consts.KEY_AUTHOR, user.getName());
						object.put(Consts.KEY_AVATAR, avatar);
						object.put(Consts.KEY_USER_OBJECT, user);
						object.put(Consts.KEY_TWEED_ID, s.getId());
						object.put(Consts.KEY_TWEET_COUNT,
								"" + user.getStatusesCount());
						object.put(Consts.KEY_FOLLOWERS,
								user.getFollowersCount());
						object.put(Consts.KEY_FOLLOWING, user.getFriendsCount());

						if (Consts.DEVELOPER_MODE)
							Log.v(Consts.TAG,
									"author:"
											+ (String) object
													.get(Consts.KEY_AUTHOR)
											+ " Status count:"
											+ (String) object
													.get(Consts.KEY_TWEET_COUNT));

						/*
						 * add status'es(tweets) info to our array of tweets for
						 * this one specific tweeter
						 */
						tweeterInfo.add(object);

					}

					mTweetersObj_map.add(tweeterInfo);

				}
				/*
				 * tweet might have no status's so we publish after check of
				 * number of status's and send a empty map for no statuses
				 */
				
			}//end for loop as we now have info on each tweeter

			/* return the global map of tweets */
			return mTweetersObj_map;

		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			
		}

		@Override
		protected void onCancelled() {
			if (mCallbacks != null) {
				mCallbacks.onCancelled();

				if(pdialog!=null)
				pdialog.dismissAllowingStateLoss();
				
			}
		}

		@Override
		protected void onPostExecute(
				ArrayList<ArrayList<HashMap<String, Object>>> result) {
			super.onPostExecute(result);

			if (pdialog != null)
				pdialog.dismissAllowingStateLoss();
			
			if (mCallbacks != null) {
				mCallbacks.onPostExecute(result);
			}

		}
	}

	/* creates a custom toast message, gives our app flavor */
	public void createToast(String msg) {

		LayoutInflater inflater = (LayoutInflater) getActivity()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View layout = inflater.inflate(R.layout.toast_layout, null);

		TextView text = (TextView) layout.findViewById(R.id.text);
		text.setText(msg);

		Toast toast = new Toast(getActivity());
		toast.setGravity(Gravity.BOTTOM, 0, 40);//TODO convert by display metric

		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(layout);
		toast.show();
	}

}