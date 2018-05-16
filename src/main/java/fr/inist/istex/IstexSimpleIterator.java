package fr.inist.istex;


import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.logging.log4j.*;

import toolbox.json.*;
import toolbox.json.JsonObject.*;



/**
 * La classe {@link IstexSimpleIterator} impl�mente un it�rateur sur les r�sultats d'une recherche ISTEX en mode scroll (voir
 * {@link "https://api.istex.fr/documentation/results/#pagination-de-type-scroll"}).<br>
 * <br>
 * Constats:
 * <ul>
 * <li>Le nombre <i>total</i> de r�sultats peut fluctuer selon les pages, mais cela ne semble pas avoir de cons�quences.
 * <li>Si le nombre total de r�sultats est un multiple du nombre de r�sultat par page, la derni�re page est vide (z�ro <code>hits</code>, pas de <code>noMoreScrollResults</code> ni de
 * <code>nextScrollURI</code>).
 * <li>La syntaxe de <code>nextScrollURI</code> est: <code>https://api.istex.fr/document/?q=...&size=...&output=...&defaultOperator=...&scroll=...&scrollId=...</code>. Les param�tres
 * <code>size</code>, <code>output</code>, <code>defaultOperator</code> et <code>scroll</code> sont ignor�s et peuvent �tre omis, mais si ils sont pr�sents, il doivent avoir des valeurs valides. Le
 * param�tre <code>q</code> doit �tre pr�sent, avec toujours la m�me valeur. Modifier <code>q</code> lance une nouvelle recherche.
 * <li>Les <code>aggregations</code> des facettes ne sont pr�sentes que sur la premi�re page.
 * </ul>
 * L'acc�s � ISTEX se faisant par r�seau, des erreurs peuvent survenir, de fa�on d'autant plus probable que le nombre de r�sultat est important. ISTEX n'offrant pas de possibilit� de reprise, la
 * recherche doit �tre compl�tement relanc�e.
 * @author Ludovic WALLE
 */
public class IstexSimpleIterator extends IstexIterator {



	/**
	 * @param query Requ�te, ne doit �tre ni vide ni ni <code>null</code>. Voir {@link "https://api.istex.fr/documentation/search/"}.
	 * @param output Donn�es � retourner, ignor� si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/results/#selection-des-champs-renvoyes"}.
	 * @param facets Facettes � retourner, ignor� si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/facets/"}.
	 * @throws IstexException En cas d'erreur de parcours des r�sultats.
	 */
	public IstexSimpleIterator(String query, String output, String facets) throws IstexException {
		super(query, output, facets);

		try {
			nextIterator("https://api.istex.fr/document/?scroll=" + SCROLL + "&size=" + SIZE + "&q=" + URLEncoder.encode(query, "UTF-8") + ((output != null) ? "&output=" + URLEncoder.encode(output, "UTF-8") : "") + ((facets != null) ? "&facet=" + URLEncoder.encode(facets, "UTF-8") : ""));
		} catch (UnsupportedEncodingException exception) {
			throw new IstexException(LOGGER, Level.ERROR, exception);
		}
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public boolean hasNext() throws IstexException {
		return (iterator != null) && (iterator.hasNext() || nextIterator(nextScrollURI));
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public JsonObject next() throws IstexException {
		JsonObject hit;

		if (iterator == null) {
			throw new NoSuchElementException();
		} else if ((hit = (JsonObject) iterator.next()) == null) {
			throw new IstexException(LOGGER, Level.ERROR, "Pour la requ�te \"" + query + "\", le r�sultat " + count + " est null.");
		} else {
			count++;
			return hit;
		}
	}



	/**
	 * Pr�pare l'it�rateur sur les r�sultats de la page correspondant � l'URL indiqu�e.<br>
	 * @param url URL.
	 * @return <code>true</code> si la page contient au moins un r�sultat, <code>false</code> sinon (le parcours est termin�).
	 * @throws IstexException En cas d'erreur de parcours des r�sultats.
	 */
	private synchronized boolean nextIterator(String url) throws IstexException {
		JsonObject json = null;
		JsonArray hits;
		String scroll;
		Boolean noMoreScrollResults;
		@SuppressWarnings("hiding") JsonObject aggregations;
		@SuppressWarnings("hiding") String scrollId;
		@SuppressWarnings("hiding") int total;
		String uri;

		try {
			iterator = null;
			if (url != null) {
				json = JsonObject.parse(new String(Readers.getBytesFromURL(url)).trim());
				if (json.has("_error")) {
					// erreur signal�e par ISTEX
					throw new IstexException(LOGGER, Level.ERROR, "Erreur ISTEX: " + json.toString());
				} else {
					// r�ponse normale
					// extraire les informations attendues
					total = json.cutInteger("total", Option.PRESENT_AND_NOT_NULL_AND_NOT_EMPTY).intValue();
					noMoreScrollResults = json.cutBoolean("noMoreScrollResults");
					nextScrollURI = json.cutString("nextScrollURI");
					hits = json.cutJsonArray("hits", Option.PRESENT_AND_NOT_NULL);
					scroll = json.cutString("scroll");
					scrollId = json.cutString("scrollId");
					aggregations = json.cutJsonObject("aggregations");
					// v�rifier qu'il n'y en a pas d'autres que celles attendues
					if (json.isNotEmpty()) {
						throw new IstexException(LOGGER, Level.WARN, "Pour la requ�te \"" + query + "\", des �l�ments de la r�ponse ISTEX ne sont pas pris en compte: " + json.toString());
					}
					// v�rifier les assertions sur le fonctionnement d'ISTEX
					if (LOGGER.isInfoEnabled()) {
						// nombre total de r�sultats
						if ((this.total != -1) && (total != this.total)) {
							LOGGER.log(Level.INFO, "Pour la requ�te \"" + query + "\", le nombre de r�ponse total (" + total + ") n'est pas celui attendu (" + this.total + ").");
						}
						// dur�e de persistence
						if (!SCROLL.equals(scroll) && (nextScrollURI != null)) {
							LOGGER.log(Level.INFO, "Pour la requ�te \"" + query + "\", la dur�e de persistence (" + scroll + ") n'est pas celle attendue (" + SCROLL + ").");
						}
						// identifiant de balayage
						if (this.scrollId == null) {
							this.scrollId = scrollId;
						} else if (!this.scrollId.equals(scrollId) && (nextScrollURI != null)) {
							LOGGER.log(Level.INFO, "Pour la requ�te \"" + query + "\", l'identifiant de balayage (" + scrollId + ") n'est pas celui attendu (" + this.scrollId + ").");
						}
						// aggregations
						if ((this.aggregations != null) && (aggregations != null)) {
							LOGGER.log(Level.INFO, "Pour la requ�te \"" + query + "\", une agr�gation (" + aggregations.serialize() + ") est pr�sente alors qu'elle a d�j� �t� pr�sente dans une page pr�c�dente (" + this.aggregations.serialize() + ").");
						}
						// URL de la page suivante
						if ((nextScrollURI != null) && !nextScrollURI.equals(uri = ("https://api.istex.fr/document/?q=" + normalize(URLEncoder.encode(query, "UTF-8")) + "&size=" + SIZE + ((output != null) ? "&output=" + output : "") + ((facets != null) ? "&facet=" + facets : "") + "&defaultOperator=OR&scroll=" + SCROLL + "&scrollId=" + this.scrollId))) {
							LOGGER.log(Level.INFO, "Pour la requ�te \"" + query + "\", l'URI d'acc�s � la page suivante (" + nextScrollURI + ") n'est pas celle attendue (" + uri + ").");
						}
						// indication d'existence de page suivante et lien vers la page suivante
						if (((noMoreScrollResults != null) && (noMoreScrollResults.booleanValue() == false)) != (nextScrollURI != null)) {
							LOGGER.log(Level.INFO, "Pour la requ�te \"" + query + "\", noMoreScrollResults est " + noMoreScrollResults + " et nextScrollURI est " + nextScrollURI + ".");
						}
						// nombre de r�sultats dans une page interm�diaire
						if ((nextScrollURI != null) && (hits.size() != SIZE)) {
							LOGGER.log(Level.INFO, "Pour la requ�te \"" + query + "\", une page qui n'est pas la derni�re contient " + hits.size() + " r�sultats au lieu de " + SIZE + ".");
						}
						// nombre de r�sultats dans la derni�re page
						if ((nextScrollURI == null) && (hits.size() > SIZE)) {
							LOGGER.log(Level.INFO, "Pour la requ�te \"" + query + "\", la derni�re page contient " + hits.size() + " r�sultats alors qu'elle ne devrait en contenir au maximum que " + SIZE + ".");
						}
					}
					// m�moriser le nombre total initial de r�sultat
					if (this.total == -1) {
						this.total = total;
					}
					if (this.aggregations == null) {
						this.aggregations = aggregations;
					}
					if (nextScrollURI == null) {
						// c'est la derni�re page (elle peut �tre vide)
						if ((this.count + hits.size()) != this.total) {
							throw new IstexException(LOGGER, Level.ERROR, "Pour la requ�te \"" + query + "\", le nombre de documents retourn�s (" + (this.count + hits.size()) + ") est diff�rent de celui attendu (" + this.total + ").");
						}
					} else if (hits.isEmpty()) {
						// ce n'est pas la derni�re page mais elle est vide
						throw new IstexException(LOGGER, Level.ERROR, "Pour la requ�te \"" + query + "\", la page ne contient aucun r�sultat (hits est vide).");
					} else if ((this.count + hits.size()) > this.total) {
						// il y a trop de r�sultats
						throw new IstexException(LOGGER, Level.ERROR, "Pour la requ�te \"" + query + "\", le nombre de documents retourn�s est sup�rieur � celui attendu (" + this.total + ").");
					}
					if (hits.isNotEmpty()) {
						iterator = hits.iterator();
					}
				}
			}
		} catch (JsonException | JsonRuntimeException | IOException exception) {
			iterator = null;
			throw new IstexException(LOGGER, Level.ERROR, exception);
		}
		return iterator != null;
	}



	/**
	 * It�rateur sur une page de r�ponse ISTEX, <code>null</code> si il n'y a plus d'�l�ments � retourner (y compris pour cause d'erreur). La valeur est initialement <code>null</code>, puis elle
	 * contient un it�rateur sur les hits d'une r�ponse ISTEX, puis <code>null</code> lorsque tous les r�sultats ont �t� r�cup�r�s ou en cas d'erreur.
	 */
	private Iterator<Json> iterator = null;



	/**
	 * URL pour acc�der � la page suivante, <code>null</code> si il n'y en a pas. La valeur est initialement <code>null</code>, puis elle prend la premi�re valeur re�ue, puis <code>null</code> lorsque
	 * tous les r�sultats ont �t� r�cup�r�s.
	 */
	private String nextScrollURI = null;



	/**
	 * Identifiant de balayage. La valeur est initialement <code>null</code>, puis elle prend la premi�re valeur re�ue.
	 */
	private String scrollId = null;



	/**
	 * Dur�e de persistence (voir {@link "https://api.istex.fr/documentation/results/#pagination-de-type-scroll"}).
	 */
	private static final String SCROLL = "5m";



	/**
	 * Taille des pages (voir {@link "https://api.istex.fr/documentation/results/#pagination-de-type-scroll"}).
	 */
	private static final int SIZE = 100;



}
