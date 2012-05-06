package imailList;

import imailList.model.MailingList;
import imailList.model.Member;
import imailList.model.Server;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.imap.IMAPFolder;

public class ListMessageHandler extends Thread {

	private Message message;
	private MailingList list;
	private Server server;
	private Store store;

	public ListMessageHandler(Server server, MailingList list, Message message) {
		this.server = server;
		this.list = list;
		this.message = message;
		this.start();
	}

	public void run() {
		try {
			this.list.getMembers().refreshCollection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (!isValid()) {
			System.out.println("List: " + list + ", Message "+ message.getMessageNumber() +": Not valid.");
			return;
		}

		if (list.isArchive()) {
			archivate();
		}

		handleMessage();
	}

	private boolean isValid() {
		try {
			if (!list.isAllowExternal()) {
				Address[] froms = message.getFrom();
				boolean ok = false;
				for (Address from : froms) {
					for (Member m : list.getMembers()) {
						if (((InternetAddress) from).getAddress()
								.equalsIgnoreCase(m.getMail())) {
							ok = true;
							break;
						}
					}
					if (ok) {
						break;
					}
				}
				if (!ok) {
					return false;
				}
			}
			if (message.getHeader("X-BeenThere") != null
					&& message.getHeader("X-BeenThere").toString().toLowerCase()
					.contains(list.getListenAddress().toLowerCase())) {
				return false;
			}
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	private void handleMessage() {
		try {
			MimeMessage msg = new MimeMessage((MimeMessage) this.message);
			
			String subject = msg.getSubject();
			if (list.getTag() != null && !list.getTag().isEmpty()
					&& !isTagged(subject)) {
				Pattern p = Pattern.compile("^(((?:[rR][eE]|[aA][wW]|[fF][wW])^?[0-9]*:) *).*",
						Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(subject);
				if (m.find()) {
					subject = m.group(2) + " " + list.getTag() + " "
							+ subject.substring(m.group(1).length());
				} else {
					subject = list.getTag() + " " + subject;
				}
				msg.setSubject(subject);
			}

			msg.setHeader("Sender", list.getListenAddress());
			msg.addHeader("X-BeenThere", list.getListenAddress());
			msg.setHeader("Return-Path",
					"<" + list.getListenAddress().replace("@", "+bounces@") + ">");
			msg.setHeader("Precedence", "list");
			msg.setHeader("Mailing-list", "list " + list.getListenAddress());
			msg.setHeader("List-ID", "<" + list.getListenAddress() + ">");

			if (list.isAnswerToAll()) {
				msg.setReplyTo(new Address[] { new InternetAddress(list
						.getListenAddress()) });
			} else {
				msg.setReplyTo(msg.getFrom());
			}
			
			List<Address> recipients = getRecipients();
			sendMessage(msg, recipients);

		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void sendMessage(Message message, List<Address> recipients) {
		Properties props = System.getProperties();

	    // Get a Session object
	    Session session = Session.getInstance(props, null);
	    
	    Transport tr;
		try {
			tr = session.getTransport("smtp");
			tr.connect(server.getHost(), server.getUser(), server.getPassword());
			message.saveChanges();
			tr.sendMessage(message, recipients.toArray(new Address[0]));
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}

	private List<Address> getRecipients() {
		try {
			Address[] fromAdd = message.getFrom();
			Set<String> froms = new HashSet<String>();
			
			for (Address from : fromAdd) {
				froms.add(((InternetAddress)from).getAddress().toLowerCase());
			}
			
			List<Address> recs = new ArrayList<Address>();
			for (Member member : list.getMembers()) {
				if (froms.contains(member.getMail().toLowerCase()) || !member.isActive()) {
					continue;
				}
				recs.add(new InternetAddress(member.getMail(), member.getName()));
			}
			
			return recs;
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;	
	}

	private void archivate() {
		try {
			System.out.println("List: " + list + ", Message "+ message.getMessageNumber() +": Archivating to "+list.getArchiveFolder());
			
			ImapConnect();
			IMAPFolder folder = (IMAPFolder) store.getFolder(list
					.getArchiveFolder());

			if (!folder.exists() && !folder.create(Folder.HOLDS_MESSAGES)) {
				System.err.println("Can't create archive folder "
						+ list.getArchiveFolder());
				return;
			}

			folder.open(Folder.READ_WRITE);

			MimeMessage aMessage = new MimeMessage((MimeMessage) message);

			String subject = aMessage.getSubject();
			if (isTagged(subject)) {
				System.out.println("List: " + list + ", Message "+ message.getMessageNumber() +": Message is tagged.");
				subject = subject
						.replaceFirst(Pattern.quote(list.getTag()) + " ?", "");
				aMessage.setSubject(subject);
			}

			aMessage.saveChanges();
			
			folder.appendMessages(new Message[] { aMessage });
			
			aMessage.setFlag(Flag.DELETED, false);
			aMessage.setFlag(Flag.FLAGGED, false);
			aMessage.setFlag(Flag.SEEN, true);
			
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} finally {
			try {
				store.close();
			} catch (MessagingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private boolean isTagged(String subject) {
		if (list.getTag() == null || list.getTag().isEmpty()) {
			return false;
		}
		Pattern p = Pattern.compile(
				"^((?:[rR][eE]|[aA][wW]|[fF][wW])^?[0-9]*: *)?" + Pattern.quote(list.getTag()) + ".*");
		return p.matcher(subject).matches();

	}

	protected void ImapConnect() throws MessagingException {
		Properties props = System.getProperties();

		// Get a Session object
		Session session = Session.getInstance(props, null);
		// session.setDebug(true);

		// Get a Store object
		store = session.getStore("imap");

		// Connect
		store.connect(server.getHost(), server.getUser(), server.getPassword());
	}
}
