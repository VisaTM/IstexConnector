package fr.inist.istex;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;
import java.util.regex.*;

import org.apache.logging.log4j.*;

import toolbox.json.*;



/**
 * La classe {@link Istex} implémente la représentation java d'une réponse ISTEX (interprétation du json reçu). Cette classe sert à la fois à marquer les classes utilisées pour la représentation, et à
 * les munir d'une méthode permettant de renseigner cette représentation à partir d'un objet json reçue d'ISTEX.<br>
 * @author Ludovic WALLE
 */

public class Istex {



	/**
	 * Construit récursivement un objet de la classe indiquée à partir du json indiqué, en utilisant la reflexion Java.<br>
	 * Les tableaux, objets ou champs vides sont récursivement ignorés (exemple: <code>{"UnChamp":"abc","UnTableau":[{"UnAutreChamp":null}]}</code> -> <code>{"UnChamp":"abc"}</code>).
	 * @param type Classe de l'objet à construire.
	 * @param json Json.
	 * @param path Chemin de l'objet ISTEX (pour les traces).
	 * @param ignored Collecteur d'éléments ignorés.
	 * @return L'objet construit, ou <code>null</code> si il ne contient rien.
	 */
	private static Object build(Class<?> type, Json json, String path, Map<String, Json> ignored) {
		StringBuilder concatenation;
		String fragment;
		JsonArray jsonArray;
		JsonObject jsonObject;
		String name;
		Class<?> componentType;
		Object object;
		Object component;
		int jsonArraySize;
		int componentCount = 0;
		Object array;
		Object packedArray;
		boolean objectHasNonNullField = false;
		Object fieldValue;

		try {
			if (json == null) {
				return null;
			} else if (TITLE_PATHES.matcher(path).matches() && (json instanceof JsonArray)) {
				// concaténer certains titres, qui sont parfois des chaines, parfois des tableaux de chaines (bug ISTEX connu)
				concatenation = new StringBuilder();
				jsonArray = (JsonArray) json;
				for (Iterator<Json> iterator = jsonArray.iterator(); iterator.hasNext();) {
					fragment = ((JsonString) iterator.next()).getValue();
					concatenation.append(fragment);
				}
				return concatenation.toString();
			} else if (type == Boolean.class) {
				return ((JsonBoolean) json).getValue();
			} else if (type == Long.class) {
				return Long.valueOf(((JsonNumber) json).getValue().intValue());
			} else if (type == Double.class) {
				return Double.valueOf(((JsonNumber) json).getValue().doubleValue());
			} else if (type == String.class) {
				return ((JsonString) json).getValue();
			} else if (type.isArray()) {
				jsonArray = ((JsonArray) json);
				jsonArraySize = jsonArray.size();
				componentType = type.getComponentType();
				array = Array.newInstance(componentType, jsonArraySize);
				for (int i = jsonArraySize - 1; i >= 0; i--) {
					component = build(componentType, jsonArray.cut(i), path + "[" + i + "]", ignored);
					if (component != null) {
						Array.set(array, componentCount++, component);
					}
				}
				if (componentCount == 0) {
					return null;
				} else if (componentCount == jsonArraySize) {
					return array;
				} else {
					packedArray = Array.newInstance(componentType, componentCount);
					for (int i = 0; i < componentCount; i++) {
						Array.set(packedArray, i, Array.get(array, i));
					}
					return packedArray;
				}
			} else {
				jsonObject = (JsonObject) json;
				object = type.getConstructor().newInstance();
				for (Field field : type.getDeclaredFields()) {
					name = computeJsonName(field.getName());
					fieldValue = build(field.getType(), jsonObject.cut(name), path + "/" + name, ignored);
					if (fieldValue != null) {
						objectHasNonNullField = true;
						field.set(object, fieldValue);
					}
				}
				if (jsonObject.isNotEmpty()) {
					if (ignored != null) {
						for (Iterator<Entry<String, Json>> iterator = jsonObject.iterator(); iterator.hasNext();) {
							Entry<String, Json> subJson = iterator.next();
							ignored.put(path + "/" + subJson.getKey() + (subJson.getValue() instanceof JsonArray ? "[]" : ""), subJson.getValue());
						}
					} else {
						throw new IstexException(LOGGER, Level.WARN, "Des éléments de \"" + (path.isEmpty() ? "/" : path) + "\" ne sont pas pris en compte: " + json.toString());
					}
				}
				if (objectHasNonNullField) {
					return object;
				} else {
					return null;
				}
			}
		} catch (ArrayIndexOutOfBoundsException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | NegativeArraySizeException | ClassCastException exception) {
			throw new RuntimeException(path, exception);
		} catch (RuntimeException exception) {
			throw exception; // pour pouvoir mettre un point d'arret éventuel
		}
	}



