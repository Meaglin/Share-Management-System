/**
 * 
 */
package com.meaglin.sms.model.tree;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.json.JSONException;

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

	
	protected List<ServerFile> files;
	protected List<ServerFile> scannedFiles;
	
	public ServerCategory(SmsServer smsServer, AbstractServer server) {
	    super(smsServer, server);
	    files = new ArrayList<>();
	    scannedFiles = new ArrayList<>();
    }
	public ServerCategory(SmsServer smsServer, AbstractServer server, ResultSet set) throws SQLException {
		super(smsServer, server, set);
		files = new ArrayList<>();
		scannedFiles = new ArrayList<>();
	}

	public List<ServerFile> scanFiles() {
		scannedFiles.clear();
		if(isDontScan()) {
			return scannedFiles;
		}
		final ServerCategory cat = this;
		
		File root = getDirectory();
		final Path rootPath = root.toPath();
		final int rootCount = rootPath.getNameCount();
		final int maxDepth = getScanDepth();
		final String[] ignores = getIgnores();
		try {
			
			Files.walkFileTree(rootPath, new HashSet<FileVisitOption>(), maxDepth + 1, new FileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir,
						BasicFileAttributes attrs) throws IOException {
					int depth = dir.getNameCount() - rootCount;
					for (String ignore : ignores) {
						if (dir.getFileName().toString().startsWith(ignore)) {
							return FileVisitResult.SKIP_SUBTREE;
						}
					}
					if(depth == maxDepth) {
						addFile(dir, attrs);
						return FileVisitResult.SKIP_SUBTREE;
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) throws IOException {
					addFile(file, attrs);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file,
						IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir,
						IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
				
				public ServerFile addFile(Path path, BasicFileAttributes attrs) {
					ServerFile file = new ServerFile();
					String dir = "";
					if(!path.getParent().equals(rootPath)) {
						dir = path.getParent().toString().substring(rootPath.toString().length() + 1);
						dir = dir.replace('\\', '/');
					}
					file.setServercategory(cat);
					file.setName(path.getFileName().toString());
					file.setDisplayname(file.getName());
					file.setDirectory(dir);
					file.setDisplaydirectory(dir);
					file.setServerpath(path.toString());
					file.setFlag(ServerFile.CREATED);
					file.setCreated();
					
					file.setType(attrs.isDirectory() ? "directory" : "file");
					file.setExtension("");
					if(!attrs.isDirectory()) {
						String name = file.getName();
						if(name.indexOf('.') != -1){
							file.setExtension(name.substring(name.lastIndexOf('.')));
						}
					}
					
					scannedFiles.add(file);
					return file;
				}

			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return scannedFiles;
	}

	public List<ServerFile> processFiles() {
		scanFiles();
		
		List<ServerFile> changed = new ArrayList<>();
		
		for (ServerFile newFile : scannedFiles) {
			ServerFile found = null;
			for (ServerFile file : files) {
				if (file.getName().equals(newFile.getName())
						&& file.getDirectory().equals(newFile.getDirectory())) {
					found = file;
					break;
				}
			}
			if (found != null) {
				if (found.getFlag() == ServerFile.UP_TO_DATE) {
					// File is already known and registered.
					getCategory().occupyFile(found.getPath());
				}
				files.remove(found);
			} else {
				changed.add(newFile);
			}
		}
		for (ServerFile file : files) {
			file.setFlag(ServerFile.DELETED);
			changed.add(file);
		}
		
		return changed;
	}
	
	public int getScanDepth() {
		try {
			return getConfig().getInt("scandepth");
		} catch(JSONException e) {
			return 1;
		}
	}
}

