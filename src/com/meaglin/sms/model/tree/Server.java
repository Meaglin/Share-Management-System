package com.meaglin.sms.model.tree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.meaglin.sms.SmsServer;
import com.meaglin.sms.model.AbstractServer;
import com.meaglin.sms.model.AbstractServerCategory;
import com.meaglin.sms.model.ServerFile;

public class Server extends AbstractServer {

	public List<ServerFile> toUpdate;
	private List<ServerFile> currentFiles;
	
	private Comparator<ServerFile> deleteOrdering, createOrdering;
	
	private int changeCount;
	
	public Server(SmsServer smsServer) {
	    super(smsServer);
    }

	public Server(SmsServer smsServer, ResultSet set) throws SQLException {
		super(smsServer, set);
	}
	
	protected void init() {
		toUpdate = new ArrayList<>();
		createSorters();
	}
	
	private void createSorters() {
		deleteOrdering = new Comparator<ServerFile>(){
			@Override
            public int compare(ServerFile o1, ServerFile o2) {
				if(o2.getLevel() > o1.getLevel()) {
					return 1;
				}
				if(o1.getLevel() > o2.getLevel()) {
					return -1;
				}
				return 0;
            }
			
		};
		createOrdering = new Comparator<ServerFile>(){
			@Override
			public int compare(ServerFile o1, ServerFile o2) {
				if(o2.getLevel() > o1.getLevel()) {
					return -1;
				}
				if(o1.getLevel() > o2.getLevel()) {
					return 1;
				}
				return 0;
			}
			
		};
		
	}

