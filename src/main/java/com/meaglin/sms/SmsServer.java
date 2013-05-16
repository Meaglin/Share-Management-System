package com.meaglin.sms;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.meaglin.sms.model.Category;
import com.meaglin.sms.model.Server;
import com.meaglin.sms.model.ServerCategory;

public class SmsServer {

	private Map<Integer, Category> categories;
	public Map<Integer, Server> servers;
	private Database db;

	private Properties config;
	private String[] dirignores;
	
	private Set<String> occupiedFiles = new HashSet<>();

	public SmsServer(Properties config) {
		categories = new ConcurrentHashMap<>();
		servers = new ConcurrentHashMap<>();
		db = new Database(config);
		this.config = config;
		this.dirignores = getConfig().getProperty("parsing.dirignores", "")
				.trim().split(" ");
	}

	public void load() {
		ResultSet res = null;
		try {
			res = db.selectQuery("SELECT * FROM servers ", new Object[0]);
			Server s;
			while (res.next()) {
				s = (new Server(this)).bind(res);
				servers.put(s.getId(), s);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			getDb().collect(null, null, res);
		}

		System.out.println("servers: " + servers.size());

		try {
			res = db.selectQuery("SELECT * FROM categories", new Object[0]);
			Category c;
			while (res.next()) {
				c = (new Category(this)).bind(res);
				categories.put(c.getId(), c);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			getDb().collect(null, null, res);
		}

		for (Category cat : categories.values()) {
			Category parent = categories.get(cat.getParentid());
			if (parent == null) {
				continue;
			}
			cat.setParent(parent);
			parent.getChildren().add(cat);
		}

		System.out.println("categories: " + categories.size());

		try {
			res = db.selectQuery("SELECT * FROM servercategories",
					new Object[0]);
			ServerCategory servercategory;
			Server server;
			while (res.next()) {
				server = servers.get(res.getInt("serverid"));
				if (server == null) {
					continue;
				}
				servercategory = new ServerCategory(this, server);
				servercategory.bind(res);
				server.addServerCategory(servercategory);
				servercategory.setCategory(categories.get(servercategory
						.getCategoryid()));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			getDb().collect(null, null, res);
		}
	}

	public void updateShares() {
		this.makeCategoryDirs();

		for (Server server : servers.values()) {
			if(!server.isEnabled()) {
				continue;
			}
			try {
				server.refreshFiles();
				server.saveChanges();
				server.log("Saved " + server.toUpdate.size() + " changes.");
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		for (Server server : servers.values()) {
			if(!server.isEnabled()) {
				continue;
			}
			try {
				server.updateServerStats();
				server.updateFileLinks();
				server.log("Updated " + server.toUpdate.size() + " filelinks.");
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

	}

	public void makeCategoryDirs() {
		String path = getConfig().getProperty("mount.categorydir");
		for (Category cat : categories.values()) {
			if (cat.getParent() == null) {
				makeCategoryDirs(path, cat);
			}
		}
	}

	private void makeCategoryDirs(String path, Category cat) {
		(new File(path + "/" + cat.getName())).mkdirs();
		for (Category sub : cat.getChildren()) {
			makeCategoryDirs(path + "/" + cat.getName(), sub);
		}
	}

	public void makeTestCategoryDirs() {
		String path = getConfig().getProperty("mount.testdir") + "/categories";
		for (Category cat : categories.values()) {
			if (cat.getParent() == null) {
				makeTestCategoryDirs(path, cat);
			}
		}
	}

	private void makeTestCategoryDirs(String path, Category cat) {
		(new File(path + "/" + cat.getName())).mkdirs();
		for (Category sub : cat.getChildren()) {
			makeTestCategoryDirs(path + "/" + cat.getName(), sub);
		}
	}

	public List<String[]> getMounts() {
		List<String[]> list = new ArrayList<>();

		String mount, mountpoint, mounttype, opts;
		for (String m : getMountLines()) {
			mount = m.substring(0, m.indexOf(" on "));
			mountpoint = m.substring(m.indexOf(" on ") + 4, m.indexOf(" type "));
			mounttype = m.substring(m.indexOf(" type ") + 6, m.indexOf(" ("));
			opts = m.substring(m.indexOf(" (") + 2, m.length() - 2);
			list.add(new String[] { mount, mountpoint, mounttype, opts });
		}

		return list;
	}

	private List<String> getMountLines() {
		List<String> lines = new ArrayList<>();
		
		ProcessBuilder task = new ProcessBuilder(new String[] { "/bin/mount" });
		task.redirectErrorStream(true); // Channel errors to stdOut.
		task.redirectInput(Redirect.INHERIT);
		
		try {
			Process proc = task.start();
			BufferedReader input = new BufferedReader (new InputStreamReader( proc.getInputStream()));
			String line;
			while ((line = input.readLine ()) != null) {
				lines.add(line);
			}
			input.close();
			proc.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return lines;
	}
	
	public Map<String, String> getFileLocks(String dir) {
		Map<String, String> map = new HashMap<>();
		
		String currentPid = null;
		for(String line : getFileLocksLines(dir)) {
			if(line == null || line.trim().length() == 0) {
				continue;
			}
			if(line.startsWith("p")) {
				currentPid = line.substring(1);
			} else if(line.startsWith("n")) {
				map.put(line.substring(1), currentPid);
			}
		}
		
		return map;
	}
	
	private List<String> getFileLocksLines(String dir) {
		//lsof -F pn -x +D /smstest/categories
		
		List<String> lines = new ArrayList<>();
		
		ProcessBuilder task = new ProcessBuilder(new String[] { "lsof", "-F", "pn", "-x", "+D", dir });
		task.redirectErrorStream(true); // Channel errors to stdOut.
		task.redirectInput(Redirect.INHERIT);
		
		try {
			Process proc = task.start();
			BufferedReader input = new BufferedReader (new InputStreamReader( proc.getInputStream()));
			String line;
			while ((line = input.readLine ()) != null) {
				lines.add(line);
			}
			input.close();
			proc.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return lines;
	}
	
	public int unmount(String mountpoint) {
		int returnid = tryUnmount(mountpoint);
		if(returnid == 0) {
			return 0;
		}
		killMountLocks(mountpoint);
		returnid = tryUnmount(mountpoint);
		if(returnid == 0) {
			return 1;
		}
		return -1;
	}
	
	private void killMountLocks(String mountpoint) {
		for(String pid : getLockingPids(mountpoint)) {
			killPid(pid);
		}
	}
		
	private void killPid(String pid) {
		ProcessBuilder task = new ProcessBuilder(new String[] {
				"kill"
				, pid });
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
		
	private Set<String> getLockingPids(String mountpoint) {
		List<String> lines = new ArrayList<>();
		
		ProcessBuilder task = new ProcessBuilder(new String[] { "lsof", "-F", "p", "+D", mountpoint });
		task.redirectErrorStream(true); // Channel errors to stdOut.
		task.redirectInput(Redirect.INHERIT);
		
		try {
			Process proc = task.start();
			BufferedReader input = new BufferedReader (new InputStreamReader( proc.getInputStream()));
			String line;
			while ((line = input.readLine ()) != null) {
				lines.add(line);
			}
			input.close();
			proc.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Set<String> pids = new HashSet<>();
		
		for(String line : lines) {
			if(line == null || line.trim().length() == 0) {
				continue;
			}
			if(line.startsWith("p")) {
				pids.add(line.substring(1));
			} 
		}
		
		return pids;
	}
	
	private int tryUnmount(String mountpoint) {
		ProcessBuilder task = new ProcessBuilder(new String[] { "/bin/umount", mountpoint });
		
		task.redirectErrorStream(true); // Channel errors to stdOut.
		task.redirectInput(Redirect.INHERIT);
		task.redirectOutput(Redirect.INHERIT);
		try {
			Process proc = task.start();
			return proc.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	public Map<String, String> getLocks(String mountpoint) {
		List<String> lines = new ArrayList<>();
		
		ProcessBuilder task = new ProcessBuilder(new String[] { "lsof", "-F", "pn", "+D", mountpoint });
		task.redirectErrorStream(true); // Channel errors to stdOut.
		task.redirectInput(Redirect.INHERIT);
		
		try {
			Process proc = task.start();
			BufferedReader input = new BufferedReader (new InputStreamReader( proc.getInputStream()));
			String line;
			while ((line = input.readLine ()) != null) {
				lines.add(line);
			}
			input.close();
			proc.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Map<String, String> locks = new HashMap<>();
		
		String currentPid = null;
		for(String line : lines) {
			if(line == null || line.trim().length() == 0) {
				continue;
			}
			if(line.startsWith("p")) {
				currentPid = line.substring(1);
			} else if(line.startsWith("n")) {
				locks.put(line.substring(1), currentPid);
			}
		}
		
		return locks;
	}
	
	public Database getDb() {
		return db;
	}

	public Category getCategory(int id) {
		return categories.get(id);
	}

	public Server getServer(int id) {
		return servers.get(id);
	}

	public Properties getConfig() {
		return config;
	}

	public String[] getDirectoryIgnores() {
		return dirignores;
	}

	public boolean occupyFile(String filepath) {
		return occupiedFiles.add(filepath);
	}

	public void forgetFile(String filepath) {
		occupiedFiles.remove(filepath);
    }
	
}
