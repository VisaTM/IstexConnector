package toolbox.json;

import java.util.*;
import java.util.Map.*;



/**
 * La classe {@link JsonObject} spécifie un objet. Un objet est un ensemble de paires nom-valeur. Les noms ne doivent pas être <code>null</code>. Il ne peut y avoir au plus qu'une paire portant un nom
 * donné.
 * @author Ludovic WALLE
 */
public class JsonObject extends JsonContainer implements Iterable<Entry<String, Json>> {



	/** */
	public JsonObject() {}



	/**
	 * @param other Autre objet Json.
	 */
	public JsonObject(JsonObject other) {
		for (Entry<String, Json> entry : other.pairs.entrySet()) {
			pairs.put(entry.getKey(), (entry.getValue() == null) ? null : entry.getValue().clone());
		}
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public JsonObject clone() {
		return new JsonObject(this);
	}



	/**
	 * Coupe l'éventuelle paire portant le nom indiqué.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @return La valeur de la paire coupée.
	 */
	public Json cut(String name) {
		checkName(name);
		return pairs.remove(name);
	}



	/**
	 * Coupe l'éventuelle paire portant le nom indiqué.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @return La valeur de la paire coupée.
	 */
	public Boolean cutBoolean(String name) {
		return cutBoolean(name, null);
	}



	/**
	 * Coupe l'éventuelle paire portant le nom indiqué.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @param option Indication de valeur obligatoire.
	 * @return La valeur de la paire coupée.
	 */
	public Boolean cutBoolean(String name, Option option) {
		Boolean value;

		value = getBoolean(name, option);
		remove(name);
		return value;
	}



	/**
	 * Coupe l'éventuelle paire portant le nom indiqué.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @return La valeur de la paire coupée.
	 */
	public Double cutDouble(String name) {
		return cutDouble(name, null);
	}



	/**
	 * Coupe l'éventuelle paire portant le nom indiqué.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @param option Indication de valeur obligatoire.
	 * @return La valeur de la paire coupée.
	 */
	public Double cutDouble(String name, Option option) {
		Double value;

		value = getDouble(name, option);
		remove(name);
		return value;
	}



	/**
	 * Coupe l'éventuelle paire portant le nom indiqué.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @return La valeur de la paire coupée.
	 */
	public Long cutInteger(String name) {
		return cutInteger(name, null);
	}



	/**
	 * Coupe l'éventuelle paire portant le nom indiqué.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @param option Indication de valeur obligatoire.
	 * @return La valeur de la paire coupée.
	 */
	public Long cutInteger(String name, Option option) {
		Long value;

		value = getInteger(name, option);
		remove(name);
		return value;
	}



	/**
	 * Coupe l'éventuelle paire portant le nom indiqué.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @return La valeur de la paire coupée.
	 */
	public JsonArray cutJsonArray(String name) {
		return cutJsonArray(name, null);
	}



	/**
	 * Coupe l'éventuelle paire portant le nom indiqué.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @param option Indication de valeur obligatoire.
	 * @return La valeur de la paire coupée.
	 */
	public JsonArray cutJsonArray(String name, Option option) {
		JsonArray value;

		value = getJsonArray(name, option);
		remove(name);
		return value;
	}



	/**
	 * Coupe l'éventuelle paire portant le nom indiqué.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @return La valeur de la paire coupée.
	 */
	public JsonObject cutJsonObject(String name) {
		return cutJsonObject(name, null);
	}



	/**
	 * Coupe l'éventuelle paire portant le nom indiqué.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @param option Indication de valeur obligatoire.
	 * @return La valeur de la paire coupée.
	 */
	public JsonObject cutJsonObject(String name, Option option) {
		JsonObject value;

		value = getJsonObject(name, option);
		remove(name);
		return value;
	}



	/**
	 * Coupe l'éventuelle paire portant le nom indiqué.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @return La valeur de la paire coupée.
	 */
	public Number cutNumber(String name) {
		return cutNumber(name, null);
	}



	/**
	 * Coupe l'éventuelle paire portant le nom indiqué.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @param option Indication de valeur obligatoire.
	 * @return La valeur de la paire coupée.
	 */
	public Number cutNumber(String name, Option option) {
		Number value;

		value = getNumber(name, option);
		remove(name);
		return value;
	}



	/**
	 * Coupe l'éventuelle paire portant le nom indiqué.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @return La valeur de la paire coupée.
	 */
	public String cutString(String name) {
		return cutString(name, null);
	}



	/**
	 * Coupe l'éventuelle paire portant le nom indiqué.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @param option Indication de valeur obligatoire.
	 * @return La valeur de la paire coupée.
	 */
	public String cutString(String name, Option option) {
		String value;

		value = getString(name, option);
		remove(name);
		return value;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JsonObject other = (JsonObject) obj;
		if (pairs == null) {
			if (other.pairs != null) {
				return false;
			}
		} else if (!pairs.equals(other.pairs)) {
			return false;
		}
		return true;
	}



	/**
	 * Retourne la valeur de la paire portant le nom indiqué, ou <code>null</code> si aucune paire ne porte ce nom.
	 * @param name Nom (peut être <code>null</code>, et dans ce cas la méthode retourne <code>null</code>).
	 * @return La valeur de la paire portant le nom indiqué, ou <code>null</code> si aucune paire ne porte ce nom.
	 */
	public Json get(String name) {
		checkName(name);
		return pairs.get(name);
	}



	/**
	 * Retourne la valeur de la paire portant le nom indiqué, ou <code>null</code> si aucune paire ne porte ce nom. Cette valeur doit être un booléen.
	 * @param name Nom (peut être <code>null</code>, et dans ce cas la méthode retourne <code>null</code>).
	 * @return La valeur de la paire portant le nom indiqué, ou <code>null</code> si aucune paire ne porte ce nom.
	 */
	public Boolean getBoolean(String name) {
		return getBoolean(name, null);
	}



	/**
	 * Coupe la valeur dont le nom est indiqué de l'objet Json indiqué.
	 * @param name Nom de la valeur à extraire.
	 * @param option Indication de valeur obligatoire.
	 * @return La valeur.
	 */
	public Boolean getBoolean(String name, Option option) {
		JsonBoolean jsonValue;
		Boolean value;
		boolean present;

		checkName(name);
		if (has(name)) {
			present = true;
			try {
				jsonValue = (JsonBoolean) pairs.get(name);
				value = (jsonValue != null) ? jsonValue.getValue() : null;
			} catch (ClassCastException exception) {
				throw new JsonRuntimeException("La valeur de \"" + name + "\" n'est pas un booléen: " + serialize());
			}
		} else {
			present = false;
			value = null;
		}
		if (option != null) {
			switch (option) {
			case PRESENT_AND_NOT_NULL_AND_NOT_EMPTY:
			case PRESENT_AND_NOT_NULL:
				if (value == null) {
					throw new JsonRuntimeException("La valeur de \"" + name + "\" est nulle ou manque: " + serialize());
				}
				break;
			case PRESENT:
				if (!present) {
					throw new JsonRuntimeException("La valeur de \"" + name + "\" manque: " + serialize());
				}
				break;
			}
		}
		return value;
	}



	/**
	 * Extrait la valeur dont le nom est indiqué de l'objet Json indiqué.
	 * @param name Nom de la valeur à extraire.
	 * @return La valeur.
	 */
	public Double getDouble(String name) {
		return getDouble(name, null);
	}



	/**
	 * Extrait la valeur dont le nom est indiqué de l'objet Json indiqué.
	 * @param name Nom de la valeur à extraire.
	 * @param option Indication de valeur obligatoire.
	 * @return La valeur.
	 */
	public Double getDouble(String name, Option option) {
		Number number;

		number = getNumber(name, option);
		return (number == null) ? null : number.doubleValue();
	}



	/**
	 * Extrait la valeur dont le nom est indiqué de l'objet Json indiqué.
	 * @param name Nom de la valeur à extraire.
	 * @return La valeur.
	 */
	public Long getInteger(String name) {
		return getInteger(name, null);
	}



	/**
	 * Extrait la valeur dont le nom est indiqué de l'objet Json indiqué.
	 * @param name Nom de la valeur à extraire.
	 * @param option Indication de valeur obligatoire.
	 * @return La valeur.
	 */
	public Long getInteger(String name, Option option) {
		Number number;

		number = getNumber(name, option);
		return (number == null) ? null : number.longValue();
	}



	/**
	 * Retourne la valeur de la paire portant le nom indiqué, ou <code>null</code> si aucune paire ne porte ce nom. Cette valeur doit être un tableau Json.
	 * @param name Nom (peut être <code>null</code>, et dans ce cas la méthode retourne <code>null</code>).
	 * @return La valeur de la paire portant le nom indiqué, ou <code>null</code> si aucune paire ne porte ce nom.
	 */
	public JsonArray getJsonArray(String name) {
		return getJsonArray(name, null);
	}



	/**
	 * Extrait la valeur dont le nom est indiqué de l'objet Json indiqué.
	 * @param name Nom de la valeur à extraire.
	 * @param option Indication de valeur obligatoire.
	 * @return La valeur.
	 */
	public JsonArray getJsonArray(String name, Option option) {
		JsonArray value;
		boolean present;

		checkName(name);
		if (has(name)) {
			present = true;
			try {
				value = (JsonArray) pairs.get(name);
			} catch (ClassCastException exception) {
				throw new JsonRuntimeException("La valeur de \"" + name + "\" n'est pas un tableau json: " + serialize());
			}
		} else {
			present = false;
			value = null;
		}
		if (option != null) {
			switch (option) {
			case PRESENT_AND_NOT_NULL_AND_NOT_EMPTY:
				if ((value == null) || value.isEmpty()) {
					throw new JsonRuntimeException("La valeur de \"" + name + "\" est vide ou nulle ou manque: " + serialize());
				}
				break;
			case PRESENT_AND_NOT_NULL:
				if (value == null) {
					throw new JsonRuntimeException("La valeur de \"" + name + "\" est nulle ou manque: " + serialize());
				}
				break;
			case PRESENT:
				if (!present) {
					throw new JsonRuntimeException("La valeur de \"" + name + "\" manque: " + serialize());
				}
				break;
			}
		}
		return value;
	}



	/**
	 * Retourne la valeur de la paire portant le nom indiqué, ou <code>null</code> si aucune paire ne porte ce nom. Cette valeur doit être un objet Json.
	 * @param name Nom (peut être <code>null</code>, et dans ce cas la méthode retourne <code>null</code>).
	 * @return La valeur de la paire portant le nom indiqué, ou <code>null</code> si aucune paire ne porte ce nom.
	 */
	public JsonObject getJsonObject(String name) {
		return getJsonObject(name, null);
	}



	/**
	 * Extrait la valeur dont le nom est indiqué de l'objet Json indiqué.
	 * @param name Nom de la valeur à extraire.
	 * @param option Indication de valeur obligatoire.
	 * @return La valeur.
	 */
	public JsonObject getJsonObject(String name, Option option) {
		JsonObject value;
		boolean present;

		checkName(name);
		if (has(name)) {
			present = true;
			try {
				value = (JsonObject) pairs.get(name);
			} catch (ClassCastException exception) {
				throw new JsonRuntimeException("La valeur de \"" + name + "\" n'est pas un objet json: " + serialize());
			}
		} else {
			present = false;
			value = null;
		}
		if (option != null) {
			switch (option) {
			case PRESENT_AND_NOT_NULL_AND_NOT_EMPTY:
				if ((value == null) || value.isEmpty()) {
					throw new JsonRuntimeException("La valeur de \"" + name + "\" est vide ou nulle ou manque: " + serialize());
				}
				break;
			case PRESENT_AND_NOT_NULL:
				if (value == null) {
					throw new JsonRuntimeException("La valeur de \"" + name + "\" est nulle ou manque: " + serialize());
				}
				break;
			case PRESENT:
				if (!present) {
					throw new JsonRuntimeException("La valeur de \"" + name + "\" manque: " + serialize());
				}
				break;
			}
		}
		return value;
	}



	/**
	 * Retourne les noms de toutes les paires.
	 * @return Les noms de toutes les paires.
	 */
	public String[] getNames() {
		return pairs.keySet().toArray(new String[pairs.size()]);
	}



	/**
	 * Retourne la valeur de la paire portant le nom indiqué, ou <code>null</code> si aucune paire ne porte ce nom. Cette valeur doit être un nombre (entier ou réel).
	 * @param name Nom (peut être <code>null</code>, et dans ce cas la méthode retourne <code>null</code>).
	 * @return La valeur de la paire portant le nom indiqué, ou <code>null</code> si aucune paire ne porte ce nom.
	 */
	public Number getNumber(String name) {
		return getNumber(name, null);
	}



	/**
	 * Extrait la valeur dont le nom est indiqué de l'objet Json indiqué.
	 * @param name Nom de la valeur à extraire.
	 * @param option Indication de valeur obligatoire.
	 * @return La valeur.
	 */
	public Number getNumber(String name, Option option) {
		JsonNumber jsonValue;
		Number value;
		boolean present;

		checkName(name);
		if (has(name)) {
			present = true;
			try {
				jsonValue = (JsonNumber) pairs.get(name);
				value = (jsonValue != null) ? jsonValue.getValue() : null;
			} catch (ClassCastException exception) {
				throw new JsonRuntimeException("La valeur de \"" + name + "\" n'est pas un nombre: " + serialize());
			}
		} else {
			present = false;
			value = null;
		}
		if (option != null) {
			switch (option) {
			case PRESENT_AND_NOT_NULL_AND_NOT_EMPTY:
			case PRESENT_AND_NOT_NULL:
				if (value == null) {
					throw new JsonRuntimeException("La valeur de \"" + name + "\" est nulle ou manque: " + serialize());
				}
				break;
			case PRESENT:
				if (!present) {
					throw new JsonRuntimeException("La valeur de \"" + name + "\" manque: " + serialize());
				}
				break;
			}
		}
		return value;
	}



	/**
	 * Retourne le nombre de paires nom-valeur de l'objet.
	 * @return Le nombre de paires nom-valeur de l'objet.
	 */
	public int getPairsCount() {
		return pairs.size();

	}



	/**
	 * Retourne la valeur de la paire portant le nom indiqué, ou <code>null</code> si aucune paire ne porte ce nom. Cette valeur doit être une chaine.
	 * @param name Nom (peut être <code>null</code>, et dans ce cas la méthode retourne <code>null</code>).
	 * @return La valeur de la paire portant le nom indiqué, ou <code>null</code> si aucune paire ne porte ce nom.
	 */
	public String getString(String name) {
		return getString(name, null);
	}



	/**
	 * Extrait la valeur dont le nom est indiqué de l'objet Json indiqué.
	 * @param name Nom de la valeur à extraire.
	 * @param option Indication de valeur obligatoire.
	 * @return La valeur.
	 */
	public String getString(String name, Option option) {
		JsonString jsonValue;
		String value;
		boolean present;

		checkName(name);
		if (has(name)) {
			present = true;
			try {
				jsonValue = (JsonString) pairs.get(name);
				value = (jsonValue != null) ? jsonValue.getValue() : null;
			} catch (ClassCastException exception) {
				throw new JsonRuntimeException("La valeur de \"" + name + "\" n'est pas une chaîne: " + serialize());
			}
		} else {
			present = false;
			value = null;
		}
		if (option != null) {
			switch (option) {
			case PRESENT_AND_NOT_NULL_AND_NOT_EMPTY:
				if ((value == null) || value.isEmpty()) {
					throw new JsonRuntimeException("La valeur de \"" + name + "\" est vide ou nulle ou manque: " + serialize());
				}
				break;
			case PRESENT_AND_NOT_NULL:
				if (value == null) {
					throw new JsonRuntimeException("La valeur de \"" + name + "\" est nulle ou manque: " + serialize());
				}
				break;
			case PRESENT:
				if (!present) {
					throw new JsonRuntimeException("La valeur de \"" + name + "\" manque: " + serialize());
				}
				break;
			}
		}
		return value;
	}



	/**
	 * Teste si une paire portant le nom indiqué existe.
	 * @param name Nom (peut être <code>null</code>, et dans ce cas la méthode retourne <code>false</code>).
	 * @return <code>true</code> si une paire portant le nom indiqué existe, <code>false</code> sinon.
	 */
	public boolean has(String name) {
		checkName(name);
		return pairs.containsKey(name);
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((pairs == null) ? 0 : pairs.hashCode());
		return result;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public boolean isEmpty() {
		return pairs.isEmpty();
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public Iterator<Entry<String, Json>> iterator() {
		return pairs.entrySet().iterator();
	}



	/**
	 * Supprime l'éventuelle paire portant le nom indiqué.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @return Cet objet, pour pouvoir chaîner les appels.
	 */
	public JsonObject remove(String name) {
		checkName(name);
		pairs.remove(name);
		return this;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public String serialize(String indentation) {
		StringBuilder builder = new StringBuilder();
		String separator;

		builder.append("{");
		separator = (indentation != null) ? "\n" + indentation + INDENTATION : "";
		for (Entry<String, Json> pair : pairs.entrySet()) {
			builder.append(separator + "\"" + encode(pair.getKey()) + "\":" + ((indentation != null) ? " " : "") + ((pair.getValue() == null) ? "null" : pair.getValue().serialize((indentation != null) ? indentation + INDENTATION : null)));
			separator = (indentation != null) ? ",\n" + indentation + INDENTATION : ",";
		}
		if (indentation != null) {
			builder.append("\n" + indentation);
		}
		builder.append("}");
		return builder.toString();
	}



	/**
	 * Crée ou remplace la paire portant le nom indiqué, avec la valeur <code>null</code>.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @return Cet objet, pour pouvoir chaîner les appels.
	 */
	public JsonObject set(String name) {
		checkName(name);
		pairs.put(name, null);
		return this;
	}



	/**
	 * Crée ou remplace la paire portant le nom indiqué, que la valeur indiquée soit <code>null</code> ou non.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @param value Valeur à ajouter (peut être <code>null</code>).
	 * @return Cet objet, pour pouvoir chaîner les appels.
	 */
	public JsonObject set(String name, Boolean value) {
		checkName(name);
		pairs.put(name, new JsonBoolean(value));
		return this;
	}



	/**
	 * Crée ou remplace la paire portant le nom indiqué, que la valeur indiquée soit <code>null</code> ou non.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @param value Valeur à ajouter (peut être <code>null</code>).
	 * @return Cet objet, pour pouvoir chaîner les appels.
	 */
	public JsonObject set(String name, Json value) {
		checkName(name);
		pairs.put(name, value);
		return this;
	}



	/**
	 * Crée ou remplace la paire portant le nom indiqué, que la valeur indiquée soit <code>null</code> ou non.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @param value Valeur à ajouter (peut être <code>null</code>).
	 * @return Cet objet, pour pouvoir chaîner les appels.
	 */
	public JsonObject set(String name, Number value) {
		checkName(name);
		pairs.put(name, new JsonNumber(value));
		return this;
	}



	/**
	 * Crée ou remplace la paire portant le nom indiqué, que la valeur indiquée soit <code>null</code> ou non.
	 * @param name Nom Json (ne doit pas être <code>null</code>).
	 * @param value Valeur à ajouter (peut être <code>null</code>).
	 * @return Cet objet, pour pouvoir chaîner les appels.
	 */
	public JsonObject set(String name, String value) {
		checkName(name);
		pairs.put(name, new JsonString(value));
		return this;
	}



	/**
	 * Vérifie que le nom indiqué est valide.
	 * @param name Nom.
	 * @throws JsonRuntimeException Si le nom est <code>null</code>.
	 */
	private static void checkName(String name) {
		if (name == null) {
			throw new JsonRuntimeException("Le nom est null.");
		}
	}



	/**
	 * Construit l'objet Json correspondant à la chaîne indiquée.
	 * @param string Objet Json sérialisé.
	 * @return L'élément Json.
	 * @throws JsonException
	 */
	public static JsonObject parse(String string) throws JsonException {
		return (JsonObject) Json.parse(string);
	}



	/**
	 * Paires nom-valeur de l'objet.
	 */
	private final SortedMap<String, Json> pairs = new TreeMap<>();



	/**
	 * La classe {@link Option} recense les niveaux de contraintes pour les méthodes <code>cut...</code> . Par convention, une valeur <code>null</code> signifie qu'il n'y a pas de contrainte.
	 * @author Ludovic WALLE
	 */
	public static enum Option {
		/**  */
		PRESENT,
		/**  */
		PRESENT_AND_NOT_NULL,
		/**  */
		PRESENT_AND_NOT_NULL_AND_NOT_EMPTY,
	}



}
