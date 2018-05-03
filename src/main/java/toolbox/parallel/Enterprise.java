package toolbox.parallel;

import java.util.*;



/**
 * La classe {@link Enterprise} permet de paralléliser l'application d'un même traitement à des objets différents.<br>
 * Elle crée le nombre de threads indiqué (ouvriers), plus un qui distribuera les missions (distributeur), et les exécute.<br>
 * La concurrence éventuelle entre des traitements exécutés simultanément doit être gérée à l'intérieur de ces traitements, et leur ordonancement ne doit pas avoir d'importance.<br>
 * Le traitement se lance par {@link #start()} pour une exécution asynchrone, ou par {@link #run()} pour une exécution synchrone. Il se termine lorsque tous les objets on été traités ou par un appel à
 * {@link #forbidForeverNewMissionsStart()}. Il peut être suspendu par un appel à {@link #postponeNewMissionsStart()} et repris par un appel à {@link #allowNewMissionsStart()}. Le nombre de threads
 * peut être ajusté dynamiquement en cours de traitement par un appel à {@link #setWishedWorkersCount(int)}.<br>
 * L'entreprise s'arrète définitivement dès qu'une exception est collectée.
 * @author Ludovic WALLE
 * @param <M> Type des missions.
 **/
public class Enterprise<M extends Mission> extends Thread {



	/**
	 * @param wishedWorkerCount Nombre d'ouvriers souhaités (doit être positif ou nul).
	 * @param missionner Distributeur de missions (ne doit pas être <code>null</code>).
	 * @param stemWorker Ouvrier (ne doit pas être <code>null</code>).
	 */
	public Enterprise(int wishedWorkerCount, Missionner<M> missionner, Worker<M> stemWorker) {
		this("Enterprise", wishedWorkerCount, missionner, stemWorker);
	}



	/**
	 * @param name Nom de l'entreprise.
	 * @param wishedWorkerCount Nombre d'ouvriers souhaités (doit être positif ou nul).
	 * @param missionner Distributeur de missions (ne doit pas être <code>null</code>).
	 * @param stemWorker Ouvrier (ne doit pas être <code>null</code>).
	 */
	public Enterprise(String name, int wishedWorkerCount, Missionner<M> missionner, Worker<M> stemWorker) {
		super(name);
		if (wishedWorkerCount <= 0) {
			throw new IllegalArgumentException("Le nombre d'ouvriers souhaité doit être strictement positif: " + wishedWorkerCount);
		}
		if ((missionner == null) || (stemWorker == null)) {
			throw new NullPointerException();
		}
		missionner.hiredBy(this);
		stemWorker.hiredBy(this);
		this.wishedWorkerCount = wishedWorkerCount;
		this.missionner = missionner;
		this.stemWorker = stemWorker;
		setDaemon(true);
	}



	/**
	 * Autorise le démarrage de nouvelles missions.
	 */
	public final void allowNewMissionsStart() {
		synchronized (newMissionsLock) {
			switch (newMissions) {
			case POSTPONNED:
				newMissions = NewMissions.ALLOWED;
				startTime = System.currentTimeMillis();
				//$FALL-THROUGH$
			case ALLOWED:
			case FORBIDDEN:
			}
		}
		synchronized (enterpriseLock) {
			enterpriseLock.notifyAll();
		}
	}



	/**
	 * Signale que la mission indiquée est terminée avec le nombre de résultats indiqué.<br>
	 * Cette méthode sera appelée par les ouvriers à chaque fois qu'ils ont fini une mission.
	 * @param mission Mission.
	 * @param count Nombre de résultats.
	 */
	protected final void collectDone(M mission, int count) {
		if (count < 0) {
			throw new IllegalArgumentException("Le nombre de résultats d'une mission doit être positif ou nul: " + count);
		}
		synchronized (doneLock) {
			lastDone = mission;
			doneCount++;
			producedCount += count;
		}
		synchronized (enterpriseLock) {
			enterpriseLock.notifyAll();
		}
	}



	/**
	 * Enregistre les exceptions indiquées.
	 * @param exceptions Exceptions.
	 */
	protected final void collectExceptions(@SuppressWarnings("hiding") Throwable... exceptions) {
		synchronized (exceptions) {
			for (Throwable exception : exceptions) {
				this.exceptions.add(exception);
			}
		}
		synchronized (enterpriseLock) {
			enterpriseLock.notifyAll();
		}
	}



