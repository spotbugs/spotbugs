/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

/**
 * @author pwilliam
 */
public class EditDistance {
	private static int minimum(int a, int b, int c){
		if (a > b) return Math.min(b, c);
		return Math.min(a, c);
	}

	private static int distance(char a, char b) {
		if (a == b) return 0;
		if (Character.toLowerCase(a) == Character.toLowerCase(b))
			return 1;
		return 2;
	}
	
	public static double editDistanceRatio(String str1, String
			 str2) {
		double distance = editDistance(str1, str2);
		int maxDistance = 2* Math.max(str1.length(), str2.length());
		return Math.min(distance / maxDistance, distance/4);
	}
	
	public static int editDistance(String str1, String
												 str2) {
		int n1 = str1.length();
		int n2 = str2.length();
		int diff = Math.abs(n1-n2);
		if (diff > 6)
			return 2 * Math.max(n1, n2);
		int[][] distance = new int[n1+1][];

		for(int i=0; i<=n1; i++){
			distance[i] = new int[n2+1];
			distance[i][0] = 2*i;
		}
		for(int j=1; j<= n2; j++)
			distance[0][j]=2*j;

		for(int i=1; i<=n1; i++)
			for(int j=1;j<=n2; j++)
				distance[i][j]= minimum(distance[i-1][j]+2,
										distance[i][j-1]+2,
										distance[i-1][j-1]+distance(str1.charAt(i-1), str2.charAt(j-1)));
				

		return distance[n1][n2];
	}
}