	/**
	 * Calcule le nom json correspondant au nom de la classe indiqué.
	 * @param name Nom de la classe.
	 * @return Le nom json correspondant au nom de la classe indiqué.
	 */
	public static String computeJsonName(String name) {
		int index;
		if (name.endsWith("$")) {
			name = name.substring(0, name.length() - 1);
		}
		while ((index = name.indexOf('$')) != -1) {
			name = name.substring(0, index) + (char) ((valueOf(name.charAt(index + 1)) << 12) + (valueOf(name.charAt(index + 2)) << 8) + (valueOf(name.charAt(index + 3)) << 4) + valueOf(name.charAt(index + 4))) + name.substring(index + 5);
		}
		return name;
	}



	/**
	 * Retourne les données correspondant à l'identifiant indiqué, dans le format indiqué.<br>
	 * Si les données sont en plusieurs parties (ex: plusieurs pages TIFF), elles sont renvoyées dans une archive au format ZIP.<br>
	 * Voir {@link "https://api.istex.fr/documentation/files/#acces-aux-fulltext"}.
	 * @param token Token d'authentification ISTEX.
	 * @param url URL des données ISTEX.
	 * @return Les octets reçus d'ISTEX.
	 */
	public static InputStream getFulltextStream(String token, String url) {
		Map<String, String> headers = new HashMap<>();

		try {
			headers.put("Authorization", "Bearer " + token);
			return Readers.getStreamForURL(headers, url);
		} catch (IOException exception) {
			throw new IstexException(LOGGER, Level.ERROR, exception);
		}
	}



	/**
	 * Retourne les données correspondant à l'URL indiquée.<br>
	 * Si les données sont en plusieurs parties (ex: plusieurs pages TIFF), elles sont renvoyées dans une archive au format ZIP.<br>
	 * Voir {@link "https://api.istex.fr/documentation/files/#acces-aux-fulltext"}.
	 * @param token Token d'authentification ISTEX.
	 * @param id Identifiant ISTEX des données.
	 * @param format Format des données (pdf, tei, zip, txt, tiff).
	 * @return Les octets reçus d'ISTEX.
	 */
	public static InputStream getFulltextStream(String token, String id, String format) {
		return getFulltextStream(token, "https://api.istex.fr/document/" + id + "/fulltext/" + format);
	}



	/**
	 * Construit récursivement un objet {@link Hit} à partir du jon indiqué.
	 * @param json Json servant à renseigner la représentation java.
	 * @param ignored Collecteur d'éléments ignorés. Les éléments présents dans le json sans équivalent dans la structure java seront placés là. Si <code>null</code>, une exception sera générée si de
	 *            tels éléments sont rencontrés.
	 * @return L'objet indiqué, pour pouvoir chainer les appels de méthode.
	 */
	public static Hit newHit(JsonObject json, Map<String, Json> ignored) {
		return (Hit) build(Hit.class, json, "", ignored);
	}



