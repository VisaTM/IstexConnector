package fr.inist.istex;


import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.logging.log4j.*;

import toolbox.json.*;
import toolbox.json.JsonObject.*;



/**
 * La classe {@link IstexSimpleIterator} implémente un itérateur sur les résultats d'une recherche ISTEX en mode scroll (voir
 * {@link "https://api.istex.fr/documentation/results/#pagination-de-type-scroll"}).<br>
 * <br>
 * Constats:
 * <ul>
 * <li>Le nombre <i>total</i> de résultats peut fluctuer selon les pages, mais cela ne semble pas avoir de conséquences.
 * <li>Si le nombre total de résultats est un multiple du nombre de résultat par page, la dernière page est vide (zéro <code>hits</code>, pas de <code>noMoreScrollResults</code> ni de
 * <code>nextScrollURI</code>).
 * <li>La syntaxe de <code>nextScrollURI</code> est: <code>https://api.istex.fr/document/?q=...&size=...&output=...&defaultOperator=...&scroll=...&scrollId=...</code>. Les paramètres
 * <code>size</code>, <code>output</code>, <code>defaultOperator</code> et <code>scroll</code> sont ignorés et peuvent être omis, mais si ils sont présents, il doivent avoir des valeurs valides. Le
 * paramètre <code>q</code> doit être présent, avec toujours la même valeur. Modifier <code>q</code> lance une nouvelle recherche.
 * <li>Les <code>aggregations</code> des facettes ne sont présentes que sur la première page.
 * </ul>
 * L'accès à ISTEX se faisant par réseau, des erreurs peuvent survenir, de façon d'autant plus probable que le nombre de résultat est important. ISTEX n'offrant pas de possibilité de reprise, la
 * recherche doit être complètement relancée.
 * @author Ludovic WALLE
 */
public class IstexSimpleIterator extends IstexIterator {



