package toolbox.json;



/**
 * La classe {@link JsonContainer} est un ancêtre commun abstrait à tous les éléments Json non atomiques (objets, tableaux).
 * @author Ludovic WALLE
 */
public abstract class JsonContainer extends Json {



	/**
	 * Teste si cet élément est vide.
	 * @return <code>true</code> si cet élément est vide, <code>false</code> sinon.
	 */
	public abstract boolean isEmpty();



	/**
	 * Teste si cet élément n'est pas vide.
	 * @return <code>true</code> si cet élément Json n'est pas vide, <code>false</code> sinon.
	 */
	public final boolean isNotEmpty() {
		return !isEmpty();
	}



}
