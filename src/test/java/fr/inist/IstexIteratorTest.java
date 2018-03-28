package fr.inist;

import java.util.*;

import fr.inist.istex.*;
import fr.inist.toolbox.json.*;



/**
 * La classe {@link IstexIteratorTest} .
 * @author Ludovic WALLE
 */
@SuppressWarnings("static-method")
public class IstexIteratorTest {



	/**
	 * Test method for {@link fr.inist.istex.Istex#newHit(JsonObject, Map)}.
	 * @throws Exception
	 */
	@org.junit.Test public void testNewHit() throws Exception {
		Istex.newHit(JsonObject.parse("{\"ark\":[\"xxx\"]}"), new TreeMap<String, Json>());
//		scanPathes(new IstexIterator("*", "*", null, null));
//		new IstexIterator("journal", "*", 5000, 0, null, null, null, null);
//		new IstexIterator("test", "*", 5000, 0, null, null, null, null);
//		new IstexIterator("conference", "*", 5000, 0, null, null, null, null);
//		new IstexIterator("book", "*", 5000, 0, null, null, null, null);
	}


//	private void scan(IstexIterator iterator) throws IstexException {
//		int count = 0;
//		while (iterator.hasNext()) {
//			try {
//				if (iterator.next() == null) {
//					if (iterator.getLastException().isFatal()) {
//						throw iterator.getLastException();
//					} else {
//						System.err.println (iterator.getLastException().getMessage() + "\n" + iterator.getLastJson());
//					}
//				}
//			} catch (RuntimeException exception) {
//				System.err.println (iterator.getLastException().getMessage() + "\n" + iterator.getLastJson());
//			}
//			System.out.println(++count);
//		}
//	}



}
