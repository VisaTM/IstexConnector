package fr.inist.toolbox.parallel;

import java.util.*;

import toolbox.parallel.*;



/**
 * La classe {@link Employee} repr�sente un employ� g�n�rique.
 * @author Ludovic WALLE
 * @param <M> Missions.
 */
abstract class Employee<M extends Mission> extends Thread {



	/**
	 * @param name Nom de l'employ� (peut �tre <code>null</code>).
	 */
	protected Employee(String name) {
		super(name);
	}



	/**
	 * @param name Nom de l'employ� (peut �tre <code>null</code>).
	 * @param other Autre employ� (ne doit pas �tre <code>null</code>).
	 */
	protected Employee(String name, Employee<M> other) {
		super(name);
		this.enterprise = other.enterprise;
	}



	/**
	 * M�thode �ventuellement surcharg�e dans les classes d�riv�es, appel�e � la fin du traitement, dans la m�thode {@link #run()} du thread.<br>
	 * On peut y mettre les op�rations � r�aliser � la fin du traitement, telles que des lib�rations de ressources.<br>
	 * Par d�faut, cette m�thode ne fait rien.
	 * @throws Throwable Pour que la m�thode puisse g�n�rer des exceptions.<br>
	 *             Le traitement sera interrompu si cette m�thode g�n�re une exception.
	 */
	protected void delegateFinalize() throws Throwable {}



	/**
	 * M�thode �ventuellement surcharg�e dans les classes d�riv�es, appel�e au d�but du traitement, dans la m�thode {@link #run()} du thread.<br>
	 * On peut y mettre les op�rations � r�aliser au d�but du traitement, telles que initialisations lentes. C'est un meilleur emplacement que dans le constructeur, car les op�rations y seront
	 * r�alis�es de fa�on asynchrone.<br>
	 * Par d�faut, cette m�thode ne fait rien.
	 * @throws Throwable Pour que la m�thode puisse g�n�rer des exceptions.<br>
	 *             Le traitement sera interrompu si cette m�thode g�n�re une exception.
	 */
	protected void delegateInitialize() throws Throwable {}



	/**
	 * Retourne l'entreprise � laquelle l'employ� doit rapporter les exceptions rencontr�es, ou <code>null</code> si elle n'est pas connue.
	 * @return L'entreprise � laquelle l'employ� doit rapporter les exceptions rencontr�es, ou <code>null</code> si elle n'est pas connue.
	 */
	public final Enterprise<M> getEnterprise() {
		return enterprise;
	}



	/**
	 * Retourne les exceptions rencontr�es par l'employ�.<br>
	 * Si aucune exception n'a �t� rencontr�e, la m�thode retourne un tableau vide, jamais <code>null</code>.<br>
	 * Cette m�thode est non bloquante.
	 * @return Retourne les exceptions rencontr�es par l'employ�.
	 */
	public final Throwable[] getExceptions() {
		synchronized (exceptions) {
			return exceptions.toArray(new Throwable[exceptions.size()]);
		}
	}



	/**
	 * Teste si cet employ� a rencontr� des exceptions.
	 * @return <code>true</code> si cet employ� a rencontr� des exceptions, <code>false</code> sinon.
	 */
	public final boolean hasExceptions() {
		return !exceptions.isEmpty();
	}



	/**
	 * Sp�cifie l'entreprise qui embauche l'employ�, et lui rapporte les �ventuelles exceptions d�j� rencontr�es.<br>
	 * Cette m�thode ne peut �tre appel�e qu'au plus une seule fois, et uniquement si l'employ� a �t� cr�� par {@link #Employee(String)}.
	 * @param enterprise Entreprise qui embauche l'employ� (ne doit pas �tre <code>null</code>).
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
	 * Enregistre les exceptions indiqu�es, et les rapporte � son entreprise, si elle est connue.
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
	 * Entreprise � laquelle cet employ� appartient. Une fois sp�cifi�e, elle ne peut plus �tre modifi�e.
	 */
	private Enterprise<M> enterprise = null;



	/**
	 * Verrou pour l'entreprise.
	 */
	private final Object enterpriseLock = "";



	/**
	 * Exceptions rencontr�es.
	 */
	private final Vector<Throwable> exceptions = new Vector<>();



	/**
	 * Capturer les exceptions non g�r�es.
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
