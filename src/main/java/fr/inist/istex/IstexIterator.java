package fr.inist.istex;

import java.util.*;

import org.apache.logging.log4j.*;

import toolbox.json.*;



/**
 * La classe {@link IstexIterator} implémente un itérateur sur une recherche ISTEX.
 * @author Ludovic WALLE
 */
public abstract class IstexIterator implements Iterator<JsonObject> {



	/**
	 * @param query Requète, ne doit être ni vide ni ni <code>null</code>. Voir {@link "https://api.istex.fr/documentation/search/"}.
	 * @param output Données à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/results/#selection-des-champs-renvoyes"}.
	 * @param facets Facettes à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/facets/"}.
	 * @throws IstexException En cas d'erreur de parcours des résultats.
	 */
	public IstexIterator(String query, String output, String facets) throws IstexException {
		if ((query == null) || query.isEmpty()) {
			throw new IstexException(LOGGER, Level.ERROR, "La requète est null ou vide.");
		}
		this.query = query;
		this.output = output;
		this.facets = facets;
	}



	/**
	 * Retourne les aggregations correspondantes aux facettes, ou <code>null</code> si aucune facette n'a été demandée.
	 * @return Les aggregations correspondantes aux facettes, ou <code>null</code> si aucune facette n'a été demandée.
	 */
	public final JsonObject getAggregations() {
		return aggregations;
	}



	/**
	 * Retourne le nombre de résultats retournés.
	 * @return Le nombre de résultats retournés.
	 */
	public final int getCount() {
		return count;
	}



	/**
	 * Retourne le nombre total de résultats, ou -1 si il n'est pas connu.
	 * @return Le nombre total de résultats, ou -1 si il n'est pas connu.
	 */
	public final int getTotal() {
		return total;
	}



	/**
	 * {@inheritDoc}
	 * @throws IstexException En cas d'erreur de parcours des résultats.
	 */
	@Override public abstract boolean hasNext() throws IstexException;



	/**
	 * {@inheritDoc}
	 * @throws IstexException En cas d'erreur de parcours des résultats.
	 */
	@Override public abstract JsonObject next() throws IstexException;



	/**
	 * Normalise la chaine indiquée pour la comparaison d'URIs.
	 * @param string Chaine à normaliser.
	 * @return La chaine normalisée.
	 */
	protected static final String normalize(String string) {
		return string.replace("%3A", ":").replace("%28", "(").replace("%29", ")").replace("%2F", "/").replace("%3F", "?").replace("%29", ")").replace("+", "%20");
	}



	/**
	 * Aggregations correspondantes aux facettes, ou <code>null</code> si aucune facette n'a été demandée. La valeur est initialement <code>null</code>, puis elle prend la première valeur reçue.
	 */
	protected JsonObject aggregations = null;



	/**
	 * Nombre de résultats retournés;
	 */
	protected int count = 0;



	/**
	 * Valeur du paramètre de recherche listant les facettes à calculer.
	 */
	protected final String facets;



	/**
	 * Valeur du paramètre de recherche listant les données à retourner.
	 */
	protected final String output;



	/**
	 * Valeur du paramètre de recherche contenant la requète.
	 */
	protected final String query;



	/**
	 * Nombre total de résultats.<br>
	 * La valeur est initialement -1, puis elle prend la première valeur reçue.
	 */
	protected int total = -1;



	/**
	 * Logger.
	 */
	protected static final Logger LOGGER = LogManager.getLogger();



}
