package com.meaglin.sms.model;

public class HistoryEntry {

	private int id;
	
	private long time;
	
	private int serverid, categoryid, servercategoryid, fileid;
	
	private String level, type, action, item;
	
	public HistoryEntry(String level, String type, String action, String item) {
		time = System.currentTimeMillis();
		this.level = level;
		this.type = type;
		this.action = action;
		this.item = item;
	}

	public HistoryEntry(String level, String type, String action, ServerFile file, String item) {
		time = System.currentTimeMillis();
		this.level = level;
		this.type = type;
		this.action = action;
		this.serverid = file.getServerid();
		this.categoryid = file.getCategoryid();
		this.servercategoryid = file.getServercategoryid();
		this.fileid = file.getId();
		this.item = item;
	}

	public HistoryEntry(String level, String type, String action, Server server, String item) {
		time = System.currentTimeMillis();
		this.level = level;
		this.type = type;
		this.action = action;
		this.serverid = server.getId();
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
	 * @return the serverid
	 */
	public int getServerid() {
		return serverid;
	}

	/**
	 * @return the categoryid
	 */
	public int getCategoryid() {
		return categoryid;
	}

	/**
	 * @return the servercategoryid
	 */
	public int getServercategoryid() {
		return servercategoryid;
	}

	/**
	 * @return the fileid
	 */
	public int getFileid() {
		return fileid;
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
