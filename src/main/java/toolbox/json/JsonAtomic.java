package toolbox.json;



/**
 * La classe {@link JsonAtomic} est un ancêtre commun abstrait à tous les éléments Json atomiques (booléens, nombres, chaines).
 * @author Ludovic WALLE
 */
public abstract class JsonAtomic extends Json {



	/**
	 * Retourne la valeur Json. La valeur peut être de type Boolean, Long, Double, String ou <code>null</code>.
	 * @return La valeur Json.
	 */
	public abstract Object getValue();



}
