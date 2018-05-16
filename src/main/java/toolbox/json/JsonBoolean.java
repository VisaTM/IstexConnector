package toolbox.json;



/**
 * La classe {@link JsonBoolean} spécifie un booléen.
 * @author Ludovic WALLE
 */
public class JsonBoolean extends JsonAtomic {



	/**	 */
	public JsonBoolean() {}



	/**
	 * @param value Valeur.
	 */
	public JsonBoolean(Boolean value) {
		this.value = value;
	}



	/**
	 * @param other Autre booléen.
	 */
	public JsonBoolean(JsonBoolean other) {
		this.value = other.value;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public JsonBoolean clone() {
		return new JsonBoolean(this);
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null) {
			return false;
		}
		if (getClass() != object.getClass()) {
			return false;
		}
		JsonBoolean other = (JsonBoolean) object;
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
	@Override public Boolean getValue() {
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
	 * @return <code>true</code> si la valeur est non nulle et fausse, <code>false</code> sinon.
	 */
	public boolean isFalse() {
		return (value != null) && !value;
	}



	/**
	 * @return <code>true</code> si la valeur est fausse ou nulle, <code>false</code> sinon.
	 */
	public boolean isFalseOrNull() {
		return (value == null) || !value;
	}



	/**
	 * @return <code>true</code> si la valeur est non nulle et vraie, <code>false</code> sinon.
	 */
	public boolean isTrue() {
		return (value != null) && value;
	}



	/**
	 * @return <code>true</code> si la valeur est vraie ou nulle, <code>false</code> sinon.
	 */
	public boolean isTrueOrNull() {
		return (value == null) || value;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override public String serialize(String indentation) {
		if (value != null) {
			return value ? "true" : "false";
		} else {
			return "null";
		}
	}



	/**
	 * Affecte la valeur indiquée.
	 * @param value Valeur (peut être <code>null</code>).
	 */
	public void setValue(Boolean value) {
		this.value = value;
	}



	/**
	 * Valeur Json.
	 */
	private Boolean value;



}
