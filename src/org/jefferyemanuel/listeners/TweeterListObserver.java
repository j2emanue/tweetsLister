package org.jefferyemanuel.listeners;

import java.util.ArrayList;
import java.util.HashMap;

/*an interface that allows the implementor to know when a connection to twitter has been completed.
 * whichever class implements this will recieve an array of each twitter users tweets , or an empty map on no results*/
public interface TweeterListObserver {
	void onConnectedToTwitterComplete(
			ArrayList<ArrayList<HashMap<String, String>>> allUserTweetsMap);

	void requestRefresh();
	
	void changePage(int index);
	
	void removeItem(String name);
	
	void requestMultiColumn(boolean multiColumnChoice);
}
