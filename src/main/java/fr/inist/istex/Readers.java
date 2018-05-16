package fr.inist.istex;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.*;



/**
 * La classe {@link Readers} des m�thodes permettant de lire des donn�es.
 * @author Ludovic WALLE
 */
public class Readers {



	/**
	 * Retourne les octets provenant du flux indiqu�.
	 * @param input Flux � lire.
	 * @return Les octets provenant du flux indiqu�.
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
	 * Retourne les octets provenant de l'url indiqu�e.
	 * @param url URL.
	 * @return Les octets re�us.
	 * @throws IOException
	 */
	public static byte[] getBytesFromURL(String url) throws IOException {
		try (InputStream input = getConnection(url).getInputStream()) {
			return Readers.getBytesFromStream(input);
		}
	}



	/**
	 * Retourne les octets provenant de l'url indiqu�e.
	 * @param url URL.
	 * @return Les octets re�us.
	 * @throws IOException
	 */
	public static HttpURLConnection getConnection(String url) throws IOException {
		HttpURLConnection connection;
		int retry = 10;

		while ((((connection = (HttpURLConnection) new URL(url).openConnection()).getResponseCode() / 100) == 3) && ((url = connection.getHeaderField("Location")) != null)) {
			if (retry-- == 0) {
				throw new IOException("Trop de redirections.");
			}
		}
		return connection;
	}



	/**
	 * Retourne les octets provenant de l'url indiqu�e.
	 * @param headers Ent�tes �ventuels (peut �tre <code>null</code>).
	 * @param url URL.
	 * @return Le flux provenant de l'url indiqu�e.
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
