package org.jefferyemanuel.willowtweetapp;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;

public class ProgressDialogFragment extends DialogFragment {

	private static final String ARG_MESSAGE = "message";
	private ProgressDialog dialog;
	public static ProgressDialogFragment newInstance(String msg) {
	
		
		Bundle args = new Bundle();
		args.putString(ProgressDialogFragment.ARG_MESSAGE, msg);			
		ProgressDialogFragment frag = new ProgressDialogFragment ();
		frag.setArguments(args);		
		return frag;
	}

	
	/*dialog kept disappearing on orientation change.  might be a bug in v4 support.
	 * http://stackoverflow.com/questions/12433397/android-dialogfragment-disappears-after-orientation-change
	 * */
	@Override
	 public void onDestroyView() {
	     if (getDialog() != null && getRetainInstance())
	         getDialog().setDismissMessage(null);
	         super.onDestroyView();
	 }
	
	  @Override
		public void onCreate(Bundle savedInstanceState) {
			// TODO Auto-generated method stub
			super.onCreate(savedInstanceState);
			setRetainInstance(false);
	    }

	  public void setProgressOfDialog(int value){
		  
		  if(dialog!=null)
			  dialog.setProgress(value);
	  }
	  
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		dialog = new ProgressDialog(getActivity());
		
		Bundle args = this.getArguments();
		String msg = args.getString(ARG_MESSAGE);
		
		dialog.setMessage(msg);
		dialog.setIndeterminate(false);
		dialog.setCancelable(false);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		// Disable the back button
		OnKeyListener keyListener = new OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				
				if( keyCode == KeyEvent.KEYCODE_BACK){					
					return true;
				}
				return false;
			}

		
		};
		dialog.setOnKeyListener(keyListener);
		return dialog;
	}

}