package toolbox.parallel;



/**
 * La classe {@link Worker} implémente un ouvrier effectuant des missions.
 * @author Ludovic WALLE
 * @param <M> Type des missions.
 */
public abstract class Worker<M extends Mission> extends Employee<M> {



	/**	 */
	protected Worker() {
		super(getNewWorkerName());
	}



	/**
	 * @param other Autre ouvrier (ne doit pas être <code>null</code>).
	 */
	protected Worker(Worker<M> other) {
		super(getNewWorkerName(), other);
	}



	/**
	 * Effectue la mission indiquée. L'entreprise sera interrompue si cette méthode génère une exception.
	 * @param mission Mission.
	 * @return Le nombre de résultats à comptabiliser.
	 * @throws Throwable Pour que la méthode puisse générer des exceptions.
	 */
	protected abstract int delegateDo(M mission) throws Throwable;



	/**
	 * Licencie l'ouvrier, qui s'arrêtera dès qu'il aura fini sa mission en cours.
	 */
	protected final void dismiss() {
		dismissed = true;
	}



	/**
	 * Crée un nouvel ouvrier semblable à celui ci.<br>
	 * Cette méthode est utilisée par {@link Enterprise} pour embaucher de nouveaux ouvriers (créer de nouvelles instances).<br>
	 * Les classes dérivées doivent doivent l'implémenter en passant par le constructeur {@link #Worker(Worker)}. Si il n'y a pas de traitement particulier à faire lors de la création d'un nouvel
	 * ouvrier, la classe dérivée devrait ressembler à:<br>
	 * <pre>
	 * public class MyWorker extends Worker<...> {
	 *
	 * 		public MyWorker() {}
	 *
	 * 		public MyWorker(MyWorker other) {
	 * 			super(other);
	 * 		}
	 *
	 * 		&#64;Override public MyWorker newOne() {
	 * 			return new MyWorker(this);
	 * 		}
	 *
	 *     ...
	 *
	 * }
	 * </pre><br>
	 * @return Le nouvel ouvrier.
	 */
	protected abstract Worker<M> newOne();



	/**
	 * Signale que l'ouvrier a fini la mission indiquée.
	 * @param mission Mission.
	 * @param count Nombre de résultats à comptabiliser.
	 */
	protected final void reportDone(M mission, int count) {
		getEnterprise().collectDone(mission, count);
	}



	/**
	 * Signale que l'ouvrier a fini de travailler.
	 */
	protected final void reportFinished() {
		getEnterprise().collectFinished(this);
	}



	/**
	 * Signale que l'ouvrier a commencé à travailler.
	 */
	protected final void reportStarted() {
		getEnterprise().collectStarted();
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public final void run() {
		M object;

		try {
			delegateInitialize();
			reportStarted();
			while (!dismissed && ((object = getEnterprise().getNext()) != null)) {
				reportDone(object, delegateDo(object));
			}
			delegateFinalize();
			reportFinished();
		} catch (Throwable exception) {
			reportExceptions(exception);
		}
	}



	/**
	 * Retourne le nom à attribuer à l'ouvrier embauché.
	 * @return Le nom à attribuer à l'ouvrier embauché.
	 */
	private static String getNewWorkerName() {
		synchronized (newWorkerIdLock) {
			return String.format("Worker%04d", newWorkerId++);
		}
	}



	/**
	 * Indication d'ouvrier licencié après la fin de la mission en cours.
	 */
	private volatile boolean dismissed = false;



	/**
	 * Numéro à attribuer au prochain ouvrier embauché.
	 */
	private static volatile int newWorkerId = 0;



	/**
	 * Verrou du numéro à attribuer au prochain ouvrier embauché.
	 */
	private static final Object newWorkerIdLock = "newWorkerId";



}