	/**
	 * Signale que l'ouvrier indiqué a fini de travailler.<br>
	 * Cette méthode sera appelée par les ouvriers quand ils finissent de travailler.
	 * @param worker Ouvrier qui a fini de travailler.
	 */
	protected final void collectFinished(Worker<M> worker) {
		synchronized (workersLock) {
			activeWorkers.remove(worker);
			dismissedWorkers.remove(worker);
		}
		synchronized (enterpriseLock) {
			enterpriseLock.notifyAll();
		}
	}



	/**
	 * Signale qu'un ouvrier a commencé à travailler.<br>
	 * Cette méthode sera appelée par les ouvriers quand ils commencent à travailler.
	 */
	protected final void collectStarted() {
		synchronized (enterpriseLock) {
			enterpriseLock.notifyAll();
		}
	}



	/**
	 * Interdit définitivement le démarrage de nouvelle mission.
	 */
	public final void forbidForeverNewMissionsStart() {
		synchronized (newMissionsLock) {
			switch (newMissions) {
			case ALLOWED:
				previouslyElapsedTime += System.currentTimeMillis() - startTime;
				//$FALL-THROUGH$
			case POSTPONNED:
				missionner.stopDispensing();
				newMissions = NewMissions.FORBIDDEN;
				//$FALL-THROUGH$
			case FORBIDDEN:
			}
		}
	}



	/**
	 * Retourne le nombre d'ouvriers actifs (non licenciés).<br>
	 * Cette méthode est non bloquante.
	 * @return Le nombre d'ouvriers actifs (non licenciés).
	 */
	public final int getActiveWorkerCount() {
		return activeWorkers.size();
	}



	/**
	 * Retourne le nombre d'ouvriers licenciés finissant leur dernière mission.<br>
	 * Cette méthode est non bloquante.
	 * @return Le nombre d'ouvriers licenciés finissant leur dernière mission.
	 */
	public final int getDismissedWorkerCount() {
		return dismissedWorkers.size();
	}



	/**
	 * Retourne le nombre de missions terminées.<br>
	 * Cette méthode est non bloquante.
	 * @return Le nombre de missions terminées.
	 */
	public final int getDoneCount() {
		return doneCount;
	}



	/**
	 * Retourne le temps de traitement écoulé en millisecondes.<br>
	 * Le temps de traitement n'est que le temps pendant lequel le démarrage de nouvelles missions a été autorisé. Il ne tient pas compte des fins de missions effectuées en dehors de ces périodes.
	 * Cette méthode est non bloquante.
	 * @return Le temps de traitement écoulé en millisecondes.
	 */
	public final long getElapsedTime() {
		return (startTime == -1) ? 0 : previouslyElapsedTime + ((newMissions == NewMissions.ALLOWED) ? System.currentTimeMillis() - startTime : 0);
	}



	/**
	 * Retourne les exceptions rencontrées par l'entreprise.<br>
	 * Si aucune exception n'a été rencontrée, la méthode retourne un tableau vide, jamais <code>null</code>.<br>
	 * Cette méthode est non bloquante.
	 * @return Retourne les exceptions rencontrées par l'entreprise.
	 */
	public final Throwable[] getExceptions() {
		synchronized (exceptions) {
			return exceptions.toArray(new Throwable[exceptions.size()]);
		}
	}



	/**
	 * Retourne le nombre attendu de résultats (positif ou nul), ou {@link Missionner#NOT_COMPUTABLE} si il n'est pas calculable, ou {@link Missionner#NOT_AVAILABLE} si le calcul est en cours, ou
	 * {@link Missionner#NOT_COMPUTED} si le calcul n'a pas été lancé (ni {@link #start()}, ni {@link #run()}, ni {@link Missionner#start()}, ni {@link Missionner#run()} n'ont été appelées au
	 * préalable).
	 * @param waitUntilComputed Indique si cette méthode attend que le résultat soit calculé ou non. Si <code>true</code>, la valeur retournée ne pourra être que le nombre attendu de résultats
	 *            (positif ou nul) ou {@link Missionner#NOT_COMPUTABLE}.
	 * @return Le nombre attendu de résultats.
	 */
	public final int getExpectedCount(boolean waitUntilComputed) {
		return missionner.getExpectedCount(waitUntilComputed);
	}



