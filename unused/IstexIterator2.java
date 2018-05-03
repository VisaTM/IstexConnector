package fr.inist.istex;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.logging.log4j.*;

import fr.inist.toolbox.*;
import fr.inist.toolbox.json.*;
import fr.inist.toolbox.json.JsonObject.*;



/**
 * La classe {@link IstexIterator2} implémente un itérateur sur une recherche ISTEX parcourant les résultats, soit en mode scroll (voir
 * {@link "https://api.istex.fr/documentation/results/#pagination-de-type-scroll"}), si le nombre de résultats est supérieur à {@link #LIMIT}, en mode simple sinon.<br>
 * Une requète préliminaire est effectuée pour connaitre le nombre de résultats total, ce qui permet de choisir le mode de parcours des résultats. Elle sert aussi à obtenir les aggrégations
 * correspondantes aux éventuelles facettes.
 * @author Ludovic WALLE
 */
public class IstexIterator2 extends IstexIterator {



	/**
	 * @param query Requète, ne doit être ni vide ni ni <code>null</code>. Voir {@link "https://api.istex.fr/documentation/search/"}.
	 * @param output Données à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/results/#selection-des-champs-renvoyes"}.
	 * @param facets Facettes à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/facets/"}.
	 * @throws IstexException En cas d'erreur de parcours des résultats.
	 */
	public IstexIterator2(String query, String output, String facets) throws IstexException {
		super(query, output, facets);

		JsonObject json;
		JsonArray hits;

		try {
			json = JsonObject.parse(new String(Readers.getBytesFromURL("https://api.istex.fr/document/?size=0&q=" + URLEncoder.encode(query, "UTF-8") + ((facets != null) ? "&facets=" + URLEncoder.encode(facets, "UTF-8") : ""))).trim());
			if (json.has("_error")) {
				throw new IstexException(LOGGER, Level.ERROR, "Erreur ISTEX: " + json.toString());
			} else {
				total = json.cutInteger("total", Option.PRESENT_AND_NOT_NULL_AND_NOT_EMPTY);
				aggregations = json.cutJsonObject("aggregations");
				hits = json.cutJsonArray("hits", Option.PRESENT_AND_NOT_NULL);
				if (hits.isNotEmpty()) {
					throw new IstexException(LOGGER, Level.WARN, "Pour la requète initiale de \"" + query + "\", il y a des réponses: " + json.toString());
				}
				if (json.isNotEmpty()) {
					throw new IstexException(LOGGER, Level.WARN, "Pour la requète initiale de \"" + query + "\", des éléments de la réponse ISTEX ne sont pas pris en compte: " + json.toString());
				}
			}
			if (total == 0) {
				iterator = null;
			} else if (total < LIMIT) {
				LOGGER.log(Level.INFO, "unique");
				uniqueIterator("https://api.istex.fr/document/?size=" + LIMIT + "&q=" + ((query != null) ? URLEncoder.encode(query, "UTF-8") : "") + ((output != null) ? "&output=" + URLEncoder.encode(output, "UTF-8") : ""));
			} else {
				LOGGER.log(Level.INFO, "pages");
				nextIterator("https://api.istex.fr/document/?scroll=" + SCROLL + "&size=" + SIZE + "&q=" + ((query != null) ? URLEncoder.encode(query, "UTF-8") : "") + ((output != null) ? "&output=" + URLEncoder.encode(output, "UTF-8") : ""));
			}
		} catch (JsonException | IOException exception) {
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
	 * Prépare un itérateur sur les résultats de la page correspondant à l'URL indiquée.<br>
	 * @param url URL.
	 * @return <code>true</code> si la page contient au moins un résultat, <code>false</code> sinon (le parcours est terminé).
	 * @throws IstexException En cas d'erreur de parcours des résultats.
	 */
	private synchronized boolean nextIterator(String url) throws IstexException {
		JsonObject json = null;
		JsonArray hits;
		String scroll;
		Boolean noMoreScrollResults;
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
					total = json.cutInteger("total", Option.PRESENT_AND_NOT_NULL_AND_NOT_EMPTY);
					noMoreScrollResults = json.cutBoolean("noMoreScrollResults");
					nextScrollURI = json.cutString("nextScrollURI");
					hits = json.cutJsonArray("hits", Option.PRESENT_AND_NOT_NULL);
					scroll = json.cutString("scroll");
					scrollId = json.cutString("scrollId");
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
						// URL de la page suivante
						if ((nextScrollURI != null) && !nextScrollURI.equals(uri = ("https://api.istex.fr/document/?q=" + normalize(URLEncoder.encode(query, "UTF-8")) + "&size=" + SIZE + ((output != null) ? "&output=" + output : "") + "&defaultOperator=OR&scroll=" + SCROLL + "&scrollId=" + this.scrollId))) {
							LOGGER.log(Level.INFO, "Pour la requète \"" + query + "\", l'URI d'accès à la page suivante (" + nextScrollURI + ") n'est pas celle attendue (" + uri + ").");
						}
						// indication d'existence de page suivante et lien vers la page suivante
						if (((noMoreScrollResults != null) && (noMoreScrollResults.booleanValue() == false)) != (nextScrollURI != null)) {
							LOGGER.log(Level.INFO, "Pour la requète \"" + query + "\", noMoreScrollResults est " + noMoreScrollResults + " et nextScrollURI est " + nextScrollURI + ".");
						}
						// pas le nombre de résultats dans une page intermédiaire
						if ((nextScrollURI != null) && (hits.size() != SIZE)) {
							LOGGER.log(Level.INFO, "Pour la requète \"" + query + "\", une page qui n'est pas la dernière contient " + hits.size() + " résultats au lieu de " + SIZE + ".");
						}
						// trop de résultats dans la dernière page
						if ((nextScrollURI == null) && (hits.size() > SIZE)) {
							LOGGER.log(Level.INFO, "Pour la requète \"" + query + "\", la dernière page contient " + hits.size() + " résultats alors qu'elle ne devrait en contenir au maximum que " + SIZE + ".");
						}
					}
					if (nextScrollURI == null) {
						// c'est la dernière page (elle peut être vide)
						if ((this.count + hits.size()) != this.total) {
							throw new IstexException(LOGGER, Level.ERROR, "Pour la requète \"" + query + "\", le nombre de documents retournés (" + (this.count + hits.size()) + ") est différent de celui attendu (" + this.total + ").");
						}
						if (hits.size() != SIZE) {
							throw new IstexException(LOGGER, Level.ERROR, "Pour la requète \"" + query + "\", la dernière page contient " + hits.size() + " résultats alors qu'elle ne devrait en contenir au maximum que " + SIZE + ".");
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
	 * Prépare l'unique itérateur sur les résultats de la recherche non paginée correspondant à l'URL indiquée.<br>
	 * Il doit y avoir au moins un résultat.
	 * @param url URL (ne doit pas être <code>null</code>).
	 * @throws IstexException En cas d'erreur de parcours des résultats ou si il n'y a aucun résultat.
	 */
	private synchronized void uniqueIterator(String url) throws IstexException {
		JsonObject json = null;
		JsonArray hits;
		@SuppressWarnings("hiding") int total;
		String uri;
		String firstPageUri;
		String lastPageUri;

		try {
			json = JsonObject.parse(new String(Readers.getBytesFromURL(url)).trim());
			if (json.has("_error")) {
				throw new IstexException(LOGGER, Level.ERROR, "Erreur ISTEX: " + json.toString());
			} else {
				total = json.cutInteger("total", Option.PRESENT_AND_NOT_NULL_AND_NOT_EMPTY);
				firstPageUri = json.cutString("firstPageURI", Option.PRESENT_AND_NOT_NULL_AND_NOT_EMPTY);
				lastPageUri = json.cutString("lastPageURI", Option.PRESENT_AND_NOT_NULL_AND_NOT_EMPTY);
				hits = json.cutJsonArray("hits", Option.PRESENT_AND_NOT_NULL);
				if (json.isNotEmpty()) {
					throw new IstexException(LOGGER, Level.WARN, "Pour la requète \"" + query + "\", des éléments de la réponse ISTEX ne sont pas pris en compte: " + json.toString());
				}
				if (LOGGER.isInfoEnabled()) {
					if (!firstPageUri.equals(uri = ("https://api.istex.fr/document/?q=" + normalize(URLEncoder.encode(query, "UTF-8")) + "&size=" + LIMIT + ((output != null) ? "&output=" + output : "") + "&defaultOperator=OR&from=0"))) {
						LOGGER.log(Level.INFO, "Pour la requète \"" + query + "\", l'URI d'accès à la première page (" + firstPageUri + ") n'est pas celle attendue (" + uri + ").");
					}
					if (!lastPageUri.equals(uri = ("https://api.istex.fr/document/?q=" + normalize(URLEncoder.encode(query, "UTF-8")) + "&size=" + LIMIT + ((output != null) ? "&output=" + output : "") + "&defaultOperator=OR&from=0"))) {
						LOGGER.log(Level.INFO, "Pour la requète \"" + query + "\", l'URI d'accès à la dernière page (" + lastPageUri + ") n'est pas celle attendue (" + uri + ").");
					}
				}
				if (total != this.total) {
					throw new IstexException(LOGGER, Level.ERROR, "Pour la requète \"" + query + "\", le nombre de réponse total (" + total + ") n'est pas celui attendu (" + this.total + ").");
				}
				iterator = hits.iterator();
			}
		} catch (JsonException | JsonRuntimeException | IOException exception) {
			throw new IstexException(LOGGER, Level.ERROR, exception);
		}
	}



	/**
	 * Itérateur sur une page de réponse ou l'unique réponse ISTEX, <code>null</code> si il n'y a plus d'éléments à retourner (y compris pour cause d'erreur).
	 */
	private Iterator<Json> iterator = null;



	/**
	 * URL pour accéder à la page suivante, <code>null</code> si il n'y en a pas.
	 */
	private String nextScrollURI = null;



	/**
	 * Identifiant de balayage.
	 */
	private String scrollId;



	/**
	 * Limite au dessus de laquelle le parcours des résultat se fait en mode scroll. Doit être inférieur à 10000, le mode scroll étant imposé par ISTEX au delà.
	 */
	public static final int LIMIT = 5000;



	/**
	 * Durée de persistence.
	 */
	private static final String SCROLL = "5m";



	/**
	 * Taille des pages en mode scroll.
	 */
	private static final int SIZE = 100;



}
