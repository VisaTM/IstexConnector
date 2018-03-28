package fr.inist.istex;



/**
 * La classe {@link IstexException} .
 * @author Ludovic WALLE
 */
public class IstexException extends Exception {



	/**
	 * @param message Message.
	 */
	public IstexException(String message) {
		super(message);
	}



	/**
	 * @param message Message.
	 * @param cause Exception.
	 */
	public IstexException(String message, Throwable cause) {
		super(message, cause);
	}



	/**
	 * @param cause Exception.
	 */
	public IstexException(Throwable cause) {
		super(cause);
	}



}
