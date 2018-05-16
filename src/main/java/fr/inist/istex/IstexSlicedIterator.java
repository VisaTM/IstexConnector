package fr.inist.istex;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.logging.log4j.*;

import toolbox.json.*;
import toolbox.json.JsonObject.*;
import toolbox.parallel.*;



/**
 * La classe {@link IstexSlicedIterator} implémente un itérateur sur une recherche ISTEX en l'appliquant avec un certain parallélisme à des sous ensembles exclusifs de documents ISTEX (tranches), et
 * en traitant les erreurs.<br>
 * En cas d'erreur sur une tranche, la recherche sera relancée sur celle-ci, en ignorant les résultats déjà récupérés. Les résultats unifiés de toutes les tranches sont récupérables par
 * {@link IstexSlicedIterator#next()}. Une recherche préliminaire permet d'obtenir le nombre total de résulats attendus, et les éventuelles agrégations correspondant aux facettes demandées.<br>
 * Cette façon de faire peut aboutir à des incohérences liées à la multiplicité des recherches et à leur exécution décalées dans le temps si le contenu d'ISTEX évolue pendant le traitement, mais c'est
 * très peu probable.
 * @author Ludovic WALLE
 */
public class IstexSlicedIterator extends IstexIterator {



	/**
	 * @param query Requète, ne doit être ni vide ni ni <code>null</code>. Voir {@link "https://api.istex.fr/documentation/search/"}.
	 * @param output Données à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/results/#selection-des-champs-renvoyes"}.
	 * @param facets Facettes à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/facets/"}.
	 * @param count Nombre d'exécutions parallèles de tranches de requètes.
	 */
	public IstexSlicedIterator(String query, String output, String facets, int count) {
		super(query, output, facets);

		@SuppressWarnings("hiding") JsonObject json;
		JsonArray hits;

		try {
			json = JsonObject.parse(new String(Readers.getBytesFromURL("https://api.istex.fr/document/?size=0&q=" + URLEncoder.encode(query, "UTF-8") + ((facets != null) ? "&facets=" + URLEncoder.encode(facets, "UTF-8") : ""))).trim());
			if (json.has("_error")) {
				throw new IstexException(LOGGER, Level.ERROR, "Erreur ISTEX: " + json.toString());
			} else {
				total = json.cutInteger("total", Option.PRESENT_AND_NOT_NULL_AND_NOT_EMPTY).intValue();
				aggregations = json.cutJsonObject("aggregations");
				hits = json.cutJsonArray("hits", Option.PRESENT_AND_NOT_NULL);
				if (hits.isNotEmpty()) {
					throw new IstexException(LOGGER, Level.WARN, "Pour la requète initiale de \"" + query + "\", il y a des réponses: " + json.toString());
				}
				if (json.isNotEmpty()) {
					throw new IstexException(LOGGER, Level.WARN, "Pour la requète initiale de \"" + query + "\", des éléments de la réponse ISTEX ne sont pas pris en compte: " + json.toString());
				}
			}
			enterprise = new Enterprise<>(count, new SliceMissionner(query, output), new SliceWorker());
			enterprise.setDaemon(true);
			enterprise.start();
		} catch (JsonException | IOException exception) {
			throw new IstexException(LOGGER, Level.ERROR, exception);
		}
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public synchronized boolean hasNext() {
		while ((this.json == null) && !enterprise.hasClosedDown()) {
			try {
				// A la fin de l'entreprise, il y a un petit moment où tout est terminé mais où l'entreprise n'a pas encore fermé. Il n'y aura donc plus d'appel à setJson, donc pas de notifyAll pour
				// interrompre le wait => limiter l'attente
				wait(100);
			} catch (InterruptedException exception) {
			}
		}
		if (this.json != null) {
			return true;
		} else if (count == total) {
			return false;
		} else {
			throw new IstexException(LOGGER, Level.ERROR, "Pour la requète \"" + query + "\", le nombre de documents retournés (" + count + ") est différent de celui attendu (" + total + ").");
		}
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public synchronized JsonObject next() {
		@SuppressWarnings("hiding") JsonObject json;

		while ((this.json == null) && !enterprise.hasClosedDown()) {
			try {
				// A la fin de l'entreprise, il y a un petit moment où tout est terminé mais où l'entreprise n'a pas encore fermé. Il n'y aura donc plus d'appel à setJson, donc pas de notifyAll pour
				// interrompre le wait => limiter l'attente
				wait(100);
			} catch (InterruptedException exception) {
			}
		}
		if (this.json == null) {
			throw new NoSuchElementException();
		}
		json = this.json;
		this.json = null;
		count++;
		notifyAll();
		return json;
	}



	/**
	 * Spécifie le prochain résultat à retourner.
	 * @param json Le prochain résultat à retourner.
	 */
	private synchronized void setJson(JsonObject json) {
		while (this.json != null) {
			try {
				wait();
			} catch (InterruptedException exception) {
			}
		}
		this.json = json;
		notifyAll();
	}



	/**
	 * Entreprise qui va gérer les recherches sur les tranches.
	 */
	private final Enterprise<SliceMission> enterprise;



	/**
	 * Le prochain résultat à retourner, ou <code>null</code> si il n'y en a pas.
	 */
	private volatile JsonObject json = null;



	/**
	 * La classe {@link SliceMission} contient les informations nécessaires à l'exécution d'une recherche sur une tranche par un {@link SliceWorker}.
	 * @author Ludovic WALLE
	 */
	private static class SliceMission implements Mission {



		/**
		 * @param sliceQuery Requète.
		 * @param output Données à retourner.
		 */
		public SliceMission(String sliceQuery, String output) {
			this.sliceQuery = sliceQuery;
			this.output = output;
		}



		/**
		 * Données à retourner.
		 */
		public final String output;



		/**
		 * Requète.
		 */
		public final String sliceQuery;



	}



	/**
	 * La classe {@link SliceMissionner} distribue des requètes sur des sous ensembles de documents. Le découpage en sous ensembles se fait sur l'ARK ISTEX, présent dans tous les documents.<br>
	 * Seule la fin de l'ARK (<code>[0-9BCDFGHJKLMNPQRSTVWXZ]{8}-[0-9BCDFGHJKLMNPQRSTVWXZ]</code>, voir {@link Istex.Hit#arkIstex}) est utilisable comme clé de découpage car les caractères semblent
	 * avoir une fréquence uniforme.<br>
	 * Le découpage se fait en fixant deux de ces caractères. Il y a 30 possibilités pour ces caractères ([0-9BCDFGHJKLMNPQRSTVWXZ]), ce qui fait 30x30 = 900 tranches de taille raisonnable.<br>
	 * Un découpage en fixant un seul caractère donne 30 tranches de taille importante. La probabilité d'avoir une erreur est donc importante aussi, ainsi que le temps nécessaire pour une nouvelle
	 * tentative, sans compter qu'il en faudra peut-être plusieurs.<br>
	 * Un découpage en fixant plus de deux caractères donne beaucoup de tranches (30x30x30 = 27000 pour 3 caractères). Le temps de traitement de tant de requètes devient trop important.
	 * @author Ludovic WALLE
	 */
	private static class SliceMissionner extends Missionner<SliceMission> {



		/**
		 * @param query Requète.
		 * @param output Données à retourner.
		 */
		public SliceMissionner(String query, String output) {
			String chars = "0123456789BCDFGHJKLMNPQRSTVWXZ";

			for (int i = 0; i < chars.length(); i++) {
				for (int j = 0; j < chars.length(); j++) {
					slicesQueries.add("arkIstex:ark\\:\\/67375\\/???-" + chars.charAt(i) + chars.charAt(j) + "*" + " AND (" + query + ")");
				}
			}
			this.output = output;
		}



		/**
		 * {@inheritDoc}
		 */
		@Override protected SliceMission delegateGetNext() throws Throwable {
			if (slicesQueries.isEmpty()) {
				return null;
			} else {
				return new SliceMission(slicesQueries.remove(0), output);
			}
		}



		/**
		 * Données à retourner.
		 */
		public final String output;



		/**
		 * Tranches de requètes à traiter.
		 */
		public final Vector<String> slicesQueries = new Vector<>();



	}



	/**
	 * La classe {@link SliceWorker} implémente la récupération des résultats d'une tranche de recherche, en recommençant autant de fois que nécessaire, et sans renvoyer plusieurs fois le même
	 * résultat.
	 * @author Ludovic WALLE
	 */
	private class SliceWorker extends Worker<SliceMission> {



		/**	 */
		public SliceWorker() {
			setDaemon(true);
		}



		/**
		 * @param other Autre collecteur
		 */
		public SliceWorker(SliceWorker other) {
			super(other);
			setDaemon(true);
		}



		/**
		 * {@inheritDoc} La mission est une valeur du champ <code>q</code> d'une requète ISTEX.
		 */
		@Override protected int delegateDo(SliceMission mission) throws Throwable {
			Set<String> ids = new HashSet<>();
			String id;
			IstexIterator istexIterator;
			@SuppressWarnings("hiding") JsonObject json;

			LOGGER.log(Level.INFO, mission.sliceQuery);
			for (;;) {
				try {
					for (istexIterator = new IstexSimpleIterator(mission.sliceQuery, mission.output, null); istexIterator.hasNext();) {
						json = istexIterator.next();
						id = json.getString("id");
						if (!ids.contains(id)) {
							setJson(json);
							ids.add(id);
						}
					}
					LOGGER.log(Level.INFO, mission.sliceQuery + " => " + ids.size());
					return ids.size();
				} catch (Throwable exception) {
					LOGGER.log(Level.INFO, mission.sliceQuery);
				}
			}
		}



		/**
		 * {@inheritDoc}
		 */
		@Override public SliceWorker newOne() {
			return new SliceWorker(this);
		}



	}



}
