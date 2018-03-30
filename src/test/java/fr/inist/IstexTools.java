package fr.inist;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;

import fr.inist.istex.*;
import fr.inist.toolbox.*;
import fr.inist.toolbox.json.*;



/**
 * La classe {@link IstexTools} implémente la représentation java d'une réponse ISTEX (interprétation du json reçu). Cette classe sert à la fois à marquer les classes utilisées pour la représentation,
 * et à les munir d'une méthode permettant de renseigner cette représentation à partir d'une chaine json reçue d'ISTEX.<br>
 * @author Ludovic WALLE
 */

public class IstexTools {



	/**
	 * Collecte récursivement les chemins décrit par les classes.
	 * @param path Chemin de l'élément.
	 * @param type Classe à traiter.
	 * @param pathes Chemins collectés.
	 */
	private static void collectPathes(String path, Class<?> type, Set<String> pathes) {
		if (type == Boolean.class) {
			pathes.add(path + "	boolean");
		} else if (type == Integer.class) {
			pathes.add(path + "	integer");
		} else if (type == Double.class) {
			pathes.add(path + "	double");
		} else if (type == String.class) {
			pathes.add(path + "	text");
		} else if (type.isArray()) {
			collectPathes(path, type.getComponentType(), pathes);
		} else {
			for (Field field : type.getDeclaredFields()) {
				collectPathes(path + "/" + Istex.computeJsonName(field.getName()), field.getType(), pathes);
			}
		}
	}



	/**
	 * Collecte récursivement les chemins décrits à l'URL {@link "https://api.istex.fr/mapping"}.
	 * @param parentPath Chemin du parent de l'élément.
	 * @param json Element à traiter.
	 * @param pathes Chemins collectés.
	 */
	private static void collectPathes(String parentPath, JsonObject json, Set<String> pathes) {
		JsonObject subJson;

		for (Iterator<Entry<String, Json>> pairIterator = json.iterator(); pairIterator.hasNext();) {
			Entry<String, Json> pair = pairIterator.next();
			if (pair.getValue() instanceof JsonObject) {
				subJson = (JsonObject) pair.getValue();
				if (subJson.has("type") && (subJson.get("type") instanceof JsonString)) {
					pathes.add(parentPath + "/" + pair.getKey() + "\t" + ((JsonString) subJson.get("type")).getValue());
				} else {
					collectPathes(parentPath + "/" + pair.getKey(), subJson, pathes);
				}
			}
		}
	}



	/**
	 * @param args
	 * @throws IOException
	 * @throws JsonException
	 */
	public static void main(String[] args) throws IOException, JsonException {
		Set<String> fromUrl = new TreeSet<>();
		Set<String> fromClasses = new TreeSet<>();
		String jsonString = null;

		try {
			collectPathes("", JsonObject.parse(jsonString = new String(Readers.getBytesFromURL("https://api.istex.fr/mapping")).trim()), fromUrl);
		} catch (Exception exception) {
			System.err.println(jsonString);
			throw exception;
		}
		collectPathes("", Istex.Hit.class, fromClasses);

		System.out.println("Chemins décrits par l'URL");
		for (Iterator<String> iterator = fromUrl.iterator(); iterator.hasNext();) {
			String string = iterator.next();
			System.out.println(string);
		}
		System.out.println();
		System.out.println("Chemins décrits par les classes");
		for (Iterator<String> iterator = fromClasses.iterator(); iterator.hasNext();) {
			String string = iterator.next();
			System.out.println(string);
		}
	}



	/**
	 * Supprime récursivement les champs sans valeur dans le json indiqué.
	 * @param json Json à corriger.
	 * @return Le json corrigé.
	 * @throws IstexException
	 */
	public static Json patch(Json json) throws IstexException {
		Json subJson;
		JsonArray jsonArray;
		JsonObject jsonObject;

		if (json instanceof JsonArray) {
			jsonArray = ((JsonArray) json);
			for (int i = jsonArray.size() - 1; i >= 0; i--) {
				if (patch(jsonArray.get(i)) == null) {
					jsonArray.cut(i);
				}
			}
			if (jsonArray.isEmpty()) {
				json = null;
			}
		} else if (json instanceof JsonObject) {
			jsonObject = ((JsonObject) json);
			for (String name : jsonObject.getNames()) {
				subJson = patch(jsonObject.get(name));
				if (subJson == null) {
					jsonObject.cut(name);
				}
			}
			if (jsonObject.isEmpty()) {
				json = null;
			}
		} else if ((json != null) && !(json instanceof JsonAtomic)) {
			throw new RuntimeException("Cas non traité dans patch.");
		}
		return json;
	}



}
