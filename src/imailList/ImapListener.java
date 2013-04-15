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

	/**
	 * Connect to IMAP server.
	 * 
	 * @throws MessagingException
	 */
	protected void connect() throws MessagingException {
		Properties props = System.getProperties();

		// Get a Session object
		Session session = Session.getInstance(props, null);
		// session.setDebug(true);

		// Get a Store object
		store = session.getStore("imap");

		// Connect
		store.connect(server.getHost(), server.getUser(), server.getPassword());
		System.out.println("Server #" + server.getId()+ ": connected.");
	}

	/**
	 * Add new message listener for folder.
	 * 
	 * @throws Exception
	 */
	protected void installFolderListener() throws Exception {

		// Open a Folder
		folder = store.getFolder(server.getListenFolder());
		if (folder == null || !folder.exists()) {
			throw new Exception("Folder does not exist.");
		}

		folder.open(Folder.READ_WRITE);
		System.out.println("Server #" + server.getId()+ ": folder opened.");

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

	/**
	 * Main loop.
	 * 
	 * Opens the folder to listen on. If activated and possible, goes to IDLE,
	 * else rechecks folder after the given interval. 
	 * 
	 */
	public void run() {
		System.out.println("Starting server #" + server.getId() + " Host: "
				+ server.getHost() + " User: " + server.getHost());
		try {
			if (store == null || !store.isConnected()) {
				connect();
			}
			installFolderListener();

			boolean supportsIdle = false;
			try {
				if (folder instanceof IMAPFolder) {
					IMAPFolder f = (IMAPFolder) folder;
					supportsIdle = true;
					System.out.println("Server #"+server.getId()+": Go into IDLE mode.");
					f.idle();
					System.out.println("Server #"+server.getId()+": IDLE done");
				}
			} catch (FolderClosedException fex) {
				System.out.println("Server #"+server.getId()+": Folder closed. Reason: "+fex.getMessage());
			} catch (MessagingException mex) {
				supportsIdle = false;
			}
			
			//wait time in seconds for retry after exception. Doubles with each exception, resets on connect.
			int waitInterval = 1;
			
			while (true) {
				try {
					System.out.println("Server #"+server.getId()+": [Debug] iteration.");

					if (store == null || !store.isConnected()) {
						connect();
					}
					if (folder == null || !folder.isOpen()) {
						installFolderListener();
					}
					
					waitInterval = 1;

					if (supportsIdle && server.isUseIdle()
							&& folder instanceof IMAPFolder) {
						IMAPFolder f = (IMAPFolder) folder;
						System.out.println("Server #"+server.getId()+": Go into IDLE mode.");
						f.idle();
						System.out.println("Server #"+server.getId()+": IDLE done");
					} else {
						System.out.println("Server #"+server.getId()+": Run without IDLE.");
						Thread.sleep(server.getCheckFrequency() * 1000);

						// This is to force the IMAP server to send us
						// EXISTS notifications.
						folder.getMessageCount();
					}
				} catch (FolderClosedException e) {
					System.out.println("Server #"+server.getId()+": Folder closed. Reason: "+e.getMessage());
				} catch (MessagingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Thread.sleep(waitInterval * 1000);
					waitInterval *= 2;
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
