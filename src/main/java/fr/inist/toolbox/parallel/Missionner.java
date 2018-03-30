package fr.inist.toolbox.parallel;

import toolbox.parallel.*;

/**
 * La classe {@link Missionner} est la classe anc�tre de tous les distributeurs de missions.<br>
 * Les distributeurs fonctionnent de fa�on asynchrone, dans un {@link Thread} s�par�. Ils pr�parent une nouvelle mission d�s que la pr�c�dente a �t� attribu�e, au lieu d'attendre qu'on leur en demande
 * une pour le faire.<br>
 * Un distributeur peut �tre utilis� comme it�rateur.<br>
 * <br>
 * Les m�thodes appel�es par la m�thode {@link #run()} sont, dans l'ordre:
 * <ul>
 * <li>{@link #delegateInitialize()}, une seule fois,
 * <li>{@link #delegateGetNext()}, plusieurs fois,
 * <li>{@link #delegateFinalize()}, une seule fois.
 * </ul>
 * Aucune de ces m�thodes n'est appel�e de fa�on concurrente, et n'a besoin d'�tre synchronis�e.<br>
 * Il est pr�f�rable de faire les initialisations lentes dans la m�thode {@link #delegateInitialize()}, ex�cut�e de fa�on asynchrone, plut�t que dans le constructeur, ex�cut� de fa�on synchrone.
 * @author Ludovic WALLE
 * @param <M> Missions.
 */
public abstract class Missionner<M extends Mission> extends Employee<M> {



	/**	 */
	protected Missionner() {
		this("Missionner");
	}



	/**
	 * @param name Nom du distributeur de missions.
	 */
	protected Missionner(String name) {
		super(name);
		setDaemon(true);
	}



	/**
	 * Retourne le nombre de r�sultats attendus pour l'ensemble des missions (positif ou nul), ou {@link #NOT_COMPUTABLE} si il n'est pas calculable.<br>
	 * Cette m�thode est destin�e � �tre surcharg�e lorsque le nombre de r�sultats attendus est calculable. Elle sera appel�e une seule fois par le {@link Missionner}, apr�s
	 * {@link #delegateInitialize()}. Elle doit pouvoir �tre ex�cut�e en parall�le avec la m�thode {@link #delegateGetNext()}.<br>
	 * Par d�faut, cette m�thode retourne {@link Missionner#NOT_COMPUTABLE}.<br>
	 * @return Le nombre de r�sultats attendus pour l'ensemble des missions (positif ou nul), ou {@link #NOT_COMPUTABLE} si il n'est pas calculable.
	 * @throws Throwable Pour que la m�thode puisse g�n�rer des exceptions.<br>
	 *             L'entreprise sera interrompue si cette m�thode g�n�re une exception.
	 */
	@SuppressWarnings("static-method") protected int delegateComputeExpectedCount() throws Throwable {
		return Missionner.NOT_COMPUTABLE;
	}



	/**
	 * Retourne la prochaine mission, ou <code>null</code> lorsqu'il n'y en a plus.
	 * @return La prochaine mission, ou <code>null</code> lorsqu'il n'y en a plus.
	 * @throws Throwable Pour que la m�thode puisse g�n�rer des exceptions.<br>
	 *             L'entreprise sera interrompue si cette m�thode g�n�re une exception.
	 */
	protected abstract M delegateGetNext() throws Throwable;



	/**
	 * Retourne le nombre attendu de r�sultats (positif ou nul), ou {@link #NOT_COMPUTABLE} si il n'est pas calculable, ou {@link #NOT_AVAILABLE} si le calcul est en cours, ou {@link #NOT_COMPUTED} si
	 * ni {@link #start()} ni {@link #run()} n'ont �t� appel�es au pr�alable (le calcul n'a pas �t� lanc�).
	 * @param waitUntilComputed Indique si cette m�thode attend que le r�sultat soit calcul� ou non. Si <code>true</code>, la valeur retourn�e ne pourra �tre que le nombre attendu de r�sultats
	 *            (positif ou nul) ou {@link #NOT_COMPUTABLE}.
	 * @return Le nombre attendu de r�sultats.
	 */
	public final int getExpectedCount(boolean waitUntilComputed) {
		if (waitUntilComputed) {
			synchronized (expectedCountLock) {
				while (((expectedCount == NOT_AVAILABLE) || (expectedCount == NOT_COMPUTED)) && !hasExceptions()) {
					try {
						expectedCountLock.wait();
					} catch (InterruptedException exception) {
						reportExceptions(exception);
					}
				}
			}
		}
		return expectedCount;
	}



	/**
	 * Distribue la mission suivante.<br>
	 * Cette m�thode est bloquante tant qu'il n'y a pas de mission disponible et qu'on ne sait pas si il n'y en a plus � distribuer.
	 * @return La mission suivante, ou <code>null</code> si il n'y en a plus.
	 */
	public final M getNext() {
		@SuppressWarnings("hiding") M next = null;

		synchronized (nextLock) {
			waitForNext();
			if (!hasExceptions()) {
				next = this.next;
				this.next = null;
			}
			nextLock.notifyAll();
			return next;
		}
	}



