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
 * La classe {@link IstexContentConnector} impl�mente un connecteur de donn�es vers ISTEX.<br>
 * <br>
 * <u>Compilation d'informations fournies par OMTD</u><br>
 * <br>
 * La classe {@link Query} contient les champs suivants:
 * <ul>
 * <li><code>keyword</code>: les mots d'une recherche g�n�rique � la google.
 * <li><code>params</code>: une liste de contraintes suppl�mentaires (ex: <code>publicationYear=2016</code> ou <code>licence=CC-BY</code>). Une liste vide si il n'y en a pas. La liste des contraintes
 * n'est pas encore bien d�finie. Les contraintes actuelles sont:
 * <ul>
 * <li><code>publicationtype</code>: le type de publication (ex: <code>article</code> ou <code>thesis</code>; voir <code>PublicationTypeEnum</code> dans le sch�ma OMTD-SHARE).
 * <li><code>publicationyear</code>: ann�e de la publication.
 * <li><code>publisher</code>: �diteur de la publication.
 * <li><code>rights</code>: licence d'utilisation de la publication. Voir <code>RightsStatementEnum</code> dans le sch�ma OMTD-SHARE et {@link eu.openminted.registry.domain.RightsStatementEnum}. La
 * valeur pour ISTEX devrait �tre <code>restrictedAccess</code>
 * <li><code>documentlanguage</code>: langue du document (iso639-1 ou iso639-3, voir {@link "http://www-01.sil.org/iso639-3/codes.asp"}).
 * <li><code>documenttype</code>: le type du texte int�gral de la publication (<code>fulltext</code> ou <code>abstract</code>).
 * <li><code>keyword</code>: un ou plusieurs mots cl�s caract�risant la publication.
 * </ul>
 * <li><code>facets</code>: la liste de champs de m�ta-donn�es pour laquelle les statistiques doivent �tre calcul�es. La liste des facettes n'est pas encore bien d�finie.
 * <li><code>from</code> and <code>to</code>: utilis�s pour la pagination des r�sultats. <code>from</code> est inclus mais <code>to</code> ne l'est pas.
 * </ul>
 * La classe {@link SearchResult} contient les champs suivants:
 * <ul>
 * <li><code>publications</code>: une page de m�ta-donn�es.
 * <li><code>totalHits</code>: le nombre total de r�sultats de la recherche.
 * <li><code>from</code> and <code>to</code>: la position de la page de r�sultats dans l'ensemble des r�sultats.
 * <li><code>facets</code>: des statistiques sur les r�sultats de la recherche.
 * </ul>
 * <u>Autres informations</u><br>
 * <br>
 * Le connecteur est appel� � partir d'une interface g�n�rique de constitution de corpus, servant � interroger tous les connecteurs de donn�es disponibles (voir
 * {@link "https://test.openminted.eu/resourceRegistration/corpus/searchForPublications"}). Cette interface propose une zone de saisie de mots cl�s, et quelques facettes pr�d�finies et fixes
 * (initialement <i>Rights</i>, <i>Publication Year</i>, <i>Language</i>, <i>Publication Type</i>, <i>Document Type</i> et <i>Content Source</i>), aggr�geant les r�sultats provenant de tous les
 * connecteurs de donn�es, ainsi que le nombre total de documents s�lectionn�s. Au d�part, lorsqu'on arrive sur la page, la zone de saisie des mots cl�s est vide, et les valeurs des facettes r�sultent
 * d'une interrogation sur l'ensemble des fonds. Ensuite, on peut affiner les crit�res, en s�lectionnant ou d�selectionant des valeurs de facettes, et en modifiant la zone de saisie de mots cl�s.
 * Lorsque les crit�res sont satisfaisants, on peut appuyer sur le bouton de construction de corpus, pour extraire les m�tadonn�es correspondantes. Le nombre de documents que l'on peut mettre dans un
 * corpus est limit�, initalement � 1000. Ce nombre pourrait �voluer, mais le principe d'une limitation � nombre de documents restreint devrait perdurer.<br>
 * <br>
 * Le contenu de la zone de saisie de mots cl�s est transmise au connecteur dans le champ <code>keyword</code> de {@link Query}, les valeurs s�lectionn�es des facettes dans <code>params</code> (qui ne
 * contient donc que les noms des facettes pr�d�finies, avec pour chacune les valeurs s�lectionn�es), et les noms des facettes � calculer dans <code>facets</code>.<br>
 * <br>
 * <i>Content Source</i> apparait sur la page, mais n'est pas une vraie facette. Elle est g�r�e enti�rement par OMTD, son libell� provient de {@link #getSourceName()} et sa valeur du champ
 * <code>totalHits</code> de {@link SearchResult}. OMTD ne la transmet pas au connecteur, ni dans <code>facets</code>, ni dans <code>params</code>.<br>
 * <i>Document Type</i> ({@value #OMTD$DOCUMENT_TYPE} dans <code>facets</code> et <code>params</code>) est une vraie facette, et un vrai crit�re pour OMTD, mais vaut implicitement
 * {@value #OMTD$DOCUMENT_TYPE$FULLTEXT} pour tous les documents d'ISTEX.<br>
 * <i>Rights</i> ({@value #OMTD$RIGHTS} dans <code>facets</code> et <code>params</code>) est une vraie facette, et un vrai crit�re pour OMTD, mais vaut implicitement
 * {@value #OMTD$RIGHTS$RESTRICTED_ACCESS} pour tous les documents d'ISTEX.<br>
 * <i>Publication Year</i>, <i>Language</i> et <i>Publication Type</i> (respectivement {@value #OMTD$PUBLICATION_YEAR}, {@value #OMTD$LANGUAGE} et {@value #OMTD$PUBLICATION_TYPE} dans
 * <code>facets</code> et <code>params</code>) sont de vraies facettes, et de vrais crit�res, qui correspondent respectivement � {@value #ISTEX$PUBLICATION_YEAR}, {@value #ISTEX$LANGUAGE} et
 * {@value #ISTEX$PUBLICATION_TYPE} dans ISTEX.<br>
 * <br>
 * {@value #OMTD$PUBLICATION_YEAR} / {@value #ISTEX$PUBLICATION_YEAR} ne devrait contenir que des ann�es sur 4 chiffres.<br>
 * {@value #OMTD$LANGUAGE} / {@value #ISTEX$LANGUAGE} doivent �tre convertis par les m�thodes de {@link Language}.<br>
 * {@value #OMTD$PUBLICATION_TYPE} / {@value #ISTEX$PUBLICATION_TYPE} doivent �tre convertis par les m�thodes de {@link PublicationType}.<br>
 * @author Ludovic WALLE
 */
public class IstexContentConnector implements ContentConnector {



	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * <u>Compilation d'informations fournies par OMTD</u><br>
	 * Retourne un flux du texte int�gral d'une publication.<br>
	 * Les m�ta-donn�es nom du fichier, type mime, ... devraient �tre dans les m�ta-donn�es correspondantes, donc elles ne sont pas n�cessaires ici.<br>
	 * <br>
	 * <u>Autres informations</u><br>
	 * Quelles informations trouve t'on dans <code>documentId</code> ?<br>
	 * Il est n�cessaire d'avoir le token d'authentification ISTEX.<br>
	 * En plus de l'identifiant de document, il est n�cessaire d'avoir le format des donn�es souhait�, � moins que l'identifiant du document soit en fait l'URL d'acc�s au document, qui inclut cette
	 * information de format, ou d'imposer un format fixe.<br>
	 * @param documentId Identifiant de la publication.
	 * @return Un flux du texte int�gral d'une publication.
	 */
	@Override public InputStream downloadFullText(String documentId) {
		return Istex.getFulltextStream(null, documentId, "tei"); // TODO obtenir le token d'authentification ISTEX, et le format de document
	}



	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * <u>Compilation d'informations fournies par OMTD</u><br>
	 * Retourne un flux XML avec les m�ta-donn�es de tous les r�sultats de la recherche.<br>
	 * L'�l�ment racine du XML est, et ses sous �l�ments sont, les m�ta-donn�es des publications, conformes au sch�ma OMTD-SHARE.<br>
	 * Effectue une recherche et retourne toutes les m�tadonn�es (sans tenir compte des param�tres <code>from</code> et <code>to</code> de la recherche). Utilis�e pour un d�chargement en masse de
	 * m�ta-donn�es. Le format de la r�ponse est:<pre>
	 * &lt;publications&gt;
	 *     &lt;ms:documentMetadataRecord&gt;...&lt;/ms:documentMetadataRecord&gt;
	 *     &lt;ms:documentMetadataRecord&gt;...&lt;/ms:documentMetadataRecord&gt;
	 *     &lt;ms:documentMetadataRecord&gt;...&lt;/ms:documentMetadataRecord&gt;
	 *     &lt;ms:documentMetadataRecord&gt;...&lt;/ms:documentMetadataRecord&gt;
	 *     ...
	 * &lt;/publications&gt;</pre><u>Autres informations</u><br>
	 * Cette m�thode est appel�e lorsqu'on demande la construction du corpus.<br>
	 * @param omtdQuery Requ�te OMTD.
	 * @return Un flux XML avec les m�ta-donn�es de tous les r�sultats de la recherche au format OMTD-SHARE.
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
	 * Dans le cas d'ISTEX, la chaine retourn�e est {@value #SOURCE_NAME}.<br>
	 * @return La chaine <code>ISTEX</code>.
	 */
	@Override public String getSourceName() {
		return SOURCE_NAME;
	}



	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * <u>Compilation d'informations fournies par OMTD</u><br>
	 * M�thode de recherche standard pour des publications.<br>
	 * Permet aux utilisateurs de faire des recherches sur les m�ta-donn�es et d'obtenir des r�sultats de recherche pagin�e contenant � la fois une page de m�ta-donn�es et les facettes.<br>
	 * <br>
	 * <u>Autres informations</u><br>
	 * Cette m�thode est appel�e it�rativement pour mettre au point le contenu d'un corpus, en ajustant les crit�res dans l'interface de constitution de corpus (zone de de mots cl�s, et facettes) et
	 * en voyant le nombre de documents s�lectionn�s et la distribution en facettes qui en r�sulte.<br>
	 * OMTD avait initialement pr�vu de retourner en plus un �chantillon de donn�es, limit� par les champs <code>from</code> et <code>to</code> de {@link Query}, et retourn� dans les champs
	 * <code>publications</code>, <code>from</code> et <code>to</code> de {@link SearchResult}, mais cela a �t� abandonn�, et tous ces champs peuvent �tre ignor�s.<br>
	 * @param omtdQuery Requ�te OMTD.
	 * @return Une page de r�sultats de recherche.
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
					case OMTD$PUBLISHER: // cit� dans la documentation, mais pas de facette dans l'interface g�n�rique de constitution de corpus
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
					omtdFacets.add(new Facet(facet, facet, values)); // TODO que mettre dans le libell� ?
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
					case OMTD$PUBLISHER: // cit� dans la documentation, mais pas de facette dans l'interface g�n�rique de constitution de corpus
						break;
					case OMTD$RIGHTS: // la valeur est implicitement restrictedAccess pour tous les documents ISTEX
						values.add(new Value(OMTD$RIGHTS$RESTRICTED_ACCESS, totalHits));
						break;
					default:
						throw new IstexException(LOGGER, Level.ERROR, "La facette " + facet + " n'est pas prise en compte.");
					}
					omtdFacets.add(new Facet(facet, facet, values)); // TODO que mettre dans le libell� ?
				}
			}
		}

		return new SearchResult(new Vector<String>(0), totalHits, 0, 0, omtdFacets);
	}



	/**
	 * Pr�pare le calcul des facettes ISTEX � partir de l'�l�ment <code>facets</code> d'une requ�te OMTD.
	 * @param facets Champ <code>facets</code> d'une requ�te OMTD.
	 * @return La valeur du param�tre <code>facet</code> pour une recherche ISTEX.
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
//					istexFacetsFragment = ISTEX$PUBLICATION_YEAR + "[*-*:1]"; // TODO les donn�es des ann�es de publication d'ISTEX comportent des valeur fausses (erreur connue) => contournement: pr�ciser un intervalle raisonnable
					istexFacetsFragment = ISTEX$PUBLICATION_YEAR + "[1000-2050:1]";
					break;
				case OMTD$PUBLISHER: // cit� dans la documentation, mais pas de facette dans l'interface g�n�rique de constitution de corpus
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
	 * Pr�pare une requ�te ISTEX � partir des �l�ments <code>keyword</code> et <code>params</code> d'une requ�te OMTD.<br>
	 * L'impl�mentation a �t� r�alis�e de fa�on probablement simpliste pour <code>keyword</code>, faute d'informations disponibles.
	 * @param keyword Champ <code>keyword</code> d'une requ�te OMTD.
	 * @param params Champ <code>params</code> d'une requ�te OMTD.
	 * @return La valeur du param�tre <code>q</code> pour une recherche ISTEX, ou <code>null</code> si les �l�ments <code>keyword</code> et <code>params</code> de la requ�te OMTD ne permettent pas de
	 *         r�cup�rer de documents.
	 */
	private static String computeIstexQuery(String keyword, Map<String, List<String>> params) {
		String istexQuery;
		String istexQueryFragment = "";
		List<String> values;

		istexQuery = ((keyword == null) || ((keyword = keyword.trim()).isEmpty())) ? "*" : keyword; // TODO sophistiquer le traitement du champ keyword, ou au minimum encoder les caract�res ou
		                                                                                            // s�quences qui pourraient poser des probl�mes (d�limiteurs, op�rateurs, &, ', ", ...)

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
					case OMTD$PUBLISHER: // cit� dans la documentation, mais absent de l'interface g�n�rique de constitution de corpus
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
						throw new IstexException(LOGGER, Level.ERROR, "Le param�tre " + paramEntry.getKey() + " n'est pas pris en compte.");
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
	 * Calcule le fragment de requ�te correspondant aux valeurs de param�tre indiqu�es dans le champ ISTEX indiqu�. Seules les valeurs valides sont prises en compte.<br>
	 * Le champ editor d'OMTD correspond � un corpus ISTEX.
	 * @param istexFieldName Nom de champ ISTEX dans lequel les valeurs doivent �tre recherch�es
	 * @param values Valeurs possibles pour ce champ.
	 * @return Le fragment de requ�te correspondant � ce param�tre.
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
	 * Calcule le fragment de requ�te correspondant aux valeurs de param�tre indiqu�es dans le champ ISTEX indiqu�.
	 * @param istexFieldName Nom de champ ISTEX dans lequel les valeurs doivent �tre recherch�es
	 * @param values Valeurs possibles pour ce champ.
	 * @return Le fragment de requ�te correspondant � ce param�tre.
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
	 * Calcule le fragment de requ�te correspondant aux valeurs de param�tre indiqu�es dans le champ ISTEX indiqu�. Seules les valeurs valides sont prises en compte.<br>
	 * @param istexFieldName Nom de champ ISTEX dans lequel les valeurs doivent �tre recherch�es
	 * @param values Valeurs possibles pour ce champ.
	 * @return Le fragment de requ�te correspondant � ce param�tre.
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
	 * Nom du champ ann�e de publication, dans ISTEX.
	 */
	private static final String ISTEX$PUBLICATION_YEAR = "publicationDate";



	/**
	 * Nom du champ �diteur, dans ISTEX.
	 */
	private static final String ISTEX$PUBLISHER = "corpusName";



	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger();



	/**
	 * Nom de la facette ou du param�tre type de document, pour OMTD.
	 */
	private static final String OMTD$DOCUMENT_TYPE = "documenttype";



	/**
	 * Unique valeur de {@link #OMTD$DOCUMENT_TYPE} possible dans le cas d'ISTEX, pour OMTD.
	 */
	private static final String OMTD$DOCUMENT_TYPE$FULLTEXT = "fulltext";



	/**
	 * Nom de la facette ou du param�tre langue, pour OMTD.
	 */
	private static final String OMTD$LANGUAGE = "documentlanguage";



	/**
	 * Nom de la facette ou du param�tre type de document, pour OMTD.
	 */
	private static final String OMTD$PUBLICATION_TYPE = "publicationtype";



	/**
	 * Nom de la facette ou du param�tre ann�e de publication, pour OMTD.
	 */
	private static final String OMTD$PUBLICATION_YEAR = "publicationyear";



	/**
	 * Nom de la facette ou du param�tre �diteur, pour OMTD.
	 */
	private static final String OMTD$PUBLISHER = "publisher";



	/**
	 * Nom de la facette ou du param�tre droits, pour OMTD.
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
