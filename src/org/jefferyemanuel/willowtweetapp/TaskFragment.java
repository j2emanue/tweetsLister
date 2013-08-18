package org.jefferyemanuel.willowtweetapp;

import static org.jefferyemanuel.willowtweetapp.Utils.printLog;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.jefferyemanuel.listeners.TaskCallbacks;

import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

/**
 * This Fragment manages a single background task and retains itself across
 * configuration changes.
 */
public class TaskFragment extends Fragment {
	//private ProgressDialogFragment pdialog;

	private Configuration twitterConfiguration;
	private volatile boolean running = true;

	public Configuration getTwitterCoonfiguration() {
		return twitterConfiguration;
	}

	public void setTwitterCoonfiguration(Configuration twitterConfiguration) {
		this.twitterConfiguration = twitterConfiguration;
	}

	

	public static TaskFragment newInstance(String[] userList) {

		TaskFragment f = new TaskFragment();
		Bundle args = new Bundle();
		args.putStringArray(Consts.TASK_ARGUMENT_USERLIST, userList);
		f.setArguments(args);
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
		printLog(Consts.TAG,
				"calling TaskFraments onAttach:" + activity.getTaskId());
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
		printLog(Consts.TAG, "calling taskFragment onCreate");
		// Retain this fragment across configuration changes.
		setRetainInstance(true);

		ConfigurationBuilder mTwitterConfigBuilder = new ConfigurationBuilder();
		mTwitterConfigBuilder.setOAuthConsumerKey(Consts.CONSUMER_KEY);
		mTwitterConfigBuilder
				.setOAuthConsumerSecret(Consts.CONSUMER_SECRET_KEY);
		mTwitterConfigBuilder.setOAuthAccessToken(Consts.ACCESS_TOKEN);
		mTwitterConfigBuilder
				.setOAuthAccessTokenSecret(Consts.ACCESS_TOKEN_SECRET);
		twitterConfiguration = mTwitterConfigBuilder.build();

		mTask = new LongOperation();
		mTask.execute();

	}

	/**
	 * Set the callback to null so we don't accidentally leak the Activity
	 * instance.
	 */
	@Override
	public void onDetach() {
		super.onDetach();
		printLog(Consts.TAG, "calling onDetach from taskFragment ");
		mCallbacks = null;
	}

