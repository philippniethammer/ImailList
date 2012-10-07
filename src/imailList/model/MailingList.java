package imailList.model;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class MailingList {

	@DatabaseField(generatedId=true)
	private int id;
	
	@DatabaseField
	private String name = "";
	
	@DatabaseField
	private String listenAddress = "";
	
	@DatabaseField
	private boolean archive;
	
	@DatabaseField
	private String archiveFolder = "";
	
	@DatabaseField
	private String tag;
	
	@DatabaseField
	private boolean answerToAll;
	
	@DatabaseField
	private boolean allowExternal;
	
	@ForeignCollectionField(eager=false)
	private ForeignCollection<Member> members;
	
	@DatabaseField(foreign=true)
	private Server server;
	
	
	public MailingList() {
		
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
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the listenAddress
	 */
	public String getListenAddress() {
		return listenAddress;
	}

	/**
	 * @param listenAddress the listenAddress to set
	 */
	public void setListenAddress(String listenAddress) {
		this.listenAddress = listenAddress;
	}

	/**
	 * @return the archive
	 */
	public boolean isArchive() {
		return archive;
	}

	/**
	 * @param archive the archive to set
	 */
	public void setArchive(boolean archive) {
		this.archive = archive;
	}

	/**
	 * @return the archiveFolder
	 */
	public String getArchiveFolder() {
		return archiveFolder;
	}

	/**
	 * @param archiveFolder the archiveFolder to set
	 */
	public void setArchiveFolder(String archiveFolder) {
		this.archiveFolder = archiveFolder;
	}

	/**
	 * @return the tag
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * @param tag the tag to set
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 * @return the answerToAll
	 */
	public boolean isAnswerToAll() {
		return answerToAll;
	}

	/**
	 * @param answerToAll the answerToAll to set
	 */
	public void setAnswerToAll(boolean answerToAll) {
		this.answerToAll = answerToAll;
	}

	/**
	 * @return the allowExternal
	 */
	public boolean isAllowExternal() {
		return allowExternal;
	}

	/**
	 * @param allowExternal the allowExternal to set
	 */
	public void setAllowExternal(boolean allowExternal) {
		this.allowExternal = allowExternal;
	}

	/**
	 * @return the members
	 */
	public ForeignCollection<Member> getMembers() {
		return members;
	}

	/**
	 * @param members the members to set
	 */
	public void setMembers(ForeignCollection<Member> members) {
		this.members = members;
	}
	
	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public String toString() {
		return this.getName();
	}
	
}
