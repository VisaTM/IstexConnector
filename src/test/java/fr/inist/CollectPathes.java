package fr.inist;

import java.util.*;
import java.util.Map.*;

import fr.inist.istex.*;
import fr.inist.toolbox.json.*;
import toolbox.parallel.*;



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
		@SuppressWarnings("hiding") Long count;
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
	 * Collecte les chemins pour la requète indiquée.
	 * @param query Requète.
	 * @return Le nombre de documents traités.
	 */
	private static int collectPathes(String query) {
		IstexIterator istexIterator = null;
		JsonObject json = null;
		Map<String, Json> ignoredFragmentByPath = new HashMap<>();
		String id;
		Set<String> ids;
		String path;
		Map<String, Long> pathes = new TreeMap<>();


		for (;;) {
			try {
				pathes.clear();
				for (istexIterator = new IstexIterator(query, "*", null); istexIterator.hasNext();) {
					json = istexIterator.next();
					ignoredFragmentByPath.clear();
					id = json.getString("id");
					collectPathes(json, "", id, pathes);
					Istex.newHit(json, ignoredFragmentByPath);
					if (!ignoredFragmentByPath.isEmpty()) {
						for (Iterator<Entry<String, Json>> iterator = ignoredFragmentByPath.entrySet().iterator(); iterator.hasNext();) {
							Entry<String, Json> entry = iterator.next();
							path = entry.getKey().replaceAll("\\[[0-9]+\\]", "[]");
							synchronized (IGNORED_IDS_SAMPLES_BY_PATH) {
								if ((ids = IGNORED_IDS_SAMPLES_BY_PATH.get(path)) == null) {
									IGNORED_IDS_SAMPLES_BY_PATH.put(path, ids = new HashSet<>());
								}
								if (ids.size() <= SAMPLE_SIZE) {
									ids.add(id);
								}
							}
						}
					}
					oneMore();
				}
				for (Entry<String, Long> pathEntry : pathes.entrySet()) {
					synchronized (PATHES) {
						if (PATHES.containsKey(pathEntry.getKey())) {
							PATHES.put(pathEntry.getKey(), PATHES.get(pathEntry.getKey()) + pathEntry.getValue());
						} else {
							PATHES.put(pathEntry.getKey(), pathEntry.getValue());
						}
					}
				}
				System.out.println(query + " => " + istexIterator.getCount());
				return istexIterator.getCount();
			} catch (Throwable exception) {
				exception.printStackTrace();
				System.err.println("Retry: " + query);
				if (istexIterator != null) {
					forget(istexIterator.getCount());
				}
			}
		}
	}



	/**
	 * Décompte le nombre de documents indiqué.
	 * @param count Nombre de documents à décompter.
	 */
	private static synchronized void forget(@SuppressWarnings("hiding") int count) {
		CollectPathes.count -= count;
		System.out.println(String.format("%8d %.3f s", count, (System.currentTimeMillis() - START) / 1000.0));
	}



	/**
	 * @param args Inutilisé.
	 * @throws Throwable
	 */
	public static void main(String[] args) throws Throwable {
		int entryCount = 0;
		String chars;
		Set<String> queries = new TreeSet<>();

		chars = "0123456789BCDFGHJKLMNPQRSTVWXZ";
		for (int i = 0; i < chars.length(); i++) {
			for (int j = 0; j < chars.length(); j++) {
				queries.add("arkIstex:*" + chars.charAt(i) + "-" + chars.charAt(j));
			}
		}
//		queries.add("arkIstex:*0-P");
//		queries.add("arkIstex:\"/ark:/67375/[0-9A-Z]{3}-[0-9A-Z]{8}-[0-9A-Z]/\"");
//		queries.add("arkIstex:\"ark:/67375/1BB-767XVVZQ-F\"");
//		queries.add("arkIstexark:/67375/1BB-767XVVZQ-F\"
//		queries.add("id:4F3B3B2124544FFDA2A196493999AB11C191B013");
//		queries.add("controllability");
//		queries.add("controllability AND flavor");
//		queries.add("controllability AND flavor AND elder");

		if (SEQUENTIAL) {
			for (String query : queries) {
				collectPathes(query);
			}
		} else {
			new Enterprise<>(10, new ArrayStringMissionner(queries.toArray(new String[queries.size()])), new CollectorWorker()).run();
		}
		System.out.println("=========================================================");
		System.out.println(String.format("%8d documents en %.3f s", count, (System.currentTimeMillis() - START) / 1000.0));
		System.out.println("=========================================================");
		System.out.println("Echantillon d'identifiants pour chaque chemin non valide");
		for (Iterator<Entry<String, Set<String>>> ignoredIdsByPathIterator = IGNORED_IDS_SAMPLES_BY_PATH.entrySet().iterator(); ignoredIdsByPathIterator.hasNext();) {
			Entry<String, Set<String>> ignoredIdsByPathEntry = ignoredIdsByPathIterator.next();
			System.out.println(ignoredIdsByPathEntry.getKey());
			entryCount = 0;
			for (Iterator<String> ignoredIdsIterator = ignoredIdsByPathEntry.getValue().iterator(); ignoredIdsIterator.hasNext();) {
				if (++entryCount >= SAMPLE_SIZE) {
					System.out.println("\t ...");
					break;
				}
				String ignoredId = ignoredIdsIterator.next();
				System.out.println("\thttps://api.istex.fr/document/" + ignoredId);
			}
		}
		System.out.println();
		System.out.println("=========================================================");
		System.out.println("Statistiques sur les chemins rencontrés, valides ou non (nombre d'occurrences de champs)");
		for (Iterator<Entry<String, Long>> pathIterator = PATHES.entrySet().iterator(); pathIterator.hasNext();) {
			Entry<String, Long> entry = pathIterator.next();
			System.out.println(entry.getKey() + "\t" + entry.getValue());
		}
		System.out.println();
		System.out.println(String.format("%.3f s", (System.currentTimeMillis() - START) / 1000.0));
	}



	/**
	 * Comptabilise un document traité de plus.
	 */
	private static synchronized void oneMore() {
		if ((++count % 1000) == 0) {
			System.out.println(String.format("%8d %.3f s", count, (System.currentTimeMillis() - START) / 1000.0));
		}
	}



	/**
	 * Nombre de documents traités.
	 */
	private static int count = 0;



	/**
	 * Echantillon de numéro de documents par chemin ignoré.
	 */
	private static final Map<String, Set<String>> IGNORED_IDS_SAMPLES_BY_PATH = new TreeMap<>();



	/**
	 * Chemins recensés.
	 */
	private static final Map<String, Long> PATHES = new TreeMap<>();



	/**
	 * Taille des échantillons d'identifiants.
	 */
	private static final int SAMPLE_SIZE = 10;



	/**
	 * Indicateur d'exécution séquentiel des threads.
	 */
	private static final boolean SEQUENTIAL = false;



	/**
	 * Instant de démarrage de la collecte.
	 */
	private static final long START = System.currentTimeMillis();



	/**
	 * La classe {@link CollectorWorker} implémente un collecteur.
	 * @author Ludovic WALLE
	 */
	private static class CollectorWorker extends Worker<StringMission> {



		/**	 */
		private CollectorWorker() {}



		/**
		 * @param other Autre collecteur
		 */
		private CollectorWorker(CollectorWorker other) {
			super(other);
		}



		/**
		 * {@inheritDoc}
		 * La mission est une valeur du champ <code>q</code> d'une requète ISTEX.
		 */
		@Override protected int delegateDo(StringMission mission) throws Throwable {
			return collectPathes(mission.getString());
		}



		/**
		 * {@inheritDoc}
		 */
		@Override public CollectorWorker newOne() {
			return new CollectorWorker(this);
		}



	}



}
