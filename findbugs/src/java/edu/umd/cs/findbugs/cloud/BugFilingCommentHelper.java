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

package edu.umd.cs.findbugs.cloud;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.SourceFile;
import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.cloud.Cloud.UserDesignation;
import edu.umd.cs.findbugs.util.Util;

/**
 * @author Keith
 */
public class BugFilingCommentHelper {
    private final Cloud cloud;

    private final String BUG_NOTE;

    private final String POSTMORTEM_NOTE;

    private final int POSTMORTEM_RANK;

    public BugFilingCommentHelper(Cloud cloud) {
        this.cloud = cloud;
        PropertyBundle properties = cloud.getPlugin().getProperties();
        BUG_NOTE = properties.getProperty("findbugs.bugnote");
        POSTMORTEM_NOTE = properties.getProperty("findbugs.postmortem.note");
        POSTMORTEM_RANK = properties.getInt("findbugs.postmortem.maxRank", 4);
    }

    public String getBugReportSummary(BugInstance b) {
        return b.getMessageWithoutPrefix() + " in " + b.getPrimaryClass().getSourceFileName();
    }

    public String getBugReportText(BugInstance b) {
        return getBugReportHead(b) + getBugReportSourceCode(b) + getLineTerminatedUserEvaluation(b) + getBugPatternExplanation(b)
                + getBugReportTail(b);
    }

    @SuppressWarnings("boxing")
    public String getBugReportSourceCode(BugInstance b) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter out = new PrintWriter(stringWriter);
        ClassAnnotation primaryClass = b.getPrimaryClass();

        int firstLine = Integer.MAX_VALUE;
        int lastLine = Integer.MIN_VALUE;
        for (BugAnnotation a : b.getAnnotations()) {
            if (a instanceof SourceLineAnnotation) {
                SourceLineAnnotation s = (SourceLineAnnotation) a;
                if (s.getClassName().equals(primaryClass.getClassName()) && s.getStartLine() > 0) {
                    firstLine = Math.min(firstLine, s.getStartLine());
                    lastLine = Math.max(lastLine, s.getEndLine());

                }

            }
        }

        SourceLineAnnotation primarySource = primaryClass.getSourceLines();
        if (primarySource.isSourceFileKnown() && firstLine >= 1 && firstLine <= lastLine && lastLine - firstLine < 50) {
            BufferedReader in = null;
            try {
                Project project = cloud.getBugCollection().getProject();
                SourceFile sourceFile = project.getSourceFinder().findSourceFile(primarySource);
                in = UTF8.bufferedReader(sourceFile.getInputStream());
                int lineNumber = 1;
                String commonWhiteSpace = null;
                List<SourceLine> source = new ArrayList<SourceLine>();
                while (lineNumber <= lastLine + 4) {
                    String txt = in.readLine();
                    if (txt == null) {
                        break;
                    }
                    if (lineNumber >= firstLine - 4) {
                        String trimmed = txt.trim();
                        if (trimmed.length() == 0) {
                            if (lineNumber > lastLine) {
                                break;
                            }
                            txt = trimmed;

                        }
                        source.add(new SourceLine(lineNumber, txt));
                        commonWhiteSpace = commonLeadingWhitespace(commonWhiteSpace, txt);
                    }
                    lineNumber++;
                }
                if (commonWhiteSpace == null) {
                    commonWhiteSpace = "";
                }
                out.println("\nRelevant source code:");
                for (SourceLine s : source) {
                    if (s.text.length() == 0) {
                        out.printf("%5d: %n", s.line);
                    } else {
                        out.printf("%5d:   %s%n", s.line, s.text.substring(commonWhiteSpace.length()));
                    }
                }

                out.println();
            } catch (IOException e) {
                assert true;
            } finally {
                Util.closeSilently(in);
            }
            out.close();
            return stringWriter.toString();

        }
        return "";
    }

    public String getBugReportHead(BugInstance b) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter out = new PrintWriter(stringWriter);
        out.println("Bug report generated from FindBugs");
        out.println(b.getMessageWithoutPrefix());
        out.println();
        ClassAnnotation primaryClass = b.getPrimaryClass();

        for (BugAnnotation a : b.getAnnotations()) {
            if (a == primaryClass) {
                out.println(a);
            } else {
                out.println("  " + a.toString(primaryClass));
            }
        }
        if (cloud.supportsSourceLinks()) {
            URL link = cloud.getSourceLink(b);

            if (link != null) {
                out.println();
                out.println(cloud.getSourceLinkToolTip(b) + ": " + link);
                out.println();
            }
        }

        if (BUG_NOTE != null) {
            out.println(BUG_NOTE);
            if (POSTMORTEM_NOTE != null && BugRanker.findRank(b) <= POSTMORTEM_RANK
                    && cloud.getConsensusDesignation(b).score() >= 0) {

                out.println(POSTMORTEM_NOTE);
            }
            out.println();
        }

        Collection<String> projects = cloud.getProjects(primaryClass.getClassName());
        if (projects != null && !projects.isEmpty()) {
            String projectList = projects.toString();
            projectList = projectList.substring(1, projectList.length() - 1);
            out.println("Possibly part of: " + projectList);
            out.println();
        }
        out.close();
        return stringWriter.toString();
    }

    public String getBugPatternExplanation(BugInstance b) {
        String detailPlainText = b.getBugPattern().getDetailPlainText();
        return "Bug pattern explanation:\n" + detailPlainText + "\n\n";
    }

    public String getLineTerminatedUserEvaluation(BugInstance b) {
        UserDesignation designation = cloud.getUserDesignation(b);

        String result;
        if (designation != UserDesignation.UNCLASSIFIED) {
            result = "Classified as: " + designation.toString() + "\n";
        } else {
            result = "";
        }
        String eval = cloud.getUserEvaluation(b).trim();
        if (eval.length() > 0) {
            result = result + eval + "\n";
        }
        return result;
    }

    public String getBugReportTail(BugInstance b) {
        return "\nFindBugs issue identifier (do not modify or remove): " + b.getInstanceHash();
    }

    // ================================= end of public methods
    // ====================================

    private String commonLeadingWhitespace(String soFar, String txt) {
        if (txt.length() == 0) {
            return soFar;
        }
        if (soFar == null) {
            return txt;
        }
        soFar = Util.commonPrefix(soFar, txt);
        for (int i = 0; i < soFar.length(); i++) {
            if (!Character.isWhitespace(soFar.charAt(i))) {
                return soFar.substring(0, i);
            }
        }
        return soFar;
    }

    // ==================================== inner classes
    // =========================================

    public static class SourceLine {
        public final int line;

        public final String text;

        public SourceLine(int line, String text) {
            this.line = line;
            this.text = text;
        }
    }
}
