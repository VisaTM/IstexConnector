package fr.inist.tables;

import java.util.*;



/**
 * La classe {@link Language} permet de faire des conversions entre des codes langue ISTEX et OMTD.<br>
 * <br>
 * <u>OMTD</u><br>
 * Le sch�ma OMTD-SHARE d�finit les <code>languageIdType</code> dans {@link "https://openminted.github.io/releases/omtd-share/3.0.2/xsd/OMTD-SHARE-ISOVocabularies.xsd"}, qui indique "<i>Base type for
 * languages according to ISO 639; the codes are taken from ISO 639-1 and, if not covered, from ISO 639-3, according to the BCP-47 guidlelines</i>". La documentation correspondante est �
 * {@link "https://openminted.github.io/releases/omtd-share/3.0.2/"}. Voir {@link "http://www-01.sil.org/iso639-3/codes.asp"}.<br>
 * D'apr�s les sp�cifications de l'API <code>ContentConnector</code>, les codes langue sont "iso639-1 or iso639-3". Voir {@link"https://github.com/openminted/content-connector-api"}.<br>
 * <br>
 * <u>ISTEX</u><br>
 * Les valeurs trouv�es dans le champ <code>/language</code> d'ISTEX sont:
 * <ul>
 * <li>Une valeur ne faisant partie d'aucune norme, fr�quente, et qui devrait probablement �tre <code>und</code>:
 * <ul>
 * <li><code>unknown</code>
 * </ul>
 * <li>Des codes sp�ciaux:
 * <ul>
 * <li><code>mul</code> (langues multiples)
 * <li><code>und</code> (langue ind�termin�e)
 * <li><code>zxx</code> (pas de contenu linguistique, inapplicable)
 * </ul>
 * <li>Des codes langue:
 * <ul>
 * <li><code>alg</code>
 * <li><code>amh</code>
 * <li><code>ang</code>
 * <li><code>ara</code>
 * <li><code>arc</code>
 * <li><code>arm</code>
 * <li><code>cat</code>
 * <li><code>chi</code>
 * <li><code>cze</code>
 * <li><code>dan</code>
 * <li><code>dut</code>
 * <li><code>eng</code>
 * <li><code>fre</code>
 * <li><code>frm</code>
 * <li><code>fro</code>
 * <li><code>ger</code>
 * <li><code>gla</code>
 * <li><code>gle</code>
 * <li><code>glg</code>
 * <li><code>glv</code>
 * <li><code>gmh</code>
 * <li><code>grc</code>
 * <li><code>gre</code>
 * <li><code>heb</code>
 * <li><code>hun</code>
 * <li><code>ita</code>
 * <li><code>lat</code>
 * <li><code>lit</code>
 * <li><code>may</code>
 * <li><code>moh</code>
 * <li><code>nai</code>
 * <li><code>new</code>
 * <li><code>nor</code>
 * <li><code>pal</code>
 * <li><code>peo</code>
 * <li><code>per</code>
 * <li><code>pol</code>
 * <li><code>por</code>
 * <li><code>roa</code>
 * <li><code>rus</code>
 * <li><code>san</code>
 * <li><code>sco</code>
 * <li><code>spa</code>
 * <li><code>swe</code>
 * <li><code>syr</code>
 * <li><code>tur</code>
 * <li><code>wel</code>
 * </ul>
 * </ul>
 * Les codes possibles sont probablement tous ceux de iso 639-3 ou iso639-2/B.<br>
 * @author Ludovic WALLE
 */
public class Language {



	/**
	 * D�finit une �quivalence de codes.
	 * @param three Code sur 3 caract�res.
	 * @param two Code sur 2 caract�res.
	 */
	private static void define(String three, String two) {
		TWO_BY_THREE.put(three, two);
		THREE_BY_TWO.put(two, three);
	}



	/**
	 * Convertit un code langue ISTEX en code langue OMTD.
	 * @param istexLanguage Code langue ISTEX.
	 * @return Le code langue OMTD, ou ISTEX si il n'a pas d'�quivalent.
	 */
	public static String istexToOmtd(String istexLanguage) {
		String omtdLanguage;

		if ((omtdLanguage = TWO_BY_THREE.get(istexLanguage)) == null) {
			omtdLanguage = istexLanguage;
		}
		return omtdLanguage;
	}



