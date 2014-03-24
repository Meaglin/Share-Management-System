/**
 * 
 */
package com.meaglin.sms.model.tree;

import gnu.trove.map.hash.TIntObjectHashMap;

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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.meaglin.sms.SmsServer;
import com.meaglin.sms.model.AbstractServer;
import com.meaglin.sms.model.AbstractServerCategory;
import com.meaglin.sms.model.ServerFile;

/**
 * 
 * @author Joseph Verburg
 * 
 */
public class ServerCategory extends AbstractServerCategory implements FileVisitor<Path> {

	
	protected List<ServerFile> files;
	protected List<ServerFile> scannedFiles;
	protected TIntObjectHashMap<ServerFile> fileMap;
	
	private long s1, s2, t1, t2;
	
	public ServerCategory(SmsServer smsServer, AbstractServer server) {
	    super(smsServer, server);
	    files = new ArrayList<>();
	    scannedFiles = new ArrayList<>();
	    fileMap = new TIntObjectHashMap<ServerFile>();
    }
	public ServerCategory(SmsServer smsServer, AbstractServer server, ResultSet set) throws SQLException {
		super(smsServer, server, set);
		files = new ArrayList<>();
		scannedFiles = new ArrayList<>();
		fileMap = new TIntObjectHashMap<ServerFile>();
	}

	public List<ServerFile> scanFiles() throws IOException {
		scannedFiles.clear();
		if(isDontScan()) {
			return scannedFiles;
		}
		s2 = System.currentTimeMillis();
		Files.walkFileTree(getRootDir(), EnumSet.noneOf(FileVisitOption.class), getScanDepth() + 1, this);
		t1 = t1 / 1000000;
		t2 = (System.currentTimeMillis() - s2) - t1;
		getServer().log("(" + getRootDir() + ")Incode: " + t1 + " in walk: " + t2);
		return scannedFiles;
	}

	public List<ServerFile> processFiles() {
		List<ServerFile> changed = new ArrayList<>();
		Map<String, ServerFile> fromDb = new HashMap<>(files.size());
		for (ServerFile file : files) {
			fromDb.put(file.getServerpath(), file);
		}
		
		for (ServerFile newFile : scannedFiles) {
			ServerFile found = fromDb.get(newFile.getServerpath());
			if (found != null) {
				if (found.getFlag() == ServerFile.UP_TO_DATE) {
					// File is already known and registered.
					getCategory().occupyFile(found);
				}
				newFile.setParentid(found.getParentid());
				newFile.setId(found.getId());
				fromDb.remove(found.getServerpath());
			} else {
//				getCategory().occupyFile(newFile);
				changed.add(newFile);
			}
		}
		for (ServerFile file : fromDb.values()) {
			file.setFlag(ServerFile.DELETED);
			changed.add(file);
		}
		
		return changed;
	}
	
    public String getConfigDefaults() {
    	return "{" +
    			"dontscan: false," +
    			"scandepth: 5" +
		"}";
    }
	
	public int getScanDepth() {
		return getConfig().getInt("scandepth");
	}
	@Override
    public void registerFile(ServerFile file) {
		file.setServercategory(this);
		files.add(file);
		fileMap.put(file.getId(), file);
    }
	@Override
    public void forgetFile(ServerFile file) {
		files.remove(file);
		fileMap.remove(file.getId());
    }
	
	public ServerFile getFile(int id) {
		return fileMap.get(id);
	}
	
	ServerFile parent = null;
	Path root = null;
	public Path getRootDir() {
		if(root == null) {
			root = getDirectory();
		}
		return root;
	}
	
	@Override
	public FileVisitResult preVisitDirectory(Path dir,
			BasicFileAttributes attrs) throws IOException {
		s1 = System.nanoTime();
		int depth = dir.getNameCount() - getRootDir().getNameCount();
		String dirname = dir.getFileName().toString();
		// System reserved.
		if(dirname.startsWith("[") || dirname.endsWith("]")) {
			return FileVisitResult.SKIP_SUBTREE;
		}
		for (String ignore : getIgnores()) {
			if (dirname.startsWith(ignore)) {
				return FileVisitResult.SKIP_SUBTREE;
			}
		}
		
		if(depth == getScanDepth()) {
			addFile(dir, attrs);
			t1 += (System.nanoTime() - s1);
			return FileVisitResult.SKIP_SUBTREE;
		}
		if(dir != getRootDir()) {
			parent = addFile(dir, attrs);
		}
		t1 += (System.nanoTime() - s1);
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file,
			BasicFileAttributes attrs) throws IOException {
		s1 = System.nanoTime();
		addFile(file, attrs);
		t1 += (System.nanoTime() - s1);
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
		if(dir != getRootDir()) {
			parent = parent.getParent();
		}
		return FileVisitResult.CONTINUE;
	}
	
	public ServerFile addFile(Path path, BasicFileAttributes attrs) {
		ServerFile file = new ServerFile();
		file.setServercategory(this);
		file.setName(path.getFileName().toString());
		file.setDisplayname(file.getName());
		file.setParent(parent);
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
}

