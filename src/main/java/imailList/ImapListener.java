package imailList;

import com.sun.mail.imap.SortTerm;
import imailList.model.Server;

import java.util.Properties;

import javax.mail.*;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.search.AndTerm;
import javax.mail.search.FlagTerm;

import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.SortTerm;

import static com.sun.mail.imap.SortTerm.REVERSE;

public class ImapListener extends Thread {

	Session session;
	IMAPStore store;
	IMAPFolder folder;
	IncommingMailHandler mh;
	private Server server;

	/**
	 * Runnable used to keep alive the connection to the IMAP server
	 *
	 * @author Juan Martín Sotuyo Dodero <jmsotuyo@monits.com>
	 */
	private static class KeepAliveRunnable implements Runnable {

	    private static final long KEEP_ALIVE_FREQ = 300000; // 5 minutes

	    private String msgPrefix;
	    private IMAPFolder folder;

	    public KeepAliveRunnable(IMAPFolder folder, String msgPrefix) {
		this.folder = folder;
		this.msgPrefix = msgPrefix;
	    }

	    @Override
	    public void run() {
		while (!Thread.interrupted()) {
		    try {
			Thread.sleep(KEEP_ALIVE_FREQ);

			// Perform a NOOP just to keep alive the connection
			System.out.println(String.format("%s Performing a NOOP to keep alvie the connection", msgPrefix));
			folder.doCommand(new IMAPFolder.ProtocolCommand() {
			    public Object doCommand(IMAPProtocol p)
				    throws ProtocolException {
				p.simpleCommand("NOOP", null);
				return null;
			    }
			});
		    } catch (InterruptedException e) {
			// Ignore, just aborting the thread...
		    } catch (MessagingException e) {
			// Shouldn't really happen...
			System.out.println(String.format("%s Unexpected exception while keeping alive the IDLE connection: %s", msgPrefix, e.getMessage()));
		    }
		}
	    }
	}

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
		props.putIfAbsent("mail.imap.connectiontimeout", "300000");

		// Get a Session object
		Session session = Session.getInstance(props, null);
		// session.setDebug(true);

		// Get a Store object
		URLName imapUrl = new URLName("imap", server.getHost(), -1, null, server.getUser(), server.getPassword());
		folder = null;
		store = new IMAPStore(session, imapUrl);

		int retries = 5;
		// Connect
		while (!store.isConnected()) {
			try {
				store.connect();
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
		folder = (IMAPFolder) store.getFolder(server.getListenFolder());
		if (folder == null || !folder.exists()) {
			throw new Exception("Folder does not exist.");
		}

		folder.open(Folder.READ_WRITE);
		System.out.println(String.format("[%d:%s@%s] folder %s opened.",
				server.getId(), server.getUser(), server.getHost(), server.getListenFolder()));

		// Get missed messages
		try {
			Message[] newMessages = folder.getSortedMessages(new SortTerm[] {SortTerm.REVERSE, SortTerm.ARRIVAL},
															 new AndTerm(new FlagTerm(new Flags(Flags.Flag.FLAGGED), false),
																    	 new FlagTerm(new Flags(Flags.Flag.DELETED), false)));
			if (newMessages != null) {
				System.out.println(String.format("[%d:%s@%s] Processing %d missed messages.",
						server.getId(), server.getUser(), server.getHost(), newMessages.length));
				for (Message msg : newMessages) {
					mh.handle(msg);
				}
			}
		} catch (MessagingException e) {
			System.out.println(String.format("[%d:%s@%s] Cannot search for missed messages: %s",
					server.getId(), server.getUser(), server.getHost(), e.getMessage()));
		}

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

		Thread keepAlive = null;
		try {
			if (store == null || !store.isConnected()) {
				connect();
			}
			installFolderListener();

			// We need to create a new thread to keep alive the connection
		    	keepAlive = new Thread(
				new KeepAliveRunnable(folder, String.format("[%d:%s@%s] ", server.getId(), server.getUser(), server.getHost())), "IdleConnectionKeepAlive"
		    	);

			keepAlive.start();

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
						if (keepAlive != null && keepAlive.isAlive()) {
							keepAlive.interrupt();
							keepAlive.join(1000);
						}
						keepAlive = new Thread(
							new KeepAliveRunnable(folder, String.format("[%d:%s@%s] ", server.getId(), server.getUser(), server.getHost())), "IdleConnectionKeepAlive"
		    				);

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
		} finally {
			// Shutdown keep alive thread
			if (keepAlive != null && keepAlive.isAlive()) {
				keepAlive.interrupt();
			}
		}

	}
}
