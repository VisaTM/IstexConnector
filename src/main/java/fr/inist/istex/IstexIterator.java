package fr.inist.istex;

import java.util.*;

import fr.inist.toolbox.json.*;



/**
 * La classe {@link IstexIterator} implémente un itérateur sur une recherche ISTEX.<br>
 * Par cohérence, comme dans le mode scroll, utilisé par certaines implémentations, les résultats ne peuvent pas être triés, la possibilité de tri n'est pas prise en compte.<br>
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
public abstract class IstexIterator implements Iterator<JsonObject> {



	/**
	 * @param query Requète, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/search/"}.
	 * @param output Données à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/results/#selection-des-champs-renvoyes"}.
	 * @param facets Facettes à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/facets/"}.
	 * @throws IstexRuntimeException En cas d'erreur de parcours des résultats.
	 */
	public IstexIterator(String query, String output, String facets) throws IstexRuntimeException {
		this.query = query;
		this.output = output;
		this.facets = facets;
	}



	/**
	 * Retourne les aggregations correspondantes aux facettes, ou <code>null</code> si aucune facette n'a été demandée.
	 * @return Les aggregations correspondantes aux facettes, ou <code>null</code> si aucune facette n'a été demandée.
	 */
	public abstract JsonObject getAggregations();



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
	public abstract int getTotal();



	/**
	 * {@inheritDoc}
	 * @throws IstexRuntimeException En cas d'erreur de parcours des résultats.
	 */
	@Override public abstract boolean hasNext() throws IstexRuntimeException;



	/**
	 * {@inheritDoc}
	 * @throws IstexRuntimeException En cas d'erreur de parcours des résultats.
	 */
	@Override public abstract JsonObject next() throws IstexRuntimeException;



	/**
	 * Normalise la chaine indiquée pour la comparaison d'URIs.
	 * @param string Chaine à normaliser.
	 * @return La chaine normalisée.
	 */
	protected static final String normalize(String string) {
		return string.replace("%3A", ":").replace("%28", "(").replace("%29", ")").replace("%2F", "/").replace("%3F", "?").replace("%29", ")").replace("+", "%20");
	}



	/**
	 * Nombre de résultats retournés;
	 */
	protected int count = 0;



	/**
	 * Facettes.
	 */
	protected final String facets;



	/**
	 * Données à retourner.
	 */
	protected final String output;



	/**
	 * Requète.
	 */
	protected final String query;



}
