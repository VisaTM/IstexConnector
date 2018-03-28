package fr.inist;

import eu.openminted.registry.domain.RightsStatementEnum;



public class RightsStmtNameConverter {



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
