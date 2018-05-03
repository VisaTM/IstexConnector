package fr.inist.tables;

import java.util.*;



/**
 * La classe {@link PublicationType} permet de faire des conversions entre des genres ISTEX et des types de publication OMTD.<br>
 * <br>
 * <u>OMTD</u><br>
 * Le schéma OMTD-SHARE définit les <code>publicationType</code>, dans {@link "https://openminted.github.io/releases/omtd-share/3.0.2/xsd/OMTD-SHARE-Publications.xsd"}, qui indique "<i>Specifies the
 * type of the publication; the list takes values from the COAR Controlled Vocabulary for Resource Type Genres (restricted to concepts under "text" that can be used for publications)</i>". La
 * documentation correspondante est à {@link "https://openminted.github.io/releases/omtd-share/3.0.2/"}.<br>
 * L'API n'apporte pas d'information. Voir {@link"https://github.com/openminted/content-connector-api"}.<br>
 * Les valeurs autorisées dans OMTD-SHARE pour <code>ms:publicationType</code> sont:
 * <ul>
 * <li><code>annotation</code>
 * <li><code>bachelorThesis</code>
 * <li><code>bibliography</code>
 * <li><code>book</code>
 * <li><code>bookPart</code>
 * <li><code>bookReview</code>
 * <li><code>conferenceObject</code>
 * <li><code>conferencePaper</code>
 * <li><code>conferencePaperNotInProceedings</code>
 * <li><code>conferencePoster</code>
 * <li><code>conferencePosterNotInProceedings</code>
 * <li><code>conferenceProceedings</code>
 * <li><code>contributionToJournal</code>
 * <li><code>dataPaper</code>
 * <li><code>doctoralThesis</code>
 * <li><code>editorial</code>
 * <li><code>internalReport</code>
 * <li><code>journal</code>
 * <li><code>journalArticle</code>
 * <li><code>lecture</code>
 * <li><code>letterToTheEditor</code>
 * <li><code>masterThesis</code>
 * <li><code>memorandum</code>
 * <li><code>other</code>
 * <li><code>otherTypeOfReport</code>
 * <li><code>patent</code>
 * <li><code>periodical</code>
 * <li><code>policyReport</code>
 * <li><code>prePrint</code>
 * <li><code>projectDeliverable</code>
 * <li><code>report</code>
 * <li><code>reportPart</code>
 * <li><code>reportToFundingAgency</code>
 * <li><code>researchArticle</code>
 * <li><code>researchProposal</code>
 * <li><code>review</code>
 * <li><code>reviewArticle</code>
 * <li><code>technicalDocumentation</code>
 * <li><code>technicalReport</code>
 * <li><code>thesis</code>
 * <li><code>workingPaper</code>
 * </ul>
 * <u>ISTEX</u><br>
 * Les valeurs trouvées dans le champ <code>/genre</code> d'ISTEX sont:
 * <ul>
 * <li><code>abstract</code>
 * <li><code>article</code>
 * <li><code>book</code>
 * <li><code>book-reviews</code>
 * <li><code>brief-communication</code>
 * <li><code>case-report</code>
 * <li><code>chapter</code>
 * <li><code>collected-courses</code>
 * <li><code>conference</code>
 * <li><code>editorial</code>
 * <li><code>other</code>
 * <li><code>research-article</code>
 * <li><code>review-article</code>
 * </ul>
 * Les valeurs du champ ISTEX <code>/originalGenre</code> sont souvent fantaisistes et ne sont pas traitées.<br>
 * @author Ludovic WALLE
 */
public class PublicationType {



	/**
	 * Calcule un tableau de types de document OMTD correspondant aux genres ISTEX indiqués.
	 * @param genres Genres ISTEX.
	 * @return Un tableau de types de document OMTD, éventuellement vide mais jamais <code>null</code>.
	 */
	public static Set<String> istexToOmtd(Set<String> genres) {
		Set<String> documentsTypes = new HashSet<>();

		if (genres != null) {
			for (String genre : genres) {
				documentsTypes.addAll(istexToOmtd(genre));
			}
		}
		return documentsTypes;
	}



	/**
	 * Convertit un genre ISTEX en types de document OMTD.
	 * @param genre Genre ISTEX.
	 * @return Un ensemble de types de document OMTD, vide si il n'y a pas d'équivalent ou si le genre ISTEX est <code>null</code>, jamais <code>null</code>.
	 */
	public static Set<String> istexToOmtd(String genre) {
		return OMTD_BY_ISTEX.containsKey(genre) ? OMTD_BY_ISTEX.get(genre) : new TreeSet<>();
	}



	/**
	 * Calcule un type de document OMTD synthétique correspondant aux genres ISTEX indiqués.<br>
	 * Si un seul type de document OMTD autre que <code>other</code> correspond aux genres ISTEX indiqués, le type de document OMTD synthétique est celui-ci. Dans tous les autres cas, le type de
	 * document OMTD synthétique est <code>other</code>.
	 * @param genres Genres ISTEX.
	 * @return Un type de document OMTD.
	 */
	public static String istexToSyntheticOmtd(Set<String> genres) {
		Set<String> documentsTypes;

		documentsTypes = istexToOmtd(genres);
		documentsTypes.remove("other");
		return (documentsTypes.size() == 1) ? documentsTypes.toArray(new String[1])[0] : "other";
	}



	/**
	 * Calcule un ensemble de genres ISTEX correspondant aux types de document OMTD indiqués.
	 * @param documentTypes Types de document OMTD.
	 * @return Un ensemble de genres ISTEX, éventuellement vide mais jamais <code>null</code>.
	 */
	public static Set<String> omtdToIstex(Set<String> documentTypes) {
		Set<String> genres = new HashSet<>();

		if (documentTypes != null) {
			for (String documentType : documentTypes) {
				genres.addAll(omtdToIstex(documentType));
			}
		}
		return genres;
	}



