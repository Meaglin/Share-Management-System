package com.meaglin.sms.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.meaglin.sms.SmsServer;

public class Category {

	private SmsServer controller;

	private int id;
	private int parentid;

	private Category parent;
	private List<Category> children;
	private List<ServerCategory> registered;

	private String name, displayname;
	
	public Category(SmsServer smsServer) {
		controller = smsServer;
		children = new ArrayList<>();
		registered = new ArrayList<>();
	}

	public void learn(ServerCategory cat) {
		registered.add(cat);
	}

	public void forget(ServerCategory cat) {
		registered.remove(cat);
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
		if (getClass() != obj.getClass())
			return false;
		Category other = (Category) obj;
		if (id != other.id)
			return false;
		return true;
	}

}
