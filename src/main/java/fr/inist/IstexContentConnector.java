package fr.inist;


import java.io.*;
import java.util.*;
import java.util.Map.*;

import org.apache.logging.log4j.*;

import eu.openminted.content.connector.*;
import eu.openminted.registry.core.domain.*;
import fr.inist.istex.*;
import fr.inist.tables.*;
import toolbox.json.*;



/**
 * La classe {@link IstexContentConnector} implémente un connecteur de données vers ISTEX.<br>
 * <br>
 * <u>Compilation d'informations fournies par OMTD</u><br>
 * <br>
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
 * <u>Autres informations</u><br>
 * <br>
 * Le connecteur est appelé à partir d'une interface générique de constitution de corpus, servant à interroger tous les connecteurs de données disponibles (voir
 * {@link "https://test.openminted.eu/resourceRegistration/corpus/searchForPublications"}). Cette interface propose une zone de saisie de mots clés, et quelques facettes prédéfinies et fixes
 * (initialement <i>Rights</i>, <i>Publication Year</i>, <i>Language</i>, <i>Publication Type</i>, <i>Document Type</i> et <i>Content Source</i>), aggrégeant les résultats provenant de tous les
 * connecteurs de données, ainsi que le nombre total de documents sélectionnés. Au départ, lorsqu'on arrive sur la page, la zone de saisie des mots clés est vide, et les valeurs des facettes résultent
 * d'une interrogation sur l'ensemble des fonds. Ensuite, on peut affiner les critères, en sélectionnant ou déselectionant des valeurs de facettes, et en modifiant la zone de saisie de mots clés.
 * Lorsque les critères sont satisfaisants, on peut appuyer sur le bouton de construction de corpus, pour extraire les métadonnées correspondantes. Le nombre de documents que l'on peut mettre dans un
 * corpus est limité, initalement à 1000. Ce nombre pourrait évoluer, mais le principe d'une limitation à nombre de documents restreint devrait perdurer.<br>
 * <br>
 * Le contenu de la zone de saisie de mots clés est transmise au connecteur dans le champ <code>keyword</code> de {@link Query}, les valeurs sélectionnées des facettes dans <code>params</code> (qui ne
 * contient donc que les noms des facettes prédéfinies, avec pour chacune les valeurs sélectionnées), et les noms des facettes à calculer dans <code>facets</code>.<br>
 * <br>
 * <i>Content Source</i> apparait sur la page, mais n'est pas une vraie facette. Elle est gérée entièrement par OMTD, son libellé provient de {@link #getSourceName()} et sa valeur du champ
 * <code>totalHits</code> de {@link SearchResult}. OMTD ne la transmet pas au connecteur, ni dans <code>facets</code>, ni dans <code>params</code>.<br>
 * <i>Document Type</i> ({@value #OMTD$DOCUMENT_TYPE} dans <code>facets</code> et <code>params</code>) est une vraie facette, et un vrai critère pour OMTD, mais vaut implicitement
 * {@value #OMTD$DOCUMENT_TYPE$FULLTEXT} pour tous les documents d'ISTEX.<br>
 * <i>Rights</i> ({@value #OMTD$RIGHTS} dans <code>facets</code> et <code>params</code>) est une vraie facette, et un vrai critère pour OMTD, mais vaut implicitement
 * {@value #OMTD$RIGHTS$RESTRICTED_ACCESS} pour tous les documents d'ISTEX.<br>
 * <i>Publication Year</i>, <i>Language</i> et <i>Publication Type</i> (respectivement {@value #OMTD$PUBLICATION_YEAR}, {@value #OMTD$LANGUAGE} et {@value #OMTD$PUBLICATION_TYPE} dans
 * <code>facets</code> et <code>params</code>) sont de vraies facettes, et de vrais critères, qui correspondent respectivement à {@value #ISTEX$PUBLICATION_YEAR}, {@value #ISTEX$LANGUAGE} et
 * {@value #ISTEX$PUBLICATION_TYPE} dans ISTEX.<br>
 * <br>
 * {@value #OMTD$PUBLICATION_YEAR} / {@value #ISTEX$PUBLICATION_YEAR} ne devrait contenir que des années sur 4 chiffres.<br>
 * {@value #OMTD$LANGUAGE} / {@value #ISTEX$LANGUAGE} doivent être convertis par les méthodes de {@link Language}.<br>
 * {@value #OMTD$PUBLICATION_TYPE} / {@value #ISTEX$PUBLICATION_TYPE} doivent être convertis par les méthodes de {@link PublicationType}.<br>
 * @author Ludovic WALLE
 */
