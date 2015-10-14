package imailList.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class Member {

	@DatabaseField(generatedId=true)
	private int id;
	
	@DatabaseField
	private String name;
	
	@DatabaseField
	private String mail;
	
	@DatabaseField
	private boolean send;
	
	@DatabaseField
	private boolean receive;
	
	@DatabaseField(foreign=true)
	private MailingList list;
	
	public Member() {
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
	 * @return the mail
	 */
	public String getMail() {
		return mail;
	}
	/**
	 * @param mail the mail to set
	 */
	public void setMail(String mail) {
		this.mail = mail;
	}
	
	/**
	 * @return the send state
	 */
	public boolean canSend() {
		return send;
	}
	/**
	 * @param send the send state to set
	 */
	public void setSend(boolean send) {
		this.send = send;
	}
	
	/**
	 * @return the send state
	 */
	public boolean canReceive() {
		return receive;
	}
	/**
	 * @param active the active to set
	 */
	public void setReceive(boolean receive) {
		this.receive = receive;
	}
	
	public String toString() {
		return this.getName() + " <" + this.getMail() + ">";
	}
	
}
