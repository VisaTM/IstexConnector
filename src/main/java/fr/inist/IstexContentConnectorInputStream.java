package fr.inist;

import java.io.*;
import java.util.*;

import fr.inist.istex.*;
import toolbox.json.*;



/**
 * La classe {@link IstexContentConnectorInputStream} implémente un flux pouvant être utilisé en retour de {@code IstexContentConnector#fetchMetadata(eu.openminted.content.connector.Query)}. Il
 * contient les metadonnées issues d'une recherche ISTEX converties en OMTD-SHARE.
 * @author Ludovic WALLE
 */
public class IstexContentConnectorInputStream extends InputStream {



	/**
	 * Recherche ISTEX sans résultats.
	 */
	public IstexContentConnectorInputStream() {
		istexIterator = null;
		bytes = "<publications/>".getBytes();
		index = 0;
	}



	/**
	 * Recherche ISTEX avec potentiellement des résultats.
	 * @param istexQuery Requète, ne doit être ni vide ni ni <code>null</code>. Voir {@link "https://api.istex.fr/documentation/search/"}.
	 * @param istexFacets Facettes à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/facets/"}.
	 */
	public IstexContentConnectorInputStream(String istexQuery, String istexFacets) {
		istexIterator = new IstexSimpleIterator(istexQuery, "*", istexFacets);
		if (istexIterator.hasNext()) {
			bytes = "<publications>".getBytes();
		} else {
			istexIterator = null;
			bytes = "<publications/>".getBytes();
		}
		index = 0;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public int available() throws IOException {
		return bytes.length - index;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public void close() throws IOException {
		istexIterator = null;
		bytes = new byte[0];
		index = 1;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public int read() throws IOException {
		Map<String, Json> ignoredFragmentByPath = new HashMap<>();

		if (index >= bytes.length) {
			if (istexIterator == null) {
				return -1;
			} else if (istexIterator.hasNext()) {
				bytes = IstexToOmtdShareConverter.convert(Istex.newHit(istexIterator.next(), ignoredFragmentByPath), false, "\t").getBytes();
			} else {
				istexIterator = null;
				bytes = "</publications>".getBytes();
			}
			index = 0;
		}
		return bytes[index++];
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public int read(@SuppressWarnings("hiding") byte[] bytes, int off, int len) throws IOException {
		Map<String, Json> ignoredFragmentByPath = new HashMap<>();
		int byteCount = 0;

		if (len > 0) {
			if (index >= this.bytes.length) {
				if (istexIterator == null) {
					return -1;
				} else if (istexIterator.hasNext()) {
					this.bytes = IstexToOmtdShareConverter.convert(Istex.newHit(istexIterator.next(), ignoredFragmentByPath), false, "\t").getBytes();
				} else {
					istexIterator = null;
					this.bytes = "</publications>".getBytes();
				}
				index = 0;
			}
			if ((this.bytes.length - index) < len) {
				byteCount = this.bytes.length - index;
			} else {
				byteCount = len;
			}
			for (int i = 0; i < byteCount; i++) {
				bytes[off + i] = this.bytes[index++];
			}
		}
		return byteCount;
	}



	/**
	 * Octets en cours d'envoi par le flux.
	 */
	private byte[] bytes;



	/**
	 * Index dans les octets en cours d'envoi par le flux, ou -1 si tout a été retourné.
	 */
	private int index = 0;



	/**
	 * Itérateur sur les métadonnées ISTEX, ou <code>null</code> si tout a été retourné.
	 */
	private IstexIterator istexIterator;



}
