package fr.inist;


import java.io.*;

import eu.openminted.content.connector.*;



/**
 * La classe {@link IstexContentConnector} implémente un connecteur de données vers ISTEX.<br>
 * La classe {@link Query} contient les champs suivants:
 * <ul>
 * <li><code>keyword</code>: les mots d'une recherche générique à la google.
 * <li><code>params</code>: une liste de contraintes supplémentaires (ex: <code>publicationYear=2016</code> ou <code>licence=CC-BY</code>). Une liste vide si il n'y en a pas. La liste des contraintes
 * n'est pas encore bien définie. Les contraintes actuelles sont:
 * <ul>
 * <li><code>publicationtype</code>: le type de publication (ex: <code>article</code> ou <code>thesis</code>; voir <code>PublicationTypeEnum</code> dans le schéma OMTD-SHARE).
 * <li><code>publicationyear</code>: année de la publication.
 * <li><code>publisher</code>: éditeur de la publication.
 * <li><code>rights</code>: licence d'utilisation de la publication. Voir <code>RightsStatementEnum</code> dans le schéma OMTD-SHARE et {@link eu.openminted.registry.domain.RightsStatementEnum}. La
 * valeur pour ISTEX devrait être <code>restrictedAccess</code>
 * <li><code>documentlanguage</code>: langue du document (iso639-1 ou iso639-3, voir {@link "http://www-01.sil.org/iso639-3/codes.asp"}).
 * <li><code>documenttype</code>: le type du texte intégral de la publication (<code>fulltext</code> ou <code>abstract</code>).
 * <li><code>keyword</code>: un ou plusieurs mots clés caractérisant la publication.
 * </ul>
 * <li><code>facets</code>: la liste de champs de méta-données pour laquelle les statistiques doivent être calculées. La liste des facettes n'est pas encore bien définie.
 * <li><code>from</code> and <code>to</code>: utilisés pour la pagination des résultats. <code>from</code> est inclus mais <code>to</code> ne l'est pas.
 * </ul>
 * La classe {@link SearchResult} contient les champs suivants:
 * <ul>
 * <li><code>publications</code>: une page de méta-données.
 * <li><code>totalHits</code>: le nombre total de résultats de la recherche.
 * <li><code>from</code> and <code>to</code>: la position de la page de résultats dans l'ensemble des résultats.
 * <li><code>facets</code>: des statistiques sur les résultats de la recherche.
 * </ul>
 * @author Ludovic WALLE
 */
public class IstexContentConnector implements ContentConnector {



	/**
	 * {@inheritDoc}<br>
	 * Retourne un flux du texte intégral d'une publication.<br>
	 * Les méta-données du nom du fichier, du type mime, ... devraient être dans les méta-données correspondantes, donc elles ne sont pas nécessaires ici.
	 * @param documentId Identifiant de la publication.
	 * @return Un flux du texte intégral d'une publication.
	 */
	@Override public InputStream downloadFullText(String documentId) {
		// TODO Auto-generated method stub
		return null;
	}



	/**
	 * {@inheritDoc}<br>
	 * Retourne un flux XML avec les méta-données de tous les résultats de la recherche.<br>
	 * L'élément racine du XML est, et ses sous éléments sont, les méta-données des publications, conformes au schéma OMTD-SHARE.<br>
	 * Effectue une recherche et retourne toutes les métadonnées (sans tenir compte des paramètres from et to de la recherche). Utilisée pour un déchargement en masse de méta-données. Le format de la
	 * réponse est:
	 *
	 * <pre>
	 * &lt;publications&gt;
	 *     &lt;ms:documentMetadataRecord&gt;...&lt;/ms:documentMetadataRecord&gt;
	 *     &lt;ms:documentMetadataRecord&gt;...&lt;/ms:documentMetadataRecord&gt;
	 *     &lt;ms:documentMetadataRecord&gt;...&lt;/ms:documentMetadataRecord&gt;
	 *     &lt;ms:documentMetadataRecord&gt;...&lt;/ms:documentMetadataRecord&gt;
	 *     ...
	 * &lt;/publications&gt;
	 * </pre>
	 *
	 * @param query Recherche.
	 * @return Un flux XML avec les méta-données de tous les résultats de la recherche.
	 */
	@Override public InputStream fetchMetadata(Query query) {
		// TODO Auto-generated method stub
		return null;
	}



	/**
	 * {@inheritDoc}<br>
	 * Retourne un identifiant pour ce connecteur (omtd, CORE, Crossref, ...).<br>
	 * Pour ISTEX, l'identifiant sera <code>ISTEX</code>.
	 * @return La chaine <code>ISTEX</code>.
	 */
	@Override public String getSourceName() {
		return "ISTEX";
	}



	/**
	 * {@inheritDoc}<br>
	 * Méthode de recherche standard pour des publications.<br>
	 * Permet aux utilisateurs de faire des recherches sur les méta-données et d'obtenir des résultats de recherche paginée contenant à la fois une page de méta-données et les facettes.
	 * @param query Requète.
	 * @return Une page de résultats de recherche.
	 */
	@Override public SearchResult search(Query query) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}



}
