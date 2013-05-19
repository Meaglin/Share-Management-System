package com.meaglin.sms.model;

public class HistoryEntry {

	private int id;
	
	private long time;
	
	private String level, type, action, item;
	
	public HistoryEntry(String level, String type, String action, String item) {
		time = System.currentTimeMillis();
		this.level = level;
		this.type = type;
		this.action = action;
		this.item = item;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the time
	 */
	public long getTime() {
		return time;
	}

	/**
	 * @return the level
	 */
	public String getLevel() {
		return level;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * @return the item
	 */
	public String getItem() {
		return item;
	}

}
