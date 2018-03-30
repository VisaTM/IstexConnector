package fr.inist.toolbox.parallel;

import java.util.*;

import toolbox.parallel.*;



/**
 * La classe {@link Enterprise} permet de parall�liser l'application d'un m�me traitement � des objets diff�rents.<br>
 * Elle cr�e le nombre de threads indiqu� (ouvriers), plus un qui distribuera les missions (distributeur), et les ex�cute.<br>
 * La concurrence �ventuelle entre des traitements ex�cut�s simultan�ment doit �tre g�r�e � l'int�rieur de ces traitements, et leur ordonancement ne doit pas avoir d'importance.<br>
 * Le traitement se lance par {@link #start()} pour une ex�cution asynchrone, ou par {@link #run()} pour une ex�cution synchrone. Il se termine lorsque tous les objets on �t� trait�s ou par un appel �
 * {@link #forbidForeverNewMissionsStart()}. Il peut �tre suspendu par un appel � {@link #postponeNewMissionsStart()} et repris par un appel � {@link #allowNewMissionsStart()}. Le nombre de threads
 * peut �tre ajust� dynamiquement en cours de traitement par un appel � {@link #setWishedWorkersCount(int)}.<br>
 * L'entreprise s'arr�te d�finitivement d�s qu'une exception est collect�e.
 * @author Ludovic WALLE
 * @param <M> Type des missions.
 **/
public class Enterprise<M extends Mission> extends Thread {



	/**
	 * @param wishedWorkerCount Nombre d'ouvriers souhait�s (doit �tre positif ou nul).
	 * @param missionner Distributeur de missions (ne doit pas �tre <code>null</code>).
	 * @param stemWorker Ouvrier (ne doit pas �tre <code>null</code>).
	 */
	public Enterprise(int wishedWorkerCount, Missionner<M> missionner, Worker<M> stemWorker) {
		this("Enterprise", wishedWorkerCount, missionner, stemWorker);
	}



	/**
	 * @param name Nom de l'entreprise.
	 * @param wishedWorkerCount Nombre d'ouvriers souhait�s (doit �tre positif ou nul).
	 * @param missionner Distributeur de missions (ne doit pas �tre <code>null</code>).
	 * @param stemWorker Ouvrier (ne doit pas �tre <code>null</code>).
	 */
	public Enterprise(String name, int wishedWorkerCount, Missionner<M> missionner, Worker<M> stemWorker) {
		super(name);
		if (wishedWorkerCount <= 0) {
			throw new IllegalArgumentException("Le nombre d'ouvriers souhait� doit �tre strictement positif: " + wishedWorkerCount);
		}
		if ((missionner == null) || (stemWorker == null)) {
			throw new NullPointerException();
		}
		missionner.hiredBy(this);
		stemWorker.hiredBy(this);
		this.wishedWorkerCount = wishedWorkerCount;
		this.missionner = missionner;
		this.stemWorker = stemWorker;
	}



