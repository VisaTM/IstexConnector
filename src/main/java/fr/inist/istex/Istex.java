package fr.inist.istex;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;
import java.util.regex.*;

import org.apache.logging.log4j.*;

import toolbox.json.*;



/**
 * La classe {@link Istex} impl�mente la repr�sentation java d'une r�ponse ISTEX (interpr�tation du json re�u). Cette classe sert � la fois � marquer les classes utilis�es pour la repr�sentation, et �
 * les munir d'une m�thode permettant de renseigner cette repr�sentation � partir d'un objet json re�ue d'ISTEX.<br>
 * @author Ludovic WALLE
 */

public class Istex {



	/**
	 * Construit r�cursivement un objet de la classe indiqu�e � partir du json indiqu�, en utilisant la reflexion Java.<br>
	 * Les tableaux, objets ou champs vides sont r�cursivement ignor�s (exemple: <code>{"UnChamp":"abc","UnTableau":[{"UnAutreChamp":null}]}</code> -> <code>{"UnChamp":"abc"}</code>).
	 * @param type Classe de l'objet � construire.
	 * @param json Json.
	 * @param path Chemin de l'objet ISTEX (pour les traces).
	 * @param ignored Collecteur d'�l�ments ignor�s.
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
				// concat�ner certains titres, qui sont parfois des chaines, parfois des tableaux de chaines (bug ISTEX connu)
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
						throw new IstexException(LOGGER, Level.WARN, "Des �l�ments de \"" + (path.isEmpty() ? "/" : path) + "\" ne sont pas pris en compte: " + json.toString());
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
			throw exception; // pour pouvoir mettre un point d'arret �ventuel
		}
	}



	/**
	 * Calcule le nom json correspondant au nom de la classe indiqu�.
	 * @param name Nom de la classe.
	 * @return Le nom json correspondant au nom de la classe indiqu�.
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
	 * Retourne les donn�es correspondant � l'identifiant indiqu�, dans le format indiqu�.<br>
	 * Si les donn�es sont en plusieurs parties (ex: plusieurs pages TIFF), elles sont renvoy�es dans une archive au format ZIP.<br>
	 * Voir {@link "https://api.istex.fr/documentation/files/#acces-aux-fulltext"}.
	 * @param token Token d'authentification ISTEX.
	 * @param url URL des donn�es ISTEX.
	 * @return Les octets re�us d'ISTEX.
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
	 * Retourne les donn�es correspondant � l'URL indiqu�e.<br>
	 * Si les donn�es sont en plusieurs parties (ex: plusieurs pages TIFF), elles sont renvoy�es dans une archive au format ZIP.<br>
	 * Voir {@link "https://api.istex.fr/documentation/files/#acces-aux-fulltext"}.
	 * @param token Token d'authentification ISTEX.
	 * @param id Identifiant ISTEX des donn�es.
	 * @param format Format des donn�es (pdf, tei, zip, txt, tiff).
	 * @return Les octets re�us d'ISTEX.
	 */
	public static InputStream getFulltextStream(String token, String id, String format) {
		return getFulltextStream(token, "https://api.istex.fr/document/" + id + "/fulltext/" + format);
	}



	/**
	 * Construit r�cursivement un objet {@link Hit} � partir du jon indiqu�.
	 * @param json Json servant � renseigner la repr�sentation java.
	 * @param ignored Collecteur d'�l�ments ignor�s. Les �l�ments pr�sents dans le json sans �quivalent dans la structure java seront plac�s l�. Si <code>null</code>, une exception sera g�n�r�e si de
	 *            tels �l�ments sont rencontr�s.
	 * @return L'objet indiqu�, pour pouvoir chainer les appels de m�thode.
	 */
	public static Hit newHit(JsonObject json, Map<String, Json> ignored) {
		return (Hit) build(Hit.class, json, "", ignored);
	}



