package org.jefferyemanuel.willowtweetapp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import twitter4j.util.TimeSpanConverter;
import twitter4j.User;
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

	public TweeterListAdapter(FragmentActivity activity,
			int textViewResourceId, ArrayList<HashMap<String, Object>> objects,
			DiskLruImageCache imageDiskCache) {

		this.context = activity;
		mTweeterInfo = objects;
		this.imageDiskCache = imageDiskCache;
		linkifier=new Linkifier();
		 mConverter = new TimeSpanConverter();
		}

	private ArrayList<HashMap<String, Object>> mTweeterInfo;
	private FragmentActivity context;
	private DiskLruImageCache imageDiskCache;
	private Linkifier linkifier;
	private TimeSpanConverter mConverter;	
	
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
		TextView tv_time=(TextView)view.findViewById(R.id.tv_time);
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
		User user=(User)tweetMap.get(Consts.KEY_USER_OBJECT);//twitter object container tweeters info
		
		tv_author.setText(author);
		tv_message.setText(tweet);
		tv_message.setTag(user);//we store the user object for linkifier class to open web page on click
		linkifier.setLinks(tv_message, tweet,user.getId());

		

	     String time=formatDate((Date)tweetMap.get(Consts.KEY_CREATED_DATE));
		tv_time.setText(time);
	     
		
		
		view.setTag(tweetMap.get(Consts.KEY_USER_OBJECT));
		
		
     if(position%2!=0)
		/* here we could add the adview back (if its a recycled view) but we leave it. It makes the ads disappear after a 
		 * few scrolls so not to affect the users experience too much with ads*/	
		((LinearLayout)view.findViewById(R.id.container_adview)).removeAllViews();
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
	
/*converts standard date to user readable date such as 5m ago, 30 mins ago, 1 hr ago etc*/
private String formatDate(Date create_date){
	//twitter date format from json response:  Wed Jul 31 13:15:10 EDT 2013

	if(create_date==null)
		return "";
	 
	return mConverter.toTimeSpanString(create_date);
	
}

}
