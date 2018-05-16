package fr.inist.istex;

import java.util.*;

import org.apache.logging.log4j.*;

import toolbox.json.*;



/**
 * La classe {@link IstexIterator} impl�mente un it�rateur sur une recherche ISTEX.
 * @author Ludovic WALLE
 */
public abstract class IstexIterator implements Iterator<JsonObject> {



	/**
	 * @param query Requ�te, ne doit �tre ni vide ni ni <code>null</code>. Voir {@link "https://api.istex.fr/documentation/search/"}.
	 * @param output Donn�es � retourner, ignor� si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/results/#selection-des-champs-renvoyes"}.
	 * @param facets Facettes � retourner, ignor� si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/facets/"}.
	 * @throws IstexException En cas d'erreur de parcours des r�sultats.
	 */
	public IstexIterator(String query, String output, String facets) throws IstexException {
		if ((query == null) || query.isEmpty()) {
			throw new IstexException(LOGGER, Level.ERROR, "La requ�te est null ou vide.");
		}
		this.query = query;
		this.output = output;
		this.facets = facets;
	}



	/**
	 * Retourne les aggregations correspondantes aux facettes, ou <code>null</code> si aucune facette n'a �t� demand�e.
	 * @return Les aggregations correspondantes aux facettes, ou <code>null</code> si aucune facette n'a �t� demand�e.
	 */
	public final JsonObject getAggregations() {
		return aggregations;
	}



	/**
	 * Retourne le nombre de r�sultats retourn�s.
	 * @return Le nombre de r�sultats retourn�s.
	 */
	public final int getCount() {
		return count;
	}



	/**
	 * Retourne le nombre total de r�sultats, ou -1 si il n'est pas connu.
	 * @return Le nombre total de r�sultats, ou -1 si il n'est pas connu.
	 */
	public final int getTotal() {
		return total;
	}



	/**
	 * {@inheritDoc}
	 * @throws IstexException En cas d'erreur de parcours des r�sultats.
	 */
	@Override public abstract boolean hasNext() throws IstexException;



	/**
	 * {@inheritDoc}
	 * @throws IstexException En cas d'erreur de parcours des r�sultats.
	 */
	@Override public abstract JsonObject next() throws IstexException;



	/**
	 * Normalise la chaine indiqu�e pour la comparaison d'URIs.
	 * @param string Chaine � normaliser.
	 * @return La chaine normalis�e.
	 */
	protected static final String normalize(String string) {
		return string.replace("%3A", ":").replace("%28", "(").replace("%29", ")").replace("%2F", "/").replace("%3F", "?").replace("%29", ")").replace("+", "%20");
	}



	/**
	 * Aggregations correspondantes aux facettes, ou <code>null</code> si aucune facette n'a �t� demand�e. La valeur est initialement <code>null</code>, puis elle prend la premi�re valeur re�ue.
	 */
	protected JsonObject aggregations = null;



	/**
	 * Nombre de r�sultats retourn�s;
	 */
	protected int count = 0;



	/**
	 * Valeur du param�tre de recherche listant les facettes � calculer.
	 */
	protected final String facets;



	/**
	 * Valeur du param�tre de recherche listant les donn�es � retourner.
	 */
	protected final String output;



	/**
	 * Valeur du param�tre de recherche contenant la requ�te.
	 */
	protected final String query;



	/**
	 * Nombre total de r�sultats.<br>
	 * La valeur est initialement -1, puis elle prend la premi�re valeur re�ue.
	 */
	protected int total = -1;



	/**
	 * Logger.
	 */
	protected static final Logger LOGGER = LogManager.getLogger();



}