	/**
	 * Calcule la valeur num�rique du chiffre hexad�cimal indiqu�.
	 * @param digit Chiffre hexad�cimal.
	 * @return La valeur num�rique du chiffre hexad�cimal indiqu�.
	 */
	private static int valueOf(char digit) {
		if ((digit >= '0') && (digit <= '9')) {
			return digit - '0';
		} else if ((digit >= 'a') && (digit <= 'f')) {
			return (digit - 'a') + 10;
		} else if ((digit >= 'A') && (digit <= 'F')) {
			return (digit - 'A') + 10;
		} else {
			throw new RuntimeException("Le caract�re \"" + digit + "\" n'est pas un chiffre hexad�cimal.");
		}
	}



	/**
	 * Mod�le de syntaxe pour une ann�e.
	 */
	public static final Pattern YEAR = Pattern.compile("(10|11|12|13|14|15|16|17|18|19|20)[0-9]{2}");



	/**
	 * Logger.
	 */
	public static final Logger LOGGER = LogManager.getLogger();



	/**
	 * Chemins des titres pour lesquels il y a un traitement particulier � appliquer (concat�nation).
	 */
	private static final Pattern TITLE_PATHES = Pattern.compile("(/title|/refBibs\\[[0-9]+\\]/title|/refBibs\\[[0-9]+\\]/host/title|/refBibs\\[[0-9]+\\]/serie/title)");



