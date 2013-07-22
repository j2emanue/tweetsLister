package org.jefferyemanuel.willowtweetapp;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TweeterJSONAdapter extends BaseAdapter {

	public TweeterJSONAdapter(FragmentActivity activity, int textViewResourceId,
			ArrayList<HashMap<String, Object>> objects) {

		this.context = activity;
		mTweeterInfo = objects;
		// TODO Auto-generated constructor stub
	}

	private ArrayList<HashMap<String, Object>> mTweeterInfo;
	private FragmentActivity context;

	/*
	 * public TweeterJSONAdapter (Context context){
	 * 
	 * this.context=context; }
	 */
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

		author = (String) tweetMap.get(Consts.KEY_AUTHOR);
		tweet = (String) tweetMap.get(Consts.KEY_TWEET_MSG);
		avatarURL = (String) tweetMap.get(Consts.KEY_AVATAR);

		/* retrieve the actual http images off the main Thread */
		//new DownloadImageTask(iv_avatar).execute(avatarURL);
		//new DownloadImageTask().fetchDrawableOnThread(avatarURL, iv_avatar);
		
		
		DownloadImageTask.getInstance(context).loadBitmap(avatarURL, iv_avatar);
		
		tv_author.setText(author);
		tv_message.setText(tweet);

		view.setTag(tweetMap.get(Consts.KEY_USER_OBJECT));
		

		return view;

	}

}