	/**
	 * Teste si des missions peuvent encore �tre distribu�es.<br>
	 * Cette m�thode est bloquante tant qu'il n'y a pas de mission disponible et qu'on ne sait pas si il n'y en a plus � distribuer.<br>
	 * ATTENTION: Que cette m�thode retourne <code>true</code> ne garantit pas qu'un appel ult�rieur � {@link #getNext()} ne retournera pas <code>null</code>, la mission disponible lors du test
	 * pouvant �tre la derni�re � distribuer, et avoir �t� distribu�e dans l'intervalle.
	 * @return <code>true</code> si des missions peuvent encore �tre distribu�es, <code>false</code> sinon.
	 */
	public final boolean hasNext() {
		synchronized (nextLock) {
			waitForNext();
			return next != null;
		}
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public final void run() {
		@SuppressWarnings("hiding") M next = null;
		ExpectedCounter expectedCounterThread = null;

		try {
			delegateInitialize();
			(expectedCounterThread = new ExpectedCounter()).start();
			synchronized (nextLock) {
				while (!finished && !hasExceptions() && ((next = delegateGetNext()) != null)) {
					while (!finished && !hasExceptions() && (this.next != null)) {
						try {
							nextLock.wait();
						} catch (Exception exception) {
							reportExceptions(exception);
						}
					}
					if (!finished && !hasExceptions()) {
						this.next = next;
						nextLock.notifyAll();
					}
				}
			}
			expectedCounterThread.interrupt();
			finished = true;
			delegateFinalize();
		} catch (Throwable exception) {
			reportExceptions(exception);
		} finally {
			synchronized (expectedCountLock) {
				expectedCountLock.notifyAll();
			}
		}
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public synchronized void start() {
		started = true;
		super.start();
	}



	/**
	 * Arr�te la distribution de missions, m�me si il en reste.
	 */
	public final void stopDispensing() {
		finished = true;
	}



	/**
	 * Attend que la mission suivante soit disponible, ou qu'il n'y ait plus de mission � distribuer.<br>
	 * Cette m�thode est bloquante.
	 */
	private void waitForNext() {
		synchronized (nextLock) {
			if (!started) {
				start();
			}
			while (!finished && !hasExceptions() && (this.next == null)) {
				try {
					nextLock.wait();
				} catch (InterruptedException exception) {
					reportExceptions(exception);
				}
			}
		}
	}



	/**
	 * Nombre de r�sultats attendus.
	 */
	private volatile int expectedCount = NOT_COMPUTED;



	/**
	 * Verrou pour le nombre de r�sultats attendus.
	 */
	private final Object expectedCountLock = "expectedCountLock";



	/**
	 * Indicateur que le distributeur de missions a fini de travailler.
	 */
	private volatile boolean finished = false;



	/**
	 * Mission suivante � distribuer, ou <code>null</code> si il n'y en a pas de disponible.
	 */
	private volatile M next = null;



	/**
	 * Verrou pour la mission suivante � distribuer.
	 */
	private final Object nextLock = "nextLock";



	/**
	 * Indicateur de thread d�marr�.
	 */
	private volatile boolean started = false;



	/**
	 * Valeur pour indiquer que le calcul du nombre de r�sultats attendus est en cours mais n'est pas encore disponible.
	 */
	public static final int NOT_AVAILABLE = -2;



	/**
	 * Valeur pour indiquer que le nombre de r�sultats attendus n'est pas calculable.
	 */
	public static final int NOT_COMPUTABLE = -1;



	/**
	 * Valeur pour indiquer que le calcul du nombre de r�sultats attendus n'a pas encore �t� d�marr�.
	 */
	public static final int NOT_COMPUTED = -3;



	/**
	 * La classe {@link ExpectedCounter} impl�mente un compteur de missions attendues.
	 * @author Ludovic WALLE
	 */
	private final class ExpectedCounter extends Thread {



		/**	*/
		private ExpectedCounter() {
			super("ExpectedCounter");
			setDaemon(true);
		}



		/** {@inheritDoc} */
		@Override public void run() {
			int count;

			try {
				if (expectedCount == NOT_COMPUTED) {
					synchronized (expectedCountLock) {
						if (expectedCount == NOT_COMPUTED) {
							expectedCount = NOT_AVAILABLE;
						}
					}
					count = delegateComputeExpectedCount();
					if ((count < 0) && (count != NOT_COMPUTABLE)) {
						reportExceptions(new Exception("La valeur renvoy�e par la m�thode delegateComputeExpectedCount est invalide: " + count));
					} else {
						synchronized (expectedCountLock) {
							expectedCount = count;
						}
					}
				}
			} catch (Throwable exception) {
				reportExceptions(exception);
			} finally {
				synchronized (expectedCountLock) {
					expectedCountLock.notifyAll();
				}
			}
		}
	}



}
