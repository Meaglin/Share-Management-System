package com.meaglin.sms.model;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.meaglin.json.JSONObject;
import com.meaglin.sms.SmsServer;

public abstract class AbstractServer {

	private SmsServer controller;

	protected static final String SELECT_FILES = "SELECT * FROM files WHERE serverid = ?";

	private int id, filecount, categorycount;
	private long lastupdate, lastchange;

	private boolean enabled, disconnected;

	private String name, displayname, code, type, description, config, status, ip;

	private boolean justReconnected;
	
	private JSONObject configMap;
	
	protected Map<Integer, AbstractServerCategory> categories;

	public AbstractServer(SmsServer smsServer) {
		controller = smsServer;
		categories = new HashMap<>();
		init();
	}
	
	public AbstractServer(SmsServer smsServer, ResultSet set) throws SQLException {
		controller = smsServer;
		categories = new HashMap<>();
		bind(set);
		init();
	}
	
	protected void init() {
		
	}
	
	public void log(String message) {
		// TODO: make proper logger.
		System.out.println("[" + getName() + "]" + message);
	}

	public boolean isAvailable() {
		boolean reachable = false;
		for (int i = 0; i < 10; i += 1) {
			if (reachable) {
				break;
			}
			Socket sock = new Socket();
			int port = 139;
			if (getMountType().equalsIgnoreCase("nfs")) {
				port = 111;
			}
			try {
				sock.connect(new InetSocketAddress(getIp(), port), 1000);
			} catch (IOException ex) {
			}

			reachable = sock.isConnected();

			try {
				sock.close();
			} catch (IOException e) {
			}
		}
		return reachable;
	}

	public Path getPath() {
		if (isWindows()) {
			return Paths.get("\\\\" + getIp());
		} else {
			return Paths.get(getController().getConfig()
					.getProperty("mount.dir") + "/" + getName());
		}
	}

	public List<String> getMountFolders() {
		List<String> dirs = new ArrayList<>();

		sc: for (AbstractServerCategory cat : this.categories.values()) {
			String path = cat.getPath();
			Iterator<String> it = dirs.iterator();
			String dir;
			while(it.hasNext()) {
				dir = it.next();
				if(path.startsWith(dir + "/")) { // We already know this mount.
					continue sc;
				}
				if(path.equalsIgnoreCase(dir)) {
					continue sc;
				}
				if(dir.startsWith(path + "/")) {
					it.remove();
				}
			}
			dirs.add(path);
		}

		return dirs;
	}

	public void addServerCategory(AbstractServerCategory toAdd) {
		categories.put(toAdd.getId(), toAdd);
		this.setCategorycount(categories.size());
	}

	public void mountFolders() throws IOException {
		if (isWindows()) {
			mountOnWindows();
		} else {
			mountOnLinux();
		}
	}

