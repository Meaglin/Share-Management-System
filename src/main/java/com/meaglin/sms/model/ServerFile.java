package com.meaglin.sms.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.meaglin.sms.Database;

public class ServerFile {

	public static final int CREATED = 1;
	public static final int UPDATED = 2;
	public static final int UP_TO_DATE = 3;
	public static final int DELETED = 4;
	public static final int DUPLICATE = 5;
	private int id, serverid, servercategoryid, categoryid;

	private int flag;
	
	private boolean duplicate;

	private String name, displayname, directory, displaydirectory, type,
			extension, path, serverpath;

	private Category category;
	private Server server;
	private ServerCategory servercategory;

	public ServerFile() {

	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the serverid
	 */
	public int getServerid() {
		return serverid;
	}

	/**
	 * @return the servercategoryid
	 */
	public int getServercategoryid() {
		return servercategoryid;
	}

	/**
	 * @return the categoryid
	 */
	public int getCategoryid() {
		return categoryid;
	}

	/**
	 * @return the flag
	 */
	public int getFlag() {
		return flag;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the displayname
	 */
	public String getDisplayname() {
		return displayname;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the extension
	 */
	public String getExtension() {
		return extension;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		if (path == null || path.trim().length() == 0) {
			generatePath();
		}
		return path;
	}

	public void generatePath() {
		if (category == null) {
			System.out.println("missing category");
		}
		path = getCategory().getPath() + "/" + getDisplaydirectory() + "/" + (isDuplicate() ? "[" + getServer().getCode() + "]" : "") + getDisplayname();
	}

	/**
	 * @return the serverpath
	 */
	public String getServerpath() {
		return serverpath;
	}

	public File getServerFile() {
		return new File(this.getServercategory().getDirectory(),
				this.getDirectory() + "/" + this.getName());
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
	public Server getServer() {
		return server;
	}

	/**
	 * @return the servercategory
	 */
	public ServerCategory getServercategory() {
		return servercategory;
	}

	/**
	 * @param id
	 *			the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @param flag
	 *			the flag to set
	 */
	public void setFlag(int flag) {
		this.flag = flag;
	}

	/**
	 * @param name
	 *			the name to set
	 */
	public void setName(String name) {
		this.name = name;
		if (name.lastIndexOf(".") != -1) {
			this.setExtension(name.substring(name.lastIndexOf(".") + 1));
		}
	}

	/**
	 * @param displayname
	 *			the displayname to set
	 */
	public void setDisplayname(String displayname) {
		this.displayname = displayname;
	}

	/**
	 * @param type
	 *			the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @param extension
	 *			the extension to set
	 */
	public void setExtension(String extension) {
		this.extension = extension;
	}

	/**
	 * @param path
	 *			the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @param serverpath
	 *			the serverpath to set
	 */
	public void setServerpath(String serverpath) {
		this.serverpath = serverpath;
	}

	/**
	 * @param category
	 *			the category to set
	 */
	public void setCategory(Category category) {
		this.category = category;
		this.categoryid = category.getId();
	}

	/**
	 * @param server
	 *			the server to set
	 */
	public void setServer(Server server) {
		this.server = server;
		this.serverid = server.getId();
	}

	/**
	 * @param servercategory
	 *			the servercategory to set
	 */
	public void setServercategory(ServerCategory servercategory) {
		this.servercategory = servercategory;
		if (servercategory == null) {
			System.out.println(this.servercategoryid);
		}
		this.servercategoryid = servercategory.getId();
		this.setCategory(getServercategory().getCategory());
	}

	/**
	 * @return the directory
	 */
	public String getDirectory() {
		return directory;
	}

	/**
	 * @param directory
	 *			the directory to set
	 */
	public void setDirectory(String directory) {
		this.directory = directory;
	}

	/**
	 * @return the displaydirectory
	 */
	public String getDisplaydirectory() {
		return displaydirectory;
	}

	/**
	 * @param displaydirectory
	 *			the displaydirectory to set
	 */
	public void setDisplaydirectory(String displaydirectory) {
		this.displaydirectory = displaydirectory;
	}

	/**
	 * @return the duplicate
	 */
    public boolean isDuplicate() {
	    return duplicate;
    }

	/**
	 * @param duplicate the duplicate to set
	 */
    public void setDuplicate(boolean duplicate) {
	    this.duplicate = duplicate;
    }

	private static final String[] columns = new String[] { "serverid",
			"servercategoryid", "categoryid", "name", "displayname",
			"directory", "displaydirectory", "type", "flag", "duplicate", "extension",
			"path", "serverpath" };

	public void save() {
		if (this.getServer() == null) {
			System.out.println("Cannot save file without server");
			return;
		}
		Database db = getServer().getController().getDb();
		if (getId() == 0) { // new
			try {
				db.insertOne("files", columns, getServerid(),
						getServercategoryid(), getCategoryid(), getName(),
						getDisplayname(), getDirectory(),
						getDisplaydirectory(), getType(), getFlag(),
						isDuplicate(), getExtension(), getPath(), getServerpath());
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			try {
				// TODO: implement update for more vars.
				db.updateQuery("UPDATE `files` SET flag = ?, duplicate = ? WHERE id = ?",
						getFlag(), isDuplicate(), getId());
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public ServerFile bind(ResultSet res) throws SQLException {
		id = res.getInt("id");
		serverid = res.getInt("serverid");
		servercategoryid = res.getInt("servercategoryid");
		categoryid = res.getInt("categoryid");
		flag = res.getInt("flag");

		name = res.getString("name");
		displayname = res.getString("displayname");
		setDirectory(res.getString("directory"));
		setDisplaydirectory(res.getString("displaydirectory"));
		type = res.getString("type");
		extension = res.getString("extension");
//		path = res.getString("path");
		serverpath = res.getString("serverpath");
		
		setDuplicate(res.getBoolean("duplicate"));
		return this;
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
		ServerFile other = (ServerFile) obj;
		if (id == 0) // newly created objects.
			return this.getPath().equals(other.getPath())
					&& this.getName().equals(other.getName());

		if (id != other.id)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	public void tryDelete(String rootFolder) {
		File file = new File(rootFolder + "/" + getPath());
		try {
			Files.delete(file.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			Files.delete(file.getParentFile().toPath());
		} catch (DirectoryNotEmptyException e) {
			// Crude, but works.
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TODO: implement.

	}

}
