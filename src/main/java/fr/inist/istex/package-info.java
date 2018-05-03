package fr.inist.istex;



/**
 * Ce package regroupe les classes permettant principalement de parcourir les méta-données résultant d'une recherche ISTEX, et de créer des objets java correspondant à un résultat.<br>
 * La récupération de résultats se fait en mode scroll ({@link "https://api.istex.fr/documentation/results/#pagination-de-type-scroll"}). Deux itérateurs sont implémentés:
 * <ul>
 * <li>{@link fr.inist.istex.IstexSimpleIterator} récupére les résultats en mode scroll sans reprise.
 * <li>{@link fr.inist.istex.IstexSlicedIterator} découpe la recherche en recherches partielles sur des sous ensembles exclusifs de documents (tranches), avec un certain parallélisme (un nombre
 * plafonné de recherches partielles simultanées). La recherche sur une tranche se fait par {@link fr.inist.istex.IstexSimpleIterator}. En cas d'erreur sur une tranche, la recherche sur celle-ci sera
 * relancée, en ignorant les résultats déjà récupérés. En pratique, c'est le seul itérateur utilisable sur un nombre important de résultats. A noter que cette recherche étant en réalité une agrégation
 * de recherches partielles, le résultat global pourrait être différent de celui d'une recherche simple si le contenu du fonds change pendant le traitement. Cette situation est peu probable.
 * </ul>
 * @author Ludovic WALLE
 */
