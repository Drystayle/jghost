package ch.drystayle.jghost.db;

import java.io.Serializable;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * An abstract base class for all persistent classes that provides the id for
 * the primary key.
 *
 * @param <T> the type of the identifier for objects of this class. It is the
 *            type of the primary key for objects of this class.
 */
@MappedSuperclass
public abstract class BaseEntity<T extends Serializable>
	implements Serializable
{

	//---- Static

	/**
	 * A serial version UID to control the compatibility of serialized instances
	 * of this class.
	 */
	private static final long serialVersionUID= -1703016165533300089L;

	//---- Fields
	
	/**
	 * The primary key of the object.
	 *
	 * @see #getId()
	 * @see #setId(Serializable)
	 */
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private T id;
	/** Field name. */
	public static final String FN_ID= "ID";
	
	//---- Constructors

	/**
	 * Create a new <code>BaseEntity</code>.
	 */
	public BaseEntity () {
		super();
	}

	//---- Methods

	/**
	 * Returns the primary key of the object.
	 *
	 * @return the primary key of the object.
	 * @see #setId(Serializable)
	 */
	public T getId () {
		return this.id;
	}

	/**
	 * Sets the primary key of the object.
	 *
	 * @param id the primary key of the object.
	 * @see #getId()
	 */
	public void setId (T id) {
		this.id= id;
	}

	/**
	 * Returns a string representation of this <code>BaseEntity</code> suitable
	 * for debugging purposes.
	 *
	 * @return a string representation of this <code>BaseEntity</code>.
	 */
	@Override
	public String toString () {
		return super.toString();
	}

	/**
	 * Checks whether the specified parameter is not <code>null</code> and
	 * throws an exception if it is.
	 *
	 * @param parameter the parameter to check.
	 * @param paramName the name of the parameter to check. This name is used in
	 *                  the {@link IllegalArgumentException} thrown if the
	 *                  parameter is <code>null</code>.
	 * @throws IllegalArgumentException if the specified parameter is
	 * <code>null</code>.
	 */
	protected void assertNotNull (Object parameter, String paramName)
		throws IllegalArgumentException
	{
		if (parameter == null) {
			throw new IllegalArgumentException("The parameter '" + paramName
				+ "' may not be null.");
		}
	}
	
	/**
	 * Always returns <code>true</code> since this class currently has no
	 * properties to be checked for integrity.
	 *
	 * @return returns <code>true</code>.
	 */
	public boolean checkIntegrity () {
		// Empty implementation to satisfy interface.
		// Subclasses should override this method.
		return true;
	}
}