	/**
	 * Calcule un tableau de codes langue OMTD correspondant aux codes langue ISTEX indiqu�s.
	 * @param istexLanguages Codes langue ISTEX.
	 * @return Un tableau de codes langue OMTD, �ventuellement vide mais jamais <code>null</code>.
	 */
	public static Set<String> istexToOmtd(Set<String> istexLanguages) {
		Set<String> omtdLanguages = new HashSet<>();
		String omtdLanguage;

		if (istexLanguages != null) {
			for (String istexLanguage : istexLanguages) {
				if ((omtdLanguage = istexToOmtd(istexLanguage)) != null) {
					omtdLanguages.add(omtdLanguage);
				}
			}
		}
		return omtdLanguages;
	}



	/**
	 * Calcule un ensemble de codes langue ISTEX correspondant aux codes langue OMTD indiqu�s.
	 * @param omtdLanguages Code langue OMTD.
	 * @return Un ensemble de codes langue ISTEX, �ventuellement vide mais jamais <code>null</code>.
	 */
	public static Set<String> omtdToIstex(Set<String> omtdLanguages) {
		Set<String> istexLanguages = new HashSet<>();
		String istexLanguage;

		if (omtdLanguages != null) {
			for (String omtdLanguage : omtdLanguages) {
				if ((istexLanguage = omtdToIstex(omtdLanguage)) != null) {
					istexLanguages.add(istexLanguage);
				}
			}
		}
		return istexLanguages;
	}



	/**
	 * Convertit un code langue OMTD en code langue ISTEX.
	 * @param omtdLanguage Code langue OMTD.
	 * @return Le code langue ISTEX, ou OMTD si il n'a pas d'�quivalent.
	 */
	public static String omtdToIstex(String omtdLanguage) {
		String istexLanguage;

		if ((istexLanguage = THREE_BY_TWO.get(omtdLanguage)) == null) {
			istexLanguage = omtdLanguage;
		}
		return istexLanguage;
	}



	/**
	 * Equivalent sur 3 caract�res pour un code sur 2 caract�res.
	 */
	private static final Map<String, String> THREE_BY_TWO = new TreeMap<>();



	/**
	 * Equivalent sur 2 caract�res pour un code sur 3 caract�res.
	 */
	private static final Map<String, String> TWO_BY_THREE = new TreeMap<>();



	/**
	 * D�finit les �quivalences pour les valeurs pr�sentes dans ISTEX, d'apr�s {@link "http://www-01.sil.org/iso639-3/codes.asp"}.<br>
	 * Les codes pr�sents dans ISTEX sans �quivalents sur 2 caract�res sont mis en commentaire, pour indiquer qu'ils ont �t� pris en compte.
	 */
	static {
		// unknown
//		define("unknown", "");

		// code sp�ciaux
//		define("mul", "");
//		define("und", "");
//		define("zxx", "");

		// codes langue, dans l'ordre alphab�tique
//		define("alg", "");
		define("amh", "am");
//		define("ang", "");
		define("ara", "ar");
//		define("arc", "");
		define("arm", "hy");
		define("cat", "ca");
		define("chi", "zh");
		define("cze", "cs");
		define("dan", "da");
		define("dut", "nl");
		define("eng", "en");
		define("fre", "fr");
//		define("frm", "");
//		define("fro", "");
		define("ger", "de");
		define("gla", "gd");
		define("gle", "ga");
		define("glg", "gl");
		define("glv", "gv");
//		define("gmh", "");
//		define("grc", "");
		define("gre", "el");
		define("heb", "he");
		define("hun", "hu");
		define("ita", "it");
		define("lat", "la");
		define("lit", "lt");
		define("may", "ms");
//		define("moh", "");
//		define("nai", "");
//		define("new", "");
		define("nor", "no");
//		define("pal", "");
//		define("peo", "");
		define("per", "fa");
		define("pol", "pl");
		define("por", "pt");
//		define("roa", "");
		define("rus", "ru");
		define("san", "sa");
//		define("sco", "");
		define("spa", "es");
		define("swe", "sv");
//		define("syr", "");
		define("tur", "tr");
		define("wel", "cy");
	}



}
