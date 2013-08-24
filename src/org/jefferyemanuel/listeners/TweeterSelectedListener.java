package org.jefferyemanuel.listeners;

public interface TweeterSelectedListener {

	/**
     * Inform the listener that an tweeter has been selected if in two pane mode.
     * 
     * @param a twetters name matching viewpager title
     * 
     */
	
	void requestListItem(String tweeterName);
	void requestItemDeletion(String name);
}