	/**
	 * Calcule la valeur numérique du chiffre hexadécimal indiqué.
	 * @param digit Chiffre hexadécimal.
	 * @return La valeur numérique du chiffre hexadécimal indiqué.
	 */
	private static int valueOf(char digit) {
		if ((digit >= '0') && (digit <= '9')) {
			return digit - '0';
		} else if ((digit >= 'a') && (digit <= 'f')) {
			return (digit - 'a') + 10;
		} else if ((digit >= 'A') && (digit <= 'F')) {
			return (digit - 'A') + 10;
		} else {
			throw new RuntimeException("Le caractère \"" + digit + "\" n'est pas un chiffre hexadécimal.");
		}
	}



	/**
	 * Modèle de syntaxe pour une année.
	 */
	public static final Pattern YEAR = Pattern.compile("(10|11|12|13|14|15|16|17|18|19|20)[0-9]{2}");



	/**
	 * Logger.
	 */
	public static final Logger LOGGER = LogManager.getLogger();



	/**
	 * Chemins des titres pour lesquels il y a un traitement particulier à appliquer (concaténation).
	 */
	private static final Pattern TITLE_PATHES = Pattern.compile("(/title|/refBibs\\[[0-9]+\\]/title|/refBibs\\[[0-9]+\\]/host/title|/refBibs\\[[0-9]+\\]/serie/title)");



