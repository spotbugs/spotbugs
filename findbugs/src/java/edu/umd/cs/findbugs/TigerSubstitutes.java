/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This class provides substitutes for 1.5 methods that we don't want to depend upon in
 * FindBugs.
 * 
 */
public class TigerSubstitutes {
    
    @SuppressWarnings("unchecked")
    public static final <T> List<T> emptyList() {
         return (List<T>) Collections.EMPTY_LIST;
     }
   @SuppressWarnings("unchecked")
   public static final <T> Set<T> emptySet() {
        return (Set<T>) Collections.EMPTY_SET;
    }

	public static boolean parseBoolean(String s) {
		// return Boolean.parseBoolean(s);
		return Boolean.valueOf(s).booleanValue();
	}
	public static Long valueOf(long value) {
		// return Long.valueOf(value);
		return  (Long) value;
	}
    
    /**
     * Copied from java.util.Arrays;
     * 
     */
      public static int hashCode(Object a[]) {
            if (a == null)
                return 0;

            int result = 1;

            for (Object element : a)
                result = 31 * result + (element == null ? 0 : element.hashCode());

            return result;
        }

      public static <U> Class<? extends U> asSubclass(Class base, Class<U> clazz) {
          if (clazz.isAssignableFrom(base))
              return (Class<? extends U>) base;
          else
              throw new ClassCastException(base.toString());
      }
      
      public static String getSimpleName(Class c) {
              if (c.isArray())
                  return getSimpleName(c.getComponentType())+"[]";
              String name = c.getName();
             name =  name.substring(name.lastIndexOf(".")+1);
             int index = name.lastIndexOf("$");
             if (index == -1) return name;
             if (index +1 ==name.length() )
                 throw new InternalError("Malformed class name");
             if (isAsciiDigit(name.charAt(index+1))) return "";
             return name.substring(index+1);
      }
      private static boolean isAsciiDigit(char c) {
          return '0' <= c && c <= '9';
      }
      
      public static String getTextContent(Node n) {
          NodeList nodeList= n.getChildNodes();
          StringBuffer buf = new StringBuffer();
          for (int j=0; j < nodeList.getLength(); j++) {
              Node k = nodeList.item(j);
              buf.append( k.getNodeValue()); 
          }
          return buf.toString();
      }


}
