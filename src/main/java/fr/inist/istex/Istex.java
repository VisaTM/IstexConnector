package fr.inist.istex;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;
import java.util.regex.*;

import fr.inist.toolbox.*;
import fr.inist.toolbox.json.*;



/**
 * La classe {@link Istex} implémente la représentation java d'une réponse ISTEX (interprétation du json reçu). Cette classe sert à la fois à marquer les classes utilisées pour la représentation, et à
 * les munir d'une méthode permettant de renseigner cette représentation à partir d'une chaine json reçue d'ISTEX.<br>
 * @author Ludovic WALLE
 */

public class Istex {



	/**
	 * Construit récursivement un objet de la classe indiquée à partir du json indiqué.
	 * @param type Classe de l'objet à construire.
	 * @param json Json.
	 * @param path Chemin de l'objet ISTEX (pour les traces).
	 * @param ignored Collecteur d'éléments ignorés.
	 * @return L'objet indiqué, pour pouvoir chainer les appels de méthode.
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
//		String separator;
//		StringBuilder message;

		try {
			if (json == null) {
				return null;
			} else if (TITLE_PATHES.matcher(path).matches() && (json instanceof JsonArray)) {
				// concaténer certains titres, qui sont parfois des chaines, parfois des tableaux de chaines (bug Istex connu)
				concatenation = new StringBuilder();
//				separator = "";
//				message = new StringBuilder();
				jsonArray = (JsonArray) json;
				for (Iterator<Json> iterator = jsonArray.iterator(); iterator.hasNext();) {
					fragment = ((JsonString) iterator.next()).getValue();
					concatenation.append(fragment);
//					message.append(separator + "\"" + fragment + "\"");
//					separator = " + ";
				}
//				message.append(" = \"" + concatenation + "\" ");
//				System.err.println(message);
				return concatenation.toString();
			} else if (type == Boolean.class) {
				return ((JsonBoolean) json).getValue();
			} else if (type == Integer.class) {
				return Integer.valueOf(((JsonNumber) json).getValue().intValue());
			} else if (type == Double.class) {
				return Double.valueOf(((JsonNumber) json).getValue().doubleValue());
			} else if (type == String.class) {
				return ((JsonString) json).getValue();
			} else if (type.isArray()) {
				jsonArray = ((JsonArray) json);
				jsonArraySize = jsonArray.size();
				if (jsonArraySize == 0) {
					return null;
				}
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
						throw new IstexRuntimeException("Des éléments de \"" + (path.isEmpty() ? "/" : path) + "\" ne sont pas pris en compte: " + json.toString());
					}
				}
				if (objectHasNonNullField) {
					return object;
				} else {
					return null;
				}
			}
		} catch (ArrayIndexOutOfBoundsException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | NegativeArraySizeException | ClassCastException exception) {
			throw new RuntimeException(exception);
		} catch (RuntimeException exception) {
			throw exception; // pour pouvoir mettre un point d'arret éventuel
		}
	}



	/**
	 * Collecte récursivement les chemins décrit par les classes.
	 * @param path Chemin de l'élément.
	 * @param type Classe à traiter.
	 * @param pathes Chemins collectés.
	 */
	private static void collectPathes(String path, Class<?> type, Set<String> pathes) {
		if (type == Boolean.class) {
			pathes.add(path + "	boolean");
		} else if (type == Integer.class) {
			pathes.add(path + "	integer");
		} else if (type == Double.class) {
			pathes.add(path + "	double");
		} else if (type == String.class) {
			pathes.add(path + "	text");
		} else if (type.isArray()) {
			collectPathes(path, type.getComponentType(), pathes);
		} else {
			for (Field field : type.getDeclaredFields()) {
				collectPathes(path + "/" + computeJsonName(field.getName()), field.getType(), pathes);
			}
		}
	}



	/**
	 * Collecte récursivement les chemins décrits à l'URL {@link "https://api.istex.fr/mapping"}.
	 * @param parentPath Chemin du parent de l'élément.
	 * @param json Element à traiter.
	 * @param pathes Chemins collectés.
	 */
	private static void collectPathes(String parentPath, JsonObject json, Set<String> pathes) {
		JsonObject subJson;

		for (Iterator<Entry<String, Json>> pairIterator = json.iterator(); pairIterator.hasNext();) {
			Entry<String, Json> pair = pairIterator.next();
			if (pair.getValue() instanceof JsonObject) {
				subJson = (JsonObject) pair.getValue();
				if (subJson.has("type") && (subJson.get("type") instanceof JsonString)) {
					pathes.add(parentPath + "/" + pair.getKey() + "\t" + ((JsonString) subJson.get("type")).getValue());
				} else {
					collectPathes(parentPath + "/" + pair.getKey(), subJson, pathes);
				}
			}
		}
	}



