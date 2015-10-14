package imailList;

import imailList.model.MailingList;
import imailList.model.Server;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.mail.Address;
import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import com.google.common.collect.Sets;

public class IncommingMailHandler extends Thread {

	private Server server;
	private HashMap<String, MailingList> lists = new HashMap<String, MailingList>();

	public IncommingMailHandler(Server server) {
		this.server = server;
		
		this.start();
	}
	
	public void handle (Message message) {
		
		System.out.println(String.format("[%d:%s@%s] Incomming message: %d",
				server.getId(), server.getUser(), server.getHost(), message.getMessageNumber()));
		try {
			//Don't handle a mail if it's deleted (processed) or flagged (deferred) 
			if (message.isSet(Flag.DELETED) || message.isSet(Flag.FLAGGED)) {
				System.out.println(String.format("[%d:%s@%s] Msg %d: Skipped (already known).",
						server.getId(), server.getUser(), server.getHost(), message.getMessageNumber()));
				return;
			}
		} catch (MessagingException e) {
			System.out.println(String.format("[%d:%s@%s] Msg %d: Can't check message flags: %s.",
					server.getId(), server.getUser(), server.getHost(), message.getMessageNumber(), e.getMessage()));
			return;
		}
		
		System.out.println(String.format("[%d:%s@%s] Msg %d is unhandled.",
				server.getId(), server.getUser(), server.getHost(), message.getMessageNumber()));
			
		Set<MailingList> lists = getAffectedLists(message);
		
		System.out.println(String.format("[%d:%s@%s] Msg %d: Sent to lists %s",
				server.getId(), server.getUser(), server.getHost(), message.getMessageNumber(), lists.toString()));
		
		for (MailingList list : lists) {
			new ListMessageHandler(server, list, message);
		}
		
		try {
			message.setFlag(Flag.SEEN, true);
			message.setFlag(Flag.FLAGGED, true);
		} catch (MessagingException e) {
			System.out.println(String.format("[%d:%s@%s] Msg %d: Can't set message flags: %s.",
					server.getId(), server.getUser(), server.getHost(), message.getMessageNumber(), e.getMessage()));
		}
	}
		

	private Set<MailingList> getAffectedLists(Message m) {
	
		this.refreshMap();
		
		Set<MailingList> affLists = new HashSet<>();
		
		Set<Address> receivers = new HashSet<>();
		
		try {
			Address[] receiverArray = m.getAllRecipients();
			if (receiverArray == null || receiverArray.length == 0) {
				System.out.println(String.format("[%d:%s@%s] Msg %d: No receivers!",
						server.getId(), server.getUser(), server.getHost(), m.getMessageNumber()));
				return affLists;
			}
			receivers.addAll(Arrays.asList(receiverArray));
		} catch (MessagingException e) {
			System.out.println(String.format("[%d:%s@%s] Msg %d: Can't extract receivers: %s",
					server.getId(), server.getUser(), server.getHost(), m.getMessageNumber(), e.getMessage()));
		}		
		
		System.out.println(String.format("[%d:%s@%s] Msg %d: Search for receiving lists in %d receivers.",
				server.getId(), server.getUser(), server.getHost(), m.getMessageNumber(), receivers.size()));
		
		for (Address add : receivers) {
			if (lists.containsKey(((InternetAddress)add).getAddress())) {
				affLists.add(lists.get(((InternetAddress)add).getAddress().toLowerCase()));
			}
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
		try {
			server.getLists().refreshCollection();
		} catch (SQLException e) {
			System.out.println(String.format("[%d:%s@%s] Can't refresh mailing lists, using cached: %s.",
					server.getId(), server.getUser(), server.getHost(), e.getMessage()));
			return;
		}
		
		lists.clear();
		for (MailingList list : server.getLists()) {
			lists.put(list.getListenAddress().toLowerCase(), list);
		}
		
		System.out.println(String.format("[%d:%s@%s] Mailing lists refreshed.",
				server.getId(), server.getUser(), server.getHost()));
		
	}

}
