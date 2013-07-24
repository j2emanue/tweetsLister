package org.jefferyemanuel.willowtweetapp;


/**
 * This class holds global constants that are used throughout the application
 * to support in-app billing.
 */


public class Consts {
	
	// lock the class from being instantiated as this is only a place holder for constants
	private Consts() {

	}
	
	//developer and logging options
    public static final boolean DEVELOPER_MODE =true;
    public static final String TAG="willowtweet";
    public static final String TAG_FRAGMENT="fragments";
			
    //MAP keys  (for json twitter object)
    public static final String KEY_USER_OBJECT="user";
    public static final String KEY_TIMEPOSTED="timePosted";
    public static final String KEY_TWEETDATE="tweetDate";
    public static final String KEY_AUTHOR="author";
    public static final String KEY_AVATAR="avatar";
    public static final String KEY_TWEED_ID="tweetId";
    public static final String KEY_TWEET_COUNT="tweetCount";
    public static final String KEY_TWEET_MSG="tweet";
    public static final String KEY_IS_RETWEET="isRetweet";
    public static final String KEY_FOLLOWERS="followers";
    public static final String KEY_FOLLOWING="following";
    
 
    
    //Twitter authentication oAUTH keys
    //TODO use a webservice call to get this key never leave it in local code base
    public static final String CONSUMER_KEY="eNLyfXOWo0rGODaI2KR0dQ"; // 
    public static final String CONSUMER_SECRET_KEY="FndHyAQryGeWKYPjmU92sv2F54xrLiOznK6eEtmS8";
    public static final String ACCESS_TOKEN="420386498-55vIULyl5xXP2FMpxc7GVv9GGqSolk7aLWm18K0o";
    public static final String ACCESS_TOKEN_SECRET="BRLfWdt5nqSmfn9BteAVrOA5BOXHxvUcq3uwiiXEOr0";
    public static final String CALLBACKURL="app://twitter";
    public static final String URL_REQUEST_TOKEN="https://twitter.com/oauth/request_token";
    public static final String URL_ACCESS_TOKEN= "https://twitter.com/oauth/access_token";
    public static final String URL_AUTHORIZE= "https://twitter.com/oauth/authorize";
    
    //configuration constaants
    public static final int NUMBER_OF_STATUSES=20;
    	  
}