	/**
	 * Cette section d�crit un ensemble de classes calqu�e sur la structure d'un �l�ment du tableau "hit" du json renvoy� par une recherche ISTEX.<br>
	 * La structure java est calqu�e sur la structure json. A chaque champ json correspond un champ java de m�me nom, avec quelques exceptions:
	 * <ul>
	 * <li>Les champs dont le nom est un mot java r�serv� sont suffix�s par $ (exemple: <code>abstract</code> -> <code>abstract$</code>).
	 * <li>Les caract�res ne pouvant pas �tre utilis�s dans un identifiant sont remplac�s par $ suivi du code unicode du caract�re sur 4 chiffre hexad�cimaux (exemple: <code>a-b</code> ->
	 * <code>a$002Db</code>).
	 * </ul>
	 * Chaque champ atomique json est stock� dans un champ java de m�me type.<br>
	 * Chaque tableau de champs atomiques json est stock� dans un tableau de champs java de m�me type.<br>
	 * Chaque objet json est stock� dans un objet java de classe ad�quate.<br>
	 * Chaque tableau d'objets json est stock� dans un tableau d'objets java de classe ad�quate.<br>
	 * Toutes ces classes d�rivent d'{@link Istex}, pour les marquer, et pour les munir de la m�thode {@link Istex#build(Istex, JsonObject, String)} permettant de les construire.<br>
	 * L'utilisation d'une hi�rarchie de classes imbriqu�e permet d'�viter d'�ventuels probl�mes d'homonymie de champs json.
	 * @see {@link "https://api.istex.fr/documentation/fields"}
	 * @see {@link "https://api.istex.fr/mapping"}.
	 */
	// @formatter:off
	// classes utilis�es � plusieurs endroits
	@SuppressWarnings("javadoc")
	public static class __File extends Istex {
		/** Extension du fichier (ex : "jpeg").<br>
		 * Les valeurs constat�es sont exclusivement: <code>ocr</code>, <code>pdf</code>, <code>tei</code>, <code>tiff</code>, <code>txt</code> et <code>zip</code>.
		 */
		public String extension;
		/** Mimetype du fichier (ex : "image/jpeg"). */
		public String mimetype;
		/** Indique si le fichier vient de l'�diteur. */
		public Boolean original;
		/** Chemin d'acc�s au fichier. */
		public String uri;
	}
	// classes utilis�es � la fois dans Hit et dans HitHost
	@SuppressWarnings("javadoc")
	public static class _Author extends Istex {
		/** Tableau des affiliations de l'auteur, li�es � l'article ou la revue. */
		public String affiliations[];
		/** Nom de l'auteur, li� � l'article ou la revue. */
		public String name;
	}
	@SuppressWarnings("javadoc")
	public static class _Editor extends Istex {
		/** Tableau des affiliations du r�dacteur, li�es � l'article ou la revue. */
		public String affiliations[];
		/** Nom du r�dacteur, li� � l'article ou la revue. */
		public String name;
	}
	@SuppressWarnings("javadoc")
	public static class _Subject extends Istex {
		/** Langue du th�me de l'article ou de la revue. */
		public String language[];
		/** Th�me de l'article ou de la revue. */
		public String value;
	}
	@SuppressWarnings("javadoc")
	// classes sp�cifiques
	public static class Hit extends Istex {
		/** R�sum� du document. */
		public String abstract$;
		/** Objet contenant les informations li�es aus fichiers d'annexe. */
		public __File annexes[];
		/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
		public String ark[];
		/** ARK ISTEX.<br>
		 * La syntaxe g�n�rale constat�e est:<br>
		 * <code>ark:/67375/[0-9BCDFGHJKLMNPQRSTVWXZ]{3}-[0-9BCDFGHJKLMNPQRSTVWXZ]{8}-[0-9BCDFGHJKLMNPQRSTVWXZ]</code><br>
		 * Plus dans le d�tail, on constate que le premier groupe de trois caract�res apr�s le pr�fixe <code>ark:/67375/</code> semble r�duit � un petit ensemble de valeurs, avec une r�partition
		 * tr�s in�gale en quantit� de documents. La syntaxe pourrait donc �tre:<br>
		 * <code>ark:/67375/(0T8|1BB|4W2|56L|6GQ|6H6|6ZK|80W|996|C41|GT4|HCB|HXZ|JKT|M70|NVC|P0J|QHD|QT4|TP3|VQC|WNG)-[0-9BCDFGHJKLMNPQRSTVWXZ]{8}-[0-9BCDFGHJKLMNPQRSTVWXZ]</code><br>
		 * mais c'est probablement trop restrictif car l'ensemble des valeurs n'est peut-�tre pas exhaustif.
		 */
		public String arkIstex;
		/** Identifiant. */
		public String articleId[];
		/** Tableau d'objets, chaque objet correspondant � un auteur. */
		public _Author author[];
		/** Objet contenant les informations li�es aux cat�gories. */
		public HitsCategories categories;
		/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
		public String chapterId[];
		/** Date de copyright de l'article ou de la revue. */
		public String copyrightDate;
		/** Nom du corpus auquel appartient le document (ex : "elsevier"). */
		public String corpusName;
		/** Objet contenant les informations li�es aux fichiers de couverture. */
		public __File covers[];
		/** Digital Object Identifier de l'article ou de la revue. */
		public String doi[];
		/** Tableau d'objets, chaque objet correspondant � un r�dacteur. */
		public _Editor editor[];
		/** Enrichissements, dont la nature est indiqu�e dans un sous �l�ment (ex : "multicat","refBibs") */
		public HitsEnrichments enrichments;
		/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
		public String erratumOf[];
		/** Objet contenant les informations li�es aux fichiers au texte. */
		public __File fulltext[];
		/** Type d'article ou de revue. */
		public String genre[];
		/** Informations relatives � la revue. */
		public HitsHost host;
		/** Identifiant. */
		public String id;
		/** Objet contenant les informations li�es aux mots-cl�s. */
		public HitsKeywords keywords;
		/** Langue de l'article ou de la revue. */
		public String language[];
		/** Objet contenant les informations li�es aux fichiers de m�tadonn�es. */
		public __File metadata[];
		/** Objet contenant les informations li�es aux entit�s nomm�es. */
		public HitsNamedEntities namedEntities;
		/** Genre du document fourni par l'�diteur.<br>
		 * Le genre est fr�quemment erron�. */
		public String originalGenre[];
		/** Personally Identifiable Information de l'article ou de la revue. */
		public String pii[];
		/** Identifiant PubMed du document. */
		public String pmid[];
		/** Date de publication de l'article ou de la revue.<br>
		  * La syntaxe constat�e et qu'on peut consid�rer comme valide est les 4 chiffres des ann�es, avec �ventuellement des <code>-</code> � la place des derniers chiffres
		  * lorsqu'ils ne sont pas connus. Il n'y a pas de date inf�rieure � 1000. Il y a aussi d'autres valeurs, mais ce sont des erreurs.
		  */
		public String publicationDate;
		/** Indicateurs de qualit�. */
		public HitsQualityIndicators qualityIndicators;
		/** R�f�rences bibliographiques. */
		public HitsRefBibs refBibs[];
		/** Score de la recherche. */
		public Double score;
		/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
		public HitsSerie serie;
		/** Tableau d'objets, chaque objet correspondant � un th�me. */
		public _Subject subject[];
		/** Titre de l'article ou de la revue. */
		public String title;
		public static class HitsCategories extends Istex {
			/** Tableau contenant toutes les cat�gories d�termin�es par m�thode bay�sienne du document. */
			public String inist[];
			/** Tableau contenant toutes les cat�gories Science-Metrix du document. */
			public String scienceMetrix[];
			/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String scopus[];
			/** Tableau contenant toutes les cat�gories Web Of Science du document. */
			public String wos[];
		}
		public static class HitsEnrichments extends Istex {
			/** Objet contenant les informations li�es aux fichiers d'enrichissement multicat. */
			public __File multicat[];
			/** Objet contenant les informations li�es aux fichiers d'enrichissement nb. */
			public __File nb[];
			/** Objet contenant les informations li�es aux fichiers d'enrichissement refBibs. */
			public __File refBibs[];
			/** Objet contenant les informations li�es aux fichiers d'enrichissement teeft. */
			public __File teeft[];
			/** Objet contenant les informations li�es aux fichiers d'enrichissement unitex. */
			public __File unitex[];
		}
		public static class HitsHost extends Istex {
			/** Tableau d'objets, chaque objet correspondant � un auteur. */
			public _Author author[];
			/** Identifiant. */
			public String bookId[];
			/** Tableau d'objets, chaque objet correspondant � une conf�rence. */
			public HitsHostConference conference[];
			/** Date de copyright de l'article ou de la revue. */
			public String copyrightDate;
			/** Digital Object Identifier de l'article ou de la revue. */
			public String doi[];
			/** Tableau d'objets, chaque objet correspondant � un r�dacteur. */
			public _Editor editor[];
			/** International Standard Book Number �lectronique. */
			public String eisbn[];
			/** International Standard Serial Number �lectronique. */
			public String eissn[];
			/** Type d'article ou de revue. */
			public String genre[];
			/** International Standard Book Number papier. */
			public String isbn[];
			/** International Standard Serial Number papier. */
			public String issn[];
			/** Num�ro de la revue. */
			public String issue;
			/** Identifiant. */
			public String journalId[];
			/** Langue de l'article ou de la revue. */
			public String language[];
			/** Objet contenant les donn�es sur les pages de l'article dans la revue. */
			public HitsHostPages pages;
			/** Personally Identifiable Information de l'article ou de la revue. */
			public String pii[];
			/** Date de publication de l'article ou de la revue. */
			public String publicationDate;
			/** Identifiant. */
			public String publisherId[];
			/** Tableau d'objets, chaque objet correspondant � un th�me. */
			public _Subject subject[];
			/** Titre de l'article ou de la revue. */
			public String title;
			/** Volume de la revue. */
			public String volume;
			public static class HitsHostConference extends Istex {
				/** Nom de la conf�rence. */
				public String name;
			}
			public static class HitsHostPages extends Istex {
				/** Premi�re page de l'article dans la revue. */
				public String first;
				/** Derni�re page de l'article dans la revue. */
				public String last;
				/** Nombre de pages de l'article dans la revue. */
				public String total;
			}
		}
		public static class HitsKeywords extends Istex {
			/** Tableau contenant tous les termes anglais extraits par �tiquetage morpho-syntaxique. */
			public String teeft[];
		}
		public static class HitsNamedEntities extends Istex {
			/** Tableau contenant toutes les entit�s nomm�es d�tect�es par Unitex-CasSys. */
			public HitsNamedEntitiesUnitex unitex;
			public static class HitsNamedEntitiesUnitex extends Istex {
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String bibl[];
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String date[];
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String geogName[];
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String orgName[];
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String orgName_funder[];
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String orgName_provider[];
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String persName[];
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String placeName[];
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String ref_bibl[];
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String ref_url[];
			}
		}
		public static class HitsQualityIndicators extends Istex {
			/** Nombre de caract�res dans le r�sum�. */
			public Long abstractCharCount;
			/** Nombre de mots dans le r�sum� (bas� sur le nombre d'espace). */
			public Long abstractWordCount;
			/** Nombre de mots cl�s pr�sents. */
			public Long keywordCount;
			/** Nombre de caract�res dans le PDF. */
			public Long pdfCharCount;
			/** Nombre de pages du PDF. */
			public Long pdfPageCount;
			/** Taille des pages du PDF (format : "X x Y pts"). */
			public String pdfPageSize;
			/** Num�ro de version du PDF. */
			public Double pdfVersion;
			/** Nombre de mots dans le PDF (bas� sur le nombre d'espace). */
			public Long pdfWordCount;
			/** Indique si les r�f�rences bibliographiques sont fournis par l'�diteur. */
			public Boolean refBibsNative;
			/** Score de qualit�, calcul� selon les crit�res pr�c�dents. */
			public Double score;
		}
		public static class HitsRefBibs extends Istex {
			/** Tableau d'objets, chaque objet correspondant � un auteur r�f�renc�. */
			public HitsRefBibsAuthor author[];
			/** Objet contenant les informations li�es � la revue ou le livre . */
			public HitsRefBibsHost host;
			/** Date de publication r�f�renc�e. */
			public String publicationDate;
			/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public HitsRefBibsSerie serie;
			/** Titre r�f�renc�. */
			public String title;
			public static class HitsRefBibsAuthor extends Istex {
				/** Nom d'un auteur r�f�renc�. */
				public String name;
			}
			public static class HitsRefBibsHost extends Istex {
				/** Tableau d'objets, chaque objet correspondant � un auteur de la revue r�f�renc�e. */
				public HitsRefBibsHostAuthor author[];
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String isbn;
				/** Num�ro de la revue r�f�renc�e. */
				public String issue;
				/** Objet contenant les donn�es sur les pages, li�s � la revue. */
				public HitsRefBibsHostPages pages;
				/** Date de publication de la revue r�f�renc�e. */
				public String publicationDate;
				/** Titre de la revue r�f�renc�e. */
				public String title;
				/** Num�ro de volume de la revue r�f�renc�e. */
				public String volume;
				public static class HitsRefBibsHostAuthor extends Istex {
					/** Nom d'un auteur de la revue r�f�renc�e. */
					public String name;
				}
				public static class HitsRefBibsHostPages extends Istex {
					/** Premi�re page de la revue r�f�renc�e. */
					public String first;
					/** Derni�re page de la revue r�f�renc�e. */
					public String last;
				}
			}
			public static class HitsRefBibsSerie extends Istex {
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String title;
			}
		}
		public static class HitsSerie extends Istex {
			/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public HitsSerieAuthor author[];
			/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public HitsSerieConference conference[];
			/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String copyrightDate;
			/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public HitsSerieEditor editor[];
			/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String eissn[];
			/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String isbn[];
			/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String issn[];
			/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String issue;
			/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String language[];
			/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public HitsSeriePages pages;
			/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String publicationDate;
			/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String title;
			/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
			public String volume;
			public static class HitsSerieAuthor extends Istex {
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String affiliations[];
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String name;
			}
			public static class HitsSerieConference extends Istex {
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String name;
			}
			public static class HitsSerieEditor extends Istex {
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String affiliations[];
				/** R�f�renc� � {@link "https://api.istex.fr/mapping"} mais non d�crit dans {@link "https://api.istex.fr/documentation/fields"}. */
				public String name;
			}
			public static class HitsSeriePages extends Istex {
				/** Premi�re page de la revue r�f�renc�e. */
				public String first;
				/** Derni�re page de la revue r�f�renc�e. */
				public String last;
			}
		}
	}
	// @formatter:on



}