	/**
	 * Cette section décrit un ensemble de classes calquée sur la structure d'un élément du tableau "hit" du json renvoyé par une recherche ISTEX.<br>
	 * La structure java est calquée sur la structure json. A chaque champ json correspond un champ java de même nom, avec quelques exceptions:
	 * <ul>
	 * <li>Les champs dont le nom est un mot java réservé sont suffixés par $ (exemple: <code>abstract</code> -> <code>abstract$</code>).
	 * <li>Les caractères ne pouvant pas être utilisés dans un identifiant sont remplacés par $ suivi du code unicode du caractère sur 4 chiffre hexadécimaux (exemple: <code>a-b</code> ->
	 * <code>a$002Db</code>).
	 * </ul>
	 * Chaque champ atomique json est stocké dans un champ java de même type.<br>
	 * Chaque tableau de champs atomiques json est stocké dans un tableau de champs java de même type.<br>
	 * Chaque objet json est stocké dans un objet java de classe adéquate.<br>
	 * Chaque tableau d'objets json est stocké dans un tableau d'objets java de classe adéquate.<br>
	 * Toutes ces classes dérivent d'{@link Istex}, pour les marquer, et pour les munir de la méthode {@link Istex#build(Istex, JsonObject, String)} permettant de les construire.<br>
	 * L'utilisation d'une hiérarchie de classes imbriquée permet d'éviter d'éventuels problèmes d'homonymie de champs json.
	 * @see {@link "https://api.istex.fr/documentation/fields"}
	 * @see {@link "https://api.istex.fr/mapping"}.
	 */
	// @formatter:off
	// classes utilisées à plusieurs endroits
	@SuppressWarnings("javadoc")
	public static class __File extends Istex {
		/** Extension du fichier (ex : "jpeg").<br>
		 * Les valeurs constatées sont exclusivement: <code>ocr</code>, <code>pdf</code>, <code>tei</code>, <code>tiff</code>, <code>txt</code> et <code>zip</code>.
		 */
		public String extension;
		/** Mimetype du fichier (ex : "image/jpeg"). */
		public String mimetype;
		/** Indique si le fichier vient de l'éditeur. */
		public Boolean original;
		/** Chemin d'accès au fichier. */
		public String uri;
	}
	// classes utilisées à la fois dans Hit et dans HitHost
	@SuppressWarnings("javadoc")
	public static class _Author extends Istex {
		/** Tableau des affiliations de l'auteur, liées à l'article ou la revue. */
		public String affiliations[];
		/** Nom de l'auteur, lié à l'article ou la revue. */
		public String name;
	}
	@SuppressWarnings("javadoc")
	public static class _Editor extends Istex {
		/** Tableau des affiliations du rédacteur, liées à l'article ou la revue. */
		public String affiliations[];
		/** Nom du rédacteur, lié à l'article ou la revue. */
		public String name;
	}
	@SuppressWarnings("javadoc")
	public static class _Subject extends Istex {
		/** Langue du thème de l'article ou de la revue. */
		public String language[];
		/** Thème de l'article ou de la revue. */
		public String value;
	}
	@SuppressWarnings("javadoc")
	// classes spécifiques
	public static class Hit extends Istex {
		/** Résumé du document. */
		public String abstract$;
		/** Objet contenant les informations liées aus fichiers d'annexe. */
		public __File annexes[];
		/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
		public String ark[];
		/** ARK ISTEX.<br>
		 * La syntaxe générale constatée est:<br>
		 * <code>ark:/67375/[0-9BCDFGHJKLMNPQRSTVWXZ]{3}-[0-9BCDFGHJKLMNPQRSTVWXZ]{8}-[0-9BCDFGHJKLMNPQRSTVWXZ]</code><br>
		 * Plus dans le détail, on constate que le premier groupe de trois caractères après le préfixe <code>ark:/67375/</code> semble réduit à un petit ensemble de valeurs, avec une répartition
		 * très inégale en quantité de documents. La syntaxe pourrait donc être:<br>
		 * <code>ark:/67375/(0T8|1BB|4W2|56L|6GQ|6H6|6ZK|80W|996|C41|GT4|HCB|HXZ|JKT|M70|NVC|P0J|QHD|QT4|TP3|VQC|WNG)-[0-9BCDFGHJKLMNPQRSTVWXZ]{8}-[0-9BCDFGHJKLMNPQRSTVWXZ]</code><br>
		 * mais c'est probablement trop restrictif car l'ensemble des valeurs n'est peut-être pas exhaustif.
		 */
		public String arkIstex;
		/** Identifiant. */
		public String articleId[];
		/** Tableau d'objets, chaque objet correspondant à un auteur. */
		public _Author author[];
		/** Objet contenant les informations liées aux catégories. */
		public HitsCategories categories;
		/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
		public String chapterId[];
		/** Date de copyright de l'article ou de la revue. */
		public String copyrightDate;
		/** Nom du corpus auquel appartient le document (ex : "elsevier"). */
		public String corpusName;
		/** Objet contenant les informations liées aux fichiers de couverture. */
		public __File covers[];
		/** Digital Object Identifier de l'article ou de la revue. */
		public String doi[];
		/** Tableau d'objets, chaque objet correspondant à un rédacteur. */
		public _Editor editor[];
		/** Enrichissements, dont la nature est indiquée dans un sous élément (ex : "multicat","refBibs") */
		public HitsEnrichments enrichments;
		/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
		public String erratumOf[];
		/** Objet contenant les informations liées aux fichiers au texte. */
		public __File fulltext[];
		/** Type d'article ou de revue. */
		public String genre[];
		/** Informations relatives à la revue. */
		public HitsHost host;
		/** Identifiant. */
		public String id;
		/** Objet contenant les informations liées aux mots-clés. */
		public HitsKeywords keywords;
		/** Langue de l'article ou de la revue. */
		public String language[];
		/** Objet contenant les informations liées aux fichiers de métadonnées. */
		public __File metadata[];
		/** Objet contenant les informations liées aux entités nommées. */
		public HitsNamedEntities namedEntities;
		/** Genre du document fourni par l'éditeur.<br>
		 * Le genre est fréquemment erroné. */
		public String originalGenre[];
		/** Personally Identifiable Information de l'article ou de la revue. */
		public String pii[];
		/** Identifiant PubMed du document. */
		public String pmid[];
		/** Date de publication de l'article ou de la revue.<br>
		  * La syntaxe constatée et qu'on peut considérer comme valide est les 4 chiffres des années, avec éventuellement des <code>-</code> à la place des derniers chiffres
		  * lorsqu'ils ne sont pas connus. Il n'y a pas de date inférieure à 1000. Il y a aussi d'autres valeurs, mais ce sont des erreurs.
		  */
		public String publicationDate;
		/** Indicateurs de qualité. */
		public HitsQualityIndicators qualityIndicators;
		/** Références bibliographiques. */
		public HitsRefBibs refBibs[];
		/** Score de la recherche. */
		public Double score;
		/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
		public HitsSerie serie;
		/** Tableau d'objets, chaque objet correspondant à un thème. */
		public _Subject subject[];
		/** Titre de l'article ou de la revue. */
		public String title;
		public static class HitsCategories extends Istex {
			/** Tableau contenant toutes les catégories déterminées par méthode bayésienne du document. */
			public String inist[];
			/** Tableau contenant toutes les catégories Science-Metrix du document. */
			public String scienceMetrix[];
			/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String scopus[];
			/** Tableau contenant toutes les catégories Web Of Science du document. */
			public String wos[];
		}
		public static class HitsEnrichments extends Istex {
			/** Objet contenant les informations liées aux fichiers d'enrichissement multicat. */
			public __File multicat[];
			/** Objet contenant les informations liées aux fichiers d'enrichissement nb. */
			public __File nb[];
			/** Objet contenant les informations liées aux fichiers d'enrichissement refBibs. */
			public __File refBibs[];
			/** Objet contenant les informations liées aux fichiers d'enrichissement teeft. */
			public __File teeft[];
			/** Objet contenant les informations liées aux fichiers d'enrichissement unitex. */
			public __File unitex[];
		}
		public static class HitsHost extends Istex {
			/** Tableau d'objets, chaque objet correspondant à un auteur. */
			public _Author author[];
			/** Identifiant. */
			public String bookId[];
			/** Tableau d'objets, chaque objet correspondant à une conférence. */
			public HitsHostConference conference[];
			/** Date de copyright de l'article ou de la revue. */
			public String copyrightDate;
			/** Digital Object Identifier de l'article ou de la revue. */
			public String doi[];
			/** Tableau d'objets, chaque objet correspondant à un rédacteur. */
			public _Editor editor[];
			/** International Standard Book Number électronique. */
			public String eisbn[];
			/** International Standard Serial Number électronique. */
			public String eissn[];
			/** Type d'article ou de revue. */
			public String genre[];
			/** International Standard Book Number papier. */
			public String isbn[];
			/** International Standard Serial Number papier. */
			public String issn[];
			/** Numéro de la revue. */
			public String issue;
			/** Identifiant. */
			public String journalId[];
			/** Langue de l'article ou de la revue. */
			public String language[];
			/** Objet contenant les données sur les pages de l'article dans la revue. */
			public HitsHostPages pages;
			/** Personally Identifiable Information de l'article ou de la revue. */
			public String pii[];
			/** Date de publication de l'article ou de la revue. */
			public String publicationDate;
			/** Identifiant. */
			public String publisherId[];
			/** Tableau d'objets, chaque objet correspondant à un thème. */
			public _Subject subject[];
			/** Titre de l'article ou de la revue. */
			public String title;
			/** Volume de la revue. */
			public String volume;
			public static class HitsHostConference extends Istex {
				/** Nom de la conférence. */
				public String name;
			}
			public static class HitsHostPages extends Istex {
				/** Première page de l'article dans la revue. */
				public String first;
				/** Dernière page de l'article dans la revue. */
				public String last;
				/** Nombre de pages de l'article dans la revue. */
				public String total;
			}
		}
		public static class HitsKeywords extends Istex {
			/** Tableau contenant tous les termes anglais extraits par étiquetage morpho-syntaxique. */
			public String teeft[];
		}
		public static class HitsNamedEntities extends Istex {
			/** Tableau contenant toutes les entités nommées détectées par Unitex-CasSys. */
			public HitsNamedEntitiesUnitex unitex;
			public static class HitsNamedEntitiesUnitex extends Istex {
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String bibl[];
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String date[];
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String geogName[];
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String orgName[];
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String orgName_funder[];
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String orgName_provider[];
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String persName[];
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String placeName[];
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String ref_bibl[];
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String ref_url[];
			}
		}
		public static class HitsQualityIndicators extends Istex {
			/** Nombre de caractères dans le résumé. */
			public Long abstractCharCount;
			/** Nombre de mots dans le résumé (basé sur le nombre d'espace). */
			public Long abstractWordCount;
			/** Nombre de mots clés présents. */
			public Long keywordCount;
			/** Nombre de caractères dans le PDF. */
			public Long pdfCharCount;
			/** Nombre de pages du PDF. */
			public Long pdfPageCount;
			/** Taille des pages du PDF (format : "X x Y pts"). */
			public String pdfPageSize;
			/** Numéro de version du PDF. */
			public Double pdfVersion;
			/** Nombre de mots dans le PDF (basé sur le nombre d'espace). */
			public Long pdfWordCount;
			/** Indique si les références bibliographiques sont fournis par l'éditeur. */
			public Boolean refBibsNative;
			/** Score de qualité, calculé selon les critères précédents. */
			public Double score;
		}
		public static class HitsRefBibs extends Istex {
			/** Tableau d'objets, chaque objet correspondant à un auteur référencé. */
			public HitsRefBibsAuthor author[];
			/** Objet contenant les informations liées à la revue ou le livre . */
			public HitsRefBibsHost host;
			/** Date de publication référencée. */
			public String publicationDate;
			/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public HitsRefBibsSerie serie;
			/** Titre référencé. */
			public String title;
			public static class HitsRefBibsAuthor extends Istex {
				/** Nom d'un auteur référencé. */
				public String name;
			}
			public static class HitsRefBibsHost extends Istex {
				/** Tableau d'objets, chaque objet correspondant à un auteur de la revue référencée. */
				public HitsRefBibsHostAuthor author[];
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String isbn;
				/** Numéro de la revue référencée. */
				public String issue;
				/** Objet contenant les données sur les pages, liés à la revue. */
				public HitsRefBibsHostPages pages;
				/** Date de publication de la revue référencée. */
				public String publicationDate;
				/** Titre de la revue référencée. */
				public String title;
				/** Numéro de volume de la revue référencée. */
				public String volume;
				public static class HitsRefBibsHostAuthor extends Istex {
					/** Nom d'un auteur de la revue référencée. */
					public String name;
				}
				public static class HitsRefBibsHostPages extends Istex {
					/** Première page de la revue référencée. */
					public String first;
					/** Dernière page de la revue référencée. */
					public String last;
				}
			}
			public static class HitsRefBibsSerie extends Istex {
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String title;
			}
		}
		public static class HitsSerie extends Istex {
			/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public HitsSerieAuthor author[];
			/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public HitsSerieConference conference[];
			/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String copyrightDate;
			/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public HitsSerieEditor editor[];
			/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String eissn[];
			/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String isbn[];
			/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String issn[];
			/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String issue;
			/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String language[];
			/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public HitsSeriePages pages;
			/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String publicationDate;
			/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String title;
			/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String volume;
			public static class HitsSerieAuthor extends Istex {
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String affiliations[];
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String name;
			}
			public static class HitsSerieConference extends Istex {
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String name;
			}
			public static class HitsSerieEditor extends Istex {
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String affiliations[];
				/** Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String name;
			}
			public static class HitsSeriePages extends Istex {
				/** Première page de la revue référencée. */
				public String first;
				/** Dernière page de la revue référencée. */
				public String last;
			}
		}
	}
	// @formatter:on



}
