package fr.inist;

import java.io.*;

import fr.inist.istex.*;



/**
 * La classe {@link IstexTest} .
 * @author Ludovic WALLE
 */
@SuppressWarnings({"javadoc","static-method"})
public class IstexTest {



	@org.junit.Test public void test_0() throws IOException {
		System.out.println(new String(Istex.getFulltextBytes(IDENTIFICATION_TOKEN, "94ECC9351E82249F2C68B19691BB2C57586DD55B", "txt")));
	}



	@org.junit.Test public void test_1() throws IOException {
		System.out.println(new String(Istex.getFulltextBytes(IDENTIFICATION_TOKEN, "94ECC9351E82249F2C68B19691BB2C57586DD55B", "tzxt")));
	}



	@org.junit.Test public void test_2() throws IOException {
		System.out.println(new String(Istex.getFulltextBytes(IDENTIFICATION_TOKEN, "94ECC9351E82249F2C68B19691BB2C57786DD55B", "txt")));
	}



	/**
	 * Token d'identification.
	 * @see {@link "https://api.istex.fr/token/"}.
	 */
	private static final String IDENTIFICATION_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6Imx1ZG92aWMud2FsbGUiLCJlbWFpbCI6Imx1ZG92aWMud2FsbGVAaW5pc3QuZnIiLCJkaXNwbGF5TmFtZSI6IldBTExFIEx1ZG92aWMiLCJjb21tb25OYW1lIjoiV0FMTEUgTHVkb3ZpYyIsImZpcnN0TmFtZSI6Ikx1ZG92aWMiLCJsYXN0TmFtZSI6IldBTExFIiwiaW5zdGl0dXRpb24iOiJ7Q05SU31VUFM3NiIsInJlZ2lvbmFsT2ZmaWNlIjoiRFIwNiIsImlhdCI6MTUyMTQ2MjM4M30.OaBdCzstNcb9OcWvrcxMrn5QJAen9j04vNHCQTS2Pfg";

//	private static final Map<String, String> BAD_NAMES = new HashMap<>();


}
