/**
 * 
 */
package com.meaglin.sms.model.simple;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.meaglin.sms.SmsServer;
import com.meaglin.sms.model.AbstractServer;
import com.meaglin.sms.model.AbstractServerCategory;
import com.meaglin.sms.model.ServerFile;

/**
 * 
 * @author Joseph Verburg
 * 
 */
public class ServerCategory extends AbstractServerCategory {

	public ServerCategory(SmsServer smsServer, AbstractServer server) {
	    super(smsServer, server);
    }
	
	public ServerCategory(SmsServer smsServer, AbstractServer server, ResultSet set) throws SQLException {
		super(smsServer, server, set);
	}

	public List<ServerFile> getServerFiles() {
		List<ServerFile> files = new ArrayList<>();
		if(isDontScan()) {
			return files;
		}
		ServerFile sf;
		File[] dirfiles;
		getServer().log("Parsing " + getDirectory().getParent() + "/" + getDirectory().getName());
		dirs: for (File directory : getFiles()) {
			if (!directory.isDirectory()) {
				continue; // only dirs.
			}

			for (String ignore : getIgnores()) {
				if (directory.getName().startsWith(ignore)) {
					continue dirs;
				}
			}

			dirfiles = directory.listFiles();
			if (dirfiles == null) { // System Volume Information >.>
				continue;
			}
			for (File sub : dirfiles) {
				sf = new ServerFile();
				sf.setServercategory(this);
				sf.setServer(getServer());
				sf.setName(sub.getName());
				sf.setDisplayname(sub.getName());
				sf.setDirectory(directory.getName());
				sf.setDisplaydirectory(directory.getName());
				sf.setServerpath(sub.getPath());
				sf.setFlag(ServerFile.CREATED);
				sf.setPath("");
				sf.setCreated();

				// TODO: this is slow, find a better way.
				// sf.setType(sub.isDirectory() ? "directory" : "file");
				sf.setType("unknown");
				// TODO: fill these.
				sf.setExtension("");
				files.add(sf);
			}

		}
		return files;
	}

	private File[] getFiles() {
		return getDirectory().listFiles();
	}

}
