package toolbox.parallel;

/**
 * La classe {@link Missionner} est la classe ancètre de tous les distributeurs de missions.<br>
 * Les distributeurs fonctionnent de façon asynchrone, dans un {@link Thread} séparé. Ils préparent une nouvelle mission dès que la précédente a été attribuée, au lieu d'attendre qu'on leur en demande
 * une pour le faire.<br>
 * Un distributeur peut être utilisé comme itérateur.<br>
 * <br>
 * Les méthodes appelées par la méthode {@link #run()} sont, dans l'ordre:
 * <ul>
 * <li>{@link #delegateInitialize()}, une seule fois,
 * <li>{@link #delegateGetNext()}, plusieurs fois,
 * <li>{@link #delegateFinalize()}, une seule fois.
 * </ul>
 * Aucune de ces méthodes n'est appelée de façon concurrente, et n'a besoin d'être synchronisée.<br>
 * Il est préférable de faire les initialisations lentes dans la méthode {@link #delegateInitialize()}, exécutée de façon asynchrone, plutôt que dans le constructeur, exécuté de façon synchrone.
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
	 * Retourne le nombre de résultats attendus pour l'ensemble des missions (positif ou nul), ou {@link #NOT_COMPUTABLE} si il n'est pas calculable.<br>
	 * Cette méthode est destinée à être surchargée lorsque le nombre de résultats attendus est calculable. Elle sera appelée une seule fois par le {@link Missionner}, après
	 * {@link #delegateInitialize()}. Elle doit pouvoir être exécutée en parallèle avec la méthode {@link #delegateGetNext()}.<br>
	 * Par défaut, cette méthode retourne {@link Missionner#NOT_COMPUTABLE}.<br>
	 * @return Le nombre de résultats attendus pour l'ensemble des missions (positif ou nul), ou {@link #NOT_COMPUTABLE} si il n'est pas calculable.
	 * @throws Throwable Pour que la méthode puisse générer des exceptions.<br>
	 *             L'entreprise sera interrompue si cette méthode génère une exception.
	 */
	@SuppressWarnings("static-method") protected int delegateComputeExpectedCount() throws Throwable {
		return Missionner.NOT_COMPUTABLE;
	}



	/**
	 * Retourne la prochaine mission, ou <code>null</code> lorsqu'il n'y en a plus.
	 * @return La prochaine mission, ou <code>null</code> lorsqu'il n'y en a plus.
	 * @throws Throwable Pour que la méthode puisse générer des exceptions.<br>
	 *             L'entreprise sera interrompue si cette méthode génère une exception.
	 */
	protected abstract M delegateGetNext() throws Throwable;



	/**
	 * Retourne le nombre attendu de résultats (positif ou nul), ou {@link #NOT_COMPUTABLE} si il n'est pas calculable, ou {@link #NOT_AVAILABLE} si le calcul est en cours, ou {@link #NOT_COMPUTED} si
	 * ni {@link #start()} ni {@link #run()} n'ont été appelées au préalable (le calcul n'a pas été lancé).
	 * @param waitUntilComputed Indique si cette méthode attend que le résultat soit calculé ou non. Si <code>true</code>, la valeur retournée ne pourra être que le nombre attendu de résultats
	 *            (positif ou nul) ou {@link #NOT_COMPUTABLE}.
	 * @return Le nombre attendu de résultats.
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
	 * Cette méthode est bloquante tant qu'il n'y a pas de mission disponible et qu'on ne sait pas si il n'y en a plus à distribuer.
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
	 * Teste si des missions peuvent encore être distribuées.<br>
	 * Cette méthode est bloquante tant qu'il n'y a pas de mission disponible et qu'on ne sait pas si il n'y en a plus à distribuer.<br>
	 * ATTENTION: Que cette méthode retourne <code>true</code> ne garantit pas qu'un appel ultérieur à {@link #getNext()} ne retournera pas <code>null</code>, la mission disponible lors du test
	 * pouvant être la dernière à distribuer, et avoir été distribuée dans l'intervalle.
	 * @return <code>true</code> si des missions peuvent encore être distribuées, <code>false</code> sinon.
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
	 * Arrête la distribution de missions, même si il en reste.
	 */
	public final void stopDispensing() {
		finished = true;
	}



	/**
	 * Attend que la mission suivante soit disponible, ou qu'il n'y ait plus de mission à distribuer.<br>
	 * Cette méthode est bloquante.
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
	 * Nombre de résultats attendus.
	 */
	private volatile int expectedCount = NOT_COMPUTED;



	/**
	 * Verrou pour le nombre de résultats attendus.
	 */
	private final Object expectedCountLock = "expectedCountLock";



	/**
	 * Indicateur que le distributeur de missions a fini de travailler.
	 */
	private volatile boolean finished = false;



	/**
	 * Mission suivante à distribuer, ou <code>null</code> si il n'y en a pas de disponible.
	 */
	private volatile M next = null;



	/**
	 * Verrou pour la mission suivante à distribuer.
	 */
	private final Object nextLock = "nextLock";



	/**
	 * Indicateur de thread démarré.
	 */
	private volatile boolean started = false;



	/**
	 * Valeur pour indiquer que le calcul du nombre de résultats attendus est en cours mais n'est pas encore disponible.
	 */
	public static final int NOT_AVAILABLE = -2;



	/**
	 * Valeur pour indiquer que le nombre de résultats attendus n'est pas calculable.
	 */
	public static final int NOT_COMPUTABLE = -1;



	/**
	 * Valeur pour indiquer que le calcul du nombre de résultats attendus n'a pas encore été démarré.
	 */
	public static final int NOT_COMPUTED = -3;



	/**
	 * La classe {@link ExpectedCounter} implémente un compteur de missions attendues.
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
						reportExceptions(new Exception("La valeur renvoyée par la méthode delegateComputeExpectedCount est invalide: " + count));
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