	/*used to set the volatile running variable to stop the background task.  Could have used onCancel in AsynchTask
	 * but found some delay*/
	public void CancelLongOperation() {

		if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED)
			running = false;
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
			AsyncTask<Void, Integer, ArrayList<ArrayList<HashMap<String, String>>>> {

		//	ConfigurationBuilder cb;
		/* alloc a twitter object to make read and write calls */
		Twitter twitter;
		/*
		 * define our global map of tweeter's object. In this case it will be an
		 * array of many arrays of tweeter objects
		 */
		ArrayList<ArrayList<HashMap<String, String>>> mTweetersObj_map;
		String[] tweeters;

		public LongOperation() {

			/* alloc a twitter object to make read and write calls */
			twitter = new TwitterFactory(getTwitterCoonfiguration())
					.getInstance();
			Bundle args = TaskFragment.this.getArguments();
			tweeters = args.getStringArray(Consts.TASK_ARGUMENT_USERLIST);
			/*arraylist default capacity is 10 on average, well set the capacity to how many tweeters we have
			 * for efficiency and avoid high cost of  resizing to another array
			 * we always insert at the end of the arraylist so time complexity is O(1)
			 * retreiving by index as well so search is 0(1)*/
			mTweetersObj_map = new ArrayList<ArrayList<HashMap<String, String>>>(tweeters.length);
		}

		@Override
		protected void onPreExecute() {

			if (mCallbacks != null) {
				mCallbacks.onPreExecute();
			}

		}

		/**
		 * Note that we do NOT call the callback object's methods directly from
		 * the background thread, as this could result in a race condition.
		 */
		@Override
		protected ArrayList<ArrayList<HashMap<String, String>>> doInBackground(
				Void... ignore) {

			List<twitter4j.Status> statuses = new ArrayList<twitter4j.Status>();

			/* define our loop variables outside the loop to avoid massive GC */
			ArrayList<HashMap<String, String>> tweeterInfo;
			HashMap<String, String> object;
			User user;
			String avatar;
			boolean stopPublishing = false;

			/* define how many pages we will retrieve from twitter time line */
			Paging pagination = new Paging(1, Consts.NUMBER_OF_STATUSES);

			/*
			 * loop through and for each tweeters name get his/her timeline and
			 * save each status's info into a hashmap
			 */
			printLog(Consts.TAG, "beginning background Task in taskFragment");

			for (String tweeter : tweeters) {
				printLog(Consts.TAG, "looping through statuses for tweeter:"
						+ tweeter);

				if (!running) {
					printLog(Consts.TAG,
							"cancelling doInBackground from TaskFragment");
					mTweetersObj_map.clear();
					break;
				}/*
				 * save all user specific info into an array ofhashmap object
				 * called tweeterInfo.
				 */
				tweeterInfo = new ArrayList<HashMap<String, String>>();

				try {
					//grab xNumber of tweets from 1st page for each tweeter
					statuses = twitter.getUserTimeline(tweeter, pagination);

				} catch (TwitterException e) {
					Log.w(Consts.TAG, "Error locating tweeter named:" + tweeter
							+ " on Twitter");
					Log.w(Consts.TAG, e.getMessage());
					Log.w(Consts.TAG, "errorcode is: " + e.getErrorCode());
					if (e.getErrorCode() == Consts.ERROR_CODE_RATE_LIMIT_EXCEEDED) {
						if (!stopPublishing)
							publishProgress(Consts.ERROR_CODE_RATE_LIMIT_EXCEEDED);
						stopPublishing = true;
					}

					/* save a blank object to display to balance the pageviewer */
					mTweetersObj_map.add(tweeterInfo);
					/* continue finding statuses of other twitters */
					continue;
				}

				if (statuses.size() > 0) {

					for (twitter4j.Status s : statuses) {

						/*
						 * loop through each status(tweet) and save the
						 * characters of that tweet to an object
						 */
						object = new HashMap<String, String>();

						//handle retweets
						object.put(Consts.KEY_IS_RETWEET,
								s.isRetweet() ? "true" : "false");
						if (s.isRetweet()) {

							s = s.getRetweetedStatus();
						}
						avatar = s.getUser().getProfileImageURL();

						user = s.getUser();

						object.put(Consts.KEY_CREATED_DATE, s.getCreatedAt()
								.toString());
						object.put(Consts.KEY_TWEET_MSG, s.getText());
						String timePosted = s.getCreatedAt().toString();
						object.put(Consts.KEY_TWEETDATE, timePosted);
						object.put(Consts.KEY_AUTHOR, user.getName());
						object.put(Consts.KEY_AVATAR, avatar);
						object.put(Consts.KEY_SCREEN_NAME, user.getScreenName());
						object.put(Consts.KEY_USERID, user.getId() + "");
						object.put(Consts.KEY_TWEED_ID, s.getId() + "");
						object.put(Consts.KEY_TWEET_COUNT,
								"" + user.getStatusesCount());
						object.put(Consts.KEY_FOLLOWERS,
								user.getFollowersCount() + "");
						object.put(Consts.KEY_FOLLOWING, user.getFriendsCount()
								+ "");

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

				}
				//else
				mTweetersObj_map.add(tweeterInfo);

				if (!stopPublishing) {
					int progressresult = (int) ((double) mTweetersObj_map
							.size() / tweeters.length * 100);

					publishProgress(progressresult);
				}
			}//end for loop as we now have info on each tweeter

			/* return the global map of tweets */
			return mTweetersObj_map;

		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			int progress = values[0];
			if (mCallbacks != null)
				mCallbacks.onProgressUpdate(progress);
		}

		@Override
		protected void onCancelled() {
			printLog(Consts.TAG, "calling onCancelled of taskFragment");
			running = false;
			if (mCallbacks != null) {
				mCallbacks.onCancelled();
			}

		}

		@Override
		protected void onPostExecute(
				ArrayList<ArrayList<HashMap<String, String>>> result) {
			//super.onPostExecute(result);

			if (mCallbacks != null) {
				mCallbacks.onPostExecute(result);

			}
			if (getActivity() != null)
				/*
				 * task is complete, lets remove outselves from the fragment
				 * list
				 */
				getActivity().getSupportFragmentManager().beginTransaction()
						.remove(TaskFragment.this).commitAllowingStateLoss();
		}
	}

}