public class IstexContentConnector implements ContentConnector {



	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * <u>Compilation d'informations fournies par OMTD</u><br>
	 * Retourne un flux du texte intégral d'une publication.<br>
	 * Les méta-données nom du fichier, type mime, ... devraient être dans les méta-données correspondantes, donc elles ne sont pas nécessaires ici.<br>
	 * <br>
	 * <u>Autres informations</u><br>
	 * Quelles informations trouve t'on dans <code>documentId</code> ?<br>
	 * Il est nécessaire d'avoir le token d'authentification ISTEX.<br>
	 * En plus de l'identifiant de document, il est nécessaire d'avoir le format des données souhaité, à moins que l'identifiant du document soit en fait l'URL d'accès au document, qui inclut cette
	 * information de format, ou d'imposer un format fixe.<br>
	 * @param documentId Identifiant de la publication.
	 * @return Un flux du texte intégral d'une publication.
	 */
	@Override public InputStream downloadFullText(String documentId) {
		return Istex.getFulltextStream(null, documentId, "tei"); // TODO obtenir le token d'authentification ISTEX, et le format de document
	}



	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * <u>Compilation d'informations fournies par OMTD</u><br>
	 * Retourne un flux XML avec les méta-données de tous les résultats de la recherche.<br>
	 * L'élément racine du XML est, et ses sous éléments sont, les méta-données des publications, conformes au schéma OMTD-SHARE.<br>
	 * Effectue une recherche et retourne toutes les métadonnées (sans tenir compte des paramètres <code>from</code> et <code>to</code> de la recherche). Utilisée pour un déchargement en masse de
	 * méta-données. Le format de la réponse est:<pre>
	 * &lt;publications&gt;
	 *     &lt;ms:documentMetadataRecord&gt;...&lt;/ms:documentMetadataRecord&gt;
	 *     &lt;ms:documentMetadataRecord&gt;...&lt;/ms:documentMetadataRecord&gt;
	 *     &lt;ms:documentMetadataRecord&gt;...&lt;/ms:documentMetadataRecord&gt;
	 *     &lt;ms:documentMetadataRecord&gt;...&lt;/ms:documentMetadataRecord&gt;
	 *     ...
	 * &lt;/publications&gt;</pre><u>Autres informations</u><br>
	 * Cette méthode est appelée lorsqu'on demande la construction du corpus.<br>
	 * @param omtdQuery Requète OMTD.
	 * @return Un flux XML avec les méta-données de tous les résultats de la recherche au format OMTD-SHARE.
	 */
	@SuppressWarnings("resource") @Override public InputStream fetchMetadata(Query omtdQuery) {
		String istexQuery;

		istexQuery = computeIstexQuery(omtdQuery.getKeyword(), omtdQuery.getParams());
		return (istexQuery == null) ? new IstexContentConnectorInputStream() : new IstexContentConnectorInputStream(istexQuery, null);
	}



	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * <u>Compilation d'informations fournies par OMTD</u><br>
	 * Retourne un identifiant pour ce connecteur (omtd, CORE, Crossref, ...).<br>
	 * Pour ISTEX, l'identifiant sera <code>ISTEX</code>.<br>
	 * <br>
	 * <u>Autres informations</u><br>
	 * Dans le cas d'ISTEX, la chaine retournée est {@value #SOURCE_NAME}.<br>
	 * @return La chaine <code>ISTEX</code>.
	 */
	@Override public String getSourceName() {
		return SOURCE_NAME;
	}



	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * <u>Compilation d'informations fournies par OMTD</u><br>
	 * Méthode de recherche standard pour des publications.<br>
	 * Permet aux utilisateurs de faire des recherches sur les méta-données et d'obtenir des résultats de recherche paginée contenant à la fois une page de méta-données et les facettes.<br>
	 * <br>
	 * <u>Autres informations</u><br>
	 * Cette méthode est appelée itérativement pour mettre au point le contenu d'un corpus, en ajustant les critères dans l'interface de constitution de corpus (zone de de mots clés, et facettes) et
	 * en voyant le nombre de documents sélectionnés et la distribution en facettes qui en résulte.<br>
	 * OMTD avait initialement prévu de retourner en plus un échantillon de données, limité par les champs <code>from</code> et <code>to</code> de {@link Query}, et retourné dans les champs
	 * <code>publications</code>, <code>from</code> et <code>to</code> de {@link SearchResult}, mais cela a été abandonné, et tous ces champs peuvent être ignorés.<br>
	 * @param omtdQuery Requète OMTD.
	 * @return Une page de résultats de recherche.
	 */
	@Override public SearchResult search(Query omtdQuery) throws IOException {
		IstexIterator istexIterator;
		int totalHits = 0;
		JsonObject istexFacets;
		List<Facet> omtdFacets = new Vector<>();
		List<Value> values;
		String istexQuery;

		if ((istexQuery = computeIstexQuery(omtdQuery.getKeyword(), omtdQuery.getParams())) != null) {
			istexIterator = new IstexSimpleIterator(istexQuery, null, computeIstexFacets(omtdQuery.getFacets()));
			totalHits = istexIterator.getTotal();
			istexFacets = istexIterator.getAggregations();

			if ((omtdQuery.getFacets() != null)) {
				for (String facet : omtdQuery.getFacets()) {
					values = new Vector<>();
					switch (facet) {
					case OMTD$DOCUMENT_TYPE: // la valeur est implicitement fulltext pour tous les documents ISTEX
						values.add(new Value(OMTD$DOCUMENT_TYPE$FULLTEXT, totalHits));
						break;
					case OMTD$LANGUAGE:
						if ((istexFacets != null) && istexFacets.has(ISTEX$LANGUAGE)) {
							for (Iterator<Json> buckerIterator = istexFacets.getJsonObject(ISTEX$LANGUAGE).getJsonArray("buckets").iterator(); buckerIterator.hasNext();) {
								JsonObject bucket = (JsonObject) buckerIterator.next();
								if (bucket.getInteger("docCount") > 0) {
									values.add(new Value(Language.istexToOmtd(bucket.getString("key")), bucket.getInteger("docCount").intValue()));
								}
							}
						}
						break;
					case OMTD$PUBLICATION_TYPE:
						if ((istexFacets != null) && istexFacets.has(ISTEX$PUBLICATION_TYPE)) {
							for (Iterator<Json> buckerIterator = istexFacets.getJsonObject(ISTEX$PUBLICATION_TYPE).getJsonArray("buckets").iterator(); buckerIterator.hasNext();) {
								JsonObject bucket = (JsonObject) buckerIterator.next();
								if (bucket.getInteger("docCount") > 0) {
									values.add(new Value(bucket.getString("key"), bucket.getInteger("docCount").intValue()));
								}
							}
						}
						break;
					case OMTD$PUBLICATION_YEAR:
						if ((istexFacets != null) && istexFacets.has(ISTEX$PUBLICATION_YEAR)) {
							for (Iterator<Json> buckerIterator = istexFacets.getJsonObject(ISTEX$PUBLICATION_YEAR).getJsonArray("buckets").iterator(); buckerIterator.hasNext();) {
								JsonObject bucket = (JsonObject) buckerIterator.next();
								if (bucket.getInteger("docCount") > 0) {
									values.add(new Value(bucket.getString("keyAsString"), bucket.getInteger("docCount").intValue()));
								}
							}
						}
						break;
					case OMTD$PUBLISHER: // cité dans la documentation, mais pas de facette dans l'interface générique de constitution de corpus
						if ((istexFacets != null) && istexFacets.has(ISTEX$PUBLISHER)) {
							for (Iterator<Json> buckerIterator = istexFacets.getJsonObject(ISTEX$PUBLISHER).getJsonArray("buckets").iterator(); buckerIterator.hasNext();) {
								JsonObject bucket = (JsonObject) buckerIterator.next();
								if (bucket.getInteger("docCount") > 0) {
									values.add(new Value(bucket.getString("key"), bucket.getInteger("docCount").intValue()));
								}
							}
						}
						break;
					case OMTD$RIGHTS: // la valeur est implicitement restrictedAccess pour tous les documents ISTEX
						values.add(new Value(OMTD$RIGHTS$RESTRICTED_ACCESS, totalHits));
						break;
					default:
						throw new IstexException(LOGGER, Level.ERROR, "La facette " + facet + " n'est pas prise en compte.");
					}
					omtdFacets.add(new Facet(facet, facet, values)); // TODO que mettre dans le libellé ?
				}
			}
		} else {
			if ((omtdQuery.getFacets() != null)) {
				for (String facet : omtdQuery.getFacets()) {
					values = new Vector<>();
					switch (facet) {
					case OMTD$DOCUMENT_TYPE: // la valeur est implicitement fulltext pour tous les documents ISTEX
						values.add(new Value(OMTD$DOCUMENT_TYPE$FULLTEXT, totalHits));
						break;
					case OMTD$LANGUAGE:
						break;
					case OMTD$PUBLICATION_TYPE:
						break;
					case OMTD$PUBLICATION_YEAR:
						break;
					case OMTD$PUBLISHER: // cité dans la documentation, mais pas de facette dans l'interface générique de constitution de corpus
						break;
					case OMTD$RIGHTS: // la valeur est implicitement restrictedAccess pour tous les documents ISTEX
						values.add(new Value(OMTD$RIGHTS$RESTRICTED_ACCESS, totalHits));
						break;
					default:
						throw new IstexException(LOGGER, Level.ERROR, "La facette " + facet + " n'est pas prise en compte.");
					}
					omtdFacets.add(new Facet(facet, facet, values)); // TODO que mettre dans le libellé ?
				}
			}
		}

		return new SearchResult(new Vector<String>(0), totalHits, 0, 0, omtdFacets);
	}



	/**
	 * Prépare le calcul des facettes ISTEX à partir de l'élément <code>facets</code> d'une requète OMTD.
	 * @param facets Champ <code>facets</code> d'une requète OMTD.
	 * @return La valeur du paramètre <code>facet</code> pour une recherche ISTEX.
	 */
	private static String computeIstexFacets(List<String> facets) {
		String istexFacets = "";
		String istexFacetsFragment;

		if ((facets != null) && !facets.isEmpty()) {
			istexFacets = "";
			for (String facet : facets) {
				switch (facet) {
				case OMTD$DOCUMENT_TYPE: // la valeur est implicitement fulltext pour tous les documents ISTEX
					istexFacetsFragment = "";
					break;
				case OMTD$LANGUAGE:
					istexFacetsFragment = ISTEX$LANGUAGE + "[*]";
					break;
				case OMTD$PUBLICATION_TYPE:
					istexFacetsFragment = ISTEX$PUBLICATION_TYPE + "[*]";
					break;
				case OMTD$PUBLICATION_YEAR:
//					istexFacetsFragment = ISTEX$PUBLICATION_YEAR + "[*-*:1]"; // TODO les données des années de publication d'ISTEX comportent des valeur fausses (erreur connue) => contournement: préciser un intervalle raisonnable
					istexFacetsFragment = ISTEX$PUBLICATION_YEAR + "[1000-2050:1]";
					break;
				case OMTD$PUBLISHER: // cité dans la documentation, mais pas de facette dans l'interface générique de constitution de corpus
					istexFacetsFragment = ISTEX$PUBLISHER + "[*]";
					break;
				case OMTD$RIGHTS: // la valeur est implicitement restrictedAccess pour tous les documents ISTEX
					istexFacetsFragment = "";
					break;
				default:
					throw new IstexException(LOGGER, Level.ERROR, "La facette " + facet + " n'est pas prise en compte.");
				}
				if (!istexFacetsFragment.isEmpty()) {
					if (!istexFacets.isEmpty()) {
						istexFacets += ",";
					}
					istexFacets += istexFacetsFragment;
				}
			}
		} else {
			istexFacets = null;
		}
		return istexFacets;
	}



	/**
	 * Prépare une requète ISTEX à partir des éléments <code>keyword</code> et <code>params</code> d'une requète OMTD.<br>
	 * L'implémentation a été réalisée de façon probablement simpliste pour <code>keyword</code>, faute d'informations disponibles.
	 * @param keyword Champ <code>keyword</code> d'une requète OMTD.
	 * @param params Champ <code>params</code> d'une requète OMTD.
	 * @return La valeur du paramètre <code>q</code> pour une recherche ISTEX, ou <code>null</code> si les éléments <code>keyword</code> et <code>params</code> de la requète OMTD ne permettent pas de
	 *         récupérer de documents.
	 */
	private static String computeIstexQuery(String keyword, Map<String, List<String>> params) {
		String istexQuery;
		String istexQueryFragment = "";
		List<String> values;

		istexQuery = ((keyword == null) || ((keyword = keyword.trim()).isEmpty())) ? "*" : keyword; // TODO sophistiquer le traitement du champ keyword, ou au minimum encoder les caractères ou
		                                                                                            // séquences qui pourraient poser des problèmes (délimiteurs, opérateurs, &, ', ", ...)

		if (params != null) {
			for (Iterator<Entry<String, List<String>>> iterator = params.entrySet().iterator(); iterator.hasNext();) {
				Entry<String, List<String>> paramEntry = iterator.next();
				if (paramEntry.getValue().size() > 0) {
					values = paramEntry.getValue();
					switch (paramEntry.getKey()) {
					case OMTD$DOCUMENT_TYPE: // la seule valeur acceptable pour ISTEX est fulltext
						if (!values.contains(OMTD$DOCUMENT_TYPE$FULLTEXT)) {
							return null;
						}
						break;
					case OMTD$LANGUAGE:
						if ((istexQueryFragment = parameter(ISTEX$LANGUAGE, Language.omtdToIstex(new TreeSet<>(values)))).isEmpty()) {
							return null;
						}
						break;
					case OMTD$PUBLICATION_TYPE:
						if ((istexQueryFragment = parameter(ISTEX$PUBLICATION_TYPE, PublicationType.omtdToIstex(new TreeSet<>(values)))).isEmpty()) {
							return null;
						}
						break;
					case OMTD$PUBLICATION_YEAR:
						if ((istexQueryFragment = yearParameter(ISTEX$PUBLICATION_YEAR, new TreeSet<>(values))).isEmpty()) {
							return null;
						}
						break;
					case OMTD$PUBLISHER: // cité dans la documentation, mais absent de l'interface générique de constitution de corpus
						if ((istexQueryFragment = editorParameter(ISTEX$PUBLISHER, new TreeSet<>(values))).isEmpty()) {
							return null;
						}
						break;
					case OMTD$RIGHTS: // la seule valeur acceptable pour ISTEX est restrictedAccess
						if (!values.contains(OMTD$RIGHTS$RESTRICTED_ACCESS)) {
							return null;
						}
						istexQueryFragment = "";
						break;
					default:
						throw new IstexException(LOGGER, Level.ERROR, "Le paramètre " + paramEntry.getKey() + " n'est pas pris en compte.");
					}
					if (!istexQueryFragment.isEmpty()) {
						if (!istexQuery.isEmpty()) {
							istexQuery += " AND ";
						}
						istexQuery += "(" + istexQueryFragment + ")";
					}
				}
			}
		}

		return istexQuery;
	}



