package fr.inist.istex;

import eu.openminted.registry.domain.RightsStatementEnum;



/**
 * La classe {@link RightsStmtNameConverter} .
 * @author Ludovic WALLE
 */
public class RightsStmtNameConverter {



	/**
	 * @param bestLicence
	 * @return
	 */
	public static RightsStatementEnum convert(String bestLicence) {
		switch (bestLicence) {
		case "Open Access":
			return RightsStatementEnum.OPEN_ACCESS;
		case "Restricted":
			return RightsStatementEnum.RESTRICTED_ACCESS;
		default:
			return null;
		}
	}
}
