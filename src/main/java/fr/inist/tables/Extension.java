package fr.inist.tables;

import java.util.*;

import fr.inist.istex.Istex.*;



/**
 * La classe {@link Extension} permet de faire des conversions entre des extensions ISTEX et des formats de données OMTD.<br>
 * <br>
 * <u>OMTD</u><br>
 * Le schéma OMTD-SHARE définit les <code>dataFormatType</code>, dans {@link "https://openminted.github.io/releases/omtd-share/3.0.2/xsd/OMTD-SHARE-ControlledVocabs.xsd"}, qui indique "<i>Specifies
 * the format that is used since often the mime type will not be sufficient for machine processing; NOTE: normally the format should be represented as a combination of the mimetype (e.g.
 * application/xml) and some name and link to the documentation about the supplementary conventions used (e.g xces, alvisED etc.)</i>". La documentation correspondante est à
 * {@link "https://openminted.github.io/releases/omtd-share/3.0.2/"}.<br>
 * Les valeurs qui nous intéressent parmi celles autorisées dans OMTD-SHARE pour <code>ms:publicationType</code> sont:
 * <ul>
 * <li><code>http://w3id.org/meta-share/omtd-share/Pdf</code>
 * <li><code>http://w3id.org/meta-share/omtd-share/Tei</code>
 * <li><code>http://w3id.org/meta-share/omtd-share/Text</code>
 * </ul>
 * <u>ISTEX</u><br>
 * Les valeurs trouvées dans les champs <code>.../extension</code> d'ISTEX sont:
 * <ul>
 * <li><code>ocr</code>
 * <li><code>pdf</code>
 * <li><code>tei</code>
 * <li><code>tiff</code>
 * <li><code>txt</code>
 * <li><code>zip</code>
 * </ul>
 * <code>ocr</code> correspond à une ré-OCRistation du document en format texte.<br>
 * <code>zip</code> est utilisé si les données sont en plusieurs parties (voir {@link "https://api.istex.fr/documentation/files/#acces-aux-fulltext"}).
 * @author Ludovic WALLE
 */
public class Extension {



	/**
	 * Définit une équivalence de codes.
	 * @param extension Extension ISTEX.
	 * @param format Format de fichier OMTD.
	 */
	private static void define(String extension, String format) {
		FORMAT_BY_EXTENSION.put(extension, format);
		EXTENSION_BY_FORMAT.put(format, extension);
	}



	/**
	 * Retourne l'URI du format de fichier OMTD correspondant à l'extension ISTEX indiquée.
	 * @param extension Extension de fichier ({@link __File#extension}.
	 * @return L'URI du format de fichier OMTD correspondant à l'extension ISTEX indiquée si elle est prise en compte, <code>null</code> sinon.
	 */
	public static String istexToOmtd(String extension) {
		return FORMAT_BY_EXTENSION.get(extension);
	}



	/**
	 * Retourne l'extension ISTEX correspondant à l'URI du format de fichier OMTD indiquée.
	 * @param format Format de fichier OMTD ({@link __File#extension}.
	 * @return L'extension ISTEX correspondant à l'URI du format de fichier OMTD indiquée si elle est prise en compte, <code>null</code> sinon.
	 */
	public static String omtdToIstex(String format) {
		return EXTENSION_BY_FORMAT.get(format);
	}



	/**
	 * Extension ISTEX par format de fichier OMTD.
	 */
	private static final Map<String, String> EXTENSION_BY_FORMAT = new TreeMap<>();



	/**
	 * Format de fichier OMTD par extension ISTEX.
	 */
	private static final Map<String, String> FORMAT_BY_EXTENSION = new TreeMap<>();



	static {
		define("pdf", "http://w3id.org/meta-share/omtd-share/Pdf");
		define("tei", "http://w3id.org/meta-share/omtd-share/Tei");
		define("txt", "http://w3id.org/meta-share/omtd-share/Text");
	}



}