	/**
	 * Retourne la première exception rencontrée par l'entreprise, ou <code>null</code> si il n'y en a pas.<br>
	 * Cette méthode est non bloquante.
	 * @return La première exception rencontrée par l'entreprise, ou <code>null</code> si il n'y en a pas.
	 */
	public final Throwable getFirstException() {
		synchronized (exceptions) {
			if (exceptions.isEmpty()) {
				return null;
			} else {
				return exceptions.firstElement();
			}
		}
	}



	/**
	 * Retourne le dernière mission terminée.<br>
	 * Cette méthode est non bloquante.
	 * @return Le dernière mission terminée.
	 */
	public final M getLastDone() {
		return lastDone;
	}



	/**
	 * Retourne la mission suivante, ou <code>null</code> si il n'y en a plus.<br>
	 * Cette méthode sera appelée par les ouvriers.<br>
	 * Cette méthode est bloquante, et attend qu'une mission soit disponible, ou qu'il n'y en ait plus à distribuer.
	 * @return La mission suivante, ou <code>null</code> si il n'y en a plus.
	 */
	public final M getNext() {
		M next = null;

		synchronized (newMissionsLock) {
			while (newMissions == NewMissions.POSTPONNED) {
				try {
					newMissionsLock.wait();
				} catch (InterruptedException exception) {
					collectExceptions(exception);
				}
			}
		}
		if (((next = missionner.getNext()) != null) && (startTime == -1)) {
			startTime = System.currentTimeMillis();
		}
		return next;
	}



	/**
	 * Retourne le nombre de résultats des missions terminées.<br>
	 * Cette méthode est non bloquante.
	 * @return Le nombre de résultats des missions terminées.
	 */
	public final int getProducedCount() {
		return producedCount;
	}



	/**
	 * Retourne le nombre d'ouvriers (actifs ou licenciés).<br>
	 * Cette méthode est non bloquante.
	 * @return Le nombre d'ouvriers (actifs ou licenciés).
	 */
	public final int getWorkerCount() {
		synchronized (workersLock) {
			return activeWorkers.size() + dismissedWorkers.size();
		}
	}



	/**
	 * Teste si l'entreprise a fermé.<br>
	 * L'entreprise ferme si toutes les missions ont été effectuées, ou si il y eu une exception ou une interruption explicite par {@link Enterprise#forbidForeverNewMissionsStart()}.<br>
	 * Il est possible que l'entreprise ne soit pas encore déclarée comme fermée bien que toutes les missions aient été effectuées.
	 * @return <code>true</code> si l'entreprise a fermé, <code>false</code> sinon.
	 */
	public final boolean hasClosedDown() {
		return closedDown;
	}



	/**
	 * Teste si l'entreprise a rencontré des exceptions.
	 * @return <code>true</code> si l'entreprise a rencontré des exceptions, <code>false</code> sinon.
	 */
	public final boolean hasExceptions() {
		return !exceptions.isEmpty();
	}



