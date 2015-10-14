package imailList;

import imailList.model.Server;

import java.util.Properties;

import javax.mail.AuthenticationFailedException;
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
	 * @throws InterruptedException 
	 */
	protected void connect() throws MessagingException, InterruptedException {
		Properties props = System.getProperties();

		// Get a Session object
		Session session = Session.getInstance(props, null);
		// session.setDebug(true);

		// Get a Store object
		store = session.getStore("imap");

		int retries = 5;
		// Connect
		while (!store.isConnected()) {
			try {
				store.connect(server.getHost(), server.getUser(), server.getPassword());
				System.out.println(String.format("[%d:%s@%s] Connected.",
						server.getId(), server.getUser(), server.getHost()));
			} catch (AuthenticationFailedException e) {
				throw e;
			} catch (MessagingException e) {
				System.out.println(String.format("[%d:%s@%s] Connection failed: %s",
						server.getId(), server.getUser(), server.getHost(), e.getMessage()));
				if (retries > 0) {
					retries--;
					System.out.println(String.format("[%d:%s@%s] Retry connect in 10 sek.",
							server.getId(), server.getUser(), server.getHost()));
					Thread.sleep(10000);
				} else {
					throw e;
				}
			}
		}
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
		System.out.println(String.format("[%d:%s@%s] folder %s opened.",
				server.getId(), server.getUser(), server.getHost(), server.getListenFolder()));

		// Add messageCountListener to listen for new messages
		folder.addMessageCountListener(new MessageCountAdapter() {
			public void messagesAdded(MessageCountEvent ev) {
				Message[] msgs = ev.getMessages();
				System.out.println(String.format("[%d:%s@%s] Got %d new messages.",
						server.getId(), server.getUser(), server.getHost(), msgs.length));

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
		System.out.println(String.format("[%d:%s@%s] Starting..", server.getId(), server.getUser(), server.getHost()));
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
					System.out.println(String.format("[%d:%s@%s] Start IDLE..", server.getId(), server.getUser(), server.getHost()));
					f.idle();
					System.out.println(String.format("[%d:%s@%s] IDLE returned.", server.getId(), server.getUser(), server.getHost()));
				}
			} catch (FolderClosedException fex) {
				System.out.println(String.format("[%d:%s@%s] Folder closed. Reason: %s",
						server.getId(), server.getUser(), server.getHost(), fex.getMessage()));
			} catch (MessagingException mex) {
				System.out.println(String.format("[%d:%s@%s] IDLE disabled: not supported by server.", server.getId(), server.getUser(), server.getHost()));
				supportsIdle = false;
			}
			
			//wait time in seconds for retry after exception. Doubles with each exception, resets on connect.
			int waitInterval = 1;
			
			while (true) {
				try {
					System.out.println(String.format("[%d:%s@%s] iterate listening loop.", server.getId(), server.getUser(), server.getHost()));

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
						System.out.println(String.format("[%d:%s@%s] Start IDLE..", server.getId(), server.getUser(), server.getHost()));
						f.idle();
						System.out.println(String.format("[%d:%s@%s] IDLE returned.", server.getId(), server.getUser(), server.getHost()));
					} else {
						System.out.println(String.format("[%d:%s@%s] Run without IDLE.", server.getId(), server.getUser(), server.getHost()));
						Thread.sleep(server.getCheckFrequency() * 1000);

						// This is to force the IMAP server to send us
						// EXISTS notifications.
						folder.getMessageCount();
					}
				} catch (FolderClosedException e) {
					System.out.println(String.format("[%d:%s@%s] Folder closed. Reason: %s",
							server.getId(), server.getUser(), server.getHost(), e.getMessage()));
				} catch (AuthenticationFailedException e) {
					throw e;
				} catch (MessagingException e) {
					System.out.println(String.format("[%d:%s@%s] Server error, attempt later retry: %s",
							server.getId(), server.getUser(), server.getHost(), e.getMessage()));
					
					Thread.sleep(waitInterval * 1000);
					waitInterval *= 2;
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuthenticationFailedException e) {
			System.out.println(String.format("[%d:%s@%s] Authentification failed: %s.",
					server.getId(), server.getUser(), server.getHost(), e.getMessage()));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
