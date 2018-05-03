package fr.inist.istex;



/**
 * Ce package regroupe les classes permettant principalement de parcourir les m�ta-donn�es r�sultant d'une recherche ISTEX, et de cr�er des objets java correspondant � un r�sultat.<br>
 * La r�cup�ration de r�sultats se fait en mode scroll ({@link "https://api.istex.fr/documentation/results/#pagination-de-type-scroll"}). Deux it�rateurs sont impl�ment�s:
 * <ul>
 * <li>{@link fr.inist.istex.IstexSimpleIterator} r�cup�re les r�sultats en mode scroll sans reprise.
 * <li>{@link fr.inist.istex.IstexSlicedIterator} d�coupe la recherche en recherches partielles sur des sous ensembles exclusifs de documents (tranches), avec un certain parall�lisme (un nombre
 * plafonn� de recherches partielles simultan�es). La recherche sur une tranche se fait par {@link fr.inist.istex.IstexSimpleIterator}. En cas d'erreur sur une tranche, la recherche sur celle-ci sera
 * relanc�e, en ignorant les r�sultats d�j� r�cup�r�s. En pratique, c'est le seul it�rateur utilisable sur un nombre important de r�sultats. A noter que cette recherche �tant en r�alit� une agr�gation
 * de recherches partielles, le r�sultat global pourrait �tre diff�rent de celui d'une recherche simple si le contenu du fonds change pendant le traitement. Cette situation est peu probable.
 * </ul>
 * @author Ludovic WALLE
 */
