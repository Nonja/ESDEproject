package com.henrikhedberg.hbdp.client;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * HTTP Bidirectional Protocol connection.
 *
 * @author Henrik Hedberg <henrik.hedberg@iki.fi>
 * @version 1.2 (20150506)
 */
public class HbdpConnection {
	private URL url;
	private URL session;
	private int serial;
	private HbdpOutputStream outputStream;
	private HbdpInputStream inputStream;
	private IOException exception;

	/* Synchronized */
	private int sending;
	private int receiving;
	private boolean closing;
	private boolean closed;

	/**
	 * Constructs a new connection to the specified HBDP URL.
	 *
	 * @param url the URL of the HBDP server
	 */
	public HbdpConnection(URL url) {
		this.url = url;
		outputStream = new HbdpOutputStream();
		inputStream = new HbdpInputStream();
		new HbdpThread().start();
	}

	/**
	 * Returns the output stream for writing data to the HBDP server.
	 *
	 * Closing the output stream closes the connection and thus, also the input stream.
	 *
	 * @return the output stream for writing data
	 */
	public OutputStream getOutputStream() {
		return outputStream;
	}

	/**
	 * Returns the input stream for reading data from the HBDP server.
	 *
	 * Closing the input stream closes the connection and thus, also the output stream.
	 *
	 * @return the input stream for reading data
	 */
	public InputStream getInputStream() {
		return inputStream;
	}

	/**
	 * Closes the connections and thus, also the input and output streams.
	 */
	public synchronized void close() {
		closing = true;
		notify();
	}

	private synchronized URL waitUntilRequest() throws InterruptedException, MalformedURLException {
		while (sending > 0 ||
			   (outputStream.available() == 0 && receiving > 0 && !closing && exception == null)) {
			wait();
		}
		if (exception != null || closed)
			return null;

		sending++;
		receiving++;

		return new URL(session + "/" + serial++);
	}

	private synchronized void sendingDone() {
		sending--;
		notify();
	}

	private synchronized void receivingDone() {
		receiving--;
		notify();
	}

	private synchronized void stateChanged() {
		notify();
	}

	private synchronized void exceptionOccurred(IOException exception) {
		this.exception = exception;
		notifyAll();
		synchronized (inputStream) {
			inputStream.notify();
		}
	}

	private class HbdpOutputStream extends OutputStream {
		private byte[] bytes;
		private int written;

		public HbdpOutputStream() {
			bytes = new byte[256];
		}

		public int available() {
			return written;
		}

		@Override
		public void close() {
			HbdpConnection.this.close();
		}

		@Override
		public synchronized void write(byte[] b, int offset, int length) throws IOException {
			if (exception != null)
				throw exception;
			if (closing)
				throw new IOException("The HBDP connection is closed.");

			ensureCapacity(written + length);
			System.arraycopy(b, offset, bytes, written, length);
			written += length;
			stateChanged();
		}

		@Override
		public synchronized void write(int b) throws IOException {
			if (exception != null)
				throw exception;
			if (closing)
				throw new IOException("The HBDP connection is closed.");

			ensureCapacity(written + 1);
			bytes[written++] = (byte)b;
			stateChanged();
		}

		public synchronized void writeTo(OutputStream output) throws IOException {
			output.write(bytes, 0, written);
			written = 0;
		}

		private void ensureCapacity(int capacity) {
			if (bytes.length >= capacity)
				return;

			int newLength = 2 * bytes.length;
			while (newLength < capacity)
				newLength *= 2;
			bytes = Arrays.copyOf(bytes, newLength);
		}
	}

	private class HbdpInputStream extends InputStream {
		private byte[] bytes;
		private int position;
		private int readable;

		HbdpInputStream() {
			bytes = new byte[256];
		}

		@Override
		public void close() {
			HbdpConnection.this.close();
		}

		@Override
		public synchronized int read(byte[] b, int offset, int length) throws IOException {
			if (exception != null)
				throw exception;

			while (readable == 0) {
				if (closing)
					return -1;
				try {
					wait();
				} catch (InterruptedException e) {
					throw new IOException(e);
				}
				if (exception != null)
					throw exception;
			}

			int give = length < readable ? length : readable;
			if (position + give <= bytes.length ) {
				System.arraycopy(bytes, position, b, offset, give);
			} else {
				int chunk = bytes.length - position;
				System.arraycopy(bytes, position, b, offset, chunk);
				System.arraycopy(bytes, 0, b, offset + chunk, give - chunk);
			}
			position = (position + give) % bytes.length;
			readable -= give;

			return give;
		}

		@Override
		public synchronized int read() throws IOException {
			if (exception != null)
				throw exception;

			while (readable == 0) {
				if (closing)
					return -1;
				try {
					wait();
				} catch (InterruptedException e) {
					throw new IOException(e);
				}
				if (exception != null)
					throw exception;
			}

			int value = bytes[position];
			position = (position + 1) % bytes.length;
			readable--;

			return value;
		}

		public synchronized void readFrom(InputStream input) throws IOException {
			while (true) {
				if (readable == bytes.length) {
					byte[] newBytes = new byte[2 * bytes.length];
					System.arraycopy(bytes, 0, newBytes, 0, position);
					System.arraycopy(bytes, position, newBytes, position + bytes.length, bytes.length - position);
					position += bytes.length;
					bytes = newBytes;
				}
			
				int offset = (position + readable) % bytes.length;
				int length = (offset < position ? position : bytes.length) - offset;
				int got = input.read(bytes, offset, length);
				if (got == -1)
					break;
				readable += got;
			}
			notify();
		}
	}

	private class HbdpThread extends Thread {
		@Override
		public void run() {
			try {
				if (session == null)
					connect();
				loop();
			} catch (InterruptedException e) {
				exceptionOccurred(new IOException(e));
			} catch (IOException e) {
				exceptionOccurred(e);
			}
		}

		private void connect() throws IOException {
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.connect();

			InputStream input = connection.getInputStream();
			byte[] bytes = new byte[64];
			int length = 0;
			while (true) {
				int got = input.read(bytes, length, bytes.length - length);
				if (got == -1)
					break;
				length += got;
			}

			connection.disconnect();

			if (length == 0) {
				exceptionOccurred(new IOException("Could not establish a HBDP session."));
				return;
			}
			
			String id = new String(bytes, 0, length, Charset.forName("UTF-8"));
			session = new URL(url + id);

			new HbdpThread().start();
		}

		private void loop() throws InterruptedException, IOException {
			URL request;
			while ((request = waitUntilRequest()) != null) {
				HttpURLConnection connection = (HttpURLConnection)request.openConnection();
				if (outputStream.available() == 0 && closing)
					doDelete(connection);
				else
					doPost(connection);

				receivingDone();
			}
		}
		
		private void doDelete(HttpURLConnection connection) throws IOException {
			connection.setRequestMethod("DELETE");
			connection.connect();
			connection.disconnect();

			closed = true;
			sendingDone();
		}

		private void doPost(HttpURLConnection connection) throws IOException {
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.connect();
			OutputStream os = connection.getOutputStream();
			outputStream.writeTo(os);
			os.close();
			sendingDone();

			InputStream is = connection.getInputStream();
			inputStream.readFrom(is);
			connection.disconnect();
		}
	}
}