	/**
	 * Interdit temporairement le démarrage de nouvelle mission.
	 */
	public final void postponeNewMissionsStart() {
		synchronized (newMissionsLock) {
			switch (newMissions) {
			case ALLOWED:
				previouslyElapsedTime += System.currentTimeMillis() - startTime;
				newMissions = NewMissions.POSTPONNED;
				//$FALL-THROUGH$
			case POSTPONNED:
			case FORBIDDEN:
			}
		}
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public void run() {
		Worker<M> worker;
		Vector<Worker<M>> remainingWorkers;
		boolean wait;

		try {
			missionner.start();
			allowNewMissionsStart();
			while ((newMissions != NewMissions.FORBIDDEN) && missionner.hasNext() && !hasExceptions()) {
				synchronized (workersLock) {
					wait = false;
					if (wishedWorkerCount > (activeWorkers.size() + dismissedWorkers.size())) {
						worker = stemWorker.newOne();
						activeWorkers.add(worker);
						worker.start();
					} else if (wishedWorkerCount < activeWorkers.size()) {
						worker = activeWorkers.lastElement();
						activeWorkers.remove(worker);
						dismissedWorkers.add(worker);
						worker.dismiss();
					} else {
						wait = true;
					}
				}
				if (wait) {
					try {
						synchronized (enterpriseLock) {
							enterpriseLock.wait();
						}
					} catch (Exception exception) {
						collectExceptions(exception);
					}
				}
			}
			forbidForeverNewMissionsStart();
			remainingWorkers = new Vector<>();
			synchronized (workersLock) {
				remainingWorkers.addAll(activeWorkers);
				remainingWorkers.addAll(dismissedWorkers);
			}
			if (hasExceptions()) {
				for (Worker<M> remainingWorker : remainingWorkers) {
					remainingWorker.interrupt();
				}
				missionner.interrupt();
			}
			for (Worker<M> remainingWorker : remainingWorkers) {
				remainingWorker.join();
			}
			closedDown = true;
			synchronized (enterpriseLock) {
				enterpriseLock.notifyAll();
			}
		} catch (Throwable exception) {
			collectExceptions(exception);
		}
	}



	/**
	 * Ajuste le nombre d'ouvriers souhaité.
	 * @param wishedWorkerCount Nombre d'ouvriers souhaité (doit être positif ou nul).
	 */
	public final void setWishedWorkersCount(int wishedWorkerCount) {
		if (wishedWorkerCount <= 0) {
			throw new IllegalArgumentException("Le nombre d'ouvriers souhaité doit être strictement positif: " + wishedWorkerCount);
		}
		this.wishedWorkerCount = wishedWorkerCount;
	}



	/**
	 * Ouvriers non licenciés.
	 */
	private final Vector<Worker<M>> activeWorkers = new Vector<>();



	/**
	 * Indique si l'entreprise a fermé.
	 */
	private boolean closedDown = false;



	/**
	 * Ouvriers licenciés finissant leur dernière mission.
	 */
	private final Vector<Worker<M>> dismissedWorkers = new Vector<>();



	/**
	 * Nombre de missions terminées.
	 */
	private volatile int doneCount = 0;



	/**
	 * Verrou pour les missions terminées.
	 */
	private final Object doneLock = "doneLock";



	/**
	 * Verrou pour les employés.
	 */
	private final Object enterpriseLock = "enterpriseLock";



	/**
	 * Exceptions rencontrées.
	 */
	private final Vector<Throwable> exceptions = new Vector<>();



	/**
	 * Dernière mission terminée.
	 */
	private volatile M lastDone = null;



	/**
	 * Distributeur de missions.
	 */
	private final Missionner<M> missionner;



	/**
	 * Autorisation de démarrage de nouvelles missions.
	 */
	private volatile NewMissions newMissions = NewMissions.POSTPONNED;



	/**
	 * Verrou pour l'autorisation de démarrage de nouvelles missions.
	 */
	private final Object newMissionsLock = "newMissionsLock";



	/**
	 * Temps écoulé avant le dernier arret temporaire ou définitif, en millisecondes.
	 */
	private volatile long previouslyElapsedTime = 0;



	/**
	 * Nombre de résulats de missions.
	 */
	private volatile int producedCount = 0;



	/**
	 * Date de dernière autorisation de commencer de nouvelles missions, ou -1 si elles ne sont pas autorisées.
	 */
	private volatile long startTime = -1;



	/**
	 * Ouvrier souche (à partir duquel on crée des clones).
	 */
	private final Worker<M> stemWorker;



	/**
	 * Nombre d'ouvriers souhaité.
	 */
	private volatile int wishedWorkerCount;



	/**
	 * Verrou pour les ouvriers actifs et licenciés.
	 */
	private final Object workersLock = "workersLock";



	/**
	 * La classe {@link NewMissions} recense les états possibles pour le démarrage de nouvelles taches.
	 * @author Ludovic WALLE
	 */
	private enum NewMissions {
	    /** Les nouvelles missions peuvent démarrer. */
		ALLOWED,
		/** Les nouvelles missions sont interdites, définitivement. */
		FORBIDDEN,
		/** Les nouvelles missions sont reportées, temporairement. */
		POSTPONNED
	}


}
