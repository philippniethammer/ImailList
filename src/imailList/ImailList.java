package imailList;

import imailList.model.MailingList;
import imailList.model.Member;
import imailList.model.Server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.logger.LocalLog;
import com.j256.ormlite.table.TableUtils;

public class ImailList extends Thread {
	
	private static JdbcPooledConnectionSource connectionSource;
	private HashMap<Integer, ImapListener> serverThread = new HashMap<Integer, ImapListener>();
	private HashMap<Integer, Server> serverList = new HashMap<Integer, Server>();
	
	private ImailList() {
		connect();
		installTables();
		
		start();
	}
	
	private void connect() {
		try {
			connectionSource = new JdbcPooledConnectionSource(Config.getInstance().getProperty("jdbcURI", "jdbc:"),
					Config.getInstance().getProperty("jdbcUser"),
					Config.getInstance().getProperty("jdbcPass"));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// only keep the connections open for 5 minutes
		connectionSource.setMaxConnectionAgeMillis(60 * 1000);
	}
	
	private void installTables() {
		try {
			TableUtils.createTableIfNotExists(connectionSource, Server.class);
			TableUtils.createTableIfNotExists(connectionSource, MailingList.class);
			TableUtils.createTableIfNotExists(connectionSource, Member.class);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private List<Server> getAllServers() {
		try {
			Dao<Server, Integer> dao =
					  DaoManager.createDao(connectionSource, Server.class);
			
			return dao.queryForAll();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private void invokeServerThread(Server server) {
			serverThread.put(server.getId(), new ImapListener(server));
	}
	
	public void run() {
		while (!this.isInterrupted()) {
			System.out.println("Main: Syncing servers.");
			List<Server> servers = getAllServers();
			
			for (Server s : servers) {
				if (!serverList.containsKey(s.getId())) {
					invokeServerThread(s);
					serverList.put(s.getId(),s);
				} else if (!serverList.get(s.getId()).equals(s)) {
					serverThread.get(s.getId()).interrupt();
					invokeServerThread(s);
					serverList.put(s.getId(),s);
				}
			}
			try {
				Thread.sleep(Integer.parseInt(Config.getInstance().getProperty("syncIntervall", "300"))*1000);
			} catch (InterruptedException e) {	}
		}
	}
	
	public static void main(String args[]) throws IOException {
		
		System.setProperty(LocalLog.LOCAL_LOG_LEVEL_PROPERTY, "WARNING");
		
		for (int i=0; i<args.length; i++) {
			if (args[i] == "-l" || args[i] == "--logfile") {
				String logfile = args[++i];
				File log = new File(logfile);
				if ((!log.exists() && !log.createNewFile()) || log.exists() && !log.canWrite()) {
					System.out.println("Can't write to log file, use standard output.");
				}
				System.setOut(new PrintStream(new FileOutputStream(log)));
			}
		}
		new ImailList();
	}
}
