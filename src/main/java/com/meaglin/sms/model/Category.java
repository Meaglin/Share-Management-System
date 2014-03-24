package com.meaglin.sms.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.meaglin.json.JSONObject;
import com.meaglin.sms.SmsServer;

public class Category {

	private SmsServer controller;

	private int id;
	private int parentid;

	private Category parent;
	private List<Category> children;
	private List<AbstractServerCategory> registered;

	private String name, displayname, config;

	private Set<String> occupiedFiles = new HashSet<>();
	
	private JSONObject configMap;
	
	public Category(SmsServer smsServer) {
		controller = smsServer;
		children = new ArrayList<>();
		registered = new ArrayList<>();
	}

	public Category(SmsServer smsServer, ResultSet set) throws SQLException {
		this(smsServer);
		bind(set);
	}
	
	public void learn(AbstractServerCategory abstractServerCategory) {
		registered.add(abstractServerCategory);
	}

	public void forget(AbstractServerCategory abstractServerCategory) {
		registered.remove(abstractServerCategory);
	}

	public List<Category> getChildren() {
		return children;
	}

	public boolean hasChild(Category cat) {
		return children.contains(cat);
	}

	public boolean hasChild(String categoryname) {
		for (Category cat : getChildren()) {
			if (cat.getName().equals(categoryname))
				return true;
		}
		return false;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the parentid
	 */
	public int getParentid() {
		return parentid;
	}

	/**
	 * @return the parent
	 */
	public Category getParent() {
		return parent;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	public String getPath() {
		return (hasParent() ? getParent().getPath() + "/" : "") + getName();
	}

	/**
	 * @return the displayname
	 */
	public String getDisplayname() {
		return displayname;
	}

	/**
	 * @return the controller
	 */
	public SmsServer getController() {
		return controller;
	}

	/**
	 * @param id
	 *			the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @param parentid
	 *			the parentid to set
	 */
	public void setParentid(int parentid) {
		this.parentid = parentid;
	}

	/**
	 * @param parent
	 *			the parent to set
	 */
	public void setParent(Category parent) {
		this.parent = parent;
	}

	/**
	 * @param name
	 *			the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param displayname
	 *			the displayname to set
	 */
	public void setDisplayname(String displayname) {
		this.displayname = displayname;
	}

	public boolean hasParent() {
		return this.parentid != 0;
	}
	
	public Category bind(ResultSet res) throws SQLException {
		id = res.getInt("id");
		parentid = res.getInt("parentid");
		name = res.getString("name");
		displayname = res.getString("displayname");
		config = res.getString("config");
		return this;
	}

    public String getConfigDefaults() {
    	return "{" +
    			"duplicationdepth: 1" +
		"}";
    }
    
    public JSONObject getConfig() {
    	if(configMap == null) {
    		configMap = new JSONObject(config);
    		configMap.defaults(getConfigDefaults());
    	}
    	return configMap;
    }
    
    public String getRawConfig() {
    	if(configMap != null) {
    		return configMap.toString();
    	}
    	return config;
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
		if (getClass() != obj.getClass())
			return false;
		Category other = (Category) obj;
		if (id != other.id)
			return false;
		return true;
	}

	public int getDuplicationDepth() {
		int duplicationDepth = getConfig().getInt("duplicationdepth");
		if(duplicationDepth <= 0) {
			duplicationDepth = 1;
		}
		return duplicationDepth;
	}

	public boolean occupyFile(ServerFile file) {
		int depth = getDuplicationDepth();
		if(file.getLevel() > depth) {
			return true;
		}
		if((file.getLevel() == depth || !file.getType().equals("directory"))
				&& !occupiedFiles.add(file.getPath())) {
			if(file.isDuplicate()) {
				return false;
			}
			file.setDuplicate(true);
		}
		return true;
	}

	public void forgetFile(ServerFile file) {
		int depth = getDuplicationDepth();
		if(file.getLevel() > depth) {
			return;
		}
		occupiedFiles.remove(file.getPath());
    }
}