	/**
	 * Autorise le d�marrage de nouvelles missions.
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
	 * Signale que la mission indiqu�e est termin�e avec le nombre de r�sultats indiqu�.<br>
	 * Cette m�thode sera appel�e par les ouvriers � chaque fois qu'ils ont fini une mission.
	 * @param mission Mission.
	 * @param count Nombre de r�sultats.
	 */
	protected final void collectDone(M mission, int count) {
		if (count < 0) {
			throw new IllegalArgumentException("Le nombre de r�sultats d'une mission doit �tre positif ou nul: " + count);
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
	 * Enregistre les exceptions indiqu�es.
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
	 * Signale que l'ouvrier indiqu� a fini de travailler.<br>
	 * Cette m�thode sera appel�e par les ouvriers quand ils finissent de travailler.
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
	 * Signale qu'un ouvrier a commenc� � travailler.<br>
	 * Cette m�thode sera appel�e par les ouvriers quand ils commencent � travailler.
	 */
	protected final void collectStarted() {
		synchronized (enterpriseLock) {
			enterpriseLock.notifyAll();
		}
	}



	/**
	 * Interdit d�finitivement le d�marrage de nouvelle mission.
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
	 * Retourne le nombre d'ouvriers actifs (non licenci�s).<br>
	 * Cette m�thode est non bloquante.
	 * @return Le nombre d'ouvriers actifs (non licenci�s).
	 */
	public final int getActiveWorkerCount() {
		return activeWorkers.size();
	}



	/**
	 * Retourne le nombre d'ouvriers licenci�s finissant leur derni�re mission.<br>
	 * Cette m�thode est non bloquante.
	 * @return Le nombre d'ouvriers licenci�s finissant leur derni�re mission.
	 */
	public final int getDismissedWorkerCount() {
		return dismissedWorkers.size();
	}



	/**
	 * Retourne le nombre de missions termin�es.<br>
	 * Cette m�thode est non bloquante.
	 * @return Le nombre de missions termin�es.
	 */
	public final int getDoneCount() {
		return doneCount;
	}



	/**
	 * Retourne le temps de traitement �coul� en millisecondes.<br>
	 * Le temps de traitement n'est que le temps pendant lequel le d�marrage de nouvelles missions a �t� autoris�. Il ne tient pas compte des fins de missions effectu�es en dehors de ces p�riodes.
	 * Cette m�thode est non bloquante.
	 * @return Le temps de traitement �coul� en millisecondes.
	 */
	public final long getElapsedTime() {
		return (startTime == -1) ? 0 : previouslyElapsedTime + ((newMissions == NewMissions.ALLOWED) ? System.currentTimeMillis() - startTime : 0);
	}



	/**
	 * Retourne les exceptions rencontr�es par l'entreprise.<br>
	 * Si aucune exception n'a �t� rencontr�e, la m�thode retourne un tableau vide, jamais <code>null</code>.<br>
	 * Cette m�thode est non bloquante.
	 * @return Retourne les exceptions rencontr�es par l'entreprise.
	 */
	public final Throwable[] getExceptions() {
		synchronized (exceptions) {
			return exceptions.toArray(new Throwable[exceptions.size()]);
		}
	}



	/**
	 * Retourne le nombre attendu de r�sultats (positif ou nul), ou {@link Missionner#NOT_COMPUTABLE} si il n'est pas calculable, ou {@link Missionner#NOT_AVAILABLE} si le calcul est en cours, ou
	 * {@link Missionner#NOT_COMPUTED} si le calcul n'a pas �t� lanc� (ni {@link #start()}, ni {@link #run()}, ni {@link Missionner#start()}, ni {@link Missionner#run()} n'ont �t� appel�es au
	 * pr�alable).
	 * @param waitUntilComputed Indique si cette m�thode attend que le r�sultat soit calcul� ou non. Si <code>true</code>, la valeur retourn�e ne pourra �tre que le nombre attendu de r�sultats
	 *            (positif ou nul) ou {@link Missionner#NOT_COMPUTABLE}.
	 * @return Le nombre attendu de r�sultats.
	 */
	public final int getExpectedCount(boolean waitUntilComputed) {
		return missionner.getExpectedCount(waitUntilComputed);
	}



	/**
	 * Retourne la premi�re exception rencontr�e par l'entreprise, ou <code>null</code> si il n'y en a pas.<br>
	 * Cette m�thode est non bloquante.
	 * @return La premi�re exception rencontr�e par l'entreprise, ou <code>null</code> si il n'y en a pas.
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
	 * Retourne le derni�re mission termin�e.<br>
	 * Cette m�thode est non bloquante.
	 * @return Le derni�re mission termin�e.
	 */
	public final M getLastDone() {
		return lastDone;
	}



	/**
	 * Retourne la mission suivante, ou <code>null</code> si il n'y en a plus.<br>
	 * Cette m�thode sera appel�e par les ouvriers.<br>
	 * Cette m�thode est bloquante, et attend qu'une mission soit disponible, ou qu'il n'y en ait plus � distribuer.
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
	 * Retourne le nombre de r�sultats des missions termin�es.<br>
	 * Cette m�thode est non bloquante.
	 * @return Le nombre de r�sultats des missions termin�es.
	 */
	public final int getProducedCount() {
		return producedCount;
	}



	/**
	 * Retourne le nombre d'ouvriers (actifs ou licenci�s).<br>
	 * Cette m�thode est non bloquante.
	 * @return Le nombre d'ouvriers (actifs ou licenci�s).
	 */
	public final int getWorkerCount() {
		synchronized (workersLock) {
			return activeWorkers.size() + dismissedWorkers.size();
		}
	}



	/**
	 * Teste si l'entreprise a ferm�.<br>
	 * L'entreprise ferme si toutes les missions ont �t� effectu�es, ou si il y eu une exception ou une interruption explicite par {@link Enterprise#forbidForeverNewMissionsStart()}.
	 * @return <code>true</code> si l'entreprise a ferm�, <code>false</code> sinon.
	 */
	public final boolean hasClosedDown() {
		return closedDown;
	}



	/**
	 * Teste si l'entreprise a rencontr� des exceptions.
	 * @return <code>true</code> si l'entreprise a rencontr� des exceptions, <code>false</code> sinon.
	 */
	public final boolean hasExceptions() {
		return !exceptions.isEmpty();
	}



	/**
	 * Interdit temporairement le d�marrage de nouvelle mission.
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
		} catch (Throwable exception) {
			collectExceptions(exception);
		}
	}



	/**
	 * Ajuste le nombre d'ouvriers souhait�.
	 * @param wishedWorkerCount Nombre d'ouvriers souhait� (doit �tre positif ou nul).
	 */
	public final void setWishedWorkersCount(int wishedWorkerCount) {
		if (wishedWorkerCount <= 0) {
			throw new IllegalArgumentException("Le nombre d'ouvriers souhait� doit �tre strictement positif: " + wishedWorkerCount);
		}
		this.wishedWorkerCount = wishedWorkerCount;
	}



	/**
	 * Ouvriers non licenci�s.
	 */
	private final Vector<Worker<M>> activeWorkers = new Vector<>();



	/**
	 * Indique si l'entreprise a ferm�.
	 */
	private boolean closedDown = false;



	/**
	 * Ouvriers licenci�s finissant leur derni�re mission.
	 */
	private final Vector<Worker<M>> dismissedWorkers = new Vector<>();



	/**
	 * Nombre de missions termin�es.
	 */
	private volatile int doneCount = 0;



	/**
	 * Verrou pour les missions termin�es.
	 */
	private final Object doneLock = "doneLock";



	/**
	 * Verrou pour les employ�s.
	 */
	private final Object enterpriseLock = "enterpriseLock";



	/**
	 * Exceptions rencontr�es.
	 */
	private final Vector<Throwable> exceptions = new Vector<>();



	/**
	 * Derni�re mission termin�e.
	 */
	private volatile M lastDone = null;



	/**
	 * Distributeur de missions.
	 */
	private final Missionner<M> missionner;



	/**
	 * Autorisation de d�marrage de nouvelles missions.
	 */
	private volatile NewMissions newMissions = NewMissions.POSTPONNED;



	/**
	 * Verrou pour l'autorisation de d�marrage de nouvelles missions.
	 */
	private final Object newMissionsLock = "newMissionsLock";



	/**
	 * Temps �coul� avant le dernier arret temporaire ou d�finitif, en millisecondes.
	 */
	private volatile long previouslyElapsedTime = 0;



	/**
	 * Nombre de r�sulats de missions.
	 */
	private volatile int producedCount = 0;



	/**
	 * Date de derni�re autorisation de commencer de nouvelles missions, ou -1 si elles ne sont pas autoris�es.
	 */
	private volatile long startTime = -1;



	/**
	 * Ouvrier souche (� partir duquel on cr�e des clones).
	 */
	private final Worker<M> stemWorker;



	/**
	 * Nombre d'ouvriers souhait�.
	 */
	private volatile int wishedWorkerCount;



	/**
	 * Verrou pour les ouvriers actifs et licenci�s.
	 */
	private final Object workersLock = "workersLock";



	/**
	 * La classe {@link NewMissions} recense les �tats possibles pour le d�marrage de nouvelles taches.
	 * @author Ludovic WALLE
	 */
	private enum NewMissions {
	    /** Les nouvelles missions peuvent d�marrer. */
		ALLOWED,
		/** Les nouvelles missions sont interdites, d�finitivement. */
		FORBIDDEN,
		/** Les nouvelles missions sont report�es, temporairement. */
		POSTPONNED
	}


}
