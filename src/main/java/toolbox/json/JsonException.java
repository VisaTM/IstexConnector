package toolbox.json;



/**
 * La classe {@link JsonException} définit une exception pouvant être générée lors du parsage Json.
 * @author Ludovic WALLE
 */
public class JsonException extends Exception {



	/**
	 * @param message Message.
	 */
	public JsonException(String message) {
		super(message);
	}



}
