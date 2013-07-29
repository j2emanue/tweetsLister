package org.jefferyemanuel.database;

import org.jefferyemanuel.willowtweetapp.Consts;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TweeterTable {

	
	 

	  // Database creation SQL statement
	  private static final String DATABASE_CREATE = "create table " 
	      + Consts.TABLE_TWEETER_INFO
	      + "(" 
	      + Consts.COLUMN_ID + " integer primary key autoincrement, " 
	      + Consts.COLUMN_USER + "  VARCHAR(255) not null,UNIQUE ("
	      + Consts.COLUMN_USER+")"
	      + ");";

	  public static void onCreate(SQLiteDatabase database) {
	    database.execSQL(DATABASE_CREATE);
	

	  
	  }

	  public static void onUpgrade(SQLiteDatabase database, int oldVersion,
	      int newVersion) {
	    Log.w(TweeterTable.class.getName(), "Upgrading database from version "
	        + oldVersion + " to " + newVersion
	        + ", which will destroy all old data");
	    database.execSQL("DROP TABLE IF EXISTS " + Consts.TABLE_TWEETER_INFO);
	    onCreate(database);
	  }
	
}
