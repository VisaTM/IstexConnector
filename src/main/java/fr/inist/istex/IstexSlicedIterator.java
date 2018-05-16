package fr.inist.istex;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.logging.log4j.*;

import toolbox.json.*;
import toolbox.json.JsonObject.*;
import toolbox.parallel.*;



/**
 * La classe {@link IstexSlicedIterator} impl�mente un it�rateur sur une recherche ISTEX en l'appliquant avec un certain parall�lisme � des sous ensembles exclusifs de documents ISTEX (tranches), et
 * en traitant les erreurs.<br>
 * En cas d'erreur sur une tranche, la recherche sera relanc�e sur celle-ci, en ignorant les r�sultats d�j� r�cup�r�s. Les r�sultats unifi�s de toutes les tranches sont r�cup�rables par
 * {@link IstexSlicedIterator#next()}. Une recherche pr�liminaire permet d'obtenir le nombre total de r�sulats attendus, et les �ventuelles agr�gations correspondant aux facettes demand�es.<br>
 * Cette fa�on de faire peut aboutir � des incoh�rences li�es � la multiplicit� des recherches et � leur ex�cution d�cal�es dans le temps si le contenu d'ISTEX �volue pendant le traitement, mais c'est
 * tr�s peu probable.
 * @author Ludovic WALLE
 */
public class IstexSlicedIterator extends IstexIterator {



	/**
	 * @param query Requ�te, ne doit �tre ni vide ni ni <code>null</code>. Voir {@link "https://api.istex.fr/documentation/search/"}.
	 * @param output Donn�es � retourner, ignor� si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/results/#selection-des-champs-renvoyes"}.
	 * @param facets Facettes � retourner, ignor� si <code>null</code>. Voir {@link "https://api.istex.fr/documentation/facets/"}.
	 * @param count Nombre d'ex�cutions parall�les de tranches de requ�tes.
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
					throw new IstexException(LOGGER, Level.WARN, "Pour la requ�te initiale de \"" + query + "\", il y a des r�ponses: " + json.toString());
				}
				if (json.isNotEmpty()) {
					throw new IstexException(LOGGER, Level.WARN, "Pour la requ�te initiale de \"" + query + "\", des �l�ments de la r�ponse ISTEX ne sont pas pris en compte: " + json.toString());
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
				// A la fin de l'entreprise, il y a un petit moment o� tout est termin� mais o� l'entreprise n'a pas encore ferm�. Il n'y aura donc plus d'appel � setJson, donc pas de notifyAll pour
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
			throw new IstexException(LOGGER, Level.ERROR, "Pour la requ�te \"" + query + "\", le nombre de documents retourn�s (" + count + ") est diff�rent de celui attendu (" + total + ").");
		}
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public synchronized JsonObject next() {
		@SuppressWarnings("hiding") JsonObject json;

		while ((this.json == null) && !enterprise.hasClosedDown()) {
			try {
				// A la fin de l'entreprise, il y a un petit moment o� tout est termin� mais o� l'entreprise n'a pas encore ferm�. Il n'y aura donc plus d'appel � setJson, donc pas de notifyAll pour
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
	 * Sp�cifie le prochain r�sultat � retourner.
	 * @param json Le prochain r�sultat � retourner.
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
	 * Entreprise qui va g�rer les recherches sur les tranches.
	 */
	private final Enterprise<SliceMission> enterprise;



	/**
	 * Le prochain r�sultat � retourner, ou <code>null</code> si il n'y en a pas.
	 */
	private volatile JsonObject json = null;



	/**
	 * La classe {@link SliceMission} contient les informations n�cessaires � l'ex�cution d'une recherche sur une tranche par un {@link SliceWorker}.
	 * @author Ludovic WALLE
	 */
	private static class SliceMission implements Mission {



		/**
		 * @param sliceQuery Requ�te.
		 * @param output Donn�es � retourner.
		 */
		public SliceMission(String sliceQuery, String output) {
			this.sliceQuery = sliceQuery;
			this.output = output;
		}



		/**
		 * Donn�es � retourner.
		 */
		public final String output;



		/**
		 * Requ�te.
		 */
		public final String sliceQuery;



	}



	/**
	 * La classe {@link SliceMissionner} distribue des requ�tes sur des sous ensembles de documents. Le d�coupage en sous ensembles se fait sur l'ARK ISTEX, pr�sent dans tous les documents.<br>
	 * Seule la fin de l'ARK (<code>[0-9BCDFGHJKLMNPQRSTVWXZ]{8}-[0-9BCDFGHJKLMNPQRSTVWXZ]</code>, voir {@link Istex.Hit#arkIstex}) est utilisable comme cl� de d�coupage car les caract�res semblent
	 * avoir une fr�quence uniforme.<br>
	 * Le d�coupage se fait en fixant deux de ces caract�res. Il y a 30 possibilit�s pour ces caract�res ([0-9BCDFGHJKLMNPQRSTVWXZ]), ce qui fait 30x30 = 900 tranches de taille raisonnable.<br>
	 * Un d�coupage en fixant un seul caract�re donne 30 tranches de taille importante. La probabilit� d'avoir une erreur est donc importante aussi, ainsi que le temps n�cessaire pour une nouvelle
	 * tentative, sans compter qu'il en faudra peut-�tre plusieurs.<br>
	 * Un d�coupage en fixant plus de deux caract�res donne beaucoup de tranches (30x30x30 = 27000 pour 3 caract�res). Le temps de traitement de tant de requ�tes devient trop important.
	 * @author Ludovic WALLE
	 */
	private static class SliceMissionner extends Missionner<SliceMission> {



		/**
		 * @param query Requ�te.
		 * @param output Donn�es � retourner.
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
		 * Donn�es � retourner.
		 */
		public final String output;



		/**
		 * Tranches de requ�tes � traiter.
		 */
		public final Vector<String> slicesQueries = new Vector<>();



	}



	/**
	 * La classe {@link SliceWorker} impl�mente la r�cup�ration des r�sultats d'une tranche de recherche, en recommen�ant autant de fois que n�cessaire, et sans renvoyer plusieurs fois le m�me
	 * r�sultat.
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
		 * {@inheritDoc} La mission est une valeur du champ <code>q</code> d'une requ�te ISTEX.
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
