package fr.inist;

import java.text.*;
import java.util.*;

import fr.inist.istex.*;
import fr.inist.istex.Istex.*;
import fr.inist.tables.*;



/**
 * La classe {@link IstexToOmtdShareConverter} implémente un convertisseur de métadonnées ISTEX en OMTD-SHARE.
 * @author Ludovic WALLE
 */
public class IstexToOmtdShareConverter {



	/**
	 * Convertit les métadonnées ISTEX en OMTD-SHARE.<br>
	 * Les données retournées sont encodées en US-ASCII.
	 * @param istex Métadonnées ISTEX.
	 * @param standalone Indicateur de document autonome (avec entête XML et <code>documentMetadataRecord</code>).
	 * @param indentation Chaine à mettre au début de chaque ligne en sortie. Ignoré si <code>standalone</code> est <code>true</code>. La suite de l'indentation est faite avec des tabulations.
	 * @return Les métadonnées au format OMTD-SHARE.
	 * @throws IllegalArgumentException Si les métadonnées ISTEX ne peuvent pas être converties en OMTD-SHARE.
	 */
	public static String convert(Hit istex, boolean standalone, String indentation) {
		StringBuilder omtd = new StringBuilder();
		boolean hasFullText = false;

		if (standalone) {
			indentation = "\t";
			omtd.append("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>\n");
			omtd.append("<ms:documentMetadataRecord xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.meta-share.org/OMTD-SHARE_XMLSchema http://www.meta-share.org/OMTD-SHARE_XMLSchema/v302/OMTD-SHARE-Publications.xsd\" xmlns:ms=\"http://www.meta-share.org/OMTD-SHARE_XMLSchema\">\n");
		} else if (indentation == null) {
			indentation = "";
		}
		omtd.append(indentation + "<ms:metadataHeaderInfo>\n");
		omtd.append(indentation + "	<ms:metadataRecordIdentifier metadataIdentifierSchemeName=\"OMTD\"/>\n");
		omtd.append(indentation + "	<ms:metadataCreationDate>" + DATE_FORMAT.format(new Date()) + "</ms:metadataCreationDate>\n");
		omtd.append(indentation + "	<ms:sourceOfMetadataRecord>\n");
		omtd.append(indentation + "		<ms:collectedFrom>\n");
		omtd.append(indentation + "			<ms:repositoryNames>\n");
		omtd.append(indentation + "				<ms:repositoryName>ISTEX</ms:repositoryName>\n");
		omtd.append(indentation + "			</ms:repositoryNames>\n");
		omtd.append(indentation + "			<ms:repositoryIdentifiers>\n");
		omtd.append(indentation + "				<ms:repositoryIdentifier repositoryIdentifierSchemeName=\"URL\">http://www.istex.fr</ms:repositoryIdentifier>\n");
		omtd.append(indentation + "			</ms:repositoryIdentifiers>\n");
		omtd.append(indentation + "		</ms:collectedFrom>\n");
		if (istex.arkIstex != null) {
			omtd.append(indentation + "		<ms:sourceMetadataLink>https://api.istex.fr/" + encode(istex.arkIstex) + "/record.mods?sid=omtd</ms:sourceMetadataLink>\n");
		} else if (istex.id != null) {
			omtd.append(indentation + "		<ms:sourceMetadataLink>https://api.istex.fr/document/" + istex.id + "/metadata/mods?sid=omtd</ms:sourceMetadataLink>\n");
		} else {
			throw new IllegalArgumentException("Il n'y a ni identifiant ISTEX, ni ARK ISTEX.");
		}
		if (istex.corpusName != null) {
			omtd.append(indentation + "		<ms:originalDataProviderInfo>\n");
			omtd.append(indentation + "			<ms:originalDataProviderType>publisher</ms:originalDataProviderType>\n");
			omtd.append(indentation + "			<ms:originalDataProviderPublisher>\n");
			omtd.append(indentation + "				<ms:organizationNames>\n");
			omtd.append(indentation + "					<ms:organizationName>" + encode(istex.corpusName) + "</ms:organizationName>\n");
			omtd.append(indentation + "				</ms:organizationNames>\n");
			if (Corpus.getIsni(istex.corpusName) != null) {
				omtd.append(indentation + "				<ms:organizationIdentifiers>\n");
				omtd.append(indentation + "					<ms:organizationIdentifier organizationIdentifierSchemeName=\"ISNI\">" + Corpus.getIsni(istex.corpusName) + "</ms:organizationIdentifier>\n");
				omtd.append(indentation + "				</ms:organizationIdentifiers>\n");
			}
			omtd.append(indentation + "			</ms:originalDataProviderPublisher>\n");
			omtd.append(indentation + "		</ms:originalDataProviderInfo>\n");
		}
		omtd.append(indentation + "	</ms:sourceOfMetadataRecord>\n");
		omtd.append(indentation + "</ms:metadataHeaderInfo>\n");
		omtd.append(indentation + "<ms:document>\n");
		omtd.append(indentation + "	<ms:publication>\n");
		omtd.append(indentation + "		<ms:documentType>withFullText</ms:documentType>\n");
		if (PublicationType.istexToSyntheticOmtd(set(istex.genre)) != null) {
			omtd.append(indentation + "		<ms:publicationType>" + encode(PublicationType.istexToSyntheticOmtd(set(istex.genre))) + "</ms:publicationType>\n");
		}
		omtd.append(indentation + "		<ms:identifiers>\n");
		if (istex.doi != null) {
			for (String doi : istex.doi) {
				omtd.append(indentation + "			<ms:publicationIdentifier publicationIdentifierSchemeName=\"DOI\">" + encode(doi) + "</ms:publicationIdentifier>\n");
			}
		}
		if (istex.pmid != null) {
			for (String pmid : istex.pmid) {
				omtd.append(indentation + "			<ms:publicationIdentifier publicationIdentifierSchemeName=\"PMID\">" + encode(pmid) + "</ms:publicationIdentifier>\n");
			}
		}
		if (istex.ark != null) {
			for (String ark : istex.ark) {
				if (!ark.equals(istex.arkIstex)) {
					omtd.append(indentation + "			<ms:publicationIdentifier publicationIdentifierSchemeName=\"ARK\">" + encode(ark) + "</ms:publicationIdentifier>\n");
				}
			}
		}
		if (istex.arkIstex != null) {
			omtd.append(indentation + "			<ms:publicationIdentifier publicationIdentifierSchemeName=\"ARK\">" + encode(istex.arkIstex) + "</ms:publicationIdentifier>\n");
		}
		omtd.append(indentation + "		</ms:identifiers>\n");
		omtd.append(indentation + "		<ms:titles>\n");
		if (istex.title != null) {
			omtd.append(indentation + "			<ms:title>" + encode(istex.title) + "</ms:title>\n");
		} else {
			throw new IllegalArgumentException("Il n'y a pas de titre.");
		}
		omtd.append(indentation + "		</ms:titles>\n");
		if (istex.author != null) {
			omtd.append(indentation + "		<ms:authors>\n");
			for (_Author author : istex.author) {
				omtd.append(indentation + "			<ms:author>\n");
				omtd.append(indentation + "				<ms:surname>" + encode(author.name) + "</ms:surname>\n");
				omtd.append(indentation + "			</ms:author>\n");
			}
			omtd.append(indentation + "		</ms:authors>\n");
		}
		if ((istex.publicationDate != null) && (Istex.YEAR.matcher(istex.publicationDate).matches())) {
			omtd.append(indentation + "		<ms:publicationDate>\n");
			omtd.append(indentation + "			<ms:year>" + istex.publicationDate + "</ms:year>\n");
			omtd.append(indentation + "		</ms:publicationDate>\n");
		}
		if (istex.corpusName != null) {
			omtd.append(indentation + "		<ms:publisher>\n");
			omtd.append(indentation + "			<ms:organizationNames>\n");
			omtd.append(indentation + "				<ms:organizationName>" + encode(istex.corpusName) + "</ms:organizationName>\n");
			omtd.append(indentation + "			</ms:organizationNames>\n");
			omtd.append(indentation + "		</ms:publisher>\n");
		}
		if ((istex.host != null) && (istex.host.genre != null) && (istex.host.genre.length == 1) && "journal".equals(istex.host.genre[0]) && (istex.host.title != null)) {
			omtd.append(indentation + "		<ms:journal>\n");
			omtd.append(indentation + "			<ms:journalTitles>\n");
			omtd.append(indentation + "				<ms:journalTitle>" + encode(istex.host.title) + "</ms:journalTitle>\n");
			omtd.append(indentation + "			</ms:journalTitles>\n");
			omtd.append(indentation + "		</ms:journal>\n");
		}
		omtd.append(indentation + "		<ms:distributions>\n");
		if (istex.fulltext != null) {
			for (__File file : istex.fulltext) {
				if (Extension.istexToOmtd(file.extension) != null) {
					omtd.append(indentation + "			<ms:documentDistributionInfo>\n");
					omtd.append(indentation + "				<ms:distributionLocation>https://api.istex.fr/" + encode(istex.arkIstex) + "/fulltext." + file.extension + "?sid=omtd</ms:distributionLocation>\n");
					omtd.append(indentation + "				<ms:hashkey> </ms:hashkey>\n");
					omtd.append(indentation + "				<ms:dataFormatInfo>\n");
					omtd.append(indentation + "					<ms:dataFormat>" + encode(Extension.istexToOmtd(file.extension)) + "</ms:dataFormat>\n");
					omtd.append(indentation + "				</ms:dataFormatInfo>\n");
					omtd.append(indentation + "			</ms:documentDistributionInfo>\n");
					hasFullText = true;
				}
			}
		}
		if (!hasFullText) {
			throw new IllegalArgumentException("Il n'y a ni pas de document en texte intégral.");
		}
		omtd.append(indentation + "		</ms:distributions>\n");
		omtd.append(indentation + "		<ms:rightsInfo>\n");
		omtd.append(indentation + "			<ms:licenceInfos>\n");
		omtd.append(indentation + "				<ms:licenceInfo>\n");
		omtd.append(indentation + "					<ms:licence>restrictedAccessUnspecified</ms:licence>\n");
		omtd.append(indentation + "				</ms:licenceInfo>\n");
		omtd.append(indentation + "			</ms:licenceInfos>\n");
		omtd.append(indentation + "			<ms:rightsStatement>restrictedAccess</ms:rightsStatement>\n");
		omtd.append(indentation + "			<ms:attributionText>\n");
		omtd.append(indentation + "				Restricted to members of French higher-education and research institutions (a.k.a. ESR)\n");
		omtd.append(indentation + "			</ms:attributionText>\n");
		omtd.append(indentation + "		</ms:rightsInfo>\n");
		for (String language : Language.istexToOmtd(set(istex.language))) {
			omtd.append(indentation + "		<ms:documentLanguages>\n");
			omtd.append(indentation + "			<ms:documentLanguage>" + Language.istexToOmtd(language) + "</ms:documentLanguage>\n");
			omtd.append(indentation + "		</ms:documentLanguages>\n");
		}
//		share.append("			<ms:keywords>\n");
//		share.append("				<ms:keyword>Pigs</ms:keyword>\n");
//		share.append("				<ms:keyword>Hepatitis</ms:keyword>\n");
//		share.append("				<ms:keyword>Hepatitis E</ms:keyword>\n");
//		share.append("				<ms:keyword>Short Report</ms:keyword>\n");
//		share.append("				<ms:keyword>Zoonosis</ms:keyword>\n");
//		share.append("				<ms:keyword>Hepatitis E Virus</ms:keyword>\n");
//		share.append("				<ms:keyword>Epidemiology</ms:keyword>\n");
//		share.append("			</ms:keywords>\n");
		if (istex.abstract$ != null) {
			omtd.append(indentation + "		<ms:abstracts>\n");
			omtd.append(indentation + "			<ms:abstract>" + encode(istex.abstract$) + "</ms:abstract>\n");
			omtd.append(indentation + "		</ms:abstracts>\n");
		}
		omtd.append(indentation + "	</ms:publication>\n");
		omtd.append(indentation + "</ms:document>\n");
		if (standalone) {
			omtd.append("</ms:documentMetadataRecord>\n");
		}
		return omtd.toString();
	}



	/**
	 * Crée un ensemble contenant les chaines indiquées.
	 * @param strings Chaines.
	 * @return L'ensemble.
	 */
	private static Set<String> set(String... strings) {
		Set<String> set = new TreeSet<>();

		if (strings != null) {
			for (String string : strings) {
				set.add(string);
			}
		}
		return set;
	}



	/**
	 * Encode la chaine indiquée en XML.
	 * @param raw Chaine à encoder.
	 * @return La chaine encodée.
	 */
	private static String encode(String raw) {
		StringBuilder encoded = new StringBuilder();
		char c;

		for (int i = 0; i < raw.length(); i++) {
			c = raw.charAt(i);
			if ((c >= 0x0020) && (c < 0x007E) && (c != '&') && (c != '<') && (c != '>') && (c != '\'') && (c != '"')) {
				encoded.append(c);
			} else {
				encoded.append(String.format("&#0x%04X;", (int) c));
			}
		}
		return encoded.toString();
	}



	/**
	 * Format de date avec une précision de la seconde.
	 */
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");



}
