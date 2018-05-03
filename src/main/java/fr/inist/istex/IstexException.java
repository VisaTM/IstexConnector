package fr.inist.istex;

import org.apache.logging.log4j.*;



/**
 * La classe {@link IstexException} définit une exception de traitement ISTEX.
 * @author Ludovic WALLE
 */
public class IstexException extends RuntimeException {



	/**
	 * @param logger Logger.
	 * @param level Niveau de log.
	 * @param message Message.
	 */
	public IstexException(Logger logger, Level level, String message) {
		super(message);
		logger.log(level, message);
	}



	/**
	 * @param logger Logger.
	 * @param level Niveau de log.
	 * @param message Message.
	 * @param cause Exception.
	 */
	public IstexException(Logger logger, Level level, String message, Throwable cause) {
		super(message, cause);
		logger.log(level, message, cause);
	}



	/**
	 * @param logger Logger.
	 * @param level Niveau de log.
	 * @param cause Exception.
	 */
	public IstexException(Logger logger, Level level, Throwable cause) {
		super(cause);
		logger.log(level, "ISTEX exception", cause);
	}



}
