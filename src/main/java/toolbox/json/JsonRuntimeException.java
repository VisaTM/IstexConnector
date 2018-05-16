package toolbox.json;

import toolbox.json.JsonObject.*;



/**
 * La classe {@link JsonRuntimeException} définit une exception pouvant être générée lors d'accès à des éléments Json, lorsque les options de présence ou de valeur ({@link Option}) ne sont pas
 * respectées.
 * @author Ludovic WALLE
 */
public class JsonRuntimeException extends RuntimeException {



	/**
	 * @param message Message.
	 */
	public JsonRuntimeException(String message) {
		super(message);
	}



}
