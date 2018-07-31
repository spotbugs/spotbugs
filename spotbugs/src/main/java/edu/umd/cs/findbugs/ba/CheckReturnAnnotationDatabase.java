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

package edu.umd.cs.findbugs.ba;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.meta.When;

import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.ElementValuePair;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.Values;

/**
 * @author pugh
 */
public class CheckReturnAnnotationDatabase extends AnnotationDatabase<CheckReturnValueAnnotation> {

    private JavaClass throwableClass, threadClass;

    public CheckReturnAnnotationDatabase() {
        setAddClassOnly(true);
        loadAuxiliaryAnnotations();
        setAddClassOnly(false);
    }

    @Override
    public void loadAuxiliaryAnnotations() {
        if (IGNORE_BUILTIN_ANNOTATIONS) {
            return;
        }
        boolean missingClassWarningsSuppressed = AnalysisContext.currentAnalysisContext().setMissingClassWarningsSuppressed(true);

        addMethodAnnotation("java.util.Iterator", "hasNext", "()Z", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);
        addMethodAnnotation(Values.DOTTED_JAVA_IO_FILE, "createNewFile", "()Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);
        addMethodAnnotation(Values.DOTTED_JAVA_IO_FILE, "delete", "()Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);
        addMethodAnnotation(Values.DOTTED_JAVA_IO_FILE, "mkdir", "()Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);
        addMethodAnnotation(Values.DOTTED_JAVA_IO_FILE, "mkdirs", "()Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);
        addMethodAnnotation(Values.DOTTED_JAVA_IO_FILE, "renameTo", "(Ljava/io/File;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);
        addMethodAnnotation(Values.DOTTED_JAVA_IO_FILE, "setLastModified", "(J)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);
        addMethodAnnotation(Values.DOTTED_JAVA_IO_FILE, "setReadOnly", "()Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);
        addMethodAnnotation(Values.DOTTED_JAVA_IO_FILE, "setWritable", "(ZZ)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);
        addMethodAnnotation("java.util.Enumeration", "hasMoreElements", "()Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
        addMethodAnnotation("java.security.MessageDigest", "digest", "([B)[B", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);

        addMethodAnnotation("java.util.concurrent.locks.ReadWriteLock", "readLock", "()Ljava/util/concurrent/locks/Lock;", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
        addMethodAnnotation("java.util.concurrent.locks.ReadWriteLock", "writeLock", "()Ljava/util/concurrent/locks/Lock;",
                false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
        addMethodAnnotation("java.util.concurrent.locks.Condition", "await", "(JLjava/util/concurrent/TimeUnit;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);
        addMethodAnnotation("java.util.concurrent.CountDownLatch", "await", "(JLjava/util/concurrent/TimeUnit;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
        addMethodAnnotation("java.util.concurrent.locks.Condition", "awaitUntil", "(Ljava/util/Date;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);
        addMethodAnnotation("java.util.concurrent.locks.Condition", "awaitNanos", "(J)J", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);
        addMethodAnnotation("java.util.concurrent.Semaphore", "tryAcquire", "(JLjava/util/concurrent/TimeUnit;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
        addMethodAnnotation("java.util.concurrent.Semaphore", "tryAcquire", "()Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
        addMethodAnnotation("java.util.concurrent.locks.Lock", "tryLock", "(JLjava/util/concurrent/TimeUnit;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
        addMethodAnnotation("java.util.concurrent.locks.Lock", "newCondition", "()Ljava/util/concurrent/locks/Condition;", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
        addMethodAnnotation("java.util.concurrent.locks.Lock", "tryLock", "()Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
        addMethodAnnotation("java.util.concurrent.BlockingQueue", "offer",
                "(Ljava/lang/Object;JLjava/util/concurrent/TimeUnit;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);
        addMethodAnnotation("java.util.concurrent.BlockingQueue", "offer", "(Ljava/lang/Object;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);

        addMethodAnnotation("java.util.concurrent.ConcurrentLinkedQueue", "offer", "(Ljava/lang/Object;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation("java.util.concurrent.DelayQueue", "offer", "(Ljava/lang/Object;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation("java.util.concurrent.LinkedBlockingQueue", "offer", "(Ljava/lang/Object;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW_BAD_PRACTICE);
        addMethodAnnotation("java.util.LinkedList", "offer", "(Ljava/lang/Object;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation("java.util.Queue", "offer", "(Ljava/lang/Object;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW_BAD_PRACTICE);
        addMethodAnnotation("java.util.concurrent.ArrayBlockingQueue", "offer", "(Ljava/lang/Object;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);
        addMethodAnnotation("java.util.concurrent.SynchronousQueue", "offer", "(Ljava/lang/Object;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);
        addMethodAnnotation("java.util.PriorityQueue", "offer", "(Ljava/lang/Object;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation("java.util.concurrent.PriorityBlockingQueue", "offer", "(Ljava/lang/Object;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);

        addWarningAboutSubmit(ExecutorService.class);
        addWarningAboutSubmit(ThreadPoolExecutor.class);
        addWarningAboutSubmit(ScheduledThreadPoolExecutor.class);
        addWarningAboutSubmit(AbstractExecutorService.class);

        addMethodAnnotation("java.util.concurrent.BlockingQueue", "poll", "(JLjava/util/concurrent/TimeUnit;)Ljava/lang/Object;",
                false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
        addMethodAnnotation("java.util.Queue", "poll", "()Ljava/lang/Object;", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);

        addDefaultMethodAnnotation(Values.DOTTED_JAVA_LANG_STRING, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
        addMethodAnnotation(Values.DOTTED_JAVA_LANG_STRING, "getBytes", "(Ljava/lang/String;)[B", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation(Values.DOTTED_JAVA_LANG_STRING, "charAt", "(I)C", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);
        addMethodAnnotation(Values.DOTTED_JAVA_LANG_STRING, "toString", "()Ljava/lang/String;", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);
        addMethodAnnotation(Values.DOTTED_JAVA_LANG_STRING, "length", "()I", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);
        addMethodAnnotation(Values.DOTTED_JAVA_LANG_STRING, "matches", "(Ljava/lang/String;)Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);
        addMethodAnnotation(Values.DOTTED_JAVA_LANG_STRING, "intern", "()Ljava/lang/String;", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
        addMethodAnnotation(Values.DOTTED_JAVA_LANG_STRING, Const.CONSTRUCTOR_NAME, "([BLjava/lang/String;)V", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation(Values.DOTTED_JAVA_LANG_STRING, Const.CONSTRUCTOR_NAME, "(Ljava/lang/String;)V", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);
        addMethodAnnotation(Values.DOTTED_JAVA_LANG_STRING, Const.CONSTRUCTOR_NAME, "()V", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);
        addDefaultMethodAnnotation("java.math.BigDecimal", CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
        addMethodAnnotation("java.math.BigDecimal", "inflate", "()Ljava/math/BigInteger;", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation("java.math.BigDecimal", "precision", "()I", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);

        addMethodAnnotation("java.math.BigDecimal", "toBigIntegerExact", "()Ljava/math/BigInteger;", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation("java.math.BigDecimal", "longValueExact", "()J", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation("java.math.BigDecimal", "intValueExact", "()I", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation("java.math.BigDecimal", "shortValueExact", "()S", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation("java.math.BigDecimal", "byteValueExact", "()B", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation("java.math.BigDecimal", Const.CONSTRUCTOR_NAME, "(Ljava/lang/String;)V", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation("java.math.BigDecimal", "intValue", "()I", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation("java.math.BigDecimal", "stripZerosToMatchScale", "(J)Ljava/math/BigDecimal;", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);

        addDefaultMethodAnnotation("java.math.BigInteger", CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
        addMethodAnnotation("java.math.BigInteger", "addOne", "([IIII)I", true,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation("java.math.BigInteger", "subN", "([I[II)I", true,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation("java.math.BigInteger", Const.CONSTRUCTOR_NAME, "(Ljava/lang/String;)V", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addDefaultMethodAnnotation("java.sql.Connection", CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
        addDefaultMethodAnnotation("java.net.InetAddress", CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
        addMethodAnnotation("java.net.InetAddress", "getByName", "(Ljava/lang/String;)Ljava/net/InetAddress;", true,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation("java.net.InetAddress", "getAllByName", "(Ljava/lang/String;)[Ljava/net/InetAddress;", true,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
        addMethodAnnotation("java.lang.ProcessBuilder", "redirectErrorStream", "()Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);

        addMethodAnnotation("java.lang.ProcessBuilder", "redirectErrorStream", "()Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
        addMethodAnnotation("java.lang.ProcessBuilder", "redirectErrorStream", "()Z", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);

        addDefaultMethodAnnotation("jsr166z.forkjoin.ParallelArray", CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
        addDefaultMethodAnnotation("jsr166z.forkjoin.ParallelLongArray", CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
        addDefaultMethodAnnotation("jsr166z.forkjoin.ParallelDoubleArray", CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);


        addMethodAnnotation(java.sql.Statement.class, "executeQuery", "(Ljava/lang/String;)Ljava/sql/ResultSet;", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
        addMethodAnnotation(java.sql.PreparedStatement.class, "executeQuery", "()Ljava/sql/ResultSet;", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
        AnalysisContext.currentAnalysisContext().setMissingClassWarningsSuppressed(missingClassWarningsSuppressed);

        try {
            throwableClass = Repository.lookupClass("java.lang.Throwable");
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
        try {
            threadClass = Repository.lookupClass("java.lang.Thread");
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
    }

    /**
     * @param c
     */
    private void addWarningAboutSubmit(Class<? extends ExecutorService> c) {
        addMethodAnnotation(c.getName(), "submit",
                "(Ljava/util/concurrent/Callable;)Ljava/util/concurrent/Future;", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);
        addMethodAnnotation(c.getName(), "submit",
                "(Ljava/lang/Runnable;)Ljava/util/concurrent/Future;", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW_BAD_PRACTICE);
        addMethodAnnotation(c.getName(), "submit",
                "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/Future;", false,
                CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM_BAD_PRACTICE);
    }

    @Nullable
    private CheckReturnValueAnnotation getResolvedAnnotationOnConstructor(XMethod m) {
        try {
            if (throwableClass != null && Repository.instanceOf(m.getClassName(), throwableClass)) {
                return CheckReturnValueAnnotation.CHECK_RETURN_VALUE_VERY_HIGH;
            }
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
        if ("java.lang.Thread".equals(m.getClassName())) {
            return CheckReturnValueAnnotation.CHECK_RETURN_VALUE_VERY_HIGH;
        }
        try {
            if (threadClass != null && Repository.instanceOf(m.getClassName(), threadClass)) {
                return CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW;
            }
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
        return null;
    }

    @Override
    public CheckReturnValueAnnotation getResolvedAnnotation(Object o, boolean getMinimal) {
        if (!(o instanceof XMethod)) {
            return null;
        }
        XMethod m = (XMethod) o;
        if (m.getName().startsWith("access$")) {
            return null;
        } else if (Const.CONSTRUCTOR_NAME.equals(m.getName())) {
            CheckReturnValueAnnotation a = getResolvedAnnotationOnConstructor(m);
            if (a != null) {
              return a;
            }
        } else if ("equals".equals(m.getName()) && "(Ljava/lang/Object;)Z".equals(m.getSignature()) && !m.isStatic()) {
            return CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM;
        } else if (m.getSignature().endsWith(")Ljava/lang/String;")
                && ("java.lang.StringBuffer".equals(m.getClassName()) || "java.lang.StringBuilder".equals(m.getClassName()))) {
            return CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM;
        }
        CheckReturnValueAnnotation annotationOnMethod = super.getResolvedAnnotation(o, getMinimal);
        if (annotationOnMethod == null) {
            // https://github.com/spotbugs/spotbugs/issues/429
            // BuildCheckReturnAnnotationDatabase does not visit non-application classes,
            // so we need to check package info dynamically

            return packageInfoCache.computeIfAbsent(m.getPackageName(), this::parsePackage);
        }
        return annotationOnMethod;
    }

    @SlashedClassName
    private static final String NAME_OF_CHECK_RETURN_NULL_SPOTBUGS = "edu/umd/cs/findbugs/annotations/CheckReturnValue";
    @SlashedClassName
    private static final String NAME_OF_CHECK_RETURN_NULL_JSR305 = "javax/annotation/CheckReturnValue";
    @SlashedClassName
    private static final String NAME_OF_CHECK_RETURN_NULL_ERRORPRONE = "com/google/errorprone/annotations/CheckReturnValue";
    @SlashedClassName
    private static final String NAME_OF_CAN_IGNORE_RETURN_VALUE = "com/google/errorprone/annotations/CanIgnoreReturnValue";

    private final Map<String, CheckReturnValueAnnotation> packageInfoCache = new HashMap<>();

    /**
     * Try to find default {@link CheckReturnValueAnnotation} for methods inside of target class.
     *
     */
    @CheckForNull
    private CheckReturnValueAnnotation parsePackage(@DottedClassName String packageName) {
        String className = ClassName.toSlashedClassName(packageName) + "/package-info";
        ClassDescriptor descriptor = DescriptorFactory.createClassDescriptor(className);
        // ClassInfoAnalysisEngine doesn't support parsing package-info to generate XClass, so use JavaClass instead
        JavaClass clazz;
        try {
            clazz = AnalysisContext.currentAnalysisContext().lookupClass(descriptor);
        } catch (ClassNotFoundException e) {
            // no annotation on package
            return null;
        }

        for (AnnotationEntry entry : clazz.getAnnotationEntries()) {
            @SlashedClassName
            String type = entry.getAnnotationType();

            switch (type) {
            case NAME_OF_CHECK_RETURN_NULL_SPOTBUGS:
                return createSpotBugsAnnotation(entry);
            case NAME_OF_CHECK_RETURN_NULL_JSR305:
                return createJSR305Annotation(entry);
            case NAME_OF_CHECK_RETURN_NULL_ERRORPRONE:
                return CheckReturnValueAnnotation.createFor(When.ALWAYS);
            case NAME_OF_CAN_IGNORE_RETURN_VALUE:
                return CheckReturnValueAnnotation.createFor(When.NEVER);
            default:
                // check next annotation
            }
        }

        return null;
    }

    private CheckReturnValueAnnotation createJSR305Annotation(AnnotationEntry entry) {
        for (ElementValuePair pair : entry.getElementValuePairs()) {
            if (pair.getNameString().equals("when")) {
                return CheckReturnValueAnnotation.createFor(When.valueOf(pair.getValue().stringifyValue()));
            }
        }
        // use default value
        return CheckReturnValueAnnotation.createFor(When.ALWAYS);
    }

    private CheckReturnValueAnnotation createSpotBugsAnnotation(AnnotationEntry entry) {
        for (ElementValuePair pair : entry.getElementValuePairs()) {
            if (pair.getNameString().equals("confidence")) {
                return CheckReturnValueAnnotation.parse(pair.getValue().stringifyValue());
            }
        }
        // use default value
        return CheckReturnValueAnnotation.parse(Confidence.MEDIUM.name());
    }
}
