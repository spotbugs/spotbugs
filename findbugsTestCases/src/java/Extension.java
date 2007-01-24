/*
 * @(#)Extension.java	1.19 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import java.io.IOException;
import java.util.Arrays;

import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

/**
 * Represent a X509 Extension Attribute.
 * 
 * <p>
 * Extensions are additional attributes which can be inserted in a X509 v3
 * certificate. For example a "Driving License Certificate" could have the
 * driving license number as a extension.
 * 
 * <p>
 * Extensions are represented as a sequence of the extension identifier (Object
 * Identifier), a boolean flag stating whether the extension is to be treated as
 * being critical and the extension value itself (this is again a DER encoding
 * of the extension value).
 * 
 * <pre>
 *  ASN.1 definition of Extension:
 *  Extension ::= SEQUENCE {
 * 	ExtensionId	OBJECT IDENTIFIER,
 * 	critical	BOOLEAN DEFAULT FALSE,
 * 	extensionValue	OCTET STRING
 *  }
 * </pre>
 * 
 * All subclasses need to implement a constructor of the form
 * 
 * <pre>
 *      &lt;subclass&gt; (Boolean, Object)
 *  &lt;pre&gt;
 *  where the Object is typically an array of DER encoded bytes.
 * <p>
 *  @author Amit Kapoor
 *  @author Hemma Prafullchandra
 *  @version 1.19
 * 
 */
public class Extension {
	protected ObjectIdentifier extensionId = null;

	protected boolean critical = false;

	protected byte[] extensionValue = null;

	/**
	 * Default constructor. Used only by sub-classes.
	 */
	public Extension() {
	}

	/**
	 * Constructs an extension from a DER encoded array of bytes.
	 */
	public Extension(DerValue derVal) throws IOException {

		DerInputStream in = derVal.toDerInputStream();

		// Object identifier
		extensionId = in.getOID();

		// If the criticality flag was false, it will not have been encoded.
		DerValue val = in.getDerValue();
		if (val.tag == DerValue.tag_Boolean) {
			critical = val.getBoolean();

			// Extension value (DER encoded)
			val = in.getDerValue();
			extensionValue = val.getOctetString();
		} else {
			critical = false;
			extensionValue = val.getOctetString();
		}
	}

	/**
	 * Constructs an Extension from individual components of ObjectIdentifier,
	 * criticality and the DER encoded OctetString.
	 * 
	 * @param extensionId
	 *            the ObjectIdentifier of the extension
	 * @param critical
	 *            the boolean indicating if the extension is critical
	 * @param extensionValue
	 *            the DER encoded octet string of the value.
	 */
	public Extension(ObjectIdentifier extensionId, boolean critical, byte[] extensionValue)
			throws IOException {
		this.extensionId = extensionId;
		this.critical = critical;
		// passed in a DER encoded octet string, strip off the tag
		// and length
		DerValue inDerVal = new DerValue(extensionValue);
		this.extensionValue = inDerVal.getOctetString();
	}

	/**
	 * Constructs an Extension from another extension. To be used for creating
	 * decoded subclasses.
	 * 
	 * @param ext
	 *            the extension to create from.
	 */
	public Extension(Extension ext) {
		this.extensionId = ext.extensionId;
		this.critical = ext.critical;
		this.extensionValue = ext.extensionValue;
	}

	/**
	 * Write the extension to the DerOutputStream.
	 * 
	 * @param out
	 *            the DerOutputStream to write the extension to.
	 * @exception IOException
	 *                on encoding errors
	 */
	public void encode(DerOutputStream out) throws IOException {

		if (extensionId == null)
			throw new IOException("Null OID to encode for the extension!");
		if (extensionValue == null)
			throw new IOException("No value to encode for the extension!");

		DerOutputStream dos = new DerOutputStream();

		dos.putOID(extensionId);
		if (critical)
			dos.putBoolean(critical);
		dos.putOctetString(extensionValue);

		out.write(DerValue.tag_Sequence, dos);
	}

	/**
	 * Returns true if extension is critical.
	 */
	public boolean isCritical() {
		return (critical);
	}

	/**
	 * Returns the ObjectIdentifier of the extension.
	 */
	public ObjectIdentifier getExtensionId() {
		return (extensionId);
	}

	/**
	 * Returns the extension value as an byte array for further processing.
	 * Note, this is the raw DER value of the extension, not the DER encoded
	 * octet string which is in the certificate.
	 */
	public byte[] getExtensionValue() {
		if (extensionValue == null)
			return null;

		byte[] dup = new byte[extensionValue.length];
		System.arraycopy(extensionValue, 0, dup, 0, dup.length);
		return (dup);
	}

	/**
	 * Returns the Extension in user readable form.
	 */
	@Override
    public String toString() {
		String s = "ObjectId: " + extensionId.toString();
		if (critical) {
			s += " Criticality=true\n";
		} else {
			s += " Criticality=false\n";
		}
		return (s);
	}

	// Value to mix up the hash
	private static final int hashMagic = 31;

	/**
	 * Returns a hashcode value for this Extension.
	 * 
	 * @return the hashcode value.
	 */
	@Override
    public int hashCode() {
		int h = 0;
		if (extensionValue != null) {
			byte[] val = extensionValue;
			int len = val.length;
			while (len > 0)
				h += len * val[--len];
		}
		h = h * hashMagic + extensionId.hashCode();
		h = h * hashMagic + (critical ? 1231 : 1237);
		return h;
	}

	/**
	 * Compares this Extension for equality with the specified object. If the
	 * <code>other</code> object is an
	 * <code>instanceof</code> <code>Extension</code>, then its encoded
	 * form is retrieved and compared with the encoded form of this Extension.
	 * 
	 * @param other
	 *            the object to test for equality with this Extension.
	 * @return true iff the other object is of type Extension, and the
	 *         criticality flag, object identifier and encoded extension value
	 *         of the two Extensions match, false otherwise.
	 */
	@Override
    public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Extension))
			return false;
		Extension otherExt = (Extension) other;
		if (critical != otherExt.critical)
			return false;
		if (!extensionId.equals(otherExt.extensionId))
			return false;
		return Arrays.equals(extensionValue, otherExt.extensionValue);
	}
}