	/**
	 * Convertit un type de document OMTD en genres ISTEX.
	 * @param documentType Type de document OMTD.
	 * @return Un ensemble de genres ISTEX, vide si il n'y a pas d'équivalent ou si le type de document OMTD est <code>null</code>, jamais <code>null</code>.
	 */
	public static Set<String> omtdToIstex(String documentType) {
		return ISTEX_BY_OMTD.containsKey(documentType) ? ISTEX_BY_OMTD.get(documentType) : new TreeSet<>();
	}



	/**
	 * Crée un ensemble contenant les chaines indiquées.
	 * @param strings Chaines.
	 * @return L'ensemble.
	 */
	private static Set<String> set(String... strings) {
		Set<String> set = new TreeSet<>();

		for (String string : strings) {
			set.add(string);
		}
		return set;
	}



	/**
	 * Types de publications OMTD par type de publication ISTEX.<br>
	 * Plusieurs types de publication OMTD peuvent correspondre à un seul type de publication ISTEX.<br>
	 * Un même type de publication ISTEX peut correspondre à plusieurs types de publication ISTEX.
	 */
	private static final Map<String, Set<String>> ISTEX_BY_OMTD = new TreeMap<>();



	/**
	 * Types de publications ISTEX par type de publication OMTD.<br>
	 * Plusieurs types de publication ISTEX peuvent correspondre à un seul type de publication OMTD.<br>
	 * Un même type de publication ISTEX peut correspondre à plusieurs types de publication OMTD.
	 */
	private static final Map<String, Set<String>> OMTD_BY_ISTEX = new TreeMap<>();



	static {
		OMTD_BY_ISTEX.put("abstract", set("other"));
		OMTD_BY_ISTEX.put("article", set("journalArticle"));
		OMTD_BY_ISTEX.put("book", set("book"));
		OMTD_BY_ISTEX.put("book-reviews", set("bookReview"));
		OMTD_BY_ISTEX.put("brief-communication", set("contributionToJournal"));
		OMTD_BY_ISTEX.put("case-report", set("contributionToJournal"));
		OMTD_BY_ISTEX.put("chapter", set("bookPart"));
		OMTD_BY_ISTEX.put("collected-courses", set("lecture"));
		OMTD_BY_ISTEX.put("conference", set("conferencePaper"));
		OMTD_BY_ISTEX.put("editorial", set("editorial"));
		OMTD_BY_ISTEX.put("research-article", set("researchArticle"));
		OMTD_BY_ISTEX.put("review-article", set("reviewArticle"));

		// les valeurs associées sont bidons, elle servent juste à pouvoir tester le code
		ISTEX_BY_OMTD.put("annotation", set("article"));
		ISTEX_BY_OMTD.put("bachelorThesis", set("article"));
		ISTEX_BY_OMTD.put("bibliography", set("article"));
		ISTEX_BY_OMTD.put("book", set("article"));
		ISTEX_BY_OMTD.put("bookPart", set("article"));
		ISTEX_BY_OMTD.put("bookReview", set("article"));
		ISTEX_BY_OMTD.put("conferenceObject", set("article"));
		ISTEX_BY_OMTD.put("conferencePaper", set("article"));
		ISTEX_BY_OMTD.put("conferencePaperNotInProceedings", set("article"));
		ISTEX_BY_OMTD.put("conferencePoster", set("article"));
		ISTEX_BY_OMTD.put("conferencePosterNotInProceedings", set("article"));
		ISTEX_BY_OMTD.put("conferenceProceedings", set("article"));
		ISTEX_BY_OMTD.put("contributionToJournal", set("article"));
		ISTEX_BY_OMTD.put("dataPaper", set("article"));
		ISTEX_BY_OMTD.put("doctoralThesis", set("article"));
		ISTEX_BY_OMTD.put("editorial", set("article"));
		ISTEX_BY_OMTD.put("internalReport", set("article"));
		ISTEX_BY_OMTD.put("journal", set("article"));
		ISTEX_BY_OMTD.put("journalArticle", set("article"));
		ISTEX_BY_OMTD.put("lecture", set("article"));
		ISTEX_BY_OMTD.put("letterToTheEditor", set("article"));
		ISTEX_BY_OMTD.put("masterThesis", set("article"));
		ISTEX_BY_OMTD.put("memorandum", set("article"));
		ISTEX_BY_OMTD.put("other", set("article"));
		ISTEX_BY_OMTD.put("otherTypeOfReport", set("article"));
		ISTEX_BY_OMTD.put("patent", set("article"));
		ISTEX_BY_OMTD.put("periodical", set("article"));
		ISTEX_BY_OMTD.put("policyReport", set("article"));
		ISTEX_BY_OMTD.put("prePrint", set("article"));
		ISTEX_BY_OMTD.put("projectDeliverable", set("article"));
		ISTEX_BY_OMTD.put("report", set("article"));
		ISTEX_BY_OMTD.put("reportPart", set("article"));
		ISTEX_BY_OMTD.put("reportToFundingAgency", set("article"));
		ISTEX_BY_OMTD.put("researchArticle", set("article"));
		ISTEX_BY_OMTD.put("researchProposal", set("article"));
		ISTEX_BY_OMTD.put("review", set("article"));
		ISTEX_BY_OMTD.put("reviewArticle", set("article"));
		ISTEX_BY_OMTD.put("technicalDocumentation", set("article"));
		ISTEX_BY_OMTD.put("technicalReport", set("article"));
		ISTEX_BY_OMTD.put("thesis", set("article"));
		ISTEX_BY_OMTD.put("workingPaper", set("article"));
	}


}
