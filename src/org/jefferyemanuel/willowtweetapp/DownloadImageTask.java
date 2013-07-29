package org.jefferyemanuel.willowtweetapp;

import java.io.InputStream;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

/* an abstraction of a LRUCache system.  Stores images in RAM once they are initially loaded from web stream.
 * Made into a singleton to be used through the application*/
public class DownloadImageTask {

	private LruCache<String, Bitmap> mMemoryCache;
	/* create a singleton class to call this from multiple classes */

	private static DownloadImageTask instance = null;

	public static DownloadImageTask getInstance(Activity activity) {
		if (instance == null) {
			instance = new DownloadImageTask(activity);

		}

		return instance;
	}

	//lock the constructor from public instances
	private DownloadImageTask(Activity activity) {
		// Get max available VM memory, exceeding this amount will throw an
		// OutOfMemory exception. Stored in kilobytes as LruCache takes an
		// int in its constructor.
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

		// Use 1/8th of the available memory for this memory cache.
		final int cacheSize = maxMemory / 8;

		//------
		RetainFragment mRetainFragment = RetainFragment
				.findOrCreateRetainFragment(activity
						.getFragmentManager());

		//save the cache incase Activity orientation changes

		mMemoryCache = RetainFragment.mRetainedCache;

		if (mMemoryCache == null) { 
			/* we dont have the cache , lets create one */
			//initialize our cache here
			mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
				@Override
				protected int sizeOf(String key, Bitmap bitmap) {
					// The cache size will be measured in kilobytes rather than
					// number of items.
					return bitmap.getByteCount() / 1024;
				}
			};

			/*
			 * definetly have a cache now, lets save it to our RETAINED fragment
			 * incase of oriengation change on Android
			 */
			mRetainFragment.mRetainedCache = mMemoryCache;
		}
		//------

	}

	/*
	 * accessors to this class should call this method solely to either add
	 * image to teh catch or spawn a child thread to fetch the http image
	 */
	public void loadBitmap(String avatarURL, ImageView imageView) {
		final String imageKey = String.valueOf(avatarURL);

		final Bitmap bitmap = getBitmapFromMemCache(imageKey);
		if (bitmap != null) {
			imageView.setImageBitmap(bitmap);
			imageView.requestLayout();
			imageView.invalidate();
		} else {
			imageView.setImageResource(R.drawable.spinning_loader);

			new DownloadImageTaskViaWeb(imageView).execute(avatarURL);
		}
	}

	private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		if (getBitmapFromMemCache(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	private Bitmap getBitmapFromMemCache(String key) {
		return mMemoryCache.get(key);
	}

	/* a background process that opens a http stream and decodes a web image. */

	class DownloadImageTaskViaWeb extends AsyncTask<String, Void, Bitmap> {
		ImageView bmImage;

		public DownloadImageTaskViaWeb(ImageView bmImage) {
			this.bmImage = bmImage;
		}

		protected Bitmap doInBackground(String... urls) {

			String urldisplay = urls[0];
			Bitmap mIcon = null;
			try {
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon = BitmapFactory.decodeStream(in);

			} catch (Exception e) {
				Log.e("Error", e.getMessage());
				e.printStackTrace();
			}

			addBitmapToMemoryCache(String.valueOf(urldisplay), mIcon);

			return mIcon;
		}

		/* after decoding we update the view on the mainUI */
		protected void onPostExecute(Bitmap result) {
			bmImage.setImageBitmap(result);
			bmImage.requestLayout();
			bmImage.invalidate();

		}

	}

}
