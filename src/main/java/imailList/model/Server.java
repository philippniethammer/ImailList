package imailList.model;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class Server {
	
	@DatabaseField(generatedId=true)
	private int id;

	@DatabaseField
	private String host;
	
	@DatabaseField
	private String user;
	
	@DatabaseField
	private String password;
	
	@DatabaseField
	private String listenFolder;
	
	@DatabaseField
	private boolean useIdle;
	
	/**
	 * Check mails at least every n seconds.
	 */
	@DatabaseField
	private int checkFrequency;
	
	@ForeignCollectionField(eager=true)
	private ForeignCollection<MailingList> mailingLists;
	
	public Server() {
	}
	
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}
	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}
	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}
	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}
	/**
	 * @param user the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}
	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}
	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	/**
	 * @return the listenFolder
	 */
	public String getListenFolder() {
		return listenFolder;
	}
	/**
	 * @param listenFolder the listenFolder to set
	 */
	public void setListenFolder(String listenFolder) {
		this.listenFolder = listenFolder;
	}
	/**
	 * @return the useIdle
	 */
	public boolean isUseIdle() {
		return useIdle;
	}
	/**
	 * @param useIdle the useIdle to set
	 */
	public void setUseIdle(boolean useIdle) {
		this.useIdle = useIdle;
	}

	/**
	 * @return the checkFrequency
	 */
	public int getCheckFrequency() {
		return checkFrequency;
	}

	/**
	 * @param checkFrequency the checkFrequency to set
	 */
	public void setCheckFrequency(int checkFrequency) {
		this.checkFrequency = checkFrequency;
	}

	/**
	 * @return the lists
	 */
	public ForeignCollection<MailingList> getLists() {
		return mailingLists;
	}

	/**
	 * @param lists the lists to set
	 */
	public void setLists(ForeignCollection<MailingList> lists) {
		this.mailingLists = lists;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Server other = (Server) obj;
		if (checkFrequency != other.checkFrequency)
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (id != other.id)
			return false;
		if (listenFolder == null) {
			if (other.listenFolder != null)
				return false;
		} else if (!listenFolder.equals(other.listenFolder))
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (useIdle != other.useIdle)
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}								
	
	
	
	
}
