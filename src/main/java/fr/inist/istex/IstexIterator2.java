package fr.inist.istex;

import java.io.*;
import java.net.*;
import java.util.*;

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
	 * @param query Requète, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/search/"}.
	 * @param output Données à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/results/#selection-des-champs-renvoyes"}.
	 * @param facets Facettes à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/facets/"}.
	 * @throws IstexRuntimeException En cas d'erreur de parcours des résultats.
	 */
	public IstexIterator2(String query, String output, String facets) throws IstexRuntimeException {
		super(query, output, facets);

		JsonObject json;
		JsonArray hits;

		try {
			json = JsonObject.parse(new String(Readers.getBytesFromURL("https://api.istex.fr/document/?size=0&q=" + ((query != null) ? URLEncoder.encode(query, "UTF-8") : "") + ((facets != null) ? "&facets=" + URLEncoder.encode(facets, "UTF-8") : ""))).trim());
			if (json.has("_error")) {
				throw new IstexRuntimeException("Erreur ISTEX: " + json.toString());
			} else {
				total = json.cutInteger("total", Option.PRESENT_AND_NOT_NULL_AND_NOT_EMPTY);
				aggregations = json.cutJsonObject("aggregations");
				hits = json.cutJsonArray("hits", Option.PRESENT_AND_NOT_NULL);
				if (hits.isNotEmpty()) {
					throw new IstexRuntimeException("Pour la requète initiale de \"" + query + "\", il y a des réponses: " + json.toString());
				}
				if (json.isNotEmpty()) {
					throw new IstexRuntimeException("Pour la requète initiale de \"" + query + "\", des éléments de la réponse ISTEX ne sont pas pris en compte: " + json.toString());
				}
			}
			if (total == 0) {
				iterator = null;
			} else if (total < LIMIT) {
				System.out.println("unique");
				uniqueIterator("https://api.istex.fr/document/?size=" + LIMIT + "&q=" + ((query != null) ? URLEncoder.encode(query, "UTF-8") : "") + ((output != null) ? "&output=" + URLEncoder.encode(output, "UTF-8") : ""));
			} else {
				System.out.println("pages");
				nextIterator("https://api.istex.fr/document/?scroll=" + SCROLL + "&size=" + SCROLL_SIZE + "&q=" + ((query != null) ? URLEncoder.encode(query, "UTF-8") : "") + ((output != null) ? "&output=" + URLEncoder.encode(output, "UTF-8") : ""));
			}
		} catch (JsonException | IOException exception) {
			throw new IstexRuntimeException(exception);
		}
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public JsonObject getAggregations() {
		return aggregations;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public int getTotal() {
		return total;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public boolean hasNext() throws IstexRuntimeException {
		return (iterator != null) && (iterator.hasNext() || nextIterator(nextScrollURI));
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public JsonObject next() throws IstexRuntimeException {
		JsonObject hit;

		if (iterator == null) {
			throw new NoSuchElementException();
		} else if ((hit = (JsonObject) iterator.next()) == null) {
			throw new IstexRuntimeException("Pour la requète \"" + query + "\", le résultat " + count + " est null.");
		} else {
			count++;
			return hit;
		}
	}



	/**
	 * Prépare un itérateur sur les résultats de la page correspondant à l'URL indiquée.<br>
	 * @param url URL.
	 * @return <code>true</code> si la page contient au moins un résultat, <code>false</code> sinon (le parcours est terminé).
	 * @throws IstexRuntimeException En cas d'erreur de parcours des résultats.
	 */
	private synchronized boolean nextIterator(String url) throws IstexRuntimeException {
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
					throw new IstexRuntimeException("Erreur ISTEX: " + json.toString());
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
						throw new IstexRuntimeException("Pour la requète \"" + query + "\", des éléments de la réponse ISTEX ne sont pas pris en compte: " + json.toString());
					}
					// vérifier les assertions sur le fonctionnement d'ISTEX
					if (WARN) {
						// nombre total de résultats
						if ((this.total != -1) && (total != this.total)) {
							System.out.println("Pour la requète \"" + query + "\", le nombre de réponse total (" + total + ") n'est pas celui attendu (" + this.total + ").");
						}
						// durée de persistence
						if (!SCROLL.equals(scroll) && (nextScrollURI != null)) {
							System.out.println("Pour la requète \"" + query + "\", la durée de persistence (" + scroll + ") n'est pas celle attendue (" + SCROLL + ").");
						}
						// identifiant de balayage
						if (this.scrollId == null) {
							this.scrollId = scrollId;
						} else if (!this.scrollId.equals(scrollId) && (nextScrollURI != null)) {
							System.out.println("Pour la requète \"" + query + "\", l'identifiant de balayage (" + scrollId + ") n'est pas celui attendu (" + this.scrollId + ").");
						}
						// URL de la page suivante
						if ((nextScrollURI != null) && !nextScrollURI.equals(uri = ("https://api.istex.fr/document/?q=" + normalize(URLEncoder.encode(query, "UTF-8")) + "&size=" + SCROLL_SIZE + ((output != null) ? "&output=" + output : "") + "&defaultOperator=OR&scroll=" + SCROLL + "&scrollId=" + this.scrollId))) {
							System.out.println("Pour la requète \"" + query + "\", l'URI d'accès à la page suivante (" + nextScrollURI + ") n'est pas celle attendue (" + uri + ").");
						}
						// indication d'existence de page suivante et lien vers la page suivante
						if (((noMoreScrollResults != null) && (noMoreScrollResults.booleanValue() == false)) != (nextScrollURI != null)) {
							System.out.println("Pour la requète \"" + query + "\", noMoreScrollResults est " + noMoreScrollResults + " et nextScrollURI est " + nextScrollURI + ".");
						}
					}
					if (nextScrollURI == null) {
						// c'est la dernière page (elle peut être vide)
						if ((this.count + hits.size()) != this.total) {
							throw new IstexRuntimeException("Pour la requète \"" + query + "\", le nombre de documents retournés (" + (this.count + hits.size()) + ") est différent de celui attendu (" + this.total + ").");
						}
					} else if (hits.isEmpty()) {
						// ce n'est pas la dernière page mais elle est vide
						throw new IstexRuntimeException("Pour la requète \"" + query + "\", la page ne contient aucun résultat (hits est vide).");
					} else if ((this.count + hits.size()) > this.total) {
						// il y a trop de résultats
						throw new IstexRuntimeException("Pour la requète \"" + query + "\", le nombre de documents retournés est supérieur à celui attendu (" + this.total + ").");
					}
					if (hits.isNotEmpty()) {
						iterator = hits.iterator();
					}
				}
			}
		} catch (JsonException | JsonRuntimeException | IOException exception) {
			iterator = null;
			throw new IstexRuntimeException(exception);
		}
		return iterator != null;
	}



	/**
	 * Prépare l'unique itérateur sur les résultats de la recherche non paginée correspondant à l'URL indiquée.<br>
	 * Il doit y avoir au moins un résultat.
	 * @param url URL (ne doit pas être <code>null</code>).
	 * @throws IstexRuntimeException En cas d'erreur de parcours des résultats ou si il n'y a aucun résultat.
	 */
	private synchronized void uniqueIterator(String url) throws IstexRuntimeException {
		JsonObject json = null;
		JsonArray hits;
		@SuppressWarnings("hiding") int total;
		String uri;
		String firstPageUri;
		String lastPageUri;

		try {
			json = JsonObject.parse(new String(Readers.getBytesFromURL(url)).trim());
			if (json.has("_error")) {
				throw new IstexRuntimeException("Erreur ISTEX: " + json.toString());
			} else {
				total = json.cutInteger("total", Option.PRESENT_AND_NOT_NULL_AND_NOT_EMPTY);
				firstPageUri = json.cutString("firstPageURI", Option.PRESENT_AND_NOT_NULL_AND_NOT_EMPTY);
				lastPageUri = json.cutString("lastPageURI", Option.PRESENT_AND_NOT_NULL_AND_NOT_EMPTY);
				hits = json.cutJsonArray("hits", Option.PRESENT_AND_NOT_NULL);
				if (json.isNotEmpty()) {
					throw new IstexRuntimeException("Pour la requète \"" + query + "\", des éléments de la réponse ISTEX ne sont pas pris en compte: " + json.toString());
				}
				if (WARN) {
					if (!firstPageUri.equals(uri = ("https://api.istex.fr/document/?q=" + normalize(URLEncoder.encode(query, "UTF-8")) + "&size=" + LIMIT + ((output != null) ? "&output=" + output : "") + "&defaultOperator=OR&from=0"))) {
						System.out.println("Pour la requète \"" + query + "\", l'URI d'accès à la première page (" + firstPageUri + ") n'est pas celle attendue (" + uri + ").");
					}
					if (!lastPageUri.equals(uri = ("https://api.istex.fr/document/?q=" + normalize(URLEncoder.encode(query, "UTF-8")) + "&size=" + LIMIT + ((output != null) ? "&output=" + output : "") + "&defaultOperator=OR&from=0"))) {
						System.out.println("Pour la requète \"" + query + "\", l'URI d'accès à la dernière page (" + lastPageUri + ") n'est pas celle attendue (" + uri + ").");
					}
				}
				if (total != this.total) {
					throw new IstexRuntimeException("Pour la requète \"" + query + "\", le nombre de réponse total (" + total + ") n'est pas celui attendu (" + this.total + ").");
				}
				iterator = hits.iterator();
			}
		} catch (JsonException | JsonRuntimeException | IOException exception) {
			throw new IstexRuntimeException(exception);
		}
	}



	/**
	 * Aggregations correspondantes aux facettes, ou <code>null</code> si aucune facette n'a été demandée.
	 */
	private final JsonObject aggregations;



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
	 * Nombre total de résultats;
	 */
	private final int total;



	/**
	 * Limite au dessus de laquelle le parcours des résultat se fait en mode scroll.
	 */
	private static final int LIMIT = 0;



	/**
	 * Durée de persistence.
	 */
	private static final String SCROLL = "5m";



	/**
	 * Taille des pages en mode scroll.
	 */
	private static final int SCROLL_SIZE = 100;



	/**
	 * Indicateur d'affichage d'avertissements.
	 */
	private static final boolean WARN = true;



}
