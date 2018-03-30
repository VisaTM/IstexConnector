package fr.inist;

import java.util.*;
import java.util.Map.*;

import fr.inist.istex.*;
import fr.inist.toolbox.json.*;



/**
 * La classe {@link CollectPathes} collecte les chemins dans les éléments hits de tous les document ISTEX.
 * @author Ludovic WALLE
 */
public class CollectPathes {



	/**
	 * Collecte récursivement les chemins.
	 * @param json Element json à analyser.
	 * @param parentPath Chemin de l'élément json.
	 * @param id Identifiant du document.
	 * @param pathes Compteurs d'occurrences de chemins.
	 */
	private static void collectPathes(Json json, String parentPath, String id, Map<String, Long> pathes) {
		Entry<String, Json> pair;
		Long count;
		String path = null;

		if (json == null) {
			path = parentPath + "\tnull";
		} else if (json instanceof JsonAtomic) {
			path = parentPath + "\t" + json.getClass().getSimpleName();
		} else if (json instanceof JsonArray) {
			for (Iterator<Json> arrayIterator = ((JsonArray) json).iterator(); arrayIterator.hasNext();) {
				collectPathes(arrayIterator.next(), parentPath + "[]", id, pathes);
			}
		} else if (json instanceof JsonObject) {
			for (Iterator<Entry<String, Json>> pairIterator = ((JsonObject) json).iterator(); pairIterator.hasNext();) {
				pair = pairIterator.next();
				collectPathes(pair.getValue(), parentPath + "/" + pair.getKey(), id, pathes);
			}
		} else {
			throw new RuntimeException();
		}
		if (path != null) {
			if ((count = pathes.get(path)) != null) {
				pathes.put(path, count + 1);
			} else {
				pathes.put(path, 1L);
			}
		}
	}



	/**
	 * @param args Inutilisé.
	 * @throws Throwable
	 */
	public static void main(String[] args) throws Throwable {

//		queries.add("arkIstex:*0-P");
//		queries.add("arkIstex:\"/ark:/67375/[0-9A-Z]{3}-[0-9A-Z]{8}-[0-9A-Z]/\"");
//		queries.add("arkIstex:\"ark:/67375/1BB-767XVVZQ-F\"");
//		queries.add("arkIstexark:/67375/1BB-767XVVZQ-F\"
//		queries.add("id:4F3B3B2124544FFDA2A196493999AB11C191B013");
//		queries.add("controllability");
//		queries.add("controllability AND flavor");
//		queries.add("controllability AND flavor AND elder");

		Map<String, Long> pathes = new TreeMap<>();
		Map<String, Json> ignoredFragmentByPath = new HashMap<>();
		Map<String, Set<String>> ignoredIdsSamplesByPath = new TreeMap<>();
		Set<String> ids;
		String id;
		String path;
		JsonObject json = null;
		int entryCount = 0;
		int count = 0;

		for (IstexSlicedIterator istexIterator = new IstexSlicedIterator("*", null, null, 20); istexIterator.hasNext();) {
//		for (IstexIterator istexIterator = new IstexIterator("stylo", "*", "publicationDate,corpusName"); istexIterator.hasNext();) {
			try {
				json = istexIterator.next();
				if (json != null) {
					ignoredFragmentByPath.clear();
					id = json.getString("id");
					collectPathes(json, "", id, pathes);
					Istex.newHit(json, ignoredFragmentByPath);
					if (!ignoredFragmentByPath.isEmpty()) {
						for (Iterator<String> iterator = ignoredFragmentByPath.keySet().iterator(); iterator.hasNext();) {
							path = iterator.next().replaceAll("\\[[0-9]+\\]", "[]");
							if ((ids = ignoredIdsSamplesByPath.get(path)) == null) {
								ignoredIdsSamplesByPath.put(path, ids = new HashSet<>());
							}
							if (ids.size() <= SAMPLE_SIZE) {
								ids.add(id);
							}
						}
					}
					if ((++count % 1000) == 0) {
						System.out.println(count);
					}
					if (count == 1000000) {
						break;
					}
				}
			} catch (Throwable exception) {
				exception.printStackTrace();
			}
		}
		System.out.println(String.format("%8d documents en %.3f s", count, (System.currentTimeMillis() - START) / 1000.0));
//		System.out.println("=========================================================");
//		System.out.println(String.format("%8d documents en %.3f s", count, (System.currentTimeMillis() - START) / 1000.0));
//		System.out.println("=========================================================");
//		System.out.println("Echantillon d'identifiants pour chaque chemin non valide");
//		for (Iterator<Entry<String, Set<String>>> ignoredIdsByPathIterator = ignoredIdsSamplesByPath.entrySet().iterator(); ignoredIdsByPathIterator.hasNext();) {
//			Entry<String, Set<String>> ignoredIdsByPathEntry = ignoredIdsByPathIterator.next();
//			System.out.println(ignoredIdsByPathEntry.getKey());
//			entryCount = 0;
//			for (Iterator<String> ignoredIdsIterator = ignoredIdsByPathEntry.getValue().iterator(); ignoredIdsIterator.hasNext();) {
//				if (++entryCount >= SAMPLE_SIZE) {
//					System.out.println("\t ...");
//					break;
//				}
//				System.out.println("\thttps://api.istex.fr/document/" + ignoredIdsIterator.next());
//			}
//		}
//		System.out.println();
//		System.out.println("=========================================================");
//		System.out.println("Statistiques sur les chemins rencontrés, valides ou non (nombre d'occurrences de champs)");
//		for (Iterator<Entry<String, Long>> pathIterator = pathes.entrySet().iterator(); pathIterator.hasNext();) {
//			Entry<String, Long> pathEntry = pathIterator.next();
//			System.out.println(pathEntry.getKey() + "\t" + pathEntry.getValue());
//		}
//		System.out.println();
//		System.out.println(String.format("%.3f s", (System.currentTimeMillis() - START) / 1000.0));
	}



	/**
	 * Taille des échantillons d'identifiants.
	 */
	private static final int SAMPLE_SIZE = 10;



	/**
	 * Instant de démarrage de la collecte.
	 */
	private static final long START = System.currentTimeMillis();



}
