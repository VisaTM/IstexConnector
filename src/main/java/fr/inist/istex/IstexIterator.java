package fr.inist.istex;

import java.io.*;
import java.net.*;
import java.util.*;

import fr.inist.toolbox.*;
import fr.inist.toolbox.json.*;
import fr.inist.toolbox.json.JsonObject.*;



/**
 * La classe {@link IstexIterator}.
 * @author Ludovic WALLE
 */
public class IstexIterator implements Iterator<JsonObject> {



	/**
	 * Construit un itérateur permettant de parcourir les résultats de la réponse à la recherche ISTEX construite à l'aide des paramètres indiqués.<br>
	 * Les résultats ne peuvent pas être triés avec un parcours des résultats par page.<br>
	 * @param query Requète, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/search/"}. Les valeurs sont .
	 * @param output Données à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/results/#selection-des-champs-renvoyes"}.
	 * @param rankBy Type de score, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/results/#choix-du-type-de-scoring"}.
	 * @throws IstexRuntimeException En cas d'erreur de parcours des résultats.
	 */
	public IstexIterator(String query, String output, String rankBy) throws IstexRuntimeException {
		StringBuilder url = new StringBuilder();

		this.query = query;
		try {
			url.append("https://api.istex.fr/document/?q=" + ((query != null) ? URLEncoder.encode(query, "UTF-8") : ""));
			if (output != null) {
				url.append("&output=" + URLEncoder.encode(output, "UTF-8"));
			}
			if (rankBy != null) {
				url.append("&rankBy=" + URLEncoder.encode(rankBy, "UTF-8"));
			}
			url.append("&scroll=5m&size=100");
			nextIterator(url.toString());
		} catch (UnsupportedEncodingException exception) {
			throw new IstexRuntimeException(exception);
		}
	}



	/**
	 * Retourne le nombre de résultats parcourus.
	 * @return Le nombre de résultatsparcourus.
	 */
	public int getCount() {
		return count;
	}



	/**
	 * Retourne le nombre total de résultats, ou -1 si il n'est pas connu.
	 * @return Le nombre total de résultats, ou -1 si il n'est pas connu.
	 */
	public int getTotal() {
		return total;
	}



	/**
	 * {@inheritDoc}
	 * @throws IstexRuntimeException En cas d'erreur de parcours des résultats.
	 */
	@Override public boolean hasNext() throws IstexRuntimeException {
		return (iterator != null) && (iterator.hasNext() || (nextIterator(nextScrollURI) && (iterator != null) && iterator.hasNext()));
	}