	/**
	 * @param query Requète, ne doit être ni vide ni ni <code>null</code>. Voir {@link "https://api.istex.fr/documentation/search/"}.
	 * @param output Données à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/results/#selection-des-champs-renvoyes"}.
	 * @param facets Facettes à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/facets/"}.
	 * @throws IstexException En cas d'erreur de parcours des résultats.
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
			throw new IstexException(LOGGER, Level.ERROR, "Pour la requète \"" + query + "\", le résultat " + count + " est null.");
		} else {
			count++;
			return hit;
		}
	}



	/**
	 * Prépare l'itérateur sur les résultats de la page correspondant à l'URL indiquée.<br>
	 * @param url URL.
	 * @return <code>true</code> si la page contient au moins un résultat, <code>false</code> sinon (le parcours est terminé).
	 * @throws IstexException En cas d'erreur de parcours des résultats.
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
					// erreur signalée par ISTEX
					throw new IstexException(LOGGER, Level.ERROR, "Erreur ISTEX: " + json.toString());
				} else {
					// réponse normale
					// extraire les informations attendues
					total = json.cutInteger("total", Option.PRESENT_AND_NOT_NULL_AND_NOT_EMPTY).intValue();
					noMoreScrollResults = json.cutBoolean("noMoreScrollResults");
					nextScrollURI = json.cutString("nextScrollURI");
					hits = json.cutJsonArray("hits", Option.PRESENT_AND_NOT_NULL);
					scroll = json.cutString("scroll");
					scrollId = json.cutString("scrollId");
					aggregations = json.cutJsonObject("aggregations");
					// vérifier qu'il n'y en a pas d'autres que celles attendues
					if (json.isNotEmpty()) {
						throw new IstexException(LOGGER, Level.WARN, "Pour la requète \"" + query + "\", des éléments de la réponse ISTEX ne sont pas pris en compte: " + json.toString());
					}
					// vérifier les assertions sur le fonctionnement d'ISTEX
					if (LOGGER.isInfoEnabled()) {
						// nombre total de résultats
						if ((this.total != -1) && (total != this.total)) {
							LOGGER.log(Level.INFO, "Pour la requète \"" + query + "\", le nombre de réponse total (" + total + ") n'est pas celui attendu (" + this.total + ").");
						}
						// durée de persistence
						if (!SCROLL.equals(scroll) && (nextScrollURI != null)) {
							LOGGER.log(Level.INFO, "Pour la requète \"" + query + "\", la durée de persistence (" + scroll + ") n'est pas celle attendue (" + SCROLL + ").");
						}
						// identifiant de balayage
						if (this.scrollId == null) {
							this.scrollId = scrollId;
						} else if (!this.scrollId.equals(scrollId) && (nextScrollURI != null)) {
							LOGGER.log(Level.INFO, "Pour la requète \"" + query + "\", l'identifiant de balayage (" + scrollId + ") n'est pas celui attendu (" + this.scrollId + ").");
						}
						// aggregations
						if ((this.aggregations != null) && (aggregations != null)) {
							LOGGER.log(Level.INFO, "Pour la requète \"" + query + "\", une agrégation (" + aggregations.serialize() + ") est présente alors qu'elle a déjà été présente dans une page précédente (" + this.aggregations.serialize() + ").");
						}
						// URL de la page suivante
						if ((nextScrollURI != null) && !nextScrollURI.equals(uri = ("https://api.istex.fr/document/?q=" + normalize(URLEncoder.encode(query, "UTF-8")) + "&size=" + SIZE + ((output != null) ? "&output=" + output : "") + ((facets != null) ? "&facet=" + facets : "") + "&defaultOperator=OR&scroll=" + SCROLL + "&scrollId=" + this.scrollId))) {
							LOGGER.log(Level.INFO, "Pour la requète \"" + query + "\", l'URI d'accès à la page suivante (" + nextScrollURI + ") n'est pas celle attendue (" + uri + ").");
						}
						// indication d'existence de page suivante et lien vers la page suivante
						if (((noMoreScrollResults != null) && (noMoreScrollResults.booleanValue() == false)) != (nextScrollURI != null)) {
							LOGGER.log(Level.INFO, "Pour la requète \"" + query + "\", noMoreScrollResults est " + noMoreScrollResults + " et nextScrollURI est " + nextScrollURI + ".");
						}
						// nombre de résultats dans une page intermédiaire
						if ((nextScrollURI != null) && (hits.size() != SIZE)) {
							LOGGER.log(Level.INFO, "Pour la requète \"" + query + "\", une page qui n'est pas la dernière contient " + hits.size() + " résultats au lieu de " + SIZE + ".");
						}
						// nombre de résultats dans la dernière page
						if ((nextScrollURI == null) && (hits.size() > SIZE)) {
							LOGGER.log(Level.INFO, "Pour la requète \"" + query + "\", la dernière page contient " + hits.size() + " résultats alors qu'elle ne devrait en contenir au maximum que " + SIZE + ".");
						}
					}
					// mémoriser le nombre total initial de résultat
					if (this.total == -1) {
						this.total = total;
					}
					if (this.aggregations == null) {
						this.aggregations = aggregations;
					}
					if (nextScrollURI == null) {
						// c'est la dernière page (elle peut être vide)
						if ((this.count + hits.size()) != this.total) {
							throw new IstexException(LOGGER, Level.ERROR, "Pour la requète \"" + query + "\", le nombre de documents retournés (" + (this.count + hits.size()) + ") est différent de celui attendu (" + this.total + ").");
						}
					} else if (hits.isEmpty()) {
						// ce n'est pas la dernière page mais elle est vide
						throw new IstexException(LOGGER, Level.ERROR, "Pour la requète \"" + query + "\", la page ne contient aucun résultat (hits est vide).");
					} else if ((this.count + hits.size()) > this.total) {
						// il y a trop de résultats
						throw new IstexException(LOGGER, Level.ERROR, "Pour la requète \"" + query + "\", le nombre de documents retournés est supérieur à celui attendu (" + this.total + ").");
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
	 * Itérateur sur une page de réponse ISTEX, <code>null</code> si il n'y a plus d'éléments à retourner (y compris pour cause d'erreur). La valeur est initialement <code>null</code>, puis elle
	 * contient un itérateur sur les hits d'une réponse ISTEX, puis <code>null</code> lorsque tous les résultats ont été récupérés ou en cas d'erreur.
	 */
	private Iterator<Json> iterator = null;



	/**
	 * URL pour accéder à la page suivante, <code>null</code> si il n'y en a pas. La valeur est initialement <code>null</code>, puis elle prend la première valeur reçue, puis <code>null</code> lorsque
	 * tous les résultats ont été récupérés.
	 */
	private String nextScrollURI = null;



	/**
	 * Identifiant de balayage. La valeur est initialement <code>null</code>, puis elle prend la première valeur reçue.
	 */
	private String scrollId = null;



	/**
	 * Durée de persistence (voir {@link "https://api.istex.fr/documentation/results/#pagination-de-type-scroll"}).
	 */
	private static final String SCROLL = "5m";



	/**
	 * Taille des pages (voir {@link "https://api.istex.fr/documentation/results/#pagination-de-type-scroll"}).
	 */
	private static final int SIZE = 100;



}
