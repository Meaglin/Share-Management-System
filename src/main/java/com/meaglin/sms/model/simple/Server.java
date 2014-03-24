package com.meaglin.sms.model.simple;

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
			while (res.next()) {
				files.add(((new ServerFile()).bind(res)));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			getController().getDb().collect(conn, st, res);
		}

		for (ServerFile file : files) {
			// TODO: unmount the belonging share somehow.
			ServerCategory cat = (ServerCategory) categories.get(file.getServercategoryid());
			if (cat == null) {
				file.setFlag(ServerFile.DELETED);
				toUpdate.add(file);
				// continue;
			}
			file.setServercategory(cat);
		}

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

		try {
	        mountFolders();
        } catch (IOException e) {
	        e.printStackTrace();
        }

		List<ServerFile> files = getFiles();
		log("Loaded " + files.size() + " from db.");

		List<ServerFile> newfiles = new ArrayList<ServerFile>();

		// TODO: clean this up and optimize.
		for (AbstractServerCategory servercategory : this.categories.values()) {
			newfiles.addAll(((ServerCategory) servercategory).getServerFiles());
		}
		log("Loaded " + newfiles.size() + " from server.");

		for (ServerFile newFile : newfiles) {
			ServerFile found = null;
			for (ServerFile file : files) {
				if (file.getName().equals(newFile.getName())
//						&& file.getDirectory().equals(newFile.getDirectory())
						) {
					found = file;
					break;
				}
			}
			if (found != null) {
				// Disabled to allow renaming outside of the system.
//				if (found.getFlag() == ServerFile.DELETED) {
//					found.setFlag(ServerFile.CREATED);
//					toUpdate.add(found);
//				} else 
				if (found.getFlag() == ServerFile.UP_TO_DATE) {
					// File is already known and registered.
					getController().occupyFile(found.getPath());
				}
				files.remove(found);
			} else {
				toUpdate.add(newFile);
			}
		}

		for (ServerFile file : files) {
			file.setFlag(ServerFile.DELETED);
			toUpdate.add(file);
		}

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
					"mount.testdir"), "categories",serverfile.getPath());
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
		for (ServerFile file : currentFiles) {
			if (file.getFlag() == ServerFile.DELETED) {
				file.delete(rootDir);
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
				if(!getController().occupyFile(serverfile.getPath())) {
					serverfile.setFlag(ServerFile.DUPLICATE);
					toUpdate.add(serverfile);
					log("Duplicate^2 file on path " + serverfile.getPath());
					continue;
				}
			}
			Path path = Paths.get(rootDir, serverfile.getPath());
			// TODO: unneeded
//			Files.createDirectories(path.getParent());

			Files.createSymbolicLink(path, serverfile.getServerFile());
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

		for (ServerFile serverfile : currentFiles) {
			if (serverfile.getFlag() == ServerFile.CREATED) { // Doesn't exist
															  // yet ;).
				continue;
			}
			serverfile.delete(getController().getConfig().getProperty(
					"mount.testdir")
					+ "/categories/");
			serverfile.setFlag(ServerFile.CREATED); // Recreates the files when
													// share is up again.
			toUpdate.add(serverfile);
		}
		this.saveChanges();
	}

	private void deleteFileLinksOnLinux() throws IOException {
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
			serverfile.delete(rootDir);
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