	public void mountOnLinux() throws IOException {
		if (!getMountType().equalsIgnoreCase("smb")
				&& !getMountType().equalsIgnoreCase("nfs")) {
			throw new RuntimeException("Nope, invalid mount type " + getMountType()
					+ " on server [" + getName() + "]" + getIp());
		}

		if (getMountType().equalsIgnoreCase("smb")) {
			for (String dir : this.getMountFolders()) {
				if(isMounted("//" + getIp() + "/" + dir, getPath() + "/" + dir)) {
					continue;
				}
				Path path = getPath().resolve(dir);
				Files.createDirectories(path);
				
				ProcessBuilder task = new ProcessBuilder(new String[] {
						"/bin/mount", "-t", "cifs", "//" + getIp() + "/" + dir,
						getPath() + "/" + dir, "-o",
						getMountOptions() });
				task.redirectErrorStream(true); // Channel errors to stdOut.
				task.redirectInput(Redirect.INHERIT);
				task.redirectOutput(Redirect.INHERIT);
				try {
					Process proc = task.start();
					proc.waitFor();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		} else if (getMountType().equalsIgnoreCase("nfs")) {
			for (String dir : this.getMountFolders()) {
				if(isMounted(getIp() + ":/" + dir, getPath() + "/" + dir)) {
					continue;
				}
				Path path = getPath().resolve(dir);
				Files.createDirectories(path);
				
				ProcessBuilder task = new ProcessBuilder(new String[] {
						"/bin/mount", "-t", "nfs", getIp() + ":/" + dir,
						getPath() + "/" + dir, "-o",
						getMountOptions() });
				task.redirectErrorStream(true); // Channel errors to stdOut.
				task.redirectInput(Redirect.INHERIT);
				task.redirectOutput(Redirect.INHERIT);
				try {
					Process proc = task.start();
					proc.waitFor();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}
	}

	private boolean isMounted(String mount, String mountpoint) {
		List<String[]> mounts = getController().getMounts();
		for(String[] mnt : mounts) {
			if(mnt[1].equals(mountpoint)) {
				if(mnt[0].equals(mount)) {
					return true;
				} else {
					throw new RuntimeException("[" + getName() + "] Trying to mount " + mount + "[" + mnt[0] + "] on already ocupied mountpoint " + mountpoint + "[" + mnt[1] + "]");
				}
			}
		}
		return false;
	}
	
	/**
	 * This doesn't actually mount anything.
	 * 
	 * Testing only ;)
	 * @throws IOException 
	 */
	public void mountOnWindows() throws IOException {
		for (String dir : this.getMountFolders()) {
			Path path = Paths.get(getController().getConfig().getProperty(
					"mount.testdir"), "mount", getName(), dir);
			Files.createDirectories(path);
		}
	}
	
	public void unmountFolders() throws IOException {
		if (isWindows()) {
			unmountFoldersOnWindows();
		} else {
			unmountFoldersOnLinux();
		}
	}

	private void unmountFoldersOnWindows() throws IOException {
		for (String dir : this.getMountFolders()) {
			Path path = Paths.get(getController().getConfig().getProperty(
					"mount.testdir"), "mount", getName(), dir);
			Files.deleteIfExists(path);
		}
	}

	/**
	 * 
	 */
	private void unmountFoldersOnLinux() {
		if (!getMountType().equalsIgnoreCase("smb")
				&& !getMountType().equalsIgnoreCase("nfs")) {
			throw new RuntimeException("Nope, invalid mount type " + getMountType()
					+ " on server [" + getName() + "]" + getIp());
		}

		for (String dir : this.getMountFolders()) {
			switch(getController().unmount(getPath() + "/" + dir)) {
				case 0:
					log("removed mount " + getPath() + "/" + dir);
					break;
				case 1:
					log("removed mount with lock-remove " + getPath() + "/" + dir);
					break;
				case -1:
					log("Error removing mount " + getPath() + "/" + dir);
					break;
			}
		}
	}
	
	public abstract void disconnect();
	
	public abstract void refreshFiles() throws IOException;
	public abstract void saveChanges();

	public abstract void updateServerStats();
	public abstract void updateFileLinks() throws IOException;
	
	public abstract int getChangeCount();

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the filecount
	 */
	public int getFilecount() {
		return filecount;
	}

	public Collection<AbstractServerCategory> getCategories() {
		return categories.values();
	}

	/**
	 * @return the categorycount
	 */
	public int getCategorycount() {
		return categorycount;
	}

	/**
	 * @return the lastupdate
	 */
	public long getLastupdate() {
		return lastupdate;
	}

	/**
	 * @return the lastchange
	 */
	public long getLastchange() {
		return lastchange;
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
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @return the ip
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * @return the controller
	 */
	public SmsServer getController() {
		return controller;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	public String getMountType() {
		return getConfig().getJSONObject("mount").getString("type");
	}

	public String getMountOptions() {
		return getConfig().getJSONObject("mount").getString("options");
	}

	/**
	 * @param id
	 *			the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @param filecount
	 *			the filecount to set
	 */
	public void setFilecount(int filecount) {
		this.filecount = filecount;
	}

	/**
	 * @param categorycount
	 *			the categorycount to set
	 */
	public void setCategorycount(int categorycount) {
		this.categorycount = categorycount;
	}

	/**
	 * @param lastupdate
	 *			the lastupdate to set
	 */
	public void setLastupdate(long lastupdate) {
		this.lastupdate = lastupdate;
	}

	/**
	 * @param lastchange
	 *			the lastchange to set
	 */
	public void setLastchange(long lastchange) {
		this.lastchange = lastchange;
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

	/**
	 * @param description
	 *			the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param status
	 *			the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @param ip
	 *			the ip to set
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}

	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the disconnected
	 */
	public boolean isDisconnected() {
		return disconnected;
	}

	/**
	 * @param disconnected
	 *			the disconnected to set
	 */
	public void setDisconnected(boolean disconnected) {
		this.disconnected = disconnected;
	}

	/**
	 * @return the enabled
	 */
    public boolean isEnabled() {
	    return enabled;
    }

	/**
	 * @param enabled the enabled to set
	 */
    public void setEnabled(boolean enabled) {
	    this.enabled = enabled;
    }

	/**
	 * @return the code
	 */
    public String getCode() {
	    return code;
    }

	/**
	 * @param code the code to set
	 */
    public void setCode(String code) {
	    this.code = code;
    }

	public AbstractServer bind(ResultSet res) throws SQLException {
		id = res.getInt("id");
		filecount = res.getInt("filecount");
		categorycount = res.getInt("categorycount");
		lastupdate = res.getLong("lastupdate");
		lastchange = res.getLong("lastchange");
		name = res.getString("name");
		displayname = res.getString("displayname");
		description = res.getString("description");
		status = res.getString("status");
		setEnabled(res.getBoolean("enabled"));
		setDisconnected(res.getBoolean("disconnected"));
		config = res.getString("config");
		ip = res.getString("ip");
		setCode(res.getString("code"));
		return this;
	}

	/**
	 * @return the justReconnected
	 */
    public boolean isJustReconnected() {
	    return justReconnected;
    }

	/**
	 * @param justReconnected the justReconnected to set
	 */
    public void setJustReconnected(boolean justReconnected) {
	    this.justReconnected = justReconnected;
    }
    
    public String getConfigDefaults() {
    	return "{" +
				"mount: {" +
				"dir: ''," +
				"options: 'ro'," +
				"type: 'smb'," +
				"testdir: ''" +
			"}" +
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
    
	public void save() {
		if (id == 0) { // We never create 'servers' inside sms.
			return;
		}
		getController().getDb().saveList(getCategories());
		getController().getDb().save(this);
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
		if(!(obj instanceof AbstractServer)) {
			return false;
		}
		AbstractServer other = (AbstractServer) obj;
		if (id != other.id)
			return false;
		return true;
	}

	private static String OS = null;

	public static String getOsName() {
		if (OS == null) {
			OS = System.getProperty("os.name");
		}
		return OS;
	}

	public static boolean isWindows() {
		return getOsName().startsWith("Windows");
	}
}
