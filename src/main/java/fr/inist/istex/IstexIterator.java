package fr.inist.istex;

import java.io.*;
import java.net.*;
import java.util.*;

import fr.inist.toolbox.*;
import fr.inist.toolbox.json.*;
import fr.inist.toolbox.json.JsonObject.*;



/**
 * La classe {@link IstexIterator} implémente un itérateur sur une recherche ISTEX.<br>
 * Pour ne pas avoir de limite au nombre de résultats, leur parcours se fera en mode scroll (voir {@link "https://api.istex.fr/documentation/results/#pagination-de-type-scroll"}). Dans ce mode, les
 * résultats ne peuvent pas être triés.<br>
 * Constats empiriques:
 * <ul>
 * <li>Le nombre total de résultats peut fluctuer selon les pages, mais cela ne semble pas avoir de conséquences.
 * <li>Si le nombre total de résultats est un multiple du nombre de résultat par page, la dernière page est vide (zéro hits, pas de noMoreScrollResults ni de nextScrollURI).
 * <li>La syntaxe de nextScrollURI est:<code>https://api.istex.fr/document/?q=...&size=...&output=...&defaultOperator=...&scroll=...&scrollId=...</code>. Les paramètres <code>size</code>,
 * <code>output</code>, <code>defaultOperator</code> et <code>scroll</code> sont ignorés et peuvent être omis, mais si ils sont présents, il doivent avoir des valeurs valides. Le paramètre
 * <code>q</code> doit être présent, avec toujours la même valeur. Modifier <code>q</code> lance une nouvelle recherche.
 * <li>Les <code>aggregations</code> des facettes ne sont présentes que sur la première page.
 * </ul>
 * L'accès à ISTEX se faisant par réseau, des erreurs peuvent survenir, de façon d'autant plus probable que le nombre de résultat est important.
 * @author Ludovic WALLE
 */
public class IstexIterator implements Iterator<JsonObject> {



	/**
	 * @param query Requète, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/search/"}.
	 * @param output Données à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/results/#selection-des-champs-renvoyes"}.
	 * @param facets Facettes à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/facets/"}.
	 * @throws IstexRuntimeException En cas d'erreur de parcours des résultats.
	 */
	public IstexIterator(String query, String output, String facets) throws IstexRuntimeException {
		try {
			this.query = query;
			this.output = output;
			nextIterator("https://api.istex.fr/document/?scroll=" + SCROLL + "&size=" + SIZE + "&q=" + ((query != null) ? URLEncoder.encode(query, "UTF-8") : "") + ((output != null) ? "&output=" + URLEncoder.encode(output, "UTF-8") : "") + ((facets != null) ? "&facets=" + URLEncoder.encode(facets, "UTF-8") : ""));
		} catch (UnsupportedEncodingException exception) {
			throw new IstexRuntimeException(exception);
		}
	}



	/**
	 * Retourne les aggregations correspondantes aux facettes, ou <code>null</code> si aucune facette n'a été demandée.
	 * @return Les aggregations correspondantes aux facettes, ou <code>null</code> si aucune facette n'a été demandée.
	 */
	public JsonObject getAggregations() {
		return aggregations;
	}



	/**
	 * Retourne le nombre de résultats retournés.
	 * @return Le nombre de résultats retournés.
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
		return (iterator != null) && (iterator.hasNext() || nextIterator(nextScrollURI));
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
	 * /** Prépare l'itérateur sur les résultats de la page correspondant à l'URL indiquée.<br>
	 * @param url URL.
	 * @return <code>true</code> si la page contient au moins un résultat, <code>false</code> sinon (le parcours est terminé).
	 * @throws IstexRuntimeException En cas d'erreur de parcours des résultats.
	 */
	private synchronized boolean nextIterator(String url) throws IstexRuntimeException {
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
					aggregations = json.cutJsonObject("aggregations");
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
						// aggregations
						if ((this.aggregations != null) && (aggregations != null)) {
							System.out.println("Pour la requète \"" + query + "\", une agrégation (" + aggregations.serialize() + ") est présente alors qu'elle a déjà été présente dans une page précédente (" + this.aggregations.serialize() + ").");
						}
						// URL de la page suivante
						if ((nextScrollURI != null) && !nextScrollURI.equals(uri = ("https://api.istex.fr/document/?q=" + URLEncoder.encode(query, "UTF-8").replace("%3A", ":").replace("%28", "(").replace("%29", ")").replace("+", "%20") + "&size=" + SIZE + ((output != null) ? "&output=" + output: "") + "&defaultOperator=OR&scroll=" + SCROLL + "&scrollId=" + this.scrollId))) {
							System.out.println("Pour la requète \"" + query + "\", l'URI d'accès à la page suivante (" + nextScrollURI + ") n'est pas celle attendue (" + uri + ").");
						}
						// indication d'existence de page suivante et lien vers la page suivante
						if (((noMoreScrollResults != null) && (noMoreScrollResults.booleanValue() == false)) != (nextScrollURI != null)) {
							System.out.println("Pour la requète \"" + query + "\", noMoreScrollResults est " + noMoreScrollResults + " et nextScrollURI est " + nextScrollURI + ".");
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
	 * Aggregations correspondantes aux facettes, ou <code>null</code> si aucune facette n'a été demandée.
	 */
	private JsonObject aggregations;



	/**
	 * Nombre de résultats retournés;
	 */
	private int count = 0;



	/**
	 * Itérateur sur une page de réponse ISTEX, <code>null</code> si il n'y a plus d'éléments à retourner (y compris pour cause d'erreur).
	 */
	private Iterator<Json> iterator;



	/**
	 * URL pour accéder à la page suivante, <code>null</code> si il n'y en a pas.
	 */
	private String nextScrollURI;



	/**
	 * Données à retourner.
	 */
	private final String output;



	/**
	 * Requète.
	 */
	private final String query;



	/**
	 * Identifiant de balayage.
	 */
	private String scrollId;



	/**
	 * Nombre total de résultats;
	 */
	private int total = -1;



	/**
	 * Durée de persistence.
	 */
	private static final String SCROLL = "5m";



	/**
	 * Taille des pages.
	 */
	private static final int SIZE = 100;



	/**
	 * Indicateur d'affichage d'avertissements.
	 */
	private static final boolean WARN = true;



}
