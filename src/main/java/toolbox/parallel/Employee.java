package toolbox.parallel;

import java.util.*;



/**
 * La classe {@link Employee} représente un employé générique.
 * @author Ludovic WALLE
 * @param <M> Missions.
 */
abstract class Employee<M extends Mission> extends Thread {



	/**
	 * @param name Nom de l'employé (peut être <code>null</code>).
	 */
	protected Employee(String name) {
		super(name);
		setDaemon(true);
	}



	/**
	 * @param name Nom de l'employé (peut être <code>null</code>).
	 * @param other Autre employé (ne doit pas être <code>null</code>).
	 */
	protected Employee(String name, Employee<M> other) {
		this(name);
		this.enterprise = other.enterprise;
	}



	/**
	 * Méthode éventuellement surchargée dans les classes dérivées, appelée à la fin du traitement, dans la méthode {@link #run()} du thread.<br>
	 * On peut y mettre les opérations à réaliser à la fin du traitement, telles que des libérations de ressources.<br>
	 * Par défaut, cette méthode ne fait rien.
	 * @throws Throwable Pour que la méthode puisse générer des exceptions.<br>
	 *             Le traitement sera interrompu si cette méthode génère une exception.
	 */
	protected void delegateFinalize() throws Throwable {}



	/**
	 * Méthode éventuellement surchargée dans les classes dérivées, appelée au début du traitement, dans la méthode {@link #run()} du thread.<br>
	 * On peut y mettre les opérations à réaliser au début du traitement, telles que initialisations lentes. C'est un meilleur emplacement que dans le constructeur, car les opérations y seront
	 * réalisées de façon asynchrone.<br>
	 * Par défaut, cette méthode ne fait rien.
	 * @throws Throwable Pour que la méthode puisse générer des exceptions.<br>
	 *             Le traitement sera interrompu si cette méthode génère une exception.
	 */
	protected void delegateInitialize() throws Throwable {}



	/**
	 * Retourne l'entreprise à laquelle l'employé doit rapporter les exceptions rencontrées, ou <code>null</code> si elle n'est pas connue.
	 * @return L'entreprise à laquelle l'employé doit rapporter les exceptions rencontrées, ou <code>null</code> si elle n'est pas connue.
	 */
	public final Enterprise<M> getEnterprise() {
		return enterprise;
	}



	/**
	 * Retourne les exceptions rencontrées par l'employé.<br>
	 * Si aucune exception n'a été rencontrée, la méthode retourne un tableau vide, jamais <code>null</code>.<br>
	 * Cette méthode est non bloquante.
	 * @return Retourne les exceptions rencontrées par l'employé.
	 */
	public final Throwable[] getExceptions() {
		synchronized (exceptions) {
			return exceptions.toArray(new Throwable[exceptions.size()]);
		}
	}



	/**
	 * Teste si cet employé a rencontré des exceptions.
	 * @return <code>true</code> si cet employé a rencontré des exceptions, <code>false</code> sinon.
	 */
	public final boolean hasExceptions() {
		return !exceptions.isEmpty();
	}



	/**
	 * Spécifie l'entreprise qui embauche l'employé, et lui rapporte les éventuelles exceptions déjà rencontrées.<br>
	 * Cette méthode ne peut être appelée qu'au plus une seule fois, et uniquement si l'employé a été créé par {@link #Employee(String)}.
	 * @param enterprise Entreprise qui embauche l'employé (ne doit pas être <code>null</code>).
	 */
	protected final void hiredBy(@SuppressWarnings("hiding") Enterprise<M> enterprise) {
		synchronized (enterpriseLock) {
			if (enterprise == null) {
				throw new NullPointerException();
			} else if (this.enterprise != null) {
				throw new IllegalStateException();
			} else {
				this.enterprise = enterprise;
				this.enterprise.collectExceptions(getExceptions());
			}
		}
	}



	/**
	 * Enregistre les exceptions indiquées, et les rapporte à son entreprise, si elle est connue.
	 * @param exceptions Exceptions.
	 */
	protected final void reportExceptions(@SuppressWarnings("hiding") Throwable... exceptions) {
		if (enterprise != null) {
			enterprise.collectExceptions(exceptions);
		}
		synchronized (exceptions) {
			for (Throwable exception : exceptions) {
				this.exceptions.add(exception);
			}
		}
	}



	/**
	 * Entreprise à laquelle cet employé appartient. Une fois spécifiée, elle ne peut plus être modifiée.
	 */
	private Enterprise<M> enterprise = null;



	/**
	 * Verrou pour l'entreprise.
	 */
	private final Object enterpriseLock = "";



	/**
	 * Exceptions rencontrées.
	 */
	private final Vector<Throwable> exceptions = new Vector<>();



	/**
	 * Capturer les exceptions non gérées.
	 */
	{
		setUncaughtExceptionHandler(new UncaughtExceptionHandler() {



			/** {@inheritDoc} */
			@Override public void uncaughtException(Thread thread, Throwable exception) {
				reportExceptions(exception);
			}



		});
	}



}
