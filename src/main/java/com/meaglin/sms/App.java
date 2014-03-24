package com.meaglin.sms;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import com.meaglin.sms.model.AbstractServer;

public class App {

	public Properties config;

	public App() {

	}

	public void start(String[] args) throws IOException {
		long systemStart = System.currentTimeMillis();
		config = new Properties();
		
		try {
			InputStream stream = Files.newInputStream(Paths.get("server.properties"),
					StandardOpenOption.READ);
			config.load(stream);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		SmsServer server = new SmsServer(config);
		server.load();

		// The default action.
		String command = "update";
		if (args.length > 0) {
			command = args[0];
		}
		switch (command) {
			case "fullclean":
				for(AbstractServer s : server.servers.values()) {
					if(!s.isEnabled() || s.isDisconnected()) {
						continue;
					}
					s.disconnect();
				}
				server.getDb().cleanFiles();
				for(String[] mount : server.getMounts()) {
					if(mount[1].startsWith(config.getProperty("mount.dir"))) {
						switch(server.unmount(mount[1])) {
							case 0:
								System.out.println("Cleaned up mount " + mount[1]);
								break;
							case 1:
								System.out.println("Cleaned up mount(with lock-remove) " + mount[1]);
								break;
							case -1:
								System.out.println("Error cleaning up mount " + mount[1]);
								break;
						}
					}
				}
				System.out.println("Cleaned up " + deleteFiles(Paths.get(config.getProperty("mount.categorydir"))) + " remaining category files.");
				System.out.println("Cleaned up " + deleteFiles(Paths.get(config.getProperty("mount.dir"))) + " remaining mount files.");
				break;
			case "clean":
				for(AbstractServer s : server.servers.values()) {
					if(!s.isEnabled() || s.isDisconnected()) {
						continue;
					}
					s.disconnect();
				}
				server.getDb().cleanFiles();
				break;
			case "listmounts":
				List<String[]> list = server.getMounts();
				System.out.println("Mounts:");
				for(String[] mount : list) {
					System.out.println(mount[0] + "@" + mount[1]);
				}
				break;
			case "listappmounts":
				for(AbstractServer s : server.servers.values()) {
					if(!s.isEnabled()) {
						continue;
					}
					for(String mount : s.getMountFolders()) {
						s.log(mount);
					}
				}
				break;
			case "listlocks":
				for(AbstractServer s : server.servers.values()) {
					if(!s.isEnabled()) {
						continue;
					}
					for(String mount : s.getMountFolders()) {
						Map<String, String> locks = server.getLocks(s.getPath() + "/" + mount);
						s.log(s.getPath() + "/" + mount);
						for(Entry<String, String> lock : locks.entrySet()) {
							s.log("  " + lock.getValue() + "@" + lock.getKey());
						}
					}
				}
				break;
			case "updatestatus":
				for(AbstractServer s : server.servers.values()) {
					if(!s.isEnabled() || s.isDisconnected()) {
						continue;
					}
					if(!s.isAvailable()) {
						s.disconnect();
						continue;
					}
					s.log("Status: Online");
				}
				server.saveHistory();
				break;
			case "update":
				server.updateShares();
				break;
			case "test":
				server.testShares();
				break;
			default:
				System.out.println("Options: fullclean|clean|listmounts|listappmounts|listlocks|updatestatus|update");
				break;
		}
		
		System.out.println("---------");
		System.out.println("Total runtime: " + (System.currentTimeMillis() - systemStart));
		System.out.println("---------");
	}

	public static void main(String[] args) throws IOException {
		App app = new App();
		app.start(args);
	}
	
	public int deleteFiles(Path start) throws IOException {
		final AtomicInteger cnt = new AtomicInteger();
		Files.walkFileTree(start, new SimpleFileVisitor<Path>(){
	        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	            Files.delete(file);
	            cnt.incrementAndGet();
	            return FileVisitResult.CONTINUE;
	        }

	        @Override
	        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
	            // try to delete the file anyway, even if its attributes
	            // could not be read, since delete-only access is
	            // theoretically possible
	            Files.delete(file);
	            cnt.incrementAndGet();
	            return FileVisitResult.CONTINUE;
	        }

	        @Override
	        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
	            if (exc == null) {
	                Files.delete(dir);
	                cnt.incrementAndGet();
	                return FileVisitResult.CONTINUE;
	            } else {
	                // directory iteration failed; propagate exception
	                throw exc;
	            }
	        }
	    });
		return cnt.get();
	}
}
