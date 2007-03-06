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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

/**
 * A list with ArrayList's fast add and set operations,
 * and HashMap's fast contains and indexOf() operations.  The
 * tradeoff is an O(n) remove. 
 */
public class HashList<E> extends ArrayList<E>
{
	private static final long serialVersionUID = 6710532766397389391L;

	// Map from hashcodes to sets of indices in the ArrayList
	private HashMap<Integer, TreeSet<Integer>> map = new HashMap<Integer, TreeSet<Integer>>(); 
	
	public HashList()
	{
		super();
	}
	
	public HashList(Collection<? extends E> c)
	{
		super();
		addAll(c);
	}

	@Override
	public boolean add(E o)
	{
		add(size(), o);
		return true;
	}
	
	@Override
	public void add(int index, E element)
	{
		addToMap(element, index);
		super.add(index, element);
	}
	
	@Override
	public boolean addAll(Collection<? extends E> c)
	{
		for (E i : c)
			add(i);
		return (c.size() > 0);
	}
	
	@Override
	public boolean addAll(int index, Collection<? extends E> c)
	{
		int offset = 0;
		for (E i : c)
		{
			add(index + offset, i);
			offset++;
		}
		return (c.size() > 0);
	}	
	
	@Override
	public void clear()
	{
		super.clear();
		map.clear();
	}
	
	@Override
	public boolean contains(Object elem)
	{
		return (indexOf(elem) != -1);
	}
	
	@Override
	public boolean containsAll(Collection<?> c)
	{
		for (Object i : c)
			if (!contains(i))
				return false;
		return true;
	}
	
	@Override
	public int indexOf(Object elem)
	{
		Set<Integer> s=map.get(elem.hashCode());
		if (s==null)
			return -1;
		for (Integer i : s)
			if (get(i).equals(elem))
				return i;
		return -1;
	}
	
	@Override
	public int lastIndexOf(Object elem)
	{
		int result = -1;
		for (Integer i : map.get(elem.hashCode()))
			if (get(i).equals(elem))
				result = i;
		return result;
	}
	
	@Override
	public E remove(int index)
	{
		E result = super.remove(index);
		removeFromMap(result, index);
		return result;
	}
	
	@Override
	public boolean remove(Object o)
	{
		if (super.remove(o))
		{
			removeFromMap((E) o, indexOf(o));
			return true;
		}
		else
			return false;
	}
	
	@Override
	public boolean removeAll(Collection<?> c)
	{
		boolean result = false;
		for (Object i : c)
			result |= remove(i);
		return result;
	}
	
	@Override
	public boolean retainAll(Collection<?> c)
	{
		boolean result = false;
        for(Iterator<E> iterator = this.iterator(); iterator.hasNext(); ) {
            E i = iterator.next();
            if (!c.contains(i))
            {
                iterator.remove();
                result = true;
              
            }
        }
		
		return result;
	}
	
	@Override
	protected void removeRange(int fromIndex, int toIndex)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public E set(int index, E element)
	{
		E result = get(index);
		
		if (map.get(result.hashCode()).size() == 1)
			map.remove(result.hashCode());
		else
			map.get(result.hashCode()).remove(index);
		
		if (!map.containsKey(element.hashCode()))
			map.put(element.hashCode(), new TreeSet<Integer>());
		map.get(element.hashCode()).add(index);
		
		super.set(index, element);
		return result;
	}
	
	private void addToMap(E o, int index)
	{
		if (index < size())
			for (Map.Entry<Integer, TreeSet<Integer>> i : map.entrySet())
			{
				TreeSet<Integer> newSet = new TreeSet<Integer>();
				for (Integer j : i.getValue())
					if (j >= index)
						newSet.add(j + 1);
                    else newSet.add(j);
				i.setValue(newSet);
			}
				
		
		if (!map.containsKey(o.hashCode()))
			map.put(o.hashCode(), new TreeSet<Integer>());
		map.get(o.hashCode()).add(index);
	}
	
	private void removeFromMap(E o, int index)
	{
		if (map.get(o.hashCode()).size() == 1)
			map.remove(o.hashCode());
		else
			map.get(o.hashCode()).remove(index);
		
		if (index < size() - 1)
			for (Map.Entry<Integer, TreeSet<Integer>> i : map.entrySet())
			{
				TreeSet<Integer> newSet = new TreeSet<Integer>();
				for (Integer j : i.getValue())
					if (j > index)
						newSet.add(j - 1);
				i.setValue(newSet);
			}
	}
	
	@Override
	public ListIterator<E> listIterator()
	{
		return new ListIterator<E>()
		{
			int cursor = -1;
			boolean removable = false;
			boolean removed = false;
			
			public void add(E o)
			{
				HashList.this.add(cursor++, o);
				removable = false;
			}

			public boolean hasNext()
			{
				return (cursor < size() - 1);
			}

			public boolean hasPrevious()
			{
				return (cursor > 0);
			}

			public E next()
			{
				if (!hasNext())
					throw new NoSuchElementException();
				
				if (removed)
				{
					removable = true;
					removed = false;
					return get(cursor);
				}
				else
				{
					removable = true;
					removed = false;
					return get(++cursor);
				}
			}

			public int nextIndex()
			{
				return (removed ? cursor : cursor + 1);
			}

			public E previous()
			{
				if (!hasPrevious())
					throw new NoSuchElementException();
				
				removable = true;
				removed = false;
				return get(--cursor);
			}

			public int previousIndex()
			{
				if (hasPrevious())
					return cursor - 1;
				else
					return -1;
			}

			public void remove()
			{
				if (!removable)
					throw new IllegalStateException("next() and previous() have not been called since the last call to remove()");
				removable = false;
				removed = true;
				HashList.this.remove(cursor);
			}

			public void set(E o)
			{
				HashList.this.set(cursor, o);
			}
		};
	}
}
