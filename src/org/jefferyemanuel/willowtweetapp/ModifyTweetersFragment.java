package org.jefferyemanuel.willowtweetapp;

import static org.jefferyemanuel.willowtweetapp.Utils.createToast;
import static org.jefferyemanuel.willowtweetapp.Utils.hideKeyboard;
import static org.jefferyemanuel.willowtweetapp.Utils.printLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jefferyemanuel.listeners.TweeterSelectedListener;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class ModifyTweetersFragment extends Fragment implements
		OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

	SoundPool mSoundPool;
	boolean sound_loaded;
	int soundID;
	SimpleCursorAdapter mDataAdapter;
	ListView tweeterList;
	TweeterSelectedListener mTweeterSelectedCallback;
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		hideKeyboard(getActivity());
		super.onResume();
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Utils.hideKeyboard(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		if (container == null) {
			// We have different layouts, and in one of them this
			// fragment's containing frame doesn't exist.  The fragment
			// may still be created from its saved state, but there is
			// no reason to try to create its view hierarchy because it
			// won't be displayed.  Note this is not needed -- we could
			// just run the code below, where we would create and return
			// the view hierarchy; it would just never be used.
			return null;
		}

		View rootView = inflater.inflate(R.layout.modifytweeters_layout, null);

		Button addNewUser = (Button) rootView
				.findViewById(R.id.btn_add_newuser);
		addNewUser.setOnClickListener(this);

		tweeterList = (ListView) rootView.findViewById(android.R.id.list);
		displayListView(rootView);
		enableHomeButton();

		return rootView;
	}

	
	
	@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
	
		try {
			mTweeterSelectedCallback = (TweeterSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement TweeterSelectedListener Listener");
		}
	
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);

		getLoaderManager().initLoader(Consts.LOADER_ID, null, this);

		/* load our sound pool on first init into RAM */
		mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		soundID = mSoundPool.load(getActivity(), R.raw.logosound, 1);
		mSoundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId,
					int status) {
				sound_loaded = true;

			}
		});
	}

	/*
	 * pre api 14 home button is automatically enaabled. Moving forwared we have
	 * to manually enable the button
	 */
	@TargetApi(14)
	public void enableHomeButton() {
		if (android.os.Build.VERSION.SDK_INT >= 14) {
			getActivity().getActionBar().setHomeButtonEnabled(true);
		}
	}

	public void deleteAllTweetListEntries() {

		//String where = Consts.COLUMN_USER + " = *";

		int numrows = getActivity().getContentResolver().delete(
				Consts.CONTENT_URI, null, null);
		if (numrows > 0)
			createToast(getActivity(), getString(R.string.warning_item_deleted));

	}

	private void displayListView(View rootView) {

		// The desired columns to be bound
		String[] from = new String[] { Consts.COLUMN_USER };

		// the XML defined views which the data will be bound to
		int[] to = new int[] { R.id.username };

		// create an adapter from the SimpleCursorAdapter
		mDataAdapter = new SimpleCursorAdapter(getActivity(),
				R.layout.tweeter_list_row_item, null, from, to, 0);

		// get reference to the ListView
		ListView listView = (ListView) rootView.findViewById(android.R.id.list);
		// Assign adapter to ListView
		listView.setAdapter(mDataAdapter);
		//Ensures a loader is initialized and active.
		getActivity().getSupportLoaderManager().initLoader(0, null, this);

		
		
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> listView, View view, int position,
					long arg3) {
				
				TextView username_tv=(TextView)view.findViewById(R.id.username);
				mTweeterSelectedCallback.selectListItem(username_tv.getText().toString());
			}
		});
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> listView, View view,
					int position, long id) {
				playSound(soundID);

				TextView userName_tv = (TextView) view
						.findViewById(R.id.username);
				String username = userName_tv.getText().toString();

				String where = Consts.COLUMN_USER + " = ?";

				int numrows = getActivity().getContentResolver().delete(
						Consts.CONTENT_URI, where, new String[] { username });
				if (numrows > 0)
					createToast(getActivity(),
							getString(R.string.warning_item_deleted));
				return true;
			}
		}
		);

	}

	public void playSound(int soundID) {

		AudioManager audioManager = (AudioManager) getActivity()
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

		return new CursorLoader(getActivity(), Consts.CONTENT_URI, null, null,
				null, Consts.COLUMN_USER + " COLLATE NOCASE ASC");

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
			EditText userName = (EditText) this.getView().findViewById(
					R.id.et_additional_username);

			if (userName.getText().toString().trim().equalsIgnoreCase("")) {
				createToast(v.getContext(),
						getString(R.string.warning_empty_addition));
				break;
			}

			ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

			List<String> userNamesBulkList = Arrays.asList(userName.getText()
					.toString().split("\\s*,\\s*"));
			for (String user : userNamesBulkList) {
				operations.add(ContentProviderOperation
						.newInsert(Consts.CONTENT_URI)
						.withValue(Consts.COLUMN_USER, user.toString())
						.withYieldAllowed(true).build());
			}

			try {
				getActivity().getContentResolver().applyBatch(Consts.AUTHORITY,
						operations);
				userName.setText("");
			} catch (RemoteException e) {
			} catch (OperationApplicationException e) {
				createToast(getActivity(), "Failed to Add item(s)");
				if (Consts.DEVELOPER_MODE)
					printLog(Consts.TAG, e.toString());
			}

			/*
			 * ContentValues values = new ContentValues();
			 * values.put(Consts.COLUMN_USER, userName.getText().toString());
			 * 
			 * Uri result = getContentResolver() .insert(Consts.CONTENT_URI,
			 * values);
			 * 
			 * long newID = ContentUris.parseId(result); if (newID < 0)
			 * createToast
			 * (getString(R.string.warning_item_inserted_previously));
			 */break;
		}
	}

}
