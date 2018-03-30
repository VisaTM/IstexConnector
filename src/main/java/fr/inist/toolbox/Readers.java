package fr.inist.toolbox;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.*;



/**
 * La classe {@link Readers} des méthodes permettant de lire des données.
 * @author Ludovic WALLE
 */
public class Readers {



	/**
	 * Retourne les octets provenant du flux indiqué.
	 * @param input Flux à lire.
	 * @return Les octets provenant du flux indiqué.
	 * @throws IOException
	 */
	public static byte[] getBytesFromStream(InputStream input) throws IOException {
		byte[] bytes = new byte[1024 * 1024];
		int byteCount = 0;
		int byteRead;

		while ((byteRead = input.read(bytes, byteCount, bytes.length - byteCount)) != -1) {
			byteCount += byteRead;
			if (byteCount == bytes.length) {
				bytes = Arrays.copyOf(bytes, byteCount + (1024 * 1024));
			}
		}
		return Arrays.copyOf(bytes, byteCount);
	}



	/**
	 * Retourne les octets correspondant à l'url indiquée.
	 * @param headers Entêtes éventuels (peut être <code>null</code>).
	 * @param url URL.
	 * @return Les octets reçus.
	 * @throws IOException
	 */
	public static byte[] getBytesFromURL(Map<String, String> headers, String url) throws IOException {
		try (InputStream input = getStreamForURL(headers, url)) {
			return getBytesFromStream(input);
		}
	}



	/**
	 * Retourne les octets correspondant à l'url indiquée.
	 * @param url URL.
	 * @return Les octets reçus.
	 * @throws IOException
	 */
	public static byte[] getBytesFromURL(String url) throws IOException {
		try (InputStream input = getConnection(url).getInputStream()) {
			return Readers.getBytesFromStream(input);
		}
	}



	/**
	 * Retourne les octets correspondant à l'url indiquée.
	 * @param url URL.
	 * @return Les octets reçus.
	 * @throws IOException
	 */
	public static HttpURLConnection getConnection(String url) throws IOException {
		HttpURLConnection connection;
		int retry = 10;

		while ((((connection = (HttpURLConnection) new URL(url).openConnection()).getResponseCode() / 100) == 3) && ((url = connection.getHeaderField("Location")) != null)) {
			if (retry-- ==0) {
				throw new IOException ("Trop de redirections.");
			}
		}
		return connection;
	}



	/**
	 * Retourne les octets correspondant à l'url indiquée.
	 * @param headers Entêtes éventuels (peut être <code>null</code>).
	 * @param url URL.
	 * @return Le flux correspondant à l'url indiquée.
	 * @throws IOException
	 */
	public static InputStream getStreamForURL(Map<String, String> headers, String url) throws IOException {
		HttpURLConnection connection;

		connection = getConnection(url);
		if (headers != null) {
			for (Iterator<Entry<String, String>> headersIterator = headers.entrySet().iterator(); headersIterator.hasNext();) {
				Entry<String, String> header = headersIterator.next();
				connection.setRequestProperty(header.getKey(), header.getValue());
			}
		}
		return connection.getInputStream();
	}



}
