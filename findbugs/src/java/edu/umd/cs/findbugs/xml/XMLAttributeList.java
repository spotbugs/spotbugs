/*
 * XML input/output support for FindBugs
 * Copyright (C) 2004, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.xml;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Helper class to format attributes in an XML tag.
 *
 * @author David Hovemeyer
 */
public class XMLAttributeList {
	public static class NameValuePair {
		private String name;
		private String value;

		public NameValuePair(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}
	}

	private static class StringBufferQuoteMetaCharacters extends QuoteMetaCharacters {
		private StringBuffer buf;

		public StringBufferQuoteMetaCharacters(String text, MetaCharacterMap map,
			StringBuffer buf) {
			super(text, map);
			this.buf = buf;
		}

		@Override
				 public void process() {
			try {
				super.process();
			} catch (java.io.IOException e) {
				// This can't actually happen - we're writing to a StringBuffer
			}
		}

		@Override
				 public void emitLiteral(String s) {
			buf.append(s);
		}
	}

	// The "<", ">", and "&" must be escaped in
	// attribute values.  I have no idea why this is useful,
	// but that's standards for you.
	private static final MetaCharacterMap attrMetaCharacterMap = new MetaCharacterMap();
	static {
		attrMetaCharacterMap.addMeta('<', "&lt;");
		attrMetaCharacterMap.addMeta('>', "&gt;");
		attrMetaCharacterMap.addMeta('&', "&amp;");
		attrMetaCharacterMap.addMeta('"', "&quot;");
	}

	// Fields
	private List<NameValuePair> nameValuePairList;

	/**
	 * Constructor.
	 * Creates an empty object.
	 */
	public XMLAttributeList() {
		this.nameValuePairList = new LinkedList<NameValuePair>();
	}

	/**
	 * Add a single attribute name and value.
	 *
	 * @param name  the attribute name
	 * @param value the attribute value
	 * @return this object (so calls to addAttribute() can be chained)
	 */
	public XMLAttributeList addAttribute(@NonNull String name, @NonNull String value) {
		if (name == null) 
			throw new NullPointerException("name must be nonnull");
		if (value == null) 
			throw new NullPointerException("value must be nonnull");
		nameValuePairList.add(new NameValuePair(name, value));
		return this;
	}

	/**
	 * Add a single attribute name and value.
	 *
	 * @param name  the attribute name
	 * @param value the attribute value
	 * @return this object (so calls to addAttribute() can be chained)
	 */
	public XMLAttributeList addOptionalAttribute(@NonNull String name, @CheckForNull String value) {
		if (value == null) return this;
		return addAttribute(name,value);
	}

	/**
	 * Return the attribute list as a String which can be
	 * directly output as part of an XML tag.
	 */
	@Override
		 public String toString() {
		StringBuffer buf = new StringBuffer();
		for (NameValuePair pair : nameValuePairList) {
			buf.append(' ');
			buf.append(pair.getName());
			buf.append('=');
			buf.append(getQuotedAttributeValue(pair.getValue()));
		}
		return buf.toString();
	}

	/**
	 * Return an Iterator over NameValuePairs.
	 */
	public Iterator<NameValuePair> iterator() {
		return nameValuePairList.iterator();
	}

	/**
	 * Return a properly quoted form for an attribute value.
	 * @param rawValue the raw value of the attribute
	 * @return a properly quoted representation of the value
	 */
	public static String getQuotedAttributeValue(@NonNull String rawValue) {
		if (rawValue == null) throw new NullPointerException("rawValue must be nonnull");
		StringBuffer buf = new StringBuffer();
		buf.append('"');
		new StringBufferQuoteMetaCharacters(rawValue, attrMetaCharacterMap, buf).process();
		buf.append('"');
		return buf.toString();
	}
}

// vim:ts=4