	/**
	 * Calcule le fragment de requète correspondant aux valeurs de paramètre indiquées dans le champ ISTEX indiqué. Seules les valeurs valides sont prises en compte.<br>
	 * Le champ editor d'OMTD correspond à un corpus ISTEX.
	 * @param istexFieldName Nom de champ ISTEX dans lequel les valeurs doivent être recherchées
	 * @param values Valeurs possibles pour ce champ.
	 * @return Le fragment de requète correspondant à ce paramètre.
	 */
	private static String editorParameter(String istexFieldName, Set<String> values) {
		String queryFragment = "";
		String separator = "";

		if (values != null) {
			for (String value : values) {
				if (Corpus.isValid(value)) {
					queryFragment += separator + istexFieldName + ":" + value;
					separator = " OR ";
				}
			}
		}
		return queryFragment.toString();
	}



	/**
	 * Calcule le fragment de requète correspondant aux valeurs de paramètre indiquées dans le champ ISTEX indiqué.
	 * @param istexFieldName Nom de champ ISTEX dans lequel les valeurs doivent être recherchées
	 * @param values Valeurs possibles pour ce champ.
	 * @return Le fragment de requète correspondant à ce paramètre.
	 */
	private static String parameter(String istexFieldName, Set<String> values) {
		String queryFragment = "";
		String separator = "";

		if (values != null) {
			for (String value : values) {
				queryFragment += separator + istexFieldName + ":" + value;
				separator = " OR ";
			}
		}
		return queryFragment.toString();
	}



