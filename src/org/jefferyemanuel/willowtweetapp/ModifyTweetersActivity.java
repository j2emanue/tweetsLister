package org.jefferyemanuel.willowtweetapp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import twitter4j.conf.ConfigurationBuilder;

import android.app.Activity;
import static org.jefferyemanuel.willowtweetapp.Utils.*;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ModifyTweetersActivity extends FragmentActivity implements
		OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

	SoundPool mSoundPool;
	boolean sound_loaded;
	int soundID;
	SimpleCursorAdapter mDataAdapter;
	ListView tweeterList;

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		hideKeyboard(this);
		super.onResume();
	}

	
	
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Utils.hideKeyboard(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.modifytweeters_layout);

		tweeterList = (ListView) findViewById(android.R.id.list);

		getActionBar().setHomeButtonEnabled(true);
		
		displayListView();
		getSupportLoaderManager().initLoader(Consts.LOADER_ID, null, this);

		/* load our sound pool on first init into RAM */
		mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		soundID = mSoundPool.load(this, R.raw.logosound, 1);
		mSoundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId,
					int status) {
				sound_loaded = true;

			}
		});

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.tweeterlist_menu, menu);
		return true;
	}

	
	
	public boolean onOptionsItemSelected(MenuItem item){
	    switch(item.getItemId()){
	    
	    case R.id.menu_load_tweeter_btn:
	    	Intent i=new Intent(this,MainActivity.class);
	    	i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(i);
			overridePendingTransition(R.anim.slide_out, R.anim.slide);
			finish();
	    
	    case android.R.id.home:
	    	onBackPressed();
	    	break;
			
	    case R.id.action_settings:
				break;
			
	    default:
			  return super.onOptionsItemSelected(item);
	    }
        return super.onOptionsItemSelected(item);
	    
	}

	
	private void displayListView() {

		// The desired columns to be bound
		String[] from = new String[] { Consts.COLUMN_USER };

		// the XML defined views which the data will be bound to
		int[] to = new int[] { R.id.username };

		// create an adapter from the SimpleCursorAdapter
		mDataAdapter = new SimpleCursorAdapter(this,
				R.layout.tweeter_list_row_item, null, from, to, 0);

		// get reference to the ListView
		ListView listView = (ListView) findViewById(android.R.id.list);
		// Assign adapter to ListView
		listView.setAdapter(mDataAdapter);
		//Ensures a loader is initialized and active.
		getSupportLoaderManager().initLoader(0, null, this);

		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> listView, View view,
					int position, long id) {
				playSound(soundID);

				TextView userName_tv = (TextView) view
						.findViewById(R.id.username);
				String username = userName_tv.getText().toString();

				String where = Consts.COLUMN_USER + " = ?";

				int numrows = getContentResolver().delete(Consts.CONTENT_URI,
						where, new String[] { username });
				if (numrows > 0)
					createToast(ModifyTweetersActivity.this,getString(R.string.warning_item_deleted));
			}
		});

	}

	public void playSound(int soundID) {

		AudioManager audioManager = (AudioManager) this
				.getSystemService(Context.AUDIO_SERVICE);
		float actualVolume = (float) audioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		float maxVolume = (float) audioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		float volume = actualVolume / maxVolume;

		if (sound_loaded) {
			mSoundPool.play(soundID, volume, 0, 0, 0, 1f);

		}

	}

	

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		// TODO Auto-generated method stub

		return new CursorLoader(this, Consts.CONTENT_URI, null, null, null,
				Consts.COLUMN_USER + " ASC");

	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		// TODO Auto-generated method stub

		/*
		 * Swap the new cursor in. (The framework will take care of closing the
		 * old cursor once we return.)
		 */
		if (mDataAdapter != null) {
			mDataAdapter.swapCursor(cursor);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

		switch (v.getId()) {

		case R.id.btn_add_newuser:
			EditText userName = (EditText) findViewById(R.id.et_additional_username);

			if (userName.getText().toString().trim().equalsIgnoreCase("")) {
				createToast(v.getContext(),getString(R.string.warning_nothingEntered));
				break;
			}
			
			ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

			List<String> userNamesBulkList = Arrays.asList(userName.getText().toString().split("\\s*,\\s*"));
			for(String user:userNamesBulkList)
			{
				operations.add(
						   ContentProviderOperation.newInsert(Consts.CONTENT_URI)
						       .withValue(Consts.COLUMN_USER, user.toString())
						       .withYieldAllowed(true)
						       .build());			
			}
			
			try {
				   getContentResolver().
				      applyBatch(Consts.AUTHORITY, operations);
				   userName.setText("");
				} catch (RemoteException e) {
				} catch (OperationApplicationException e) {
				createToast(ModifyTweetersActivity.this,"Failed to Add item(s)");
				if(Consts.DEVELOPER_MODE)
					printLog(Consts.TAG,e.toString());
				}

			/*
			ContentValues values = new ContentValues();
			values.put(Consts.COLUMN_USER, userName.getText().toString());

			Uri result = getContentResolver()
					.insert(Consts.CONTENT_URI, values);

			long newID = ContentUris.parseId(result);
			if (newID < 0)
				createToast(getString(R.string.warning_item_inserted_previously));
*/			break;
		}
	}

}
