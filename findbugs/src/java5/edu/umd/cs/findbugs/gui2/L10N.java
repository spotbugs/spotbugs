/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;

import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.gui.AnnotatedString;

public class L10N {
	private static final boolean DEBUG = SystemProperties.getBoolean("i18n.debug");
	private static ResourceBundle bundle;
	private static ResourceBundle bundle_en;

	private static Map<String,String> reverseMap = new HashMap<String,String>();
	static {
		try {
			bundle = ResourceBundle.getBundle("edu.umd.cs.findbugs.gui2.bundle.findbugs", I18N.defaultLocale);
			bundle_en = ResourceBundle.getBundle("edu.umd.cs.findbugs.gui2.bundle.findbugs", Locale.ENGLISH);

			for(Enumeration<String> e = bundle.getKeys(); e.hasMoreElements(); ) {
				String k = e.nextElement();
				Object v = bundle.getObject(k);
				if (v instanceof String && !reverseMap.containsKey(v))
					reverseMap.put((String)v, k);
			}
			
				
		} catch (MissingResourceException mre) {
			bundle = null;
		}
	}

	private L10N() {
	}

	public static String translate(String english) {
		String key = reverseMap.get(english);
		return getLocalString(key, english);
		
	}
	private static  String lookup(ResourceBundle b, String key) {
		if (b == null || key == null ) throw new MissingResourceException(null,null,null);
		
		return bundle.getString(key);
	}
	public static String getLocalString(String key, String defaultString) {
		if (key == null) return "TRANSLATE("+defaultString+")";
		try {
			return lookup(bundle, key);
		} catch (MissingResourceException mre) {
			try {
				String en = lookup(bundle_en, key);
				if (DEBUG) return "TRANSLATE("+en+")";
				else return en;
			} catch (MissingResourceException mre2) {
				String en = defaultString;
				if (DEBUG) return "TRANSLATE("+en+")";
				else return en;
		}
		}
	}

	/**
	 * Localise the given AbstractButton, setting the text and optionally mnemonic
	 * Note that AbstractButton includes menus and menu items.
	 * @param button		The button to localise
	 * @param key		   The key to look up in resource bundle
	 * @param defaultString default String to use if key not found
	 * @param setMnemonic	whether or not to set the mnemonic. According to Sun's
	 *					  guidelines, default/cancel buttons should not have mnemonics
	 *					  but instead should use Return/Escape
	 */
	public static void localiseButton(AbstractButton button, String key, String defaultString,
								boolean setMnemonic) {
		AnnotatedString as = new AnnotatedString(getLocalString(key, defaultString));
		button.setText(as.toString());
		int mnemonic;
		if (setMnemonic &&
			(mnemonic = as.getMnemonic()) != KeyEvent.VK_UNDEFINED) {
			button.setMnemonic(mnemonic);
			button.setDisplayedMnemonicIndex(as.getMnemonicIndex());
		}
	}
}
