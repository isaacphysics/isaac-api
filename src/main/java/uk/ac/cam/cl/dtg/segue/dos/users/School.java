package uk.ac.cam.cl.dtg.segue.dos.users;

/**
 * School information POJO.
 * 
 */
public class School {
	private String urn;
	private String establishmentNumber;
	private String name;
	private String postcode;

	/**
	 * Default Constructor for mappers.
	 */
	public School() {

	}

	/**
	 * Full constructor.
	 * 
	 * @param urn
	 *            - unique id
	 * @param establishmentNumber
	 *            - unique id for the establishment.
	 * @param name
	 *            - name of the school.
	 * @param postcode
	 *            -postcode of the school
	 */
	public School(final String urn, final String establishmentNumber, final String name,
			final String postcode) {
		this.urn = urn;
		this.establishmentNumber = establishmentNumber;
		this.name = name;
		this.postcode = postcode;
	}

	/**
	 * Gets the urn.
	 * 
	 * @return the urn
	 */
	public String getUrn() {
		return urn;
	}

	/**
	 * Sets the urn.
	 * 
	 * @param urn
	 *            the urn to set
	 */
	public void setUrn(final String urn) {
		this.urn = urn;
	}

	/**
	 * Gets the establishmentNumber.
	 * @return the establishmentNumber
	 */
	public String getEstablishmentNumber() {
		return establishmentNumber;
	}

	/**
	 * Sets the establishmentNumber.
	 * @param establishmentNumber the establishmentNumber to set
	 */
	public void setEstablishmentNumber(final String establishmentNumber) {
		this.establishmentNumber = establishmentNumber;
	}

	/**
	 * Gets the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name.
	 * 
	 * @param name
	 *            the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Gets the postcode.
	 * 
	 * @return the postcode
	 */
	public String getPostcode() {
		return postcode;
	}

	/**
	 * Sets the postcode.
	 * 
	 * @param postcode
	 *            the postcode to set
	 */
	public void setPostcode(final String postcode) {
		this.postcode = postcode;
	}
}
