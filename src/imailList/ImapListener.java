package imailList;

import imailList.model.Server;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;

import com.sun.mail.imap.IMAPFolder;

public class ImapListener extends Thread {

	Session session;
	Store store;
	Folder folder;
	IncommingMailHandler mh;
	private Server server;

	public ImapListener(Server server) {
		this.server = server;
		this.mh = new IncommingMailHandler(server);
		this.start();
	}

	protected void connect() throws MessagingException {
		Properties props = System.getProperties();

		// Get a Session object
		Session session = Session.getInstance(props, null);
		// session.setDebug(true);

		// Get a Store object
		store = session.getStore("imap");

		// Connect
		store.connect(server.getHost(), server.getUser(), server.getPassword());
		System.out.println("Connected to Server #" + server.getId());
	}

	protected void installFolderListener() throws Exception {

		// Open a Folder
		folder = store.getFolder(server.getListenFolder());
		if (folder == null || !folder.exists()) {
			throw new Exception("Folder does not exist.");
		}

		folder.open(Folder.READ_WRITE);

		// Add messageCountListener to listen for new messages
		folder.addMessageCountListener(new MessageCountAdapter() {
			public void messagesAdded(MessageCountEvent ev) {
				Message[] msgs = ev.getMessages();
				System.out.println("Got " + msgs.length + " new messages");

				// Just dump out the new messages
				for (Message m : msgs) {
					mh.handle(m);
				}
			}
		});
	}

	public void run() {
		System.out.println("Starting server #" + server.getId() + " Host: "
				+ server.getHost() + " User: " + server.getHost());
		while (true) {
			try {
				if (store == null || !store.isConnected()) {
					connect();
				}
				installFolderListener();

				boolean supportsIdle = false;
				try {
					if (folder instanceof IMAPFolder) {
						IMAPFolder f = (IMAPFolder) folder;
						f.idle();
						supportsIdle = true;
					}
				} catch (FolderClosedException fex) {
					fex.printStackTrace();
				} catch (MessagingException mex) {
					supportsIdle = false;
				}
				while (true) {
					if (store == null || !store.isConnected()) {
						connect();
					}
					if (folder == null || !folder.isOpen()) {
						installFolderListener();
					}
					
					if (supportsIdle && server.isUseIdle()
							&& folder instanceof IMAPFolder) {
						IMAPFolder f = (IMAPFolder) folder;
						f.idle();
						System.out.println("IDLE done");
					} else {
						Thread.sleep(server.getCheckFrequency() * 1000); // sleep
																			// for
																			// freq
																			// milliseconds

						// This is to force the IMAP server to send us
						// EXISTS notifications.
						folder.getMessageCount();
					}
				}
			} catch (FolderClosedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MessagingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
