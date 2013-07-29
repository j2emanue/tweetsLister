package org.jefferyemanuel.database;



import org.jefferyemanuel.willowtweetapp.Consts;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TweeterUserDatabaseHelper extends SQLiteOpenHelper {


	  public TweeterUserDatabaseHelper(Context context) {
	    super(context, Consts.DATABASE_NAME, null, Consts.DATABASE_VERSION);
	  }

	  // Method is called during creation of the database
	  @Override
	  public void onCreate(SQLiteDatabase database) {
	    TweeterTable.onCreate(database);
	  }

	  // Method is called during an upgrade of the database,
	  // e.g. if you increase the database version
	  @Override
	  public void onUpgrade(SQLiteDatabase database, int oldVersion,
	      int newVersion) {
	    TweeterTable.onUpgrade(database, oldVersion, newVersion);
	  }
	}
	 
