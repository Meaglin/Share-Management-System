package com.meaglin.sms.model.tree;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.meaglin.sms.SmsServer;
import com.meaglin.sms.model.AbstractServer;
import com.meaglin.sms.model.AbstractServerCategory;
import com.meaglin.sms.model.ServerFile;

public class Server extends AbstractServer {

	public List<ServerFile> toUpdate;
	private List<ServerFile> currentFiles;
	
	public Server(SmsServer smsServer) {
	    super(smsServer);
		toUpdate = new ArrayList<>();
    }

	public Server(SmsServer smsServer, ResultSet set) throws SQLException {
		super(smsServer, set);
		toUpdate = new ArrayList<>();
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
				cat.files.add(file);
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
	
	public void refreshFiles() {
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

		List<ServerFile> files = getFiles();
		log("Loaded " + files.size() + " from db.");


		for (AbstractServerCategory cat : getCategories()) {
			ServerCategory scat = (ServerCategory) cat;
			scat.scanFiles();
		}
//		List<ServerFile> newfiles = new ArrayList<ServerFile>();
//
//		// TODO: clean this up and optimize.
//		for (ServerCategory servercategory : this.categories.values()) {
//			newfiles.addAll(servercategory.getServerFiles());
//		}
//		log("Loaded " + newfiles.size() + " from server.");
//
//		for (ServerFile newFile : newfiles) {
//			ServerFile found = null;
//			for (ServerFile file : files) {
//				if (file.getName().equals(newFile.getName())
//						&& file.getDirectory().equals(newFile.getDirectory())) {
//					found = file;
//					break;
//				}
//			}
//			if (found != null) {
//				// Disabled to allow renaming outside of the system.
////				if (found.getFlag() == ServerFile.DELETED) {
////					found.setFlag(ServerFile.CREATED);
////					toUpdate.add(found);
////				} else 
//				if (found.getFlag() == ServerFile.UP_TO_DATE) {
//					// File is already known and registered.
//					getController().occupyFile(found.getPath());
//				}
//				files.remove(found);
//			} else {
//				toUpdate.add(newFile);
//			}
//		}
//
//		for (ServerFile file : files) {
//			file.setFlag(ServerFile.DELETED);
//			toUpdate.add(file);
//		}

		log("Recorded " + toUpdate.size() + " changes on server.");
	}

	public void saveChanges() {
		this.getController().getDb().save(toUpdate);
		if (toUpdate.size() != 0) {
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

	public void updateFileLinks() {
		if (isWindows()) {
			updateFileLinksOnWindows();
		} else {
			updateFileLinksOnLinux();
		}
	}

	public void updateFileLinksOnWindows() {
		if (currentFiles == null) {
			currentFiles = getFiles();
		}

		toUpdate.clear();

		List<ServerFile> toDelete = new ArrayList<>();
		for (ServerFile file : currentFiles) {
			if (file.getFlag() == ServerFile.DELETED) {
				file.tryDelete(getController().getConfig().getProperty(
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
			File file = new File(getController().getConfig().getProperty(
					"mount.testdir")
					+ "/categories/" + serverfile.getPath());
			file.getParentFile().mkdirs();
			try {
				Files.write(file.toPath(), serverfile.getServerpath()
						.getBytes(), StandardOpenOption.CREATE,
						StandardOpenOption.WRITE,
						StandardOpenOption.TRUNCATE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
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

	public void updateFileLinksOnLinux() {
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
		for (ServerFile file : currentFiles) {
			if (file.getFlag() == ServerFile.DELETED) {
				file.tryDelete(rootDir);
				toDelete.add(file);
				getController().track(file);
				getController().forgetFile(file.getPath());
			}
		}
		this.getController().getDb().delete(toDelete);

		for (ServerFile serverfile : currentFiles) {
			if (serverfile.getFlag() != ServerFile.CREATED
					&& serverfile.getFlag() != ServerFile.UPDATED) {
				continue;
			}
			// Account for files with the same dir and name on different servers.
			if(!getController().occupyFile(serverfile.getPath())) {
				if(serverfile.isDuplicate()) {
					serverfile.setFlag(ServerFile.DUPLICATE);
					toUpdate.add(serverfile);
					log("Duplicate^2 file known on path " + serverfile.getPath());
					continue;
				}
				serverfile.setDuplicate(true);
				serverfile.generatePath(); // Update the path.
				if(!getController().occupyFile(serverfile.getPath())) {
					serverfile.setFlag(ServerFile.DUPLICATE);
					toUpdate.add(serverfile);
					log("Duplicate^2 file on path " + serverfile.getPath());
					continue;
				}
			}
			
			File file = new File(rootDir + "/" + serverfile.getPath());
			file.getParentFile().mkdirs();
			try {
				Files.createSymbolicLink(file.toPath(), serverfile
						.getServerFile().toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
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

	public void disconnect() {
		this.setDisconnected(true);
		log("disconnecting...");
		deleteFileLinks();
		log("deleted file links.");
		unmountFolders();
		log("unmounted shares");
		getController().track(this);
	}

	public void deleteFileLinks() {
		if (isWindows()) {
			deleteFileLinksOnWindows();
		} else {
			deleteFileLinksOnLinux();
		}
	}

	private void deleteFileLinksOnWindows() {
		if (currentFiles == null) {
			currentFiles = getFiles();
		}

		toUpdate.clear();

		for (ServerFile serverfile : currentFiles) {
			if (serverfile.getFlag() == ServerFile.CREATED) { // Doesn't exist
															  // yet ;).
				continue;
			}
			serverfile.tryDelete(getController().getConfig().getProperty(
					"mount.testdir")
					+ "/categories/");
			serverfile.setFlag(ServerFile.CREATED); // Recreates the files when
													// share is up again.
			toUpdate.add(serverfile);
		}
		this.saveChanges();
	}

	private void deleteFileLinksOnLinux() {
		if (currentFiles == null) {
			currentFiles = getFiles();
		}

		toUpdate.clear();

		String rootDir = getController().getConfig().getProperty(
				"mount.categorydir");

		for (ServerFile serverfile : currentFiles) {
			if (serverfile.getFlag() == ServerFile.CREATED) { // Doesn't exist
															  // yet ;).
				continue;
			}
			serverfile.tryDelete(rootDir);
			serverfile.setFlag(ServerFile.CREATED); // Recreates the files when
													// share is up again.
			toUpdate.add(serverfile);
		}

		this.saveChanges();
	}

	@Override
    public int getChangeCount() {
	    return toUpdate.size();
    }
	
}
