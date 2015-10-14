package imailList;

import imailList.model.MailingList;
import imailList.model.Member;
import imailList.model.Server;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.sun.mail.imap.IMAPFolder;

public class ListMessageHandler extends Thread {

	private Message message;
	private MailingList list;
	private Server server;
	private Store store;
	
	private String bounce;

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
			System.out.println(String.format("[%d:%s@%s/%s] Can't refresh member list: %s",
					server.getId(), server.getUser(), server.getHost(), list.getName(), e.getMessage()));
		}
		
		if (!message.getFolder().isOpen()) {
			try {
				message.getFolder().open(Folder.READ_WRITE);
			} catch (MessagingException e) {
				System.out.println(String.format("[%d:%s@%s/%s] Can't open folder: %s",
						server.getId(), server.getUser(), server.getHost(), list.getName(), e.getMessage()));
			}
		}

		if (!isValid()) {
			System.out.println(String.format("[%d:%s@%s/%s] Msg %d: Ignored.",
					server.getId(), server.getUser(), server.getHost(), list.getName(), message.getMessageNumber()));
			return;
		}
		
		try {
			System.out.println(String.format("[%d:%s@%s/%s] Msg %d: Subject: %s",
					server.getId(), server.getUser(), server.getHost(), list.getName(),
					message.getMessageNumber(), message.getSubject()));
		} catch (MessagingException e) {}

		if (list.isArchive()) {
			archivate();
		}

		handleMessage();
	}

	private boolean isValid() {
		try {
			if (!list.isAllowExternal()) {
				Set<String> froms = Sets.newHashSet(Collections2.transform(Arrays.asList(message.getFrom()),
						new Function<Address, String>() {
							@Override
							public String apply(Address input) {
								return ((InternetAddress) input).getAddress();
							}
				}));
				
				
				boolean ok = false;
				for (Member m : list.getMembers()) {
					if (froms.contains(m.getMail().toLowerCase()) && m.canSend()) {
						ok = true;
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
			System.out.println(String.format("[%d:%s@%s/%s] Msg %d: Error while validating: %s",
					server.getId(), server.getUser(), server.getHost(), list.getName(), message.getMessageNumber(), e.getMessage()));
		}
		return true;
	}

	private void handleMessage() {
		try {
			MimeMessage msg = new MimeMessage((MimeMessage) this.message);
			
			String subject = msg.getSubject();
			
			subject = removePrefixQueues(subject);
			
			if (list.getTag() != null && !list.getTag().isEmpty()
					&& !isTagged(subject)) {
				Pattern p = Pattern.compile("^((?:(?:RE|AW|FWD|WG)^?[0-9]*: *)*)",
						Pattern.CASE_INSENSITIVE);
				subject = p.matcher(subject).replaceFirst("$1 " + list.getTag() + " ").trim();
				
			}

			msg.setSubject(subject);
			
			bounce = "<" + list.getListenAddress().replace("@", "+bounces@") + ">";

			//msg.setHeader("Sender", list.getListenAddress());
			msg.addHeader("X-BeenThere", list.getListenAddress());
			msg.removeHeader("Delivered-To");
			msg.removeHeader("Return-Path");
			msg.setHeader("Return-Path", bounce);
			msg.setHeader("Precedence", "list");
			msg.setHeader("Mailing-list", "list " + list.getListenAddress());
			msg.setHeader("List-Id", list.getName() + "<" + list.getListenAddress() + ">");
			msg.setHeader("Errors-To", bounce);
			msg.setHeader("List-Post", "<mailto:" + list.getListenAddress() + ">");

			if (list.isAnswerToAll()) {
				msg.setReplyTo(new Address[] { new InternetAddress(list
						.getListenAddress()) });
			} else {
				msg.setReplyTo(msg.getFrom());
			}
			
			List<Address> recipients = getRecipients();
			System.out.println(String.format("[%d:%s@%s/%s] Msg %d: Send message to %d recipients",
					server.getId(), server.getUser(), server.getHost(), list.getName(), message.getMessageNumber(), recipients.size()));
			sendMessage(msg, recipients);

		} catch (MessagingException e) {
			System.out.println("Got error: " + e.getMessage());
			e.printStackTrace();
		}

	}

	private void sendMessage(Message message, List<Address> recipients) {
		Properties props = System.getProperties();
		
		//Send if some recipients failed check.
		props.put("mail.smtp.sendpartial", true);
		
		// Set "MAIL FROM" address for return-path
		props.put("mail.smtp.from", bounce);

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
			System.out.println("Got error: " + e.getMessage());
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
			
			Set<String> tos = new HashSet<String>();
			Address[] recipients = message.getAllRecipients();
			if (recipients != null && recipients.length > 0) {
				for (Address to : recipients) {
					tos.add(((InternetAddress)to).getAddress().toLowerCase());
				}
			}
			
			List<Address> recs = new ArrayList<Address>();
			for (Member member : list.getMembers()) {
				if (!member.canReceive()) {
					continue;
				}
				if (this.list.isExcludeSender() && froms.contains(member.getMail().toLowerCase())) {
					continue;
				}
				if (this.list.isPreventMultipleSends() && tos.contains(member.getMail().toLowerCase())) {
					continue;
				}
				recs.add(new InternetAddress(member.getMail(), member.getName()));
			}
			
			return recs;
		} catch (MessagingException e) {
			System.out.println("Got error: " + e.getMessage());
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;	
	}

	private void archivate() {
		try {
			System.out.println(String.format("[%d:%s@%s/%s] Msg %d: Archivate to: %s",
					server.getId(), server.getUser(), server.getHost(), list.getName(),
					message.getMessageNumber(), list.getArchiveFolder()));
			
			ImapConnect();
			IMAPFolder folder = (IMAPFolder) store.getFolder(list
					.getArchiveFolder());

			if (!folder.exists() && !folder.create(Folder.HOLDS_MESSAGES)) {
				System.out.println(String.format("[%d:%s@%s/%s] Can't create archive folder %s.",
						server.getId(), server.getUser(), server.getHost(), list.getName(), list.getArchiveFolder()));
				return;
			}

			folder.open(Folder.READ_WRITE);

			MimeMessage aMessage = new MimeMessage((MimeMessage) message);

			String subject = aMessage.getSubject();
			if (isTagged(subject)) {
				System.out.println(String.format("[%d:%s@%s/%s] Msg %d: Removing tag for archive.",
						server.getId(), server.getUser(), server.getHost(), list.getName(),
						message.getMessageNumber()));
				
				subject = subject.replaceFirst(Pattern.quote(list.getTag()) + " ?", "");
				aMessage.setSubject(subject);
			}

			aMessage.saveChanges();
			
			folder.appendMessages(new Message[] { aMessage });
			
			aMessage.setFlag(Flag.DELETED, false);
			aMessage.setFlag(Flag.FLAGGED, false);
			aMessage.setFlag(Flag.SEEN, true);
			
		} catch (MessagingException e) {
			System.out.println("Got error: " + e.getMessage());
			e.printStackTrace();
			return;
		} finally {
			try {
				store.close();
			} catch (MessagingException e) {
				System.out.println("Got error: " + e.getMessage());
				e.printStackTrace();
			}
		}

	}

	private boolean isTagged(String subject) {
		if (list.getTag() == null || list.getTag().isEmpty()) {
			return false;
		}
		Pattern p = Pattern.compile(
				"^(?i:(?:RE|AW|FWD|WG)^?[0-9]*: *)*" + Pattern.quote(list.getTag()));
		return p.matcher(subject).find();

	}

	private String removePrefixQueues(String subject) {
		String result;
		Pattern pRE = Pattern.compile(
				"((?i:RE|AW)^?[0-9]*:)\\s*("+ Pattern.quote(list.getTag()) +")?"
				+ "\\s*(?:(?i:RE|AW)^?[0-9]*:\\s*(?:"+ Pattern.quote(list.getTag()) + ")?\\s*)*");
		Pattern pFWD = Pattern.compile(
				"((?i:FWD|WG)^?[0-9]*:)\\s*("+ Pattern.quote(list.getTag()) +")?"
				+ "\\s*(?:(?i:FWD|WG)^?[0-9]*:\\s*(?:"+ Pattern.quote(list.getTag()) + ")?\\s*)*");
		
		result =  pRE.matcher(subject).replaceAll("Re: $2 ");
		result =  pFWD.matcher(result).replaceAll("Fwd: $2 ");
		
		Pattern p = Pattern.compile(
				"^((?:(?i:RE|AW|FWD|WG)^?[0-9]*:\\s*)*(?i:RE|AW|FWD|WG)^?[0-9]*:)\\s*("+ Pattern.quote(list.getTag()) +")"
				+ "\\s*((?i:RE|AW|FWD|WG)^?[0-9]*:)\\s*(?:"+ Pattern.quote(list.getTag()) + "\\s*)?");
		Matcher m = p.matcher(result);
		while (m.find()) {
			result = m.replaceFirst("$1 $3 $2 ");
			m = p.matcher(result);
		}
		
		return result;
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
