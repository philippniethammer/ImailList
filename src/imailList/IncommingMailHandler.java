package imailList;

import imailList.model.MailingList;
import imailList.model.Server;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.mail.Address;
import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

public class IncommingMailHandler extends Thread {

	private Server server;
	private HashMap<String, MailingList> lists = new HashMap<String, MailingList>();

	public IncommingMailHandler(Server server) {
		this.server = server;
		
		this.start();
	}
	
	public void handle (Message message) {
		
		System.out.println("Server #" + server.getId() + ": New incomming message: " + message.getMessageNumber());
		try {
			//Don't handle a mail if it's deleted (processed) or flagged (deferred) 
			if (message.isSet(Flag.DELETED) || message.isSet(Flag.FLAGGED)) {
				System.out.println("Message "+ message.getMessageNumber() +": Message already known.");
				return;
			}
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			System.out.println("Message "+ message.getMessageNumber() +": Message exception while checken flags");
			e.printStackTrace();
			return;
		}
			
		Set<MailingList> lists = getAffectedLists(message);
		
		System.out.println("Message "+ message.getMessageNumber() +": Affected lists:" + lists.toString());
		
		for (MailingList list : lists) {
			new ListMessageHandler(server, list, message);
		}
	}
		

	private Set<MailingList> getAffectedLists(Message m) {
	
		Set<MailingList> affLists = new HashSet<MailingList>();
		
		try {
			Address[] adds =  m.getRecipients(RecipientType.TO);
			if (adds != null) {
				for (Address add : adds) {
					if (lists.containsKey(((InternetAddress)add).getAddress())) {
						affLists.add(lists.get(((InternetAddress)add).getAddress()));
					}
				}
			}
			
			adds =  m.getRecipients(RecipientType.CC);
			if (adds != null) {
				for (Address add : adds) {
					if (lists.containsKey(((InternetAddress)add).getAddress())) {
						affLists.add(lists.get(((InternetAddress)add).getAddress()));
					}
				}
			}
			
			adds =  m.getRecipients(RecipientType.BCC);
			if (adds != null) {
				for (Address add : adds) {
					if (lists.containsKey(((InternetAddress)add).getAddress())) {
						affLists.add(lists.get(((InternetAddress)add).getAddress()));
					}
				}
			}
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return affLists;
	}
	
	public void run() {
		while (!this.isInterrupted()) {
			refreshMap();
			try {
				Thread.sleep(Integer.parseInt(Config.getInstance().getProperty("syncIntervall", "300"))*1000);
			} catch (InterruptedException e) {
				this.interrupt();
				break;
			}
		}
	}

	private void refreshMap() {
		System.out.println("Server #"+server.getId()+": Syncing lists.");
		try {
			server.getLists().refreshCollection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		lists.clear();
		for (MailingList list : server.getLists()) {
			lists.put(list.getListenAddress(), list);
		}
		
	}

}
