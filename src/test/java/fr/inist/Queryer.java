package fr.inist;

import java.net.*;

import fr.inist.toolbox.*;
import fr.inist.toolbox.json.*;



/**
 * La classe {@link Queryer} .
 * @author Ludovic WALLE
 */
public class Queryer {



	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		JsonObject json;
		String chars;
		String query;
		int total;

		chars = "0123456789BCDFGHJKLMNPQRSTVWXZ";
		for (int i = 0; i < chars.length(); i++) {
			for (int j = 0; j < chars.length(); j++) {
				for (int k = 0; k < chars.length(); k++) {
//				query = "arkIstex:*" + chars.charAt(i) + "-" + chars.charAt(j);
					query = "arkIstex:ark\\:\\/67375\\/" + chars.charAt(i) + chars.charAt(j) + chars.charAt(k) + "*";
					json = JsonObject.parse(new String(Readers.getBytesFromURL("https://api.istex.fr/document/?size=0&q=" + URLEncoder.encode(query, "UTF-8"))).trim());
					total = json.cutInteger("total");
					if (total > 0) {
						System.out.println(query + ": " + total);
					}
				}
			}
		}
	}


}
