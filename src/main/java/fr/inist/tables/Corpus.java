package fr.inist.tables;

import java.util.*;



/**
 * La classe {@link Corpus} associe le code ISNI à un nom de corpus ISTEX.
 * @author Ludovic WALLE
 */
public class Corpus {



	/**
	 * Définit les caractéristiques d'un corpus.
	 * @param corpusName Nom du corpus.
	 * @param fullName Nom développé du corpus.
	 * @param publisher Editeur.
	 * @param isni ISNI.
	 */
	private static void define(String corpusName, @SuppressWarnings("unused") String fullName, @SuppressWarnings("unused") String publisher, String isni) {
		ISNI_BY_CORPUS_NAME.put(corpusName, isni);
	}



	/**
	 * Retourne l'ISNI correspondant au corpus dont le nom est indiqué.
	 * @param corpusName Nom du corpus.
	 * @return L'ISNI correspondant au corpus, si il est connu, <code>null</code> sinon.
	 */
	public static String getIsni(String corpusName) {
		return ISNI_BY_CORPUS_NAME.get(corpusName);
	}



	/**
	 * Teste si le corpus dont le nom est indiqué existe.
	 * @param corpusName Nom du corpus.
	 * @return true si le corpus dont le nom est indiqué existe, <code>false</code> sinon.
	 */
	public static boolean isValid(String corpusName) {
		return ISNI_BY_CORPUS_NAME.containsKey(corpusName);
	}



	/**
	 * Code ISNI par nom de corpus.
	 */
	private static final Map<String, String> ISNI_BY_CORPUS_NAME = new TreeMap<>();



	static {
		define("bmj", "British Medical Journal (BMJ)", "BMJ Publishing group", "0000 0001 0727 9735");
		define("brill-hacco", "Brill — The Hague Academy Collected Courses Online", "Brill", "0000 0000 9816 9752");
		define("brill-journals", "Brill — journals", "Brill", "0000 0000 9816 9752");
		define("cambridge", "Cambridge University Press", "Cambridge University Press", "0000 0001 1088 0337");
		define("degruyter-journals", "De Gruyter", "De Gruyter", "0000 0001 1702 9756");
		define("ecco", "Gale — Eigtheenth Century Collections Online (ECCO)", "Gale", "0000 0004 4691 8589");
		define("edp-sciences", "EDP Sciences", "EDP Sciences", "0000 0001 1135 5331");
		define("eebo", "ProQuest — Early English Books Online (EEBO)", "ProQuest", "0000 0001 2167 7924");
		define("elsevier", "Elsevier", "Elsevier", "0000 0001 0672 9757");
		define("emerald", "Emerald", "Emerald", "0000 0004 0379 4459");
		define("gsl", "The Geological Society of London (GSL)", "The Geological Society of London (GSL)", "0000 0004 0423 9350");
		define("iop", "Institute of Physics Publishing (IOP)", "Institute of Physics Publishing (IOP)", "0000 0004 0442 3553");
		define("nature", "Nature", "Nature", "0000 0004 0637 920X");
		define("numerique-premium", "Numérique Premium", "Numérique Premium", null);
		define("oup", "Oxford University Press (OUP)", "Oxford University Press (OUP)", "0000 0001 2292 9185");
		define("rsc-ebooks", "The Royal Society of Chemistry (RSC) — e-books", "The Royal Society of Chemistry (RSC)", "0000 0001 2097 3756");
		define("rsc-journals", "The Royal Society of Chemistry (RSC)  — journals", "The Royal Society of Chemistry (RSC)", "0000 0001 2097 3756");
		define("sage", "Sage", "Sage", "0000 0004 0502 5717");
		define("springer-ebooks", "Springer — e-books", "Springer", "0000 0001 2242 518X");
		define("springer-journals", "Springer  — journals", "Springer", "0000 0001 2242 518X");
		define("wiley", "Wiley", "Wiley", "0000 0004 0380 1313");
	}



}
