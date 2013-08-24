package org.jefferyemanuel.willowtweetapp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import twitter4j.util.TimeSpanConverter;
import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TweeterListAdapter extends BaseAdapter {

	/*
	 * pass in our cache map and UI resouce to poulate as well as each objects
	 * info here
	 */
	public TweeterListAdapter(FragmentActivity activity,
			int textViewResourceId, ArrayList<HashMap<String, String>> objects,
			DiskLruImageCache imageDiskCache) {

		this.context = activity;
		mTweeterInfo = objects;
		this.imageDiskCache = imageDiskCache;
		linkifier = new Linkifier();
		mConverter = new TimeSpanConverter();

		isOrientationLandscape = getRotation(context) == Configuration.ORIENTATION_LANDSCAPE;

		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	private ArrayList<HashMap<String, String>> mTweeterInfo;
	private FragmentActivity context;
	private DiskLruImageCache imageDiskCache;
	private Linkifier linkifier;
	private TimeSpanConverter mConverter;
	private LayoutInflater mInflater;
	private HashMap<String, String> mTweetMap;
	private boolean isOrientationLandscape;

	@Override
	public int getCount() {
		return mTweeterInfo.size();
	}

	@Override
	public HashMap<String, String> getItem(int position) {
		// TODO Auto-generated method stub
		return mTweeterInfo.get(position);

	}

	@Override
	public long getItemId(int position) {
		// TODO fix this if we ever need to call by id
		return 0;
	}

	//TODO create image caching for images instead of http stream call continuously

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		Holder viewHolder = null;
		String author = null, tweet = null, avatarURL = null;

		/*
		 * GET the specific user status based on position from our array of
		 * hashmaps. This will give a list of all our users statuses
		 */
		mTweetMap = getItem(position);

		 View view = null;

		//view might be recycled we check here
		if (convertView != null) {
			view = convertView;
			viewHolder = (Holder) view.getTag();
		}
		/*
		 * otherwise view is freshly created, lets store our info into the
		 * viewHolder object. We have to check if viewholder is still null as the 3rd party library we use called
		 * https://github.com/umano/MultiItemRowListAdapter. if the row item is not even it puts a blank view in its place
		 * so then a recycled view could indeed have no ViewHolder still. 
		 * 
		 */
		if(viewHolder==null) {

			viewHolder = new Holder();
			view = mInflater.inflate(R.layout.list_item_child, parent, false);
			//alloc our views to load with textual data
			viewHolder.tv_message = (TextView) view
					.findViewById(R.id.tv_tweet_status);
			viewHolder.tv_author = (TextView) view.findViewById(R.id.tv_name);
			viewHolder.iv_avatar = (ImageView) view
					.findViewById(R.id.iv_avatar);
			viewHolder.tv_time = (TextView) view.findViewById(R.id.tv_time);

			/* save tweet specific info for later use inside of the views tag */

			viewHolder.id = mTweetMap.get(Consts.KEY_USERID);
			viewHolder.screen_name = mTweetMap.get(Consts.KEY_SCREEN_NAME);
			viewHolder.adViewContainer = ((LinearLayout) view
					.findViewById(R.id.container_adview));

			view.setTag(viewHolder);
		}

		
		/*
		 * here we check if we are in landscape mode give another look to the
		 * app for all odd positions, we could have also done this by inflating
		 * an odd and even list child view
		 */
		if (isOrientationLandscape )
			if (position % 2 == 0 ) {

				view.setBackgroundResource(R.color.pink);
				viewHolder.tv_author.setTextColor(context.getResources()
						.getColorStateList(R.color.white));

			} else {
				view.setBackgroundResource(R.color.white);
				viewHolder.tv_author.setTextColor(context.getResources()
						.getColorStateList(R.color.blue));
			}

		author = (String) mTweetMap.get(Consts.KEY_AUTHOR);
		tweet = (String) mTweetMap.get(Consts.KEY_TWEET_MSG);
		avatarURL = (String) mTweetMap.get(Consts.KEY_AVATAR);

		//DownloadImageTask.getInstance(context).loadBitmap(avatarURL, iv_avatar);
		if (imageDiskCache != null)
			imageDiskCache.getBitmap(avatarURL, viewHolder.iv_avatar);

		viewHolder.tv_author.setText(author);
		viewHolder.tv_message.setText(tweet);
		viewHolder.tv_message.setTag(viewHolder);//we store the user object for linkifier class to open web page on click

		/* make any links highilighted and clickable */
		linkifier.setLinks(viewHolder.tv_message, tweet,
				Long.parseLong(mTweetMap.get(Consts.KEY_USERID)));

		String time = formatDate(new Date(
				mTweetMap.get(Consts.KEY_CREATED_DATE)));
		viewHolder.tv_time.setText(time);

		/*
		 * here we could add the adview back (if its a recycled view) but we
		 * leave it. It makes the ads disappear after a few scrolls so not to
		 * affect the users experience too much with ads
		 */

		if (position % 2 != 0)
			if (viewHolder.adViewContainer != null)
				viewHolder.adViewContainer.removeAllViews();

		return view;

	}

	/* returns orientation of device */
	private int getRotation(Context context) {
		final int rotation = ((WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
				.getRotation();
		switch (rotation) {
		case Surface.ROTATION_0:
			return Configuration.ORIENTATION_PORTRAIT;
		case Surface.ROTATION_90:
			return Configuration.ORIENTATION_LANDSCAPE;
		case Surface.ROTATION_180:
			return Configuration.ORIENTATION_PORTRAIT;
		default:
			return Configuration.ORIENTATION_LANDSCAPE;
		}
	}

	/*
	 * converts standard date to user readable date such as 5m ago, 30 mins ago,
	 * 1 hr ago etc
	 */
	private String formatDate(Date create_date) {
		//twitter date format from json response:  Wed Jul 31 13:15:10 EDT 2013

		if (create_date == null)
			return "";

		return mConverter.toTimeSpanString(create_date);

	}

	/*
	 * more efficient to have a holder container our info so if the view is a
	 * recycled one the info can be obtained quickly as inflating or finding
	 * views has over head.
	 */
	protected static class Holder {
		String id;
		String screen_name;
		TextView tv_author, tv_message, tv_time;;
		ImageView iv_avatar;
		LinearLayout adViewContainer;
	}

}
