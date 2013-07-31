package org.jefferyemanuel.willowtweetapp;

import java.util.ArrayList;
import java.util.HashMap;

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

public class TweeterJSONAdapter extends BaseAdapter {

	public TweeterJSONAdapter(FragmentActivity activity,
			int textViewResourceId, ArrayList<HashMap<String, Object>> objects,
			DiskLruImageCache imageDiskCache) {

		this.context = activity;
		mTweeterInfo = objects;
		this.imageDiskCache = imageDiskCache;
		// TODO Auto-generated constructor stub
	}

	private ArrayList<HashMap<String, Object>> mTweeterInfo;
	private FragmentActivity context;
	private DiskLruImageCache imageDiskCache;

	@Override
	public int getCount() {
		return mTweeterInfo.size();
	}

	@Override
	public HashMap<String, Object> getItem(int position) {
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

		String author = null, tweet = null, avatarURL = null;

		/*
		 * GET the specific user status based on position from our array of
		 * hashmaps. This will give a list of all our users statuses
		 */
		HashMap<String, Object> tweetMap = getItem(position);

		final View view;

		//view might be recycled we check here
		if (convertView != null) {
			view = convertView;

		} else {

			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.list_item_child, parent, false);
		}

		//alloc our views to load with textual data
		TextView tv_message = (TextView) view
				.findViewById(R.id.tv_tweet_status);
		TextView tv_author = (TextView) view.findViewById(R.id.tv_name);
		ImageView iv_avatar = (ImageView) view.findViewById(R.id.iv_avatar);

		/*
		 * here we check if we are in landscape mode give another look to the
		 * app for all odd positions, we could have also done this by inflating
		 * an odd and even list child view
		 */
		if (getRotation(context) == Configuration.ORIENTATION_LANDSCAPE)
			if (position % 2 == 0) {
				
				view.setBackgroundResource(R.color.pink);
				tv_author.setTextColor(context.getResources()
						.getColorStateList(R.color.white));

			} else {
				view.setBackgroundResource(R.color.white);
				tv_author.setTextColor(context.getResources()
						.getColorStateList(R.color.blue));
			}

		author = (String) tweetMap.get(Consts.KEY_AUTHOR);
		tweet = (String) tweetMap.get(Consts.KEY_TWEET_MSG);
		avatarURL = (String) tweetMap.get(Consts.KEY_AVATAR);

		//DownloadImageTask.getInstance(context).loadBitmap(avatarURL, iv_avatar);
		imageDiskCache.getBitmap(avatarURL, iv_avatar);

		tv_author.setText(author);
		tv_message.setText(tweet);

		view.setTag(tweetMap.get(Consts.KEY_USER_OBJECT));

	if(position%2!=0)
		/* here we could add the adview back (if its a recycled view) but we leave it. It makes the ads disappear after a 
		 * few scrolls so not to affect the users experience too much with ads*/	
		((LinearLayout)view.findViewById(R.id.container_adview)).removeAllViews();

	
		return view;

	}

	/* returns orientation of device */
	public int getRotation(Context context) {
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

	
	/* load ads into already inflated linear layouts */
	public void setAdvertisment(View parent) {
		// set up our advertisment
		/*String admob_publisherID = Consts.admob_publisherID;

		int[] idArray = {  R.id.adthree 
																 ,
																 * R.id.adfour,
																 * R.id.adfive,
																 * R.id.adsix
																 }; //add your ads from the xml here and thats it, all done
		int numberOfAds = idArray.length;

		// Create the adView and layout arrays
		AdRequest AD = new AdRequest();
		AdView[] adViews = new AdView[numberOfAds];
		LinearLayout[] adlayouts = new LinearLayout[numberOfAds];

		for (int i = 0; i < numberOfAds; i++) {
			adViews[i] = new AdView(context, AdSize.BANNER, admob_publisherID);
			adlayouts[i] = (LinearLayout) parent.findViewById(idArray[i]);
			adlayouts[i].addView(adViews[i]);
			adViews[i].loadAd(AD);
		}
		*/
	}
}
