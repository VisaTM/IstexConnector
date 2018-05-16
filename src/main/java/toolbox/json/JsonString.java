package toolbox.json;



/**
 * La classe {@link JsonString} spécifie une chaine.
 * @author Ludovic WALLE
 */
public class JsonString extends JsonAtomic {



	/**
	 * @param other Autre chaine Json.
	 */
	public JsonString(JsonString other) {
		this.value = other.value;
	}



	/**
	 * @param value Valeur.
	 */
	public JsonString(String value) {
		this.value = value;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public JsonString clone() {
		return new JsonString(this);
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
		JsonString other = (JsonString) obj;
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public String getValue() {
		return value;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((value == null) ? 0 : value.hashCode());
		return result;
	}



	/**
	 * {@inheritDoc} Les # doivent être encodés pour éviter les interférences avec les templates Kendo.
	 */
	@Override public String serialize(String indentation) {
		if (value != null) {
			return "\"" + encode(value) + "\"";
		} else {
			return "null";
		}
	}



	/**
	 * Affecte la valeur indiquée.
	 * @param value Valeur (peut être <code>null</code>).
	 */
	public void setValue(String value) {
		this.value = value;
	}



	/**
	 * Valeur.
	 */
	private String value = null;



}
