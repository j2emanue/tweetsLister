package org.jefferyemanuel.database;
import java.util.Arrays;
import java.util.HashSet;

import org.jefferyemanuel.willowtweetapp.Consts;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;


public class TweeterContentProvider extends ContentProvider {

		  // database
		  private TweeterUserDatabaseHelper database;

		  // Used for the UriMacher
		  private static final int TOTAL_ELEMENTS = 10;
		  private static final int ELEMENT_ID = 20;
		
		 
		private static final String BASE_PATH = "tweeters";
		  public static final Uri CONTENT_URI = Uri.parse("content://" + Consts.AUTHORITY
		      + "/" + BASE_PATH);

		  public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
		      + "/tweeter";
		  public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
		      + Consts.TABLE_TWEETER_INFO;

		  private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		  static {
		    sURIMatcher.addURI(Consts.AUTHORITY, BASE_PATH, TOTAL_ELEMENTS);
		    sURIMatcher.addURI(Consts.AUTHORITY, BASE_PATH + "/#", ELEMENT_ID);
		  }

		 
		  
		  @Override
		  public boolean onCreate() {
		    database = new TweeterUserDatabaseHelper(getContext());
		    return false;
		  }

		  

		  @Override
		  public String getType(Uri uri) {
		    return null;
		  }

		  
		  @Override
		  public Cursor query(Uri uri, String[] projection, String selection,
		      String[] selectionArgs, String sortOrder) {

		    // UsIng SQLiteQueryBuilder instead of query() method
		    SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		    // Check if the caller has requested a column which does not exists
		    checkColumns(projection);

		    // Set the table
		    queryBuilder.setTables(Consts.TABLE_TWEETER_INFO);

		    int uriType = sURIMatcher.match(uri);
		    switch (uriType) {
		    case TOTAL_ELEMENTS:
		      break;
		    case ELEMENT_ID:
		      // Adding the ID to the original query
		      queryBuilder.appendWhere(Consts.COLUMN_ID + "="
		          + uri.getLastPathSegment());
		      break;
		    default:
		      throw new IllegalArgumentException("Unknown URI: " + uri);
		    }

		    SQLiteDatabase db = database.getWritableDatabase();
		    Cursor cursor = queryBuilder.query(db, projection, selection,
		        selectionArgs, null, null, sortOrder);
		    // Make sure that potential listeners are getting notified
		    cursor.setNotificationUri(getContext().getContentResolver(), uri);

		    return cursor;
		  }

		  @Override
		  public Uri insert(Uri uri, ContentValues values) {
		    int uriType = sURIMatcher.match(uri);
		    SQLiteDatabase sqlDB = database.getWritableDatabase();
		  
		    int rowsDeleted = 0;
		    long id = 0;
		    switch (uriType) {
		    case TOTAL_ELEMENTS:
		      id = sqlDB.insert(Consts.TABLE_TWEETER_INFO, null, values);
		      break;
		    default:
		      throw new IllegalArgumentException("Unknown URI: " + uri);
		    }
		    getContext().getContentResolver().notifyChange(uri, null);
		    return Uri.parse(BASE_PATH + "/" + id);
		  }

		  @Override
		  public int delete(Uri uri, String selection, String[] selectionArgs) {
		    int uriType = sURIMatcher.match(uri);
		    SQLiteDatabase sqlDB = database.getWritableDatabase();
		    int rowsDeleted = 0;
		    switch (uriType) {
		    case TOTAL_ELEMENTS:
		      rowsDeleted = sqlDB.delete(Consts.TABLE_TWEETER_INFO, selection,
		          selectionArgs);
		      break;
		    case ELEMENT_ID:
		      String id = uri.getLastPathSegment();
		      if (TextUtils.isEmpty(selection)) {
		        rowsDeleted = sqlDB.delete(Consts.TABLE_TWEETER_INFO,
		            Consts.COLUMN_ID + "=" + id, 
		            null);
		      } else {
		        rowsDeleted = sqlDB.delete(Consts.TABLE_TWEETER_INFO,
		            Consts.COLUMN_ID + "=" + id 
		            + " and " + selection,
		            selectionArgs);
		      }
		      break;
		    default:
		      throw new IllegalArgumentException("Unknown URI: " + uri);
		    }
		    /*lets not notify content observers on deletes of less then 1 as each delete would cause a network call.
		     * user could delete multiple entries at once. if the deletes are greater then 1 then its probably a 
		     * request to remove the entire list, this we will allow*/
		    //if(rowsDeleted>1)
		    	getContext().getContentResolver().notifyChange(uri, null);
		    return rowsDeleted;
		  }

		  @Override
		  public int update(Uri uri, ContentValues values, String selection,
		      String[] selectionArgs) {

		    int uriType = sURIMatcher.match(uri);
		    SQLiteDatabase sqlDB = database.getWritableDatabase();
		    int rowsUpdated = 0;
		    switch (uriType) {
		    case TOTAL_ELEMENTS:
		      rowsUpdated = sqlDB.update(Consts.TABLE_TWEETER_INFO, 
		          values, 
		          selection,
		          selectionArgs);
		      break;
		    case ELEMENT_ID:
		      String id = uri.getLastPathSegment();
		      if (TextUtils.isEmpty(selection)) {
		        rowsUpdated = sqlDB.update(Consts.TABLE_TWEETER_INFO, 
		            values,
		            Consts.COLUMN_ID + "=" + id, 
		            null);
		      } else {
		        rowsUpdated = sqlDB.update(Consts.TABLE_TWEETER_INFO, 
		            values,
		            Consts.COLUMN_ID + "=" + id 
		            + " and " 
		            + selection,
		            selectionArgs);
		      }
		      break;
		    default:
		      throw new IllegalArgumentException("Unknown URI: " + uri);
		    }
		    getContext().getContentResolver().notifyChange(uri, null);
		    return rowsUpdated;
		  }

		  private void checkColumns(String[] projection) {
		    String[] available = { 
		        Consts.COLUMN_USER,
		        Consts.COLUMN_ID}
		       ;
		    if (projection != null) {
		      HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
		      HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
		      // Check if all columns which are requested are available
		      if (!availableColumns.containsAll(requestedColumns)) {
		        throw new IllegalArgumentException("Unknown columns in projection");
		      }
		    }
		  }
}
