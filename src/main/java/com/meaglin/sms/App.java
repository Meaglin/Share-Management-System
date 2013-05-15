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

import com.meaglin.sms.model.Server;

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
			case "clean":
				for(Server s : server.servers.values()) {
					if(!s.isEnabled()) {
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
			case "listlocks":
				for(Server s : server.servers.values()) {
					if(!s.isEnabled()) {
						continue;
					}
					for(String mount : s.getMountFolders()) {
						Map<String, String> locks = s.getLocks(s.getPath() + "/" + mount);
						s.log(s.getPath() + "/" + mount);
						for(Entry<String, String> lock : locks.entrySet()) {
							s.log("  " + lock.getValue() + "@" + lock.getKey());
						}
					}
				}
				break;
			case "updatestatus":
				for(Server s : server.servers.values()) {
					if(!s.isEnabled() || s.isDisconnected()) {
						continue;
					}
					if(!s.isAvailable()) {
						s.disconnect();
						continue;
					}
					s.log("Status: Online");
				}
				break;
			case "update":
				server.updateShares();
				break;
			default:
				System.out.println("Options: clean|update|listmounts|listlocks");
				break;
		}
	}

	public static void main(String[] args) {
		App app = new App();
		app.start(args);
	}
}
