package com.meaglin.sms;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.meaglin.sms.model.AbstractServer;

public class App {

	public Properties config;

	public App() {

	}

	public void start(String[] args) {
		config = new Properties();
		File configfile = new File("server.properties");
		try {
			InputStream stream = Files.newInputStream(configfile.toPath(),
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
				System.out.println("Cleaned up " + deleteFiles(new File(config.getProperty("mount.categorydir", "/thisshouldnotexists"))) + " remaining category files.");
				System.out.println("Cleaned up " + deleteFiles(new File(config.getProperty("mount.dir", "/thisshouldnotexists"))) + " remaining mount files.");
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
			default:
				System.out.println("Options: fullclean|clean|listmounts|listappmounts|listlocks|updatestatus|update");
				break;
		}
	}

	public static void main(String[] args) {
		App app = new App();
		app.start(args);
	}
	
	public int deleteFiles(File start) {
		File[] list = start.listFiles();
		int cnt = 0;
		if(list != null) {
			for(File sub : list) {
				cnt += deleteFiles(sub);
				sub.delete();
				cnt += 1;
			}
		}
		return cnt;
	}
}