	/**
	 * Calcule le nom json correspondant au nom de la classe indiqué.
	 * @param name Nom de la classe.
	 * @return Le nom json correspondant au nom de la classe indiqué.
	 */
	private static String computeJsonName(String name) {
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
	 * Retourne les octets correspondant à l'url indiquée.
	 * @param token Token d'identification ISTEX.
	 * @param url URL.
	 * @return Les octets reçus.
	 * @throws IOException
	 */
	private static byte[] get(String token, String url) throws IOException {
		Map<String, String> headers = new HashMap<>();

		headers.put("Authorization", "Bearer " + token);
		return Readers.getBytesFromURL(headers, url);
	}



	/**
	 * Retourne le document correspondant à l'identifiant indiqué, dans le format indiqué.<br>
	 * Si plusieurs documents d'un format (ex: plusieurs pages TIFF) existent, ils sont renvoyés dans une archive au format ZIP.<br>
	 * Voir {@link "https://api.istex.fr/documentation/files/#acces-aux-fulltext"}.
	 * @param token Token d'identification ISTEX.
	 * @param id Identifiant ISTEX du document.
	 * @param format Format du document (pdf, tei, zip, txt, tiff).
	 * @return Les octets reçus d'ISTEX.
	 * @throws IOException
	 */
	public static byte[] getFulltextBytes(String token, String id, String format) throws IOException {
		return get(token, "https://api.istex.fr/document/" + id + "/fulltext/" + format);
	}



	/**
	 * Retourne le document correspondant à l'identifiant indiqué, dans le format indiqué.<br>
	 * Si plusieurs documents d'un format (ex: plusieurs pages TIFF) existent, ils sont renvoyés dans une archive au format ZIP.<br>
	 * Voir "https://api.istex.fr/documentation/files/#acces-aux-fulltext"}.
	 * @param token Token d'identification ISTEX.
	 * @param id Identifiant Istex du document.
	 * @param format Format du document (pdf, tei, zip, txt, tiff).
	 * @return Les octets reçus d'ISTEX.
	 * @throws IOException
	 */
	public static InputStream getFulltextStream(String token, String id, String format) throws IOException {
		Map<String, String> headers = new HashMap<>();

		headers.put("Authorization", "Bearer " + token);
		return Readers.getStreamForURL(headers, "https://api.istex.fr/document/" + id + "/fulltext/" + format);
	}



	/**
	 * @param args
	 * @throws IOException
	 * @throws JsonException
	 */
	public static void main(String[] args) throws IOException, JsonException {
		Set<String> fromUrl = new TreeSet<>();
		Set<String> fromClasses = new TreeSet<>();
		String jsonString = null;

		try {
			collectPathes("", JsonObject.parse(jsonString = new String(Readers.getBytesFromURL("https://api.istex.fr/mapping")).trim()), fromUrl);
		} catch (Exception exception) {
			System.err.println(jsonString);
			throw exception;
		}
		collectPathes("", Hit.class, fromClasses);

		System.out.println("Chemins décrits par l'URL");
		for (Iterator<String> iterator = fromUrl.iterator(); iterator.hasNext();) {
			String string = iterator.next();
			System.out.println(string);
		}
		System.out.println();
		System.out.println("Chemins décrits par les classes");
		for (Iterator<String> iterator = fromClasses.iterator(); iterator.hasNext();) {
			String string = iterator.next();
			System.out.println(string);
		}
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
	 * Supprime récursivement les champs sans valeur dans le json indiqué.
	 * @param json Json à corriger.
	 * @return Le json corrigé.
	 * @throws IstexException
	 */
	public static Json patch(Json json) throws IstexException {
		Json subJson;
		JsonArray jsonArray;
		JsonObject jsonObject;

		if (json instanceof JsonArray) {
			jsonArray = ((JsonArray) json);
			for (int i = jsonArray.size() - 1; i >= 0; i--) {
				if (patch(jsonArray.get(i)) == null) {
					jsonArray.cut(i);
				}
			}
			if (jsonArray.isEmpty()) {
				json = null;
			}
		} else if (json instanceof JsonObject) {
			jsonObject = ((JsonObject) json);
			for (String name : jsonObject.getNames()) {
				subJson = patch(jsonObject.get(name));
				if (subJson == null) {
					jsonObject.cut(name);
				}
			}
			if (jsonObject.isEmpty()) {
				json = null;
			}
		} else if ((json != null) && !(json instanceof JsonAtomic)) {
			throw new RuntimeException("Cas non traité dans patch.");
		}
		return json;
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
	 * Chemins des titres pour lesquels il y a un traitement particulier à appliquer (concaténation).
	 */
	private static final Pattern TITLE_PATHES = Pattern.compile("(/title|/refBibs\\[[0-9]+\\]/title|/refBibs\\[[0-9]+\\]/host/title)");



	/**
	 * Racine des urls d'accès à ISTEX.
	 */
//public static final String URL_ROOT = "https://api.istex.fr";



	/**
	 * Cette section décrit un ensemble de classes calquée sur la structure d'un élément du tableau "hit" du json renvoyé par une recherche ISTEX.<br>
	 * La structure java est calquée sur la structure json. A chaque champ json correspond un champ java de même nom, avec quelques exceptions:
	 * <ul>
	 * <li>Les champs dont le nom est un mot java réservé sont suffixés par $ (exemple: <code>abstract</code> -> <code>abstract$</code>).
	 * <li>Les caractères ne pouvant pas être utilisés dans un identifiant sont remplacés par $ suivi du code unicode du caractère sur 4 chiffre hexadécimaux (exemple: <code>a b</code> ->
	 * <code>a$0020b</code>).
	 * </ul>
	 * Chaque champ atomique json est stocké dans un champ java de même type.<br>
	 * Chaque tableau de champs atomiques json est stocké dans un tableau de champs java de même type.<br>
	 * Chaque objet json est stocké dans un objet java de classe adéquate.<br>
	 * Chaque tableau d'objets json est stocké dans un tableau d'objets java de classe adéquate.<br>
	 * Toutes ces classes dérivent d'{@link Istex}, pour les marquer, et pour les munir de la méthode {@link Istex#build(Istex, JsonObject, String)} permettant de les renseigner.<br>
	 * L'utilisation d'une hiérarchie de classes imbriquée permet d'éviter d'éventuels problèmes d'homonymie de champs json.
	 * @see {@link "https://api.istex.fr/documentation/fields"}
	 * @see {@link "https://api.istex.fr/mapping"}.
	 */
	// @formatter:off
//	public static class __NameAndAffiliations extends Istex {
//		public String affiliations[]; /* Tableau des affiliations de l'auteur, liées à l'article ou la revue. */
//		public String name; /* Nom de l'auteur, lié à l'article ou la revue. */
//	}
//	public static class __Name extends Istex {
//		public String name; /* Nom. */
//	}
//	public static class __Pages extends Istex {
//		public String first; /* Première page de la revue référencée. */
//		public String last; /* Dernière page de la revue référencée. */
//		public String total;
//	}
	@SuppressWarnings("javadoc")
	public static class __Fichier extends Istex {
		public String extension; /* Extension du fichier (ex : "jpeg"). */
		public String mimetype; /* Mimetype du fichier (ex : "image/jpeg"). */
		public Boolean original; /* Indique si le fichier vient de l'éditeur. */
		public String uri; /* Chemin d'accès au fichier. */
	}
	@SuppressWarnings("javadoc")
	public static class _Author extends Istex {
		public String affiliations[]; /* Tableau des affiliations de l'auteur, liées à l'article ou la revue. */
		public String name; /* Nom de l'auteur, lié à l'article ou la revue. */
	}
	@SuppressWarnings("javadoc")
	public static class _Editor extends Istex {
		public String affiliations[]; /* Tableau des affiliations du rédacteur, liées à l'article ou la revue. */
		public String name; /* Nom du rédacteur, lié à l'article ou la revue. */
	}
	@SuppressWarnings("javadoc")
	public static class _Subject extends Istex {
		public String language[]; /* Langue du thème de l'article ou de la revue. */
		public String value; /* Thème de l'article ou de la revue. */
	}
	@SuppressWarnings("javadoc")
	public static class Hit extends Istex {
		public String abstract$; /* Résumé du document. */
		public __Fichier annexes[]; /* Objet contenant les informations liées aus fichiers d'annexe. */
		public String ark[]; /* Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
		public String arkIstex; /* ARK ISTEX. La syntaxe est "ark:/67375/[0-9BCDFGHJKLMNPQRSTVWXZ]{3}-[0-9BCDFGHJKLMNPQRSTVWXZ]{8}-[0-9BCDFGHJKLMNPQRSTVWXZ]". */
		public String articleId[]; /* Identifiant. */
//		public __Name attributed$0020name[]; /* . */
//		public __Name aut[]; /* . */
		public _Author author[]; /* Tableau d'objets, chaque objet correspondant à un auteur. */
//		public __Name bookseller[]; /* . */
		public HitsCategories categories; /* Objet contenant les informations liées aux catégories. */
//		public __Name conference[]; /* . */
//		public __Name contributor[]; /* . */
		public String copyrightDate; /* Date de copyright de l'article ou de la revue. */
//		public __NameAndAffiliations corporate[]; /* . */
		public String corpusName; /* Nom du corpus auquel appartient le document (ex : "elsevier"). */
		public __Fichier covers[]; /* Objet contenant les informations liées aux fichiers de couverture. */
//		public __Name dedicatee[]; /* . */
//		public __Name defendant$002E[]; /* . */
		public String doi[]; /* Digital Object Identifier de l'article ou de la revue. */
		public _Editor editor[]; /* Tableau d'objets, chaque objet correspondant à un rédacteur. */
		public HitsEnrichments enrichments; /* Enrichissements, dont la nature est indiquée dans un sous élément (ex : "multicat","refBibs") */
		public String erratumOf[]; /* Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
//		public FormerOwner former$0020owner[]; /* . */
		public __Fichier fulltext[]; /* Objet contenant les informations liées aux fichiers au texte. */
		public String genre[]; /* Type d'article ou de revue. */
		public HitsHost host; /* Informations relatives à la revue. */
		public String id; /* Identifiant. */
		public HitsKeywords keywords; /* Objet contenant les informations liées aux mots-clés. */
		public String language[]; /* Langue de l'article ou de la revue. */
		public __Fichier metadata[]; /* Objet contenant les informations liées aux fichiers de métadonnées. */
		public HitsNamedEntities namedEntities; /* Objet contenant les informations liées aux entités nommées. */
		public String originalGenre[]; /* Genre du document fourni par l'éditeur. */
		public String pii[]; /* Personally Identifiable Information de l'article ou de la revue. */
		public String pmid[]; /* Identifiant PubMed du document. */
//		public __Name printer[]; /* . */
		public String publicationDate; /* Date de publication de l'article ou de la revue. */
//		public __Name publisher[]; /* . */
		public HitsQualityIndicators qualityIndicators; /* Indicateurs de qualité. */
		public HitsRefBibs refBibs[]; /* Références bibliographiques. */
		public Double score; /* Score de la recherche. */
//		public HitsSerie serie; /* . */
		public _Subject subject[]; /* Tableau d'objets, chaque objet correspondant à un thème. */
		public String title; /* Titre de l'article ou de la revue. */
//		public __Name translator[]; /* . */
//		public __Name undefined[]; /* . */
//		public static class FormerOwner extends Istex {
//			public String name; /* . */
//			public __Name printer[]; /* . */
//		}
		public static class HitsCategories extends Istex {
			public String inist[]; /* Tableau contenant toutes les catégories déterminées par méthode bayésienne du document. */
			public String scienceMetrix[]; /* Tableau contenant toutes les catégories Science-Metrix du document. */
			public String scopus[]; /* Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String wos[]; /* Tableau contenant toutes les catégories Web Of Science du document. */
		}
		public static class HitsEnrichments extends Istex {
			public __Fichier multicat[]; /* Objet contenant les informations liées aux fichiers d'enrichissement multicat. */
			public __Fichier nb[]; /* Objet contenant les informations liées aux fichiers d'enrichissement nb. */
			public __Fichier refBibs[]; /* Objet contenant les informations liées aux fichiers d'enrichissement refBibs. */
			public __Fichier teeft[]; /* Objet contenant les informations liées aux fichiers d'enrichissement teeft. */
			public __Fichier unitex[]; /* Objet contenant les informations liées aux fichiers d'enrichissement unitex. */
		}
		public static class HitsHost extends Istex {
//			public String abstract$; /* . */
			public _Author author[]; /* Tableau d'objets, chaque objet correspondant à un auteur. */
			public String bookId[]; /* Identifiant. */
			public HitsHostConference conference[]; /* Tableau d'objets, chaque objet correspondant à une conférence. */
			public String copyrightDate; /* Date de copyright de l'article ou de la revue. */
			public String doi[]; /* Digital Object Identifier de l'article ou de la revue. */
			public _Editor editor[]; /* Tableau d'objets, chaque objet correspondant à un rédacteur. */
			public String eisbn[]; /* International Standard Book Number électronique. */
			public String eissn[]; /* International Standard Serial Number électronique. */
			public String genre[]; /* Type d'article ou de revue. */
			public String isbn[]; /* International Standard Book Number papier. */
			public String issn[]; /* International Standard Serial Number papier. */
			public String issue; /* Numéro de la revue. */
			public String journalId[]; /* Identifiant. */
			public String language[]; /* Langue de l'article ou de la revue. */
			public HitsHostPages pages; /* Objet contenant les données sur les pages de l'article dans la revue. */
			public String pii[]; /* Personally Identifiable Information de l'article ou de la revue. */
			public String publicationDate; /* Date de publication de l'article ou de la revue. */
			public String publisherId[]; /* Identifiant. */
			public _Subject subject[]; /* Tableau d'objets, chaque objet correspondant à un thème. */
			public String title; /* Titre de l'article ou de la revue. */
			public String volume; /* Volume de la revue. */
			public static class HitsHostConference extends Istex {
				public String name; /* Nom de la conférence. */
			}
			public static class HitsHostPages extends Istex {
				public String first; /* Première page de l'article dans la revue. */
				public String last; /* Dernière page de l'article dans la revue. */
				public String total; /* Nombre de pages de l'article dans la revue. */
			}
		}
		public static class HitsKeywords extends Istex {
			public String teeft[]; /* Tableau contenant tous les termes anglais extraits par étiquetage morpho-syntaxique. */
		}
		public static class HitsNamedEntities extends Istex {
			public HitsNamedEntitiesUnitex unitex; /* Tableau contenant toutes les entités nommées détectées par Unitex-CasSys. */
			public static class HitsNamedEntitiesUnitex extends Istex {
				public String bibl[]; /* Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String date[]; /* Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String geogName[]; /* Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String orgName[]; /* Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String orgName_funder[]; /* Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String orgName_provider[]; /* Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String persName[]; /* Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String placeName[]; /* Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String ref_bibl[]; /* Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String ref_url[]; /* Référencé à {@link "https://api.istex.fr/mapping"} mais non décrit dans {@link "https://api.istex.fr/documentation/fields"}. */
			}
		}
		public static class HitsQualityIndicators extends Istex {
			public Integer abstractCharCount; /* Nombre de caractères dans le résumé. */
			public Integer abstractWordCount; /* Nombre de mots dans le résumé (basé sur le nombre d'espace). */
			public Integer keywordCount; /* Nombre de mots clés présents. */
			public Integer pdfCharCount; /* Nombre de caractères dans le PDF. */
			public Integer pdfPageCount; /* Nombre de pages du PDF. */
			public String pdfPageSize; /* Taille des pages du PDF (format : "X x Y pts"). */
			public Double pdfVersion; /* Numéro de version du PDF. */
			public Integer pdfWordCount; /* Nombre de mots dans le PDF (basé sur le nombre d'espace). */
			public Boolean refBibsNative; /* Indique si les références bibliographiques sont fournis par l'éditeur. */
			public Double score; /* Score de qualité, calculé selon les critères précédents. */
		}
		public static class HitsRefBibs extends Istex {
			public HitsRefBibsAuthor author[]; /* Tableau d'objets, chaque objet correspondant à un auteur référencé. */
			public HitsRefBibsHost host; /* Objet contenant les informations liées à la revue ou le livre . */
			public String publicationDate; /* Date de publication référencée. */
//			public HitsRefBibsSerie serie; /* . */
			public String title; /* Titre référencé. */
			public static class HitsRefBibsAuthor extends Istex {
				public String name; /* Nom d'un auteur référencé. */
			}
			public static class HitsRefBibsHost extends Istex {
				public HitsRefBibsHostAuthor author[]; /* Tableau d'objets, chaque objet correspondant à un auteur de la revue référencée. */
//				public String eissn[]; /* . */
//				public String issn[]; /* . */
				public String issue; /* Numéro de la revue référencée. */
				public HitsRefBibsPages pages; /* Objet contenant les données sur les pages, liés à la revue. */
				public String publicationDate; /* Date de publication de la revue référencée. */
				public String title; /* Titre de la revue référencée. */
				public String volume; /* Numéro de volume de la revue référencée. */
				public static class HitsRefBibsHostAuthor extends Istex {
					public String name; /* Nom d'un auteur de la revue référencée. */
				}
			}
			public static class HitsRefBibsPages extends Istex {
				public String first; /* Première page de la revue référencée. */
				public String last; /* Dernière page de la revue référencée. */
			}
//			public static class HitsRefBibsSerie extends Istex {
//				public __NameAndAffiliations author[]; /* . */
//				public String title; /* . */
//			}
		}
//		public static class HitsSerie extends Istex {
//			public __Name conference[]; /* . */
//			public __Name editor[]; /* . */
//			public String eissn[]; /* . */
//			public String issn[]; /* . */
//			public String issue; /* . */
//			public String language[]; /* . */
//			public __Pages pages; /* . */
//			public String title; /* . */
//			public String volume; /* . */
//		}
	}
	// @formatter:on



}
