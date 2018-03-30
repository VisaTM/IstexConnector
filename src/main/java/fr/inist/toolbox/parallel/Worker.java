package fr.inist.toolbox.parallel;

import toolbox.parallel.*;

/**
 * La classe {@link Worker} impl�mente un ouvrier effectuant des missions.
 * @author Ludovic WALLE
 * @param <M> Type des missions.
 */
public abstract class Worker<M extends Mission> extends Employee<M> {



	/**	 */
	protected Worker() {
		super(getNewWorkerName());
	}



	/**
	 * @param other Autre ouvrier (ne doit pas �tre <code>null</code>).
	 */
	protected Worker(Worker<M> other) {
		super(getNewWorkerName(), other);
	}



	/**
	 * Effectue la mission indiqu�e. L'entreprise sera interrompue si cette m�thode g�n�re une exception.
	 * @param mission Mission.
	 * @return Le nombre de r�sultats � comptabiliser.
	 * @throws Throwable Pour que la m�thode puisse g�n�rer des exceptions.
	 */
	protected abstract int delegateDo(M mission) throws Throwable;



	/**
	 * Licencie l'ouvrier, qui s'arr�tera d�s qu'il aura fini sa mission en cours.
	 */
	protected final void dismiss() {
		dismissed = true;
	}



	/**
	 * Cr�e un nouvel ouvrier semblable � celui ci.<br>
	 * Cette m�thode est utilis�e par {@link Enterprise} pour embaucher de nouveaux ouvriers (cr�er de nouvelles instances).<br>
	 * Les classes d�riv�es doivent doivent l'impl�menter en passant par le constructeur {@link #Worker(Worker)}. Si il n'y a pas de traitement particulier � faire lors de la cr�ation d'un nouvel
	 * ouvrier, la classe d�riv�e devrait ressembler �:<br>
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
	 * Signale que l'ouvrier a fini la mission indiqu�e.
	 * @param mission Mission.
	 * @param count Nombre de r�sultats � comptabiliser.
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
	 * Signale que l'ouvrier a commenc� � travailler.
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
	 * Retourne le nom � attribuer � l'ouvrier embauch�.
	 * @return Le nom � attribuer � l'ouvrier embauch�.
	 */
	private static String getNewWorkerName() {
		synchronized (newWorkerIdLock) {
			return String.format("Worker%04d", newWorkerId++);
		}
	}



	/**
	 * Indication d'ouvrier licenci� apr�s la fin de la mission en cours.
	 */
	private volatile boolean dismissed = false;



	/**
	 * Num�ro � attribuer au prochain ouvrier embauch�.
	 */
	private static volatile int newWorkerId = 0;



	/**
	 * Verrou du num�ro � attribuer au prochain ouvrier embauch�.
	 */
	private static final Object newWorkerIdLock = "newWorkerId";



}
