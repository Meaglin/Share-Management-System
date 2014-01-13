package com.meaglin.sms.model;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;

import com.meaglin.sms.SmsServer;

public abstract class AbstractServerCategory {
	private SmsServer controller;

	private int id, categoryid, serverid;
	private String name, path, config;
	
	private JSONObject configMap;
	
	private Category category;
	private AbstractServer server;

	private String[] ignores;

	public AbstractServerCategory(SmsServer smsServer, AbstractServer server) {
		controller = smsServer;
		this.server = server;
		ignores = controller.getDirectoryIgnores();
	}
	
	public AbstractServerCategory(SmsServer smsServer, AbstractServer server, ResultSet set) throws SQLException {
		this(smsServer, server);
		bind(set);
	}
	
	public File getDirectory() {
		return new File(getServer().getPath(), getPath());
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the categoryid
	 */
	public int getCategoryid() {
		return categoryid;
	}

	/**
	 * @return the serverid
	 */
	public int getServerid() {
		return serverid;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return the category
	 */
	public Category getCategory() {
		return category;
	}

	/**
	 * @return the server
	 */
	public AbstractServer getServer() {
		return server;
	}

	/**
	 * @return the controller
	 */
	public SmsServer getController() {
		return controller;
	}
	
	/**
	 * @return the ignores
	 */
	public String[] getIgnores() {
		return ignores;
	}

    public JSONObject getConfig() {
    	if(configMap == null) {
    		configMap = new JSONObject(config);
    	}
    	return configMap;
    }
	
	public boolean isDontScan() {
		try {
			return getConfig().getBoolean("dontscan");
		} catch(JSONException e) {
			return false;
		}
	}
	/**
	 * @param id
	 *			the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @param categoryid
	 *			the categoryid to set
	 */
	public void setCategoryid(int categoryid) {
		this.categoryid = categoryid;
	}

	/**
	 * @param serverid
	 *			the serverid to set
	 */
	public void setServerid(int serverid) {
		this.serverid = serverid;
	}

	/**
	 * @param name
	 *			the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param path
	 *			the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @param category
	 *			the category to set
	 */
	public void setCategory(Category category) {
		if (this.category != null) {
			this.category.forget(this);
		}
		this.category = category;
		this.category.learn(this);
	}

	public AbstractServerCategory bind(ResultSet res) throws SQLException {
		id = res.getInt("id");
		categoryid = res.getInt("categoryid");
		serverid = res.getInt("serverid");
		name = res.getString("name");
		path = res.getString("path");
		config = res.getString("config");
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AbstractServerCategory))
			return false;
		
		AbstractServerCategory other = (AbstractServerCategory) obj;
		if (id != other.id)
			return false;
		return true;
	}
}