	/**
	 * Calcule le fragment de requète correspondant aux valeurs de paramètre indiquées dans le champ ISTEX indiqué. Seules les valeurs valides sont prises en compte.<br>
	 * @param istexFieldName Nom de champ ISTEX dans lequel les valeurs doivent être recherchées
	 * @param values Valeurs possibles pour ce champ.
	 * @return Le fragment de requète correspondant à ce paramètre.
	 */
	private static String yearParameter(String istexFieldName, Set<String> values) {
		String queryFragment = "";
		String separator = "";

		if (values != null) {
			for (String value : values) {
				if (Istex.YEAR.matcher(value).matches()) {
					queryFragment += separator + istexFieldName + ":" + value;
					separator = " OR ";
				}
			}
		}
		return queryFragment.toString();
	}



	/**
	 * Nom du champ langue, dans ISTEX.
	 */
	private static final String ISTEX$LANGUAGE = "language";



	/**
	 * Nom du champ type de publication, dans ISTEX.
	 */
	private static final String ISTEX$PUBLICATION_TYPE = "genre";



	/**
	 * Nom du champ année de publication, dans ISTEX.
	 */
	private static final String ISTEX$PUBLICATION_YEAR = "publicationDate";



	/**
	 * Nom du champ éditeur, dans ISTEX.
	 */
	private static final String ISTEX$PUBLISHER = "corpusName";



	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger();



	/**
	 * Nom de la facette ou du paramètre type de document, pour OMTD.
	 */
	private static final String OMTD$DOCUMENT_TYPE = "documenttype";



	/**
	 * Unique valeur de {@link #OMTD$DOCUMENT_TYPE} possible dans le cas d'ISTEX, pour OMTD.
	 */
	private static final String OMTD$DOCUMENT_TYPE$FULLTEXT = "fulltext";



	/**
	 * Nom de la facette ou du paramètre langue, pour OMTD.
	 */
	private static final String OMTD$LANGUAGE = "documentlanguage";



	/**
	 * Nom de la facette ou du paramètre type de document, pour OMTD.
	 */
	private static final String OMTD$PUBLICATION_TYPE = "publicationtype";



	/**
	 * Nom de la facette ou du paramètre année de publication, pour OMTD.
	 */
	private static final String OMTD$PUBLICATION_YEAR = "publicationyear";



	/**
	 * Nom de la facette ou du paramètre éditeur, pour OMTD.
	 */
	private static final String OMTD$PUBLISHER = "publisher";



	/**
	 * Nom de la facette ou du paramètre droits, pour OMTD.
	 */
	private static final String OMTD$RIGHTS = "rights";



	/**
	 * Unique valeur de {@link #OMTD$RIGHTS} possible dans le cas d'ISTEX, pour OMTD.
	 */
	private static final String OMTD$RIGHTS$RESTRICTED_ACCESS = "restrictedAccess";



	/**
	 * Identifiant du connecteur.
	 */
	private static final String SOURCE_NAME = "ISTEX";



}
