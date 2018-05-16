package toolbox.json;

import java.util.*;



/**
 * La classe {@link JsonArray} spécifie un tableau.
 * @author Ludovic WALLE
 */
public class JsonArray extends JsonContainer implements Iterable<Json> {



	/**
	 */
	public JsonArray() {}



	/**
	 * @param booleans Chaines.
	 */
	public JsonArray(Boolean... booleans) {
		if (booleans != null) {
			for (Boolean flag : booleans) {
				this.jsons.add(new JsonBoolean(flag));
			}
		}
	}



	/**
	 * @param bytes Octets.
	 */
	public JsonArray(byte... bytes) {
		if (bytes != null) {
			for (int number : bytes) {
				this.jsons.add(new JsonNumber(number));
			}
		}
	}



	/**
	 * @param numbers Nombres.
	 */
	public JsonArray(int... numbers) {
		if (numbers != null) {
			for (int number : numbers) {
				this.jsons.add(new JsonNumber(number));
			}
		}
	}



	/**
	 * @param jsons Elements Json. Les <code>null</code> sont ignorés.
	 */
	public JsonArray(Json... jsons) {
		for (Json json : jsons) {
			if (json != null) {
				this.jsons.add(json);
			}
		}
	}



	/**
	 * @param other Autre tableau Json.
	 */
	public JsonArray(JsonArray other) {
		for (Json json : other.jsons) {
			jsons.add(json.clone());
		}
	}



	/**
	 * @param numbers Nombres.
	 */
	public JsonArray(long... numbers) {
		if (numbers != null) {
			for (long number : numbers) {
				this.jsons.add(new JsonNumber(number));
			}
		}
	}



	/**
	 * @param strings Chaines.
	 */
	public JsonArray(String... strings) {
		if (strings != null) {
			for (String string : strings) {
				this.jsons.add(new JsonString(string));
			}
		}
	}



	/**
	 * Insère la valeur indiquée en dernière position.
	 * @param value Valeur à ajouter.
	 * @return Ce tableau, pour pouvoir chainer les appels.
	 */
	public JsonArray append(Boolean value) {
		jsons.add(new JsonBoolean(value));
		return this;
	}



	/**
	 * Insère la valeur indiquée en dernière position.
	 * @param json Element à ajouter.
	 * @return Ce tableau, pour pouvoir chainer les appels.
	 */
	public JsonArray append(Json json) {
		jsons.add(json);
		return this;
	}



	/**
	 * Insère la valeur indiquée en dernière position.
	 * @param value Valeur à ajouter.
	 * @return Ce tableau, pour pouvoir chainer les appels.
	 */
	public JsonArray append(Number value) {
		jsons.add(new JsonNumber(value));
		return this;
	}



	/**
	 * Insère la valeur indiquée en dernière position.
	 * @param value Valeur à ajouter.
	 * @return Ce tableau, pour pouvoir chainer les appels.
	 */
	public JsonArray append(String value) {
		jsons.add(new JsonString(value));
		return this;
	}



