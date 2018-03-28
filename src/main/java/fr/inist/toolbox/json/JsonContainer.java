package fr.inist.toolbox.json;



/**
 * La classe {@link JsonContainer} .
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