	/**
	 * {@inheritDoc}
	 * @throws IstexRuntimeException En cas d'erreur de parcours des résultats.
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
	 * Prépare l'itérateur de page correspondant à l'URL indiquée.<br>
	 * Constats empiriques:
	 * <ul>
	 * <li>Le nombre total de résultats peut fluctuer selon les pages, mais cela ne semble pas avoir de conséquences.
	 * <li>Si le nombre total de résultats est un multiple du nombre de résultat par page, la dernière page peut être (est ?) vide (zéro hits, pas de noMoreScrollResults ni de nextScrollURI).
	 * <li>La syntaxe de nextScrollURI est:<code>https://api.istex.fr/document/?q=arkIstex:*0-0&size=10&defaultOperator=OR&scroll=5m&scrollId=</code>... . Les paramètres <code>size</code>,
	 * <code>defaultOperator</code> et <code>scroll</code> sont ignorés et peuvent être omis, mais si ils sont présents, il doivent avoir des valeurs valides. Le paramètre <code>q</code> doit être
	 * présent, avec toujours la même valeur. Modifier <code>q</code> lance une nouvelle recherche.
	 * </ul>
	 * @param url URL.
	 * @return <code>true</code> si l'itérateur de page a pu être créé, <code>false</code> sinon.
	 * @throws IstexRuntimeException En cas d'erreur de parcours des résultats.
	 */
	private boolean nextIterator(String url) throws IstexRuntimeException {
		JsonObject json = null;
		JsonArray hits;
		@SuppressWarnings("hiding") int total;
		String page;


		try {
			if (url != null) {
				json = JsonObject.parse(page = new String(Readers.getBytesFromURL(url)).trim());
				if (json.has("_error")) {
					// erreur signalée par ISTEX
					iterator = null;
					throw new IstexRuntimeException("Erreur ISTEX: " + json.toString());
				} else {
					// réponse normale
					// extraire les informations attendues
					total = json.cutInteger("total", Option.PRESENT_AND_NOT_NULL_AND_NOT_EMPTY);
					noMoreScrollResults = json.cutBoolean("noMoreScrollResults");
					nextScrollURI = json.cutString("nextScrollURI");
					hits = json.cutJsonArray("hits", Option.PRESENT_AND_NOT_NULL);
					json.cutString("scroll");
					json.cutString("scrollId");
					// vérifier qu'il n'y en a pas d'autres
					if (json.isNotEmpty()) {
						iterator = null;
						throw new IstexRuntimeException("Pour la requète \"" + query + "\", des éléments de la réponse ISTEX ne sont pas pris en compte: " + json.toString());
					}
					// vérifier la cohérence du nombre total de résultats
					if (this.total == -1) {
						this.total = total;
						this.lastTotal = total;
					} else if (total != this.lastTotal) {
						System.out.println("Pour la requète \"" + query + "\", le nombre de réponse total a changé (" + this.count + "/" + this.total + "): " + this.lastTotal + " -> " + total);
						this.lastTotal = total;
					}
					if ((noMoreScrollResults == null) && (nextScrollURI == null) && hits.isEmpty()) {
						// la page est vide et rien pour continuer
						System.out.println("Pour la requète \"" + query + "\", la page est vide: " + page);
						if (this.count != this.total) {
							// mais tous les résultats n'ont pas été retournés
							throw new IstexRuntimeException("Pour la requète \"" + query + "\", le nombre de documents retournés (" + this.count + ") est différent de celui attendu (" + this.total + ").");
						}
						iterator = null;
					} else {
						// vérifier la cohérence des informations
						if ((noMoreScrollResults == null) || (noMoreScrollResults.booleanValue() != (nextScrollURI == null))) {
							// incohérence entre l'indication d'existence de page suivante et le lien vers la page suivante
							System.out.println("Pour la requète \"" + query + "\", noMoreScrollResults est " + noMoreScrollResults + " et nextScrollURI est " + nextScrollURI + ".");
						}
						if (((this.count + hits.size()) > this.total) || ((this.count + hits.size()) > this.lastTotal)) {
							// incohérence entre le nombre de résultats retournés et le nombre de résultats total
							System.out.println("Pour la requète \"" + query + "\", le nombre de résultats (" + this.count + " + " + hits.size() + ") est supérieur au total (" + this.total + ") ou au dernier total (" + this.lastTotal + ").");
						}
						if (hits.isEmpty()) {
							// aucun résultat
							iterator = null;
							throw new IstexRuntimeException("Pour la requète \"" + query + "\", la page ne contient aucun résultat (hits est vide).");
						}
						iterator = hits.iterator();
					}
				}
			} else {
				iterator = null;
			}
		} catch (JsonException | JsonRuntimeException | IOException exception) {
			iterator = null;
			throw new IstexRuntimeException(exception);
		}
		return iterator != null;
	}



	/**
	 * Nombre de résultats parcourus;
	 */
	private int count = 0;



	/**
	 * Itérateur sur une page de réponse ISTEX, <code>null</code> si il n'y a plus d'éléments à retourner (y compris pour cause d'erreur).
	 */
	private Iterator<Json> iterator;



	/**
	 * Nombre total de résultats indiqué dans la dernière page reçue;
	 */
	private int lastTotal = -1;



	/**
	 * URL pour accéder à la page suivante, <code>null</code> si il n'y en a pas.
	 */
	private String nextScrollURI;



	/**
	 * Indicateur de fin de parcours.
	 */
	private Boolean noMoreScrollResults;



	/**
	 * Requète.
	 */
	private final String query;



	/**
	 * Nombre total de résultats;
	 */
	private int total = -1;



}
