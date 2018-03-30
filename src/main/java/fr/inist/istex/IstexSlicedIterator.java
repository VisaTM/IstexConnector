package fr.inist.istex;

import java.io.*;
import java.net.*;
import java.util.*;

import fr.inist.toolbox.*;
import fr.inist.toolbox.json.*;
import fr.inist.toolbox.json.JsonObject.*;
import fr.inist.toolbox.parallel.*;



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
	 * @param query Requète. Voir {@link "https://api.istex.fr/documentation/search/"}.
	 * @param output Données à retourner. Voir {@link "https://api.istex.fr/documentation/results/#selection-des-champs-renvoyes"}.
	 * @param count Nombre d'exécutions parallèles de tranches de requètes.
	 * @param facets Facettes à retourner, ignoré si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/facets/"}.
	 */
	public IstexSlicedIterator(String query, String output, String facets, int count) {
		super(query, output, facets);

		@SuppressWarnings("hiding") JsonObject json;
		JsonArray hits;

		try {
			json = JsonObject.parse(new String(Readers.getBytesFromURL("https://api.istex.fr/document/?size=0&q=" + ((query != null) ? URLEncoder.encode(query, "UTF-8") : "") + ((facets != null) ? "&facets=" + URLEncoder.encode(facets, "UTF-8") : ""))).trim());
			if (json.has("_error")) {
				throw new IstexRuntimeException("Erreur ISTEX: " + json.toString());
			} else {
				total = json.cutInteger("total", Option.PRESENT_AND_NOT_NULL_AND_NOT_EMPTY);
				aggregations = json.cutJsonObject("aggregations");
				hits = json.cutJsonArray("hits", Option.PRESENT_AND_NOT_NULL);
				if (hits.isNotEmpty()) {
					throw new IstexRuntimeException("Pour la requète initiale de \"" + query + "\", il y a des réponses: " + json.toString());
				}
				if (json.isNotEmpty()) {
					throw new IstexRuntimeException("Pour la requète initiale de \"" + query + "\", des éléments de la réponse ISTEX ne sont pas pris en compte: " + json.toString());
				}
			}
			enterprise = new Enterprise<>(count, new SliceMissionner(query, output), new SliceWorker());
			enterprise.setDaemon(true);
			enterprise.start();
		} catch (JsonException | IOException exception) {
			throw new IstexRuntimeException(exception);
		}
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public JsonObject getAggregations() {
		return aggregations;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public int getTotal() {
		return total;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public synchronized boolean hasNext() {
		if ((json != null) || !enterprise.hasClosedDown()) {
			return true;
		} else if (count == total) {
			return false;
		} else {
			throw new IstexRuntimeException("Pour la requète \"" + query + "\", le nombre de documents retournés (" + count + ") est différent de celui attendu (" + total + ").");
		}
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public synchronized JsonObject next() {
		@SuppressWarnings("hiding") JsonObject json;

		while ((this.json == null) && !enterprise.hasClosedDown()) {
			try {
				wait(100);
			} catch (InterruptedException exception) {
			}
		}
		json = this.json;
		this.json = null;
		if (json != null) {
			count++;
		}
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
	 * Aggregations correspondantes aux facettes, ou <code>null</code> si aucune facette n'a été demandée.
	 */
	private JsonObject aggregations;



	/**
	 * Entreprise qui va parcourir les tranches de recherche.
	 */
	private final Enterprise<SliceMission> enterprise;



	/**
	 * Le résultat à retourner, ou <code>null</code> si il n'y en a pas.
	 */
	private volatile JsonObject json = null;



	/**
	 * Nombre total de résultats;
	 */
	private final int total;



	/**
	 * La classe {@link SliceMission} contient les informations nécessaires à l'exécution d'une mission par un {@link SliceWorker}.
	 * @author Ludovic WALLE
	 */
	private static class SliceMission implements Mission {



		/**
		 * @param query Requète.
		 * @param output Données à retourner.
		 */
		public SliceMission(String query, String output) {
			this.query = query;
			this.output = output;
		}



		/**
		 * Données à retourner.
		 */
		public final String output;



		/**
		 * Requète.
		 */
		public final String query;



	}



	/**
	 * La classe {@link SliceMissionner} distribue des requètes sur des sous ensembles de documents. Le découpage en sous ensembles se fait sur l'ARK ISTEX, présent dans tous les documents.<br>
	 * La syntaxe générale constatée est:<br>
	 * <code>ark:/67375/[0-9BCDFGHJKLMNPQRSTVWXZ]{3}-[0-9BCDFGHJKLMNPQRSTVWXZ]{8}-[0-9BCDFGHJKLMNPQRSTVWXZ]</code<br>
	 * Plus dans le détail, on constate que le premier groupe de trois caractères après le préfixe "ark:/67375/" semblent réduit à un petit ensemble de valeurs, avec une répartition très inégale en quantité de
	 * documents. La syntaxe pourrait donc être:
	 * <code>ark:/67375/(0T8|1BB|4W2|56L|6GQ|6H6|6ZK|80W|996|C41|GT4|HCB|HXZ|JKT|M70|NVC|P0J|QHD|QT4|TP3|VQC|WNG)-[0-9BCDFGHJKLMNPQRSTVWXZ]{8}-[0-9BCDFGHJKLMNPQRSTVWXZ]</code><br>
	 * Mais l'ensemble des valeurs n'est peut-être pas exhaustif. Dans tous les cas, ces trois premiers caractères ne sont pas une bonne clé de découpage. Par contre, les autres caractères semblent
	 * avoir une fréquence uniforme et peuvent être utilisés.<br>
	 * Le découpage se fait en fixant deux de ces caractères de l'ARK. Il y a 30 possibilités pour ces caractères ([0-9BCDFGHJKLMNPQRSTVWXZ]), ce qui fait 30x30 = 900 tranches de taille
	 * raisonnable.<br>
	 * Un découpage en fixant un seul caractère donne 30 tranches de taille importante. La probabilité d'avoir une erreur est donc importante aussi, ainsi que le temps nécessaire pour une nouvelle
	 * tentative, sans compter qu'il en faudra peut-être plusieurs.<br>
	 * Un découpage en fixant plus de deux caractères donne trop de tranches (30x30x30 = 27000 pour 3 caractères). Le temps de traitement d'autant de requètes devient trop important.
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
		private final String output;



		/**
		 * Tranches de requètes à traiter.
		 */
		private final Vector<String> slicesQueries = new Vector<>();



	}



	/**
	 * La classe {@link SliceWorker} implémente le parcours d'une tranche de recherche, en recommençant autant de fois que nécessaire.
	 * @author Ludovic WALLE
	 */
	private class SliceWorker extends Worker<SliceMission> {



		/**	 */
		private SliceWorker() {
			setDaemon(true);
		}



		/**
		 * @param other Autre collecteur
		 */
		private SliceWorker(SliceWorker other) {
			super(other);
			setDaemon(true);
		}



		/**
		 * {@inheritDoc} La mission est une valeur du champ <code>q</code> d'une requète ISTEX.
		 */
		@Override protected int delegateDo(SliceMission mission) throws Throwable {
			Set<String> ids = new HashSet<>();
			String id;
			IstexIterator1 istexIterator;
			@SuppressWarnings("hiding") JsonObject json;

			System.out.println(mission.query);
			for (;;) {
				try {
					for (istexIterator = new IstexIterator1(mission.query, mission.output, null); istexIterator.hasNext();) {
						json = istexIterator.next();
						id = json.getString("id");
						if (!ids.contains(id)) {
							setJson(json);
							ids.add(id);
						}
					}
					System.out.println(mission.query + " => " + ids.size());
					return ids.size();
				} catch (Throwable exception) {
					System.err.println(mission.query);
					exception.printStackTrace();
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