	/**
	 * Supprime tous les éléments du tableau.
	 * @return Ce tableau, pour pouvoir chainer les appels.
	 */
	public JsonArray clear() {
		jsons.clear();
		return this;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public JsonArray clone() {
		return new JsonArray(this);
	}



	/**
	 * Coupe l'élément à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return La valeur coupée (peut être <code>null</code>).
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 */
	public Json cut(int index) {
		return jsons.remove(index);
	}



	/**
	 * Coupe l'élément à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return La valeur coupée (peut être <code>null</code>).
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 * @throws ClassCastException Si la valeur n'est pas un booléen.
	 */
	public Boolean cutBoolean(int index) {
		JsonBoolean element;

		element = (JsonBoolean) jsons.remove(index);
		return (element != null) ? (Boolean) element.getValue() : null;
	}



	/**
	 * Coupe l'élément à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return La valeur coupée (peut être <code>null</code>).
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 * @throws ClassCastException Si la valeur n'est pas un nombre.
	 */
	public Double cutDouble(int index) {
		JsonNumber element;

		element = (JsonNumber) jsons.remove(index);
		return (element != null) ? element.getValue().doubleValue() : null;
	}



	/**
	 * Coupe l'élément à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return La valeur coupée (peut être <code>null</code>).
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 * @throws ClassCastException Si la valeur n'est pas un nombre.
	 */
	public Long cutInteger(int index) {
		JsonNumber element;

		element = (JsonNumber) jsons.remove(index);
		return (element != null) ? element.getValue().longValue() : null;
	}



	/**
	 * Coupe l'élément à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return La valeur coupée (peut être <code>null</code>).
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 * @throws ClassCastException Si la valeur n'est pas tableau Json.
	 */
	public JsonArray cutJsonArray(int index) {
		return (JsonArray) jsons.remove(index);
	}



	/**
	 * Coupe l'élément à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return La valeur coupée (peut être <code>null</code>).
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 * @throws ClassCastException Si la valeur n'est pas un objet Json.
	 */
	public JsonObject cutJsonObject(int index) {
		return (JsonObject) jsons.remove(index);
	}



	/**
	 * Coupe l'élément à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return La valeur coupée (peut être <code>null</code>).
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 * @throws ClassCastException Si la valeur n'est pas un nombre.
	 */
	public Number cutNumber(int index) {
		JsonNumber element;

		element = (JsonNumber) jsons.remove(index);
		return (element != null) ? element.getValue() : null;
	}



	/**
	 * Coupe l'élément à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return La valeur coupée (peut être <code>null</code>).
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 * @throws ClassCastException Si la valeur n'est pas une chaîne.
	 */
	public String cutString(int index) {
		JsonString element;

		element = (JsonString) jsons.remove(index);
		return (element != null) ? element.getValue() : null;
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
		JsonArray other = (JsonArray) obj;
		if (jsons == null) {
			if (other.jsons != null) {
				return false;
			}
		} else if (!jsons.equals(other.jsons)) {
			return false;
		}
		return true;
	}



	/**
	 * Retourne l'élément à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return L'élément à l'index indiqué (peut être <code>null</code>).
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 */
	public Json get(int index) {
		return jsons.get(index);
	}



	/**
	 * Retourne la valeur à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return L'élément à l'index indiqué (peut être <code>null</code>).
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 * @throws ClassCastException Si la valeur n'est pas un booléen.
	 */
	public Boolean getBoolean(int index) {
		JsonBoolean element;

		element = (JsonBoolean) jsons.get(index);
		return (element != null) ? (Boolean) element.getValue() : null;
	}



	/**
	 * Retourne la valeur à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return L'élément à l'index indiqué (peut être <code>null</code>).
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 * @throws ClassCastException Si la valeur n'est pas un nombre.
	 */
	public Double getDouble(int index) {
		JsonNumber element;

		element = (JsonNumber) jsons.get(index);
		return (element != null) ? element.getValue().doubleValue() : null;
	}



	/**
	 * Retourne la valeur à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return L'élément à l'index indiqué (peut être <code>null</code>).
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 * @throws ClassCastException Si la valeur n'est pas un nombre.
	 */
	public Long getInteger(int index) {
		JsonNumber element;

		element = (JsonNumber) jsons.get(index);
		return (element != null) ? element.getValue().longValue() : null;
	}



	/**
	 * Retourne la valeur à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return L'élément à l'index indiqué (peut être <code>null</code>).
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 * @throws ClassCastException Si la valeur n'est pas tableau Json.
	 */
	public JsonArray getJsonArray(int index) {
		return (JsonArray) jsons.get(index);
	}



	/**
	 * Retourne la valeur à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return L'élément à l'index indiqué (peut être <code>null</code>).
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 * @throws ClassCastException Si la valeur n'est pas un objet Json.
	 */
	public JsonObject getJsonObject(int index) {
		return (JsonObject) jsons.get(index);
	}



	/**
	 * Retourne la valeur à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return L'élément à l'index indiqué (peut être <code>null</code>).
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 * @throws ClassCastException Si la valeur n'est pas un nombre.
	 */
	public Number getNumber(int index) {
		JsonNumber element;

		element = (JsonNumber) jsons.get(index);
		return (element != null) ? element.getValue() : null;
	}



	/**
	 * Retourne la valeur à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return L'élément à l'index indiqué (peut être <code>null</code>).
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 * @throws ClassCastException Si la valeur n'est pas une chaîne.
	 */
	public String getString(int index) {
		JsonString element;

		element = (JsonString) jsons.get(index);
		return (element != null) ? element.getValue() : null;
	}



	/**
	 * Retourne un tableau contenant les éléments Json.<br>
	 * Le tableau retourné est une copie.
	 * @return Un tableau contenant les éléments Json, jamais <code>null</code>.
	 */
	public Json[] getValues() {
		return jsons.toArray(new Json[jsons.size()]);
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((jsons == null) ? 0 : jsons.hashCode());
		return result;
	}



	/**
	 * Insêre la valeur indiquée à la position indiquée.
	 * @param index Index (doit être dans le tableau ou juste après la fin du tableau).
	 * @param value Valeur à ajouter.
	 * @return Ce tableau, pour pouvoir chainer les appels.
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau et pas non plus juste après la fin du tableau.
	 */
	public JsonArray insert(Boolean value, int index) {
		jsons.add(index, new JsonBoolean(value));
		return this;
	}



	/**
	 * Insère la valeur indiqué à la position indiquée.
	 * @param index Index (doit être dans le tableau ou juste après la fin du tableau).
	 * @param json Element à ajouter.
	 * @return Ce tableau, pour pouvoir chainer les appels.
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau et pas non plus juste après la fin du tableau.
	 */
	public JsonArray insert(Json json, int index) {
		jsons.add(index, json);
		return this;
	}



	/**
	 * Insère la valeur indiquée à la position indiquée.
	 * @param index Index (doit être dans le tableau ou juste après la fin du tableau).
	 * @param value Valeur à ajouter.
	 * @return Ce tableau, pour pouvoir chainer les appels.
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau et pas non plus juste après la fin du tableau.
	 */
	public JsonArray insert(Number value, int index) {
		jsons.add(index, new JsonNumber(value));
		return this;
	}



	/**
	 * Insère la valeur indiquée à la position indiquée.
	 * @param index Index (doit être dans le tableau ou juste après la fin du tableau).
	 * @param value Valeur à ajouter.
	 * @return Ce tableau, pour pouvoir chainer les appels.
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau et pas non plus juste après la fin du tableau.
	 */
	public JsonArray insert(String value, int index) {
		jsons.add(index, new JsonString(value));
		return this;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public boolean isEmpty() {
		return jsons.isEmpty();
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public Iterator<Json> iterator() {
		return jsons.iterator();
	}



	/**
	 * Supprime l'élément à l'index indiqué.
	 * @param index Index (doit être dans le tableau).
	 * @return Ce tableau, pour pouvoir chainer les appels.
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 */
	public JsonArray remove(int index) {
		jsons.remove(index);
		return this;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public String serialize(String indentation) {
		StringBuilder builder = new StringBuilder();
		String separator;
		Json previous = null;
		boolean first = true;
		boolean hasNonAtomic = false;

		builder.append("[");
		for (Json json : jsons) {
			if ((json != null) && (!(json instanceof JsonAtomic))) {
				hasNonAtomic = true;
				break;
			}
		}
		separator = (hasNonAtomic && (indentation != null)) ? "\n" + indentation + INDENTATION : "";
		for (Json json : jsons) {
			builder.append(separator);
			separator = ",";
			if (hasNonAtomic && (indentation != null)) {
				if (first) {
					first = false;
				} else if ((previous instanceof JsonObject) && (json instanceof JsonObject)) {
					builder.append(" ");
				} else {
					builder.append("\n" + indentation + INDENTATION);
				}
				previous = json;
			}
			builder.append((json == null) ? "null" : json.serialize((indentation != null) ? indentation + INDENTATION : null));
		}
		if ((indentation != null) && !first) {
			builder.append("\n" + indentation);
		}
		builder.append("]");
		return builder.toString();
	}



	/**
	 * Spécifie la valeur à la position indiquée.
	 * @param index Index (doit être dans le tableau).
	 * @param value Valeur de l'élément (peut être <code>null</code>).
	 * @return Ce tableau, pour pouvoir chainer les appels.
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 */
	public JsonArray set(Boolean value, int index) {
		jsons.set(index, new JsonBoolean(value));
		return this;
	}



	/**
	 * Spécifie la valeur à la position indiquée.
	 * @param index Index (doit être dans le tableau).
	 * @param json Valeur de l'élément (peut être <code>null</code>).
	 * @return Ce tableau, pour pouvoir chainer les appels.
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 */
	public JsonArray set(Json json, int index) {
		jsons.set(index, json);
		return this;
	}



	/**
	 * Spécifie la valeur à la position indiquée.
	 * @param index Index (doit être dans le tableau).
	 * @param value Valeur de l'élément (peut être <code>null</code>).
	 * @return Ce tableau, pour pouvoir chainer les appels.
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 */
	public JsonArray set(Number value, int index) {
		jsons.set(index, new JsonNumber(value));
		return this;
	}



	/**
	 * Spécifie la valeur à la position indiquée.
	 * @param index Index (doit être dans le tableau).
	 * @param value Valeur de l'élément (peut être <code>null</code>).
	 * @return Ce tableau, pour pouvoir chainer les appels.
	 * @throws ArrayIndexOutOfBoundsException Si l'index est en dehors du tableau.
	 */
	public JsonArray set(String value, int index) {
		jsons.set(index, new JsonString(value));
		return this;
	}



	/**
	 * Retourne le nombre d'éléments dans le tableau.
	 * @return Le nombre d'éléments dans le tableau.
	 */
	public int size() {
		return jsons.size();
	}



	/**
	 * Construit l'objet Json correspondant à la chaine indiquée.
	 * @param string Objet Json sérialisé.
	 * @return L'élément Json.
	 * @throws JsonException
	 */
	public static JsonArray parse(String string) throws JsonException {
		return (JsonArray) Json.parse(string);
	}



	/**
	 * Contenu du tableau.
	 */
	private final Vector<Json> jsons = new Vector<>();



}
