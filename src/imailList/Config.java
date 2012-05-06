package imailList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Config extends Properties {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private File[] locations = new File[]{
			new File(System.getProperty("user.dir") + "/imaillist.cfg"),
			new File(System.getProperty("user.home") + "/.config/imaillist.cfg"),
			new File("/etc/imaillist.cfg")
	};
	
	private static Config instance = new Config();
	
	private Config() {
		for (File f : locations) {
			if (f.exists() && f.canRead()) {
				
				try {
					this.load(new FileInputStream(f));
				} catch (FileNotFoundException e) {
				} catch (IOException e) {
				}
			}
		}
	}
	
	public static Config getInstance() {
		return instance;
	}

}
