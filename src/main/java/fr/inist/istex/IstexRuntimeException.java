package fr.inist.istex;



/**
 * La classe {@link IstexRuntimeException} .
 * @author Ludovic WALLE
 */
public class IstexRuntimeException extends RuntimeException {



	/**
	 * @param message Message.
	 */
	public IstexRuntimeException(String message) {
		super(message);
	}



	/**
	 * @param message Message.
	 * @param cause Exception.
	 */
	public IstexRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}



	/**
	 * @param cause Exception.
	 */
	public IstexRuntimeException(Throwable cause) {
		super(cause);
	}



}
