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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.log.Profiler;

/**
 * @author pugh
 */
public class TopologicalSort {
	final static boolean DEBUG = SystemProperties.getBoolean("tsort.debug");
	
	public interface OutEdges<E> {
		Collection<E> getOutEdges(E e);
	}

	public static class OutEdgesCache<E> implements OutEdges<E> {

		final Map<E, Collection<E>> map = new IdentityHashMap<E, Collection<E>>();
		final OutEdges<E> base;
		public OutEdgesCache(OutEdges<E> base) {
			this.base = base;
		}
		
        public Collection<E> getOutEdges(E e) {
	       Collection<E> result = map.get(e);
	       if (result == null) {
	    	   result = base.getOutEdges(e);
	    	   map.put(e, result);
	       }
	        return result;
        }
		
	}
	public static <E> List<E> sortByCallGraph(Collection<E> elements, OutEdges<E> outEdges) {
		Profiler profile = Profiler.getInstance();
		profile.start(TopologicalSort.class);
		try {
		SortAlgorithm<E> instance = new Worker2<E>(elements, outEdges);
		return instance.compute();
		} finally {
			profile.end(TopologicalSort.class);
		}
	}
	
	public static <E> int countBadEdges(List<E> elements, OutEdges<E> outEdges) {
		HashSet<E> seen = new HashSet<E>();
		HashSet<E> all = new HashSet<E>(elements);
		int result = 0;
		int total = 0;
		for(E e : elements) {
			for(E e2 : outEdges.getOutEdges(e)) if (e != e2 && all.contains(e2) && !outEdges.getOutEdges(e2).contains(e)) {
					total++;
					if (!seen.contains(e2)) result++;
			}
			seen.add(e);
		}
		if (DEBUG) System.out.println(" bad edges are " + result + "/" + total);
		return result;
	}

	interface SortAlgorithm<E> {
		List<E> compute();
	}
	static class Worker<E> implements SortAlgorithm<E> {
		Worker(Collection<E> consider, OutEdges<E> outEdges) {
			this.consider = new LinkedHashSet<E>(consider);
			this.outEdges = outEdges;
			this.result = new ArrayList<E>(consider.size());

		}
		OutEdges<E> outEdges;

		List<E> result;

		HashSet<E> visited = new HashSet<E>();
		Set<E> consider = new HashSet<E>();

		public List<E> compute() {
				for (E e : consider)
					visit(e);
				return result;
		}
		void visit(E e) {
			if (!consider.contains(e)) return;
			if (!visited.add(e))
				return;
			 for(E e2  :outEdges.getOutEdges(e))
				visit(e2);

			result.add(e);
		}
	}
	static class Worker2<E> implements SortAlgorithm<E> {
		Worker2(Collection<E> consider, OutEdges<E> outEdges) {
			if (outEdges == null)
				throw new IllegalArgumentException("outEdges must not be null");
			this.consider = new LinkedHashSet<E>(consider);
			this.outEdges = outEdges;

		}
		OutEdges<E> outEdges;


		Set<E> consider = new HashSet<E>();
		MultiMap<E, E> iEdges, oEdges;
		private void removeVertex(E e) {
			Collection<E> outEdges = oEdges.get(e);
			
			Collection<E> inEdges = iEdges.get(e);
			for(E e2 : outEdges) {
				iEdges.remove(e2, e);
			}
			for(E e2 : inEdges) {
				oEdges.remove(e2, e);
			}
			iEdges.removeAll(e);
			oEdges.removeAll(e);
		}
		public List<E> compute() {
			    ArrayList<E> doFirst = new ArrayList<E>(consider.size());
			    ArrayList<E> doLast = new ArrayList<E>(consider.size());
			    
				HashSet<E> remaining = new HashSet<E>(consider);
				iEdges = new MultiMap<E, E>(LinkedList.class);
				oEdges = new MultiMap<E, E>(LinkedList.class);
				
				for(E e : consider) 
					for(E e2 : outEdges.getOutEdges(e)) 
						  if (e != e2 && consider.contains(e2)) {
						    iEdges.add(e2, e);
						    oEdges.add(e,e2);
					      }
				for(E e : consider) {
					HashSet<E> both = new HashSet<E>(iEdges.get(e));
					both.retainAll(oEdges.get(e));
					for(E e2 : both) {
						iEdges.remove(e, e2);
						oEdges.remove(e, e2);
					}
				}
				while (!remaining.isEmpty()) {
					boolean foundSomething = false;
					E best = null;
					int bestScore = Integer.MIN_VALUE;
					for(Iterator<E> i = remaining.iterator(); i.hasNext(); ) {
						E e = i.next();
						if (oEdges.get(e).isEmpty()) {
							doFirst.add(e); 
							removeVertex(e);
							if (DEBUG) System.out.println("do " + e + " first");
							i.remove();
							foundSomething = true;
						} else if (iEdges.get(e).isEmpty()) {
							doLast.add(e);
							removeVertex(e);
							if (DEBUG) System.out.println("do " + e + " last");
							i.remove();
							foundSomething = true;
						} else {
							// Higher score: more likely to choose
							int myScore = score(e);
							
							// myScore -= oEdges.get(e).size(); // more needs, more reluctant
							// myScore += iEdges.get(e).size(); // needed more, more eager
							if (bestScore < myScore) {
								// my score is better than the best seen so far
								bestScore = myScore;
								best = e;
							}
						}
					} // iterator
					if (!foundSomething) {
						if (DEBUG) {
							if (best.toString().equals("org/eclipse/jdt/internal/core/JavaModel")) {
								System.out.println("Full dump for org/eclipse/jdt/internal/core/JavaModel {");
								for(E e : remaining) {
									System.out.printf(" %4d %s\n", score(e), e);
									System.out.println("  needs: " + oEdges.get(e));
									System.out.println("  needed by: " + iEdges.get(e));
									}
								System.out.println("} Full dump for org/eclipse/jdt/internal/core/JavaModel");
								
							}
						System.out.println("do " + best + " first, reluctantly");
						System.out.println("  needs: " + oEdges.get(best));
						System.out.println("  needed by: " + iEdges.get(best));
						}
						doFirst.add(best);
						removeVertex(best);
						remaining.remove(best);
					}
				} // while 
				Collections.reverse(doLast);
				doFirst.addAll(doLast);

				return doFirst;
		}
		/**
         * @param e
         * @return
         */
        private int score(E e) {
	        int myScore = 0;
	        for(E e2 : oEdges.get(e)) 
	        	if (iEdges.get(e2).size() == 1)
	        		myScore -= 2;
	        	else myScore -= 1;
	        for(E e2 : iEdges.get(e)) 
	        	if (oEdges.get(e2).size() == 1)
	        		myScore += 2;
	        	else myScore += 1;
	        return myScore;
        }
		
	}
}
