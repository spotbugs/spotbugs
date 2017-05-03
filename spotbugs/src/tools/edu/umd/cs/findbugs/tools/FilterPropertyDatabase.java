package edu.umd.cs.findbugs.tools;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.WillClose;

import org.apache.bcel.Constants;

import edu.umd.cs.findbugs.tools.FilterAndCombineBitfieldPropertyDatabase.Status;
import edu.umd.cs.findbugs.util.Util;

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

/**
 * Filter a property database, only passing through the annotations on public or
 * protected methods
 */
public class FilterPropertyDatabase {

    final static int FLAGS = Const.ACC_PROTECTED | Const.ACC_PUBLIC;

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        InputStream inSource = System.in;
        if (args.length > 0) {
            inSource = new FileInputStream(args[0]);
        }
        process(inSource);

    }

    /**
     * @param inSource
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    private static void process(@WillClose InputStream inSource) throws UnsupportedEncodingException, IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(Util.getReader(inSource));

            Pattern p = Pattern.compile("^(([^,]+),.+),([0-9]+)\\|(.+)$");

            while (true) {
                String s = in.readLine();
                if (s == null) {
                    break;
                }
                Matcher m = p.matcher(s);
                if (m.find()) {
                    String className = m.group(2);
                    if (FilterAndCombineBitfieldPropertyDatabase.getStatus(className) == Status.UNEXPOSED) {
                        continue;
                    }
                    int accFlags = Integer.parseInt(m.group(3));

                    if ((accFlags & FLAGS) != 0) {
                        System.out.println(s);
                    }
                }

            }
        } finally {
            Util.closeSilently(in);
        }
    }

}
