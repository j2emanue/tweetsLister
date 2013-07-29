package org.jefferyemanuel.willowtweetapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static org.jefferyemanuel.willowtweetapp.Utils.*;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

public class TweeterUsersLoader extends AsyncTaskLoader<ArrayList<ArrayList<HashMap<String,Object>>>>{
	
	ArrayList<ArrayList<HashMap<String, Object>>> mResults;
	ConfigurationBuilder cb;
	/* alloc a twitter object to make read and write calls */
	Twitter twitter;

	/*
	 * define our global map of tweeter's object. In this case it will be an
	 * array of many arrays of tweeter objects
	 */
	ArrayList<ArrayList<HashMap<String, Object>>> mTweetersObj_map = new ArrayList<ArrayList<HashMap<String, Object>>>();
	String[] tweeters;
	
	public String[] getTweeters() {
		return tweeters;
	}


	public void setTweeters(String[] tweeters) {
		this.tweeters = tweeters;
	}


	public TweeterUsersLoader(Context context,String[] usernamesList) {
		super(context);
		
		this.tweeters=usernamesList;
		
		//----
		ConfigurationBuilder mTwitterConfigBuilder = new ConfigurationBuilder();
		mTwitterConfigBuilder.setOAuthConsumerKey(Consts.CONSUMER_KEY);
		mTwitterConfigBuilder
				.setOAuthConsumerSecret(Consts.CONSUMER_SECRET_KEY);
		mTwitterConfigBuilder.setOAuthAccessToken(Consts.ACCESS_TOKEN);
		mTwitterConfigBuilder
				.setOAuthAccessTokenSecret(Consts.ACCESS_TOKEN_SECRET);
			this.cb=mTwitterConfigBuilder;
		
		
		/* alloc a twitter object to make read and write calls */
		twitter = new TwitterFactory(cb.build()).getInstance();
		}
	
	
	 @Override
	    protected void onStartLoading() {
	        if (mResults != null) {
	            deliverResult(mResults); // Use cache
	        } else
	        
	        {
	            forceLoad();
	        }
	    }
	
	
	
	@Override
	public ArrayList<ArrayList<HashMap<String, Object>>> loadInBackground() {
		List<twitter4j.Status> statuses = new ArrayList<twitter4j.Status>();

		/* define our loop variables outside the loop to avoid massive GC */
		ArrayList<HashMap<String, Object>> tweeterInfo;
		HashMap<String, Object> object;
		User user;
		String avatar;
		
		/* define how many pages we will retrieve from twitter time line */
		Paging pagination = new Paging(1, Consts.NUMBER_OF_STATUSES);

		/*
		 * loop through and for each tweeters name get his/her timeline and
		 * save each status's info into a hashmap
		 */
int i =0+5;
i++;
		for (String tweeter : tweeters) {
			printLog(Consts.TAG,"looping through statuses for tweeter:"+tweeter);
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

			//	mTweetersObj_map.add(tweeterInfo);

			}
			//else
				mTweetersObj_map.add(tweeterInfo);
			/*
			 * tweet might have no status's so we publish after check of
			 * number of status's and send a empty map for no statuses
			 */
			
		}//end for loop as we now have info on each tweeter

		/* return the global map of tweets */
		return mTweetersObj_map;

	}
	
	 @Override
	    public void deliverResult(ArrayList<ArrayList<HashMap<String, Object>>> data) {
	        mResults = data; // Caching
	        super.deliverResult(data);
	    }
	
	 @Override
	    protected void onReset() {
	       super.onReset();
	       // Stop the loader if it is currently running
	       onStopLoading();
	       // Get rid of our cache if it exists
	       mResults = null;
	    }

	 @Override
	    public void onCanceled(ArrayList<ArrayList<HashMap<String, Object>>> data) {
	       // Attempt to cancel the current async load
	       super.onCanceled(data);
	       mResults = null;
	   }


}
