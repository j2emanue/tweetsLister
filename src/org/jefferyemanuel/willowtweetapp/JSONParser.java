package org.jefferyemanuel.willowtweetapp;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/* this METHOD IS CURRENTLY NOT USED AS USING TWITTER4J.  kept incase we need oauth or twitter4j breaks we can parse json.
 * twitter requires oauth to make restful calls  we can ignore this class*/

public class JSONParser {

	static InputStream is = null;
	static JSONObject jObj = null;
	static String json = "";

	CommonsHttpOAuthProvider httpOauthprovider;
	CommonsHttpOAuthConsumer httpOauthConsumer;
	String UserKey, UserKeySecret;

	// constructor
	public JSONParser(CommonsHttpOAuthProvider httpOauthprovider,
			CommonsHttpOAuthConsumer httpOauthConsumer, String UserKey,
			String UserKeySecret) {
		this.httpOauthprovider = httpOauthprovider;
		this.httpOauthConsumer = httpOauthConsumer;
		this.UserKey = UserKey;
		this.UserKeySecret = UserKeySecret;
	}

	public JSONArray getJSONFromUrl(String url)
			throws OAuthMessageSignerException,
			OAuthExpectationFailedException, OAuthCommunicationException {

		JSONArray array = null;
		HttpGet get = new HttpGet(url);
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setUseExpectContinue(params, false);
		get.setParams(params);

		try {

			httpOauthConsumer.setTokenWithSecret(UserKey, UserKeySecret);
			httpOauthConsumer.sign(get);

			DefaultHttpClient client = new DefaultHttpClient();
			String response = client.execute(get, new BasicResponseHandler());
			array = new JSONArray(response);
			// JSONObject j=new JSONObject(response);
			if (Consts.DEVELOPER_MODE)
				Log.v(Consts.TAG,  array.toString());

			// String test=array.get)

		} catch (Exception e) {
			// handle this somehow
			Log.e(Consts.TAG, e.toString());

		}
		// return JSON String
		return array;

	}

	public void parseJson(JSONArray jsonArray) throws JSONException {

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject obj = (JSONObject) jsonArray.get(i);
			String message = obj.getString("text");

			JSONObject userObj = obj.getJSONObject("user");
			String profileImageURL = userObj
					.getString("profile_image_url_https");

			Log.v("jeff", profileImageURL);

		}
	}

}
