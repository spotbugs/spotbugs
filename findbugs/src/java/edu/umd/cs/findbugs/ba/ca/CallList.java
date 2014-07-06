/*
 * Bytecode Analysis Framework
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
package edu.umd.cs.findbugs.ba.ca;

import java.util.ArrayList;
import java.util.Iterator;

public class CallList {
    private boolean isTop, isBottom;

    private final ArrayList<Call> callList;

    public CallList() {
        this.callList = new ArrayList<Call>();
    }

    public boolean isValid() {
        return !(isTop() || isBottom());
    }

    public Iterator<Call> callIterator() {
        return callList.iterator();
    }

    public boolean isTop() {
        return isTop;
    }

    public boolean isBottom() {
        return isBottom;
    }

    public void setTop() {
        this.isTop = true;
        this.isBottom = false;
        this.callList.clear();
    }

    public void setBottom() {
        this.isTop = false;
        this.isBottom = true;
        this.callList.clear();
    }

    public void clear() {
        this.isTop = this.isBottom = false;
        this.callList.clear();
    }

    public void add(Call call) {
        callList.add(call);
    }

    public int size() {
        return callList.size();
    }

    public Call get(int index) {
        return callList.get(index);
    }

    public void copyFrom(CallList other) {
        this.isTop = other.isTop;
        this.isBottom = other.isBottom;
        this.callList.clear();
        this.callList.addAll(other.callList);
    }

    public static CallList merge(CallList a, CallList b) {
        CallList result = new CallList();

        if (a.isBottom || b.isBottom) {
            result.isBottom = true;
        } else if (a.isTop) {
            result.copyFrom(b);
        } else if (b.isTop) {
            result.copyFrom(a);
        } else {
            // Result is the common prefix
            int len = Math.min(a.size(), b.size());
            for (int i = 0; i < len; ++i) {
                if (!a.get(i).equals(b.get(i))) {
                    break;
                }
                result.add(a.get(i));
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        CallList other = (CallList) obj;
        return this.callList.equals(other.callList);
    }

    @Override
    public int hashCode() {
        return callList.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (Call call : callList) {
            if (buf.length() > 0) {
                buf.append(',');
            }
            buf.append(call.getMethodName());
        }
        return buf.toString();
    }
}