	public List<ServerFile> getFiles() {
		List<ServerFile> files = new ArrayList<>();

		Connection conn = null;
		PreparedStatement st = null;
		ResultSet res = null;
		try {
			conn = getController().getDb().getConnection();
			st = conn.prepareStatement(SELECT_FILES);
			st.setInt(1, getId());

			res = st.executeQuery();
			ServerFile file;
			ServerCategory cat = null;
			while (res.next()) {
				file = new ServerFile(res);
				if(cat == null || file.getServercategoryid() != cat.getId()) {
					cat = (ServerCategory) categories.get(file.getServercategoryid());
				}
				if(cat == null) {
					file.setFlag(ServerFile.DELETED);
					toUpdate.add(file);
					continue;
				}
				cat.registerFile(file);
				files.add(file);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			getController().getDb().collect(conn, st, res);
		}

//		for (ServerFile file : files) {
//			// TODO: unmount the belonging share somehow.
//			ServerCategory cat = categories.get(file.getServercategoryid());
//			if (cat == null) {
//				file.setFlag(ServerFile.DELETED);
//				toUpdate.add(file);
//				// continue;
//			}
//			file.setServercategory(cat);
//			file.setServer(this);
//		}

		return files;
	}
	
	public void refreshFiles() throws IOException {
		boolean reachable = isAvailable();
		setLastupdate(System.currentTimeMillis());
		setStatus(reachable ? "Online" : "Offline");
		save();

		toUpdate.clear();
		log("Status: " + this.getStatus());

		if (!reachable) {
			if (!isDisconnected()) {
				disconnect();
			}
			return;
		}
		if(isDisconnected()) {
			setDisconnected(false);
			setJustReconnected(true);
			getController().track(this);			
		}
		log("Loading...");

		mountFolders();

		long s = System.currentTimeMillis();
		
		List<ServerFile> files = getFiles();
		
		log("Loaded " + files.size() + " from db[" + (System.currentTimeMillis() - s) + "]");
		s = System.currentTimeMillis();
		
		int cnt = 0;
		for (AbstractServerCategory cat : getCategories()) {
			ServerCategory scat = (ServerCategory) cat;
			cnt += scat.scanFiles().size();
		}
		
		log("Scanned " + cnt + " files on server[" + (System.currentTimeMillis() - s) + "]");
		s = System.currentTimeMillis();
		
		for (AbstractServerCategory cat : getCategories()) {
			ServerCategory scat = (ServerCategory) cat;
			toUpdate.addAll(scat.processFiles());
		}
		log("Recorded " + toUpdate.size() + " changes on server[" + (System.currentTimeMillis() - s) + "]");
	}

	public void saveChanges() {
		this.getController().getDb().save(toUpdate);
		if (toUpdate.size() != 0) {
			List<ServerFile> update = new ArrayList<>();
			for(ServerFile file: toUpdate) {
				if(file.getParent() != null && file.getParentid() == 0) {
					file.setParent(file.getParent());
					update.add(file);
				}
			}
			getController().getDb().save(update);
			this.setLastchange(System.currentTimeMillis());
		}
		this.save();
	}

	public void updateServerStats() {
		if (currentFiles == null) {
			currentFiles = getFiles();
		}
		int cnt = 0;
		for (ServerFile file : currentFiles) {
			if (file.getFlag() != ServerFile.DELETED) {
				cnt += 1;
			}
		}
		this.setFilecount(cnt);
		this.save();
	}

	public void updateFileLinks() throws IOException {
		if (isWindows()) {
			updateFileLinksOnWindows();
		} else {
			updateFileLinksOnLinux();
		}
	}

	public void updateFileLinksOnWindows() throws IOException {
		if (currentFiles == null) {
			currentFiles = getFiles();
		}

		toUpdate.clear();

		List<ServerFile> toDelete = new ArrayList<>();
		for (ServerFile file : currentFiles) {
			if (file.getFlag() == ServerFile.DELETED) {
				file.delete(getController().getConfig().getProperty(
						"mount.testdir")
						+ "/categories/");
				toDelete.add(file);
			}
		}
		this.getController().getDb().delete(toDelete);

		for (ServerFile serverfile : currentFiles) {
			if (serverfile.getFlag() != ServerFile.CREATED
					&& serverfile.getFlag() != ServerFile.UPDATED) {
				continue;
			}
			
			Path path = Paths.get(getController().getConfig().getProperty(
					"mount.testdir")
					+ "/categories/" + serverfile.getPath());
			Files.createDirectories(path.getParent());
			Files.write(path, serverfile.getServerpath()
					.getBytes(), StandardOpenOption.CREATE,
					StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING);
			// Prevent history spam after reconnects.
			if(!isJustReconnected()) {
				getController().track(serverfile);
			}
			serverfile.setFlag(ServerFile.UP_TO_DATE);
			serverfile.setModified();
			toUpdate.add(serverfile);
		}
		this.saveChanges();
	}

	public void updateFileLinksOnLinux() throws IOException {
		if(isDisconnected()) {
			return;
		}
		
		if (currentFiles == null) {
			currentFiles = getFiles();
		}

		toUpdate.clear();

		String rootDir = getController().getConfig().getProperty(
				"mount.categorydir");

		List<ServerFile> toDelete = new ArrayList<>();
		Iterator<ServerFile> it = currentFiles.iterator();
		ServerFile ent;
		while (it.hasNext()) {
			ent = it.next();
			if (ent.getFlag() == ServerFile.DELETED) {
				it.remove();
				toDelete.add(ent);
			}
		}
		Collections.sort(toDelete, deleteOrdering);
		
		for(ServerFile file : toDelete) {
			getController().track(file);
			file.getCategory().forgetFile(file);
			file.delete(rootDir);
		}
		this.getController().getDb().delete(toDelete);

		Collections.sort(currentFiles, createOrdering);
		
		for (ServerFile serverfile : currentFiles) {
			if (serverfile.getFlag() != ServerFile.CREATED
					&& serverfile.getFlag() != ServerFile.UPDATED) {
				continue;
			}
			// Account for files with the same dir and name on different servers.
			if(!serverfile.getCategory().occupyFile(serverfile)) {
				serverfile.setFlag(ServerFile.DUPLICATE);
				toUpdate.add(serverfile);
				log("Duplicate^2 file known on path " + serverfile.getPath());
			}
			
			Path path = Paths.get(rootDir, serverfile.getPath());
			if(serverfile.getType().equals("directory") && serverfile.getLevel() != ((ServerCategory) serverfile.getServercategory()).getScanDepth()) {
				Files.createDirectory(path);
			} else {
				Files.createSymbolicLink(path, serverfile.getServerFile());
			}
			// Prevent history spam after reconnects.
			if(!isJustReconnected()) {
				getController().track(serverfile);
			}
			serverfile.setFlag(ServerFile.UP_TO_DATE);
			serverfile.setModified();
			toUpdate.add(serverfile);
		}
		changeCount = toUpdate.size() + toDelete.size();
		this.saveChanges();
	}

	public void disconnect() {
		this.setDisconnected(true);
		log("disconnecting...");
		try {
	        deleteFileLinks();
        } catch (IOException e1) {
	        e1.printStackTrace();
        }
		log("deleted file links.");
		try {
	        unmountFolders();
        } catch (IOException e) {
	        e.printStackTrace();
        }
		log("unmounted shares");
		getController().track(this);
	}

	public void deleteFileLinks() throws IOException {
		if (isWindows()) {
			deleteFileLinksOnWindows();
		} else {
			deleteFileLinksOnLinux();
		}
	}

	private void deleteFileLinksOnWindows() throws IOException {
		if (currentFiles == null) {
			currentFiles = getFiles();
		}

		toUpdate.clear();
		Collections.sort(currentFiles, deleteOrdering);
		
		for (ServerFile serverfile : currentFiles) {
			if (serverfile.getFlag() == ServerFile.CREATED) { // Doesn't exist yet ;).
				continue;
			}
			serverfile.delete(getController().getConfig().getProperty(
					"mount.testdir")
					+ "/categories/");
			serverfile.setFlag(ServerFile.CREATED); // Recreates the files when
													// share is up again.
			toUpdate.add(serverfile);
		}
		changeCount = toUpdate.size();
		this.saveChanges();
	}

	private void deleteFileLinksOnLinux() throws IOException {
		if (currentFiles == null) {
			currentFiles = getFiles();
		}
		toUpdate.clear();
		
		Collections.sort(currentFiles, deleteOrdering);

		String rootDir = getController().getConfig().getProperty("mount.categorydir");

		for (ServerFile serverfile : currentFiles) {
			if (serverfile.getFlag() == ServerFile.CREATED) { // Doesn't exist yet ;).
				continue;
			}
			serverfile.delete(rootDir);
			serverfile.setFlag(ServerFile.CREATED); // Recreates the files when
													// share is up again.
			toUpdate.add(serverfile);
		}
		changeCount = toUpdate.size();
		this.saveChanges();
	}

	@Override
    public int getChangeCount() {
	    return changeCount;
    }
	
}
