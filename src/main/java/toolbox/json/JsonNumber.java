package toolbox.json;



/**
 * La classe {@link JsonNumber} spécifie un nombre.
 * @author Ludovic WALLE
 */
public class JsonNumber extends JsonAtomic {



	/**
	 * @param other Autre nombre Json.
	 */
	public JsonNumber(JsonNumber other) {
		this.value = other.value;
	}



	/**
	 * @param value Valeur.
	 */
	public JsonNumber(Number value) {
		this.value = value;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public JsonNumber clone() {
		return new JsonNumber(this);
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
		JsonNumber other = (JsonNumber) obj;
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
	@Override public Number getValue() {
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
	 * {@inheritDoc}
	 */
	@Override public String serialize(String indentation) {
		if (value != null) {
			return value.toString();
		} else {
			return "null";
		}
	}



	/**
	 * Affecte la valeur indiquée.
	 * @param value Valeur (peut être <code>null</code>).
	 */
	public void setValue(Number value) {
		this.value = value;
	}



	/**
	 * Valeur.
	 */
	private Number value = null;



}
