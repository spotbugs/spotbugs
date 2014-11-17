/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

import edu.umd.cs.findbugs.SystemProperties;

/**
 * @author David Hovemeyer
 */
public class DefaultNullnessAnnotations {
    public static final boolean ICSE10_NULLNESS_PAPER = SystemProperties.getBoolean("icse10");

    /**
     * Add default NullnessAnnotations to given INullnessAnnotationDatabase.
     *
     * @param database
     *            an INullnessAnnotationDatabase
     */
    public static void addDefaultNullnessAnnotations(INullnessAnnotationDatabase database) {
        if (AnnotationDatabase.IGNORE_BUILTIN_ANNOTATIONS) {
            return;
        }

        boolean missingClassWarningsSuppressed = AnalysisContext.currentAnalysisContext().setMissingClassWarningsSuppressed(true);

        database.addDefaultAnnotation(AnnotationDatabase.Target.METHOD, "java.lang.String", NullnessAnnotation.NONNULL);
        database.addFieldAnnotation("java.lang.System", "out", "Ljava/io/PrintStream;", true, NullnessAnnotation.NONNULL);
        database.addFieldAnnotation("java.lang.System", "err", "Ljava/io/PrintStream;", true, NullnessAnnotation.NONNULL);
        database.addFieldAnnotation("java.lang.System", "in", "Ljava/io/InputStream;", true, NullnessAnnotation.NONNULL);

        database.addFieldAnnotation("java.math.BigInteger", "ZERO", "Ljava/math/BigInteger;", true, NullnessAnnotation.NONNULL);
        database.addFieldAnnotation("java.math.BigInteger", "ONE", "Ljava/math/BigInteger;", true, NullnessAnnotation.NONNULL);
        database.addFieldAnnotation("java.math.BigInteger", "TEN", "Ljava/math/BigInteger;", true, NullnessAnnotation.NONNULL);

        database.addMethodAnnotation("java.nio.file.Files", "probeContentType", "(Ljava/nio/file/Path;)Ljava/lang/String;", true,
                NullnessAnnotation.CHECK_FOR_NULL);

        database.addMethodAnnotation("java.nio.file.Path", "getRoot", "()Ljava/nio/file/Path;", false,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodAnnotation("java.nio.file.Path", "getFileName", "()Ljava/nio/file/Path;", false,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodAnnotation("java.nio.file.Path", "getParent", "()Ljava/nio/file/Path;", false,
                NullnessAnnotation.CHECK_FOR_NULL);


        database.addMethodAnnotation("java.io.File", "list", "()[Ljava/lang/String;", false,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodAnnotation("java.io.File", "list", "(Ljava/io/FilenameFilter;)[Ljava/lang/String;", false,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodAnnotation("java.io.File", "listFiles", "()[Ljava/io/File;", false,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodAnnotation("java.io.File", "listFiles", "(Ljava/io/FilenameFilter;)[Ljava/io/File;", false,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodAnnotation("java.io.File", "listFiles", "(Ljava/io/FileFilter;)[Ljava/io/File;", false,
                NullnessAnnotation.CHECK_FOR_NULL);

        database.addMethodAnnotation("java.lang.ref.ReferenceQueue", "poll", "()Ljava/lang/ref/Reference;", false,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodAnnotation("java.lang.ref.Reference", "get", "()Ljava/lang/Object;", false,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodAnnotation("java.lang.Class", "newInstance", "()Ljava/lang/Object;", false, NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("java.lang.Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", true,
                NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("java.lang.reflect.Method", "getParameterTypes", "()[Ljava/lang/Class;", false,
                NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("java.lang.Object", "clone", "()Ljava/lang/Object;", false, NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("java.lang.Object", "toString", "()Ljava/lang/String;", false, NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("java.lang.Object", "getClass", "()Ljava/lang/Class;", false, NullnessAnnotation.NONNULL);

        database.addMethodParameterAnnotation("java.lang.Object", "equals", "(Ljava/lang/Object;)Z", false, 0,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;", true, 0,
                NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.lang.Integer", "<init>", "(Ljava/lang/String;)V", false, 0,
                NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.lang.Integer", "parseInt", "(Ljava/lang/String;I)I", true, 0,
                NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.lang.Integer", "parseInt", "(Ljava/lang/String;)I", true, 0,
                NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("java.nio.channels.SocketChannel", "open", "()Ljava/nio/channels/SocketChannel;", true,
                NullnessAnnotation.NONNULL);

        database.addMethodAnnotation("java.sql.Statement", "executeQuery", "(Ljava/lang/String;)Ljava/sql/ResultSet;", false,
                NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("java.sql.PreparedStatement", "executeQuery", "()Ljava/sql/ResultSet;", false,
                NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("java.sql.Connection", "prepareStatement",
                "(Ljava/lang/String;)Ljava/sql/PreparedStatement;", false, NullnessAnnotation.NONNULL);
        database.addDefaultAnnotation(AnnotationDatabase.Target.METHOD, "java.sql.DatabaseMetaData", NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("java.sql.DatabaseMetaData", "getConnection", "()Ljava/sql/Connection;", false,
                NullnessAnnotation.NULLABLE);
        database.addMethodAnnotation("java.sql.DatabaseMetaData", "getAttributes",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/sql/ResultSet;", false,
                NullnessAnnotation.NULLABLE);
        database.addMethodAnnotation("java.sql.DatabaseMetaData", "getColumns",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/sql/ResultSet;", false,
                NullnessAnnotation.NULLABLE);
        database.addMethodAnnotation("java.sql.DatabaseMetaData", "getSuperTables",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/sql/ResultSet;", false,
                NullnessAnnotation.NULLABLE);
        database.addMethodAnnotation("java.sql.DatabaseMetaData", "getSuperTypes",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/sql/ResultSet;", false,
                NullnessAnnotation.NULLABLE);
        database.addMethodAnnotation("java.sql.DatabaseMetaData", "getTimeDateFunctions", "()Ljava/lang/String;", false,
                NullnessAnnotation.NULLABLE);
        database.addMethodAnnotation("java.sql.DatabaseMetaData", "getTypeInfo", "()Ljava/sql/ResultSet;", false,
                NullnessAnnotation.NULLABLE);
        database.addMethodAnnotation("java.sql.DatabaseMetaData", "getURL", "()Ljava/lang/String;", false,
                NullnessAnnotation.NULLABLE);

        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.package-info",
                NullnessAnnotation.NONNULL);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.CopyOnWriteArrayList",
                NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.CopyOnWriteArraySet",
                NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.ConcurrentLinkedQueue$Node",
                NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.Exchanger",
                NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.FutureTask",
                NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.LinkedBlockingQueue$Node",
                NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER,
                "java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask", NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.SynchronousQueue$WaitQueue",
                NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.SynchronousQueue$Node",
                NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.ThreadPoolExecutor$Worker",
                NullnessAnnotation.UNKNOWN_NULLNESS);

        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.AbstractExecutorService",
                NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER,
                "java.util.concurrent.ConcurrentSkipListMap$ConcurrentSkipListSubMap", NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER,
                "java.util.concurrent.ConcurrentSkipListMap$HeadIndex", NullnessAnnotation.UNKNOWN_NULLNESS);

        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.ConcurrentSkipListMap$Index",
                NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.ConcurrentSkipListMap$Node",
                NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.ConcurrentSkipListMap$SubMap",
                NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER,
                "java.util.concurrent.ConcurrentSkipListSet$ConcurrentSkipListSubSet", NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.LinkedBlockingDeque$Node",
                NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.SynchronousQueue$TransferQueue",
                NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER,
                "java.util.concurrent.SynchronousQueue$TransferQueue$QNode", NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.SynchronousQueue$TransferStack",
                NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.SynchronousQueue$Transferer",
                NullnessAnnotation.UNKNOWN_NULLNESS);
        database.addMethodParameterAnnotation("java.util.concurrent.ConcurrentMap", "containsKey", "(Ljava/lang/Object;)Z",
                false, 0, NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ConcurrentMap", "containsValue", "(Ljava/lang/Object;)Z",
                false, 0, NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ConcurrentMap", "get",
                "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ConcurrentMap", "remove",
                "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ConcurrentMap", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ConcurrentMap", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false, 1, NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ConcurrentHashMap", "remove",
                "(Ljava/lang/Object;Ljava/lang/Object;)Z", false, 1, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ConcurrentHashMap", "setEntryAt",
                "([Ljava/util/concurrent/ConcurrentHashMap$HashEntry;ILjava/util/concurrent/ConcurrentHashMap$HashEntry;)V", false, 1, NullnessAnnotation.NULLABLE);
        database.addMethodParameterAnnotation("java.util.concurrent.ForkJoinPool", "<init>",
                "(ILjava/util/concurrent/ForkJoinPool$ForkJoinWorkerThreadFactory;Ljava/lang/Thread$UncaughtExceptionHandler;Z)V", false, 1, NullnessAnnotation.NULLABLE);
        database.addMethodParameterAnnotation("java.util.concurrent.PriorityBlockingQueue", "<init>",
                "(ILjava/util/Comparator;)V", false, 1, NullnessAnnotation.NULLABLE);

        database.addDefaultAnnotation(AnnotationDatabase.Target.PARAMETER, "java.util.concurrent.ConcurrentLinkedDeque$Node",
                NullnessAnnotation.UNKNOWN_NULLNESS);

        database.addMethodParameterAnnotation("java.util.concurrent.ConcurrentMap", "remove",
                "(Ljava/lang/Object;Ljava/lang/Object;)Z", false, 1, NullnessAnnotation.NULLABLE);

        database.addMethodParameterAnnotation("java.util.concurrent.FutureTask", "<init>",
                "(Ljava/lang/Runnable;Ljava/lang/Object;)V", false, 1, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.Executors", "callable",
                "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/Callable;", true, 1,
                NullnessAnnotation.CHECK_FOR_NULL);

        database.addMethodParameterAnnotation("java.util.concurrent.ThreadPoolExecutor", "addWorker", "(Ljava/lang/Runnable;Z)Z",
                false, 0, NullnessAnnotation.CHECK_FOR_NULL);

        database.addMethodParameterAnnotation("java.util.concurrent.ConcurrentHashMap$Segment", "remove",
                "(Ljava/lang/Object;ILjava/lang/Object;)Ljava/lang/Object;", false, 2, NullnessAnnotation.CHECK_FOR_NULL);

        database.addMethodParameterAnnotation("java.util.concurrent.CyclicBarrier", "<init>", "(ILjava/lang/Runnable;)V", false,
                1, NullnessAnnotation.CHECK_FOR_NULL);



        database.addMethodParameterAnnotation("java.util.concurrent.BrokenBarrierException", "<init>",
                "(Ljava/lang/String;)V", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.CancellationException", "<init>",
                "(Ljava/lang/String;)V", false, 0, NullnessAnnotation.CHECK_FOR_NULL);

        database.addMethodParameterAnnotation("java.util.concurrent.ExecutionException", "<init>", "(Ljava/lang/String;)V",
                false, 0, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ExecutionException", "<init>",
                "(Ljava/lang/String;Ljava/lang/Throwable;)V", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ExecutionException", "<init>",
                "(Ljava/lang/String;Ljava/lang/Throwable;)V", false, 1, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ExecutionException", "<init>",
                "(Ljava/lang/Throwable;)V", false, 0, NullnessAnnotation.CHECK_FOR_NULL);

        database.addMethodParameterAnnotation("java.util.concurrent.RejectedExecutionException", "<init>",
                "(Ljava/lang/String;)V", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.RejectedExecutionException", "<init>",
                "(Ljava/lang/String;Ljava/lang/Throwable;)V", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.RejectedExecutionException", "<init>",
                "(Ljava/lang/String;Ljava/lang/Throwable;)V", false, 1, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.RejectedExecutionException", "<init>",
                "(Ljava/lang/Throwable;)V", false, 0, NullnessAnnotation.CHECK_FOR_NULL);

        database.addMethodParameterAnnotation("java.util.concurrent.TimeoutException", "<init>", "(Ljava/lang/String;)V",
                false, 0, NullnessAnnotation.CHECK_FOR_NULL);


        database.addMethodParameterAnnotation("java.util.concurrent.Executors$RunnableAdapter", "<init>",
                "(Ljava/lang/Runnable;Ljava/lang/Object;)V", false, 1, NullnessAnnotation.CHECK_FOR_NULL);


        database.addMethodParameterAnnotation("java.util.concurrent.ConcurrentSkipListMap", "<init>",
                "(Ljava/util/Comparator;)V", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ConcurrentSkipListMap", "doRemove",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false, 1, NullnessAnnotation.CHECK_FOR_NULL);

        database.addMethodParameterAnnotation("java.util.concurrent.ForkJoinPool", "casBarrierStack",
                "(Ljava/util/concurrent/ForkJoinPool$WaitQueueNode;Ljava/util/concurrent/ForkJoinPool$WaitQueueNode;)Z", false,
                0, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ForkJoinPool", "casBarrierStack",
                "(Ljava/util/concurrent/ForkJoinPool$WaitQueueNode;Ljava/util/concurrent/ForkJoinPool$WaitQueueNode;)Z", false,
                1, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ForkJoinPool", "casSpareStack",
                "(Ljava/util/concurrent/ForkJoinPool$WaitQueueNode;Ljava/util/concurrent/ForkJoinPool$WaitQueueNode;)Z", false,
                1, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ForkJoinTask", "adapt",
                "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/ForkJoinTask;", true, 1,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ForkJoinTask", "awaitDone",
                "(Ljava/util/concurrent/ForkJoinWorkerThread;J)I", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ForkJoinTask", "awaitDone",
                "(Ljava/util/concurrent/ForkJoinWorkerThread;Z)I", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ForkJoinTask$AdaptedRunnable", "<init>",
                "(Ljava/lang/Runnable;Ljava/lang/Object;)V", false, 1, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ForkJoinWorkerThread", "onTermination",
                "(Ljava/lang/Throwable;)V", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ForkJoinWorkerThread", "setSlot",
                "([Ljava/util/concurrent/ForkJoinTask;ILjava/util/concurrent/ForkJoinTask;)V", true, 2,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.LinkedTransferQueue", "casCleanMe",
                "(Ljava/util/concurrent/LinkedTransferQueue$Node;Ljava/util/concurrent/LinkedTransferQueue$Node;)Z", false, 0,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.LinkedTransferQueue", "casCleanMe",
                "(Ljava/util/concurrent/LinkedTransferQueue$Node;Ljava/util/concurrent/LinkedTransferQueue$Node;)Z", false, 1,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.LinkedTransferQueue", "casHead",
                "(Ljava/util/concurrent/LinkedTransferQueue$Node;Ljava/util/concurrent/LinkedTransferQueue$Node;)Z", false, 0,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.LinkedTransferQueue", "xfer",
                "(Ljava/lang/Object;ZIJ)Ljava/lang/Object;", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.LinkedTransferQueue$Itr", "advance",
                "(Ljava/util/concurrent/LinkedTransferQueue$Node;)V", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.LinkedTransferQueue$Node", "casItem",
                "(Ljava/lang/Object;Ljava/lang/Object;)Z", false, 1, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.LinkedTransferQueue$Node", "casNext",
                "(Ljava/util/concurrent/LinkedTransferQueue$Node;Ljava/util/concurrent/LinkedTransferQueue$Node;)Z", false, 0,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.Phaser", "<init>", "(Ljava/util/concurrent/Phaser;)V", false,
                0, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.Phaser", "<init>", "(Ljava/util/concurrent/Phaser;I)V",
                false, 0, NullnessAnnotation.CHECK_FOR_NULL);

        database.addMethodAnnotation("java.util.concurrent.locks.ReadWriteLock", "readLock",
                "()Ljava/util/concurrent/locks/Lock;", false, NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("java.util.concurrent.locks.ReadWriteLock", "writeLock",
                "()Ljava/util/concurrent/locks/Lock;", false, NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("java.util.concurrent.locks.ReentrantReadWriteLock", "readLock",
                "()Ljava/util/concurrent/locks/ReentrantReadWriteLock$ReadLock;", false, NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("java.util.concurrent.locks.ReentrantReadWriteLock", "writeLock",
                "()Ljava/util/concurrent/locks/ReentrantReadWriteLock$WriteLock;", false, NullnessAnnotation.NONNULL);

        database.addMethodParameterAnnotation("java.util.concurrent.ExecutorService", "submit",
                "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/Future;", false, 1,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.AbstractExecutorService", "submit",
                "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/Future;", false, 1,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ExecutorCompletionService", "submit",
                "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/Future;", false, 1,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.AbstractExecutorServiceNullnessAnnotationDatabase",
                "newTaskFor", "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/Future;", false, 1,
                NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ExecutorCompletionService", "newTaskFor",
                "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/RunnableFuture;", false, 1,
                NullnessAnnotation.CHECK_FOR_NULL);

        database.addMethodParameterAnnotation("java.util.concurrent.ThreadPoolExecutor", "addIfUnderCorePoolSize",
                "(Ljava/lang/Runnable;)Z", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ThreadPoolExecutor", "addThread",
                "(Ljava/lang/Runnable;)Ljava/lang/Thread;", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodParameterAnnotation("java.util.concurrent.ThreadPoolExecutor", "afterExecute",
                "(Ljava/lang/Runnable;Ljava/lang/Throwable;)V", false, 1, NullnessAnnotation.CHECK_FOR_NULL);

        database.addMethodParameterAnnotation("java.util.EnumMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0,
                NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.EnumMap", "containsKey", "(Ljava/lang/Object;)Z", false, 0,
                NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.EnumMap", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.EnumMap", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0,
                NullnessAnnotation.NONNULL);

        database.addMethodParameterAnnotation("java.util.SortedMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0,
                NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.SortedMap", "containsKey", "(Ljava/lang/Object;)Ljava/lang/Object;",
                false, 0, NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.SortedMap", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.SortedMap", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", false,
                0, NullnessAnnotation.NONNULL);

        database.addMethodParameterAnnotation("java.util.SortedSet", "add", "(Ljava/lang/Object;)Z", false, 0,
                NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.SortedSet", "remove", "(Ljava/lang/Object;)Z", false, 0,
                NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.SortedSet", "contains", "(Ljava/lang/Object;)Z", false, 0,
                NullnessAnnotation.NONNULL);

        database.addMethodParameterAnnotation("java.util.Hashtable", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0,
                NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.Hashtable", "containsKey", "(Ljava/lang/Object;)Z", false, 0,
                NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.Hashtable", "containsValue", "(Ljava/lang/Object;)Z", false, 0,
                NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.Hashtable", "contains", "(Ljava/lang/Object;)Z", false, 0,
                NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.Hashtable", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.Hashtable", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false, 1, NullnessAnnotation.NONNULL);

        database.addMethodParameterAnnotation("javax.swing.UIDefaults", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false, 1, NullnessAnnotation.CHECK_FOR_NULL);

        database.addMethodParameterAnnotation("java.util.Properties", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;",
                false, 0, NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.Properties", "setProperty",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false, 1, NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.Properties", "setProperty",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false, 0, NullnessAnnotation.NONNULL);

        database.addMethodParameterAnnotation("org.w3c.dom.Element", "setAttribute", "(Ljava/lang/String;Ljava/lang/String;)V",
                false, 0, NullnessAnnotation.NONNULL);

        database.addMethodParameterAnnotation("java.text.DateFormat", "parse",
                "(Ljava/lang/String;Ljava/text/ParsePosition;)Ljava/util/Date;", false, 0, NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.text.DateFormat", "parse", "(Ljava/lang/String;)Ljava/util/Date;", false, 0,
                NullnessAnnotation.NONNULL);

        // addMethodAnnotation("java.util.Queue", "poll",
        // "()Ljava/lang/Object;", false, NullnessAnnotation.CHECK_FOR_NULL);
        database.addMethodAnnotation("java.io.BufferedReader", "readLine", "()Ljava/lang/String;", false,
                NullnessAnnotation.CHECK_FOR_NULL);

        database.addMethodParameterAnnotation("com.google.common.base.Preconditions","checkNotNull","(Ljava/lang/Object;)Ljava/lang/Object;",
                true, 0, NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("com.google.common.base.Preconditions","checkNotNull","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                true, 0, NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("com.google.common.base.Preconditions","checkNotNull","(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;",
                true, 0, NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("com.google.common.base.Preconditions","checkNotNull","(Ljava/lang/Object;)Ljava/lang/Object;",
                true, NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("com.google.common.base.Preconditions","checkNotNull","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                true, NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("com.google.common.base.Preconditions","checkNotNull","(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;",
                true, NullnessAnnotation.NONNULL);

        database.addMethodParameterAnnotation("java.util.Objects","requireNonNull", "(Ljava/lang/Object;)Ljava/lang/Object;",
                true, 0, NullnessAnnotation.NONNULL);
        database.addMethodParameterAnnotation("java.util.Objects","requireNonNull", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",
                true, 0, NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("java.util.Objects","requireNonNull", "(Ljava/lang/Object;)Ljava/lang/Object;",
                true, NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("java.util.Objects","requireNonNull", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",
                true, NullnessAnnotation.NONNULL);


        database.addMethodAnnotation("org.w3c.dom.Element","getAttribute", "(Ljava/lang/String;)Ljava/lang/String;",
                false, NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("org.w3c.dom.Element","getAttributeNS", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false, NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("org.w3c.dom.Element","getElementsByTagName", "(Ljava/lang/String;)Lorg/w3c/dom/NodeList;",
                false, NullnessAnnotation.NONNULL);
        database.addMethodAnnotation("org.w3c.dom.Element","getElementsByTagNameNS", "(Ljava/lang/String;Ljava/lang/String;)Lorg/w3c/dom/NodeList;",
                false, NullnessAnnotation.NONNULL);


        addEclipseSpecificAnnotations(database);

        AnalysisContext.currentAnalysisContext().setMissingClassWarningsSuppressed(missingClassWarningsSuppressed);

    }

    private static void addEclipseSpecificAnnotations(INullnessAnnotationDatabase db) {
        //        if(true){
        //            return;
        //        }

        // usually either uses known common services or checks for unknown. Too much noise
        //        db.addMethodAnnotation("org.eclipse.core.runtime.IAdaptable","getAdapter","(Ljava/lang/Class;)Ljava/lang/Object;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.core.runtime.IAdapterFactory","getAdapter","(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.core.runtime.IAdapterManager","getAdapter","(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.eclipse.core.runtime.IAdapterManager","getAdapter","(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.eclipse.core.runtime.IAdapterManager","loadAdapter","(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        // usually reads are in-sync with writes, so too much noise
        //        db.addMethodAnnotation("org.eclipse.ui.IMemento","getChild","(Ljava/lang/String;)Lorg/eclipse/ui/IMemento;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.ui.IMemento","getID","()Ljava/lang/String;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.ui.IMemento","getInteger","(Ljava/lang/String;)Ljava/lang/Integer;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.ui.IMemento","getString","(Ljava/lang/String;)Ljava/lang/String;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.ui.IMemento","getTextData","()Ljava/lang/String;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        // too seldom, usually used after job.join.
        //        db.addMethodAnnotation("org.eclipse.core.runtime.jobs.Job","getResult","()Lorg/eclipse/core/runtime/IStatus;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.core.runtime.FileLocator","find","(Ljava/net/URL;)Ljava/net/URL;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.eclipse.core.runtime.FileLocator","find","(Lorg/osgi/framework/Bundle;Lorg/eclipse/core/runtime/IPath;Ljava/util/Map;)Ljava/net/URL;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.osgi.framework.Bundle","getEntry","(Ljava/lang/String;)Ljava/net/URL;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.osgi.framework.Bundle","getEntryPaths","(Ljava/lang/String;)Ljava/util/Enumeration;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.osgi.framework.Bundle","getResource","(Ljava/lang/String;)Ljava/net/URL;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.osgi.framework.Bundle","getResources","(Ljava/lang/String;)Ljava/util/Enumeration;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.osgi.framework.Bundle","getServicesInUse","()[Lorg/osgi/framework/ServiceReference;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        // usually not a problem - and there is a way to ask before if it is not empty
        //        db.addMethodAnnotation("org.eclipse.core.runtime.IPath","lastSegment","()Ljava/lang/String;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        // too much noise: similar as if annotating List.get(i)
        //        db.addMethodAnnotation("org.eclipse.core.runtime.IPath","segment","(I)Ljava/lang/String;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        // too much noise: usually search results are validated
        //        db.addMethodAnnotation("org.eclipse.core.resources.IContainer","findMember","(Ljava/lang/String;)Lorg/eclipse/core/resources/IResource;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.core.resources.IContainer","findMember","(Ljava/lang/String;Z)Lorg/eclipse/core/resources/IResource;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.core.resources.IContainer","findMember","(Lorg/eclipse/core/runtime/IPath;)Lorg/eclipse/core/resources/IResource;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.core.resources.IContainer","findMember","(Lorg/eclipse/core/runtime/IPath;Z)Lorg/eclipse/core/resources/IResource;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.core.resources.IContainer","getDefaultCharset","(Z)Ljava/lang/String;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.core.resources.IFile","getCharset","(Z)Ljava/lang/String;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.eclipse.core.resources.IFile","getContentDescription","()Lorg/eclipse/core/runtime/content/IContentDescription;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.core.resources.IProject","getNature","(Ljava/lang/String;)Lorg/eclipse/core/resources/IProjectNature;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        //        db.addMethodAnnotation("org.eclipse.core.resources.IProject","getWorkingLocation","(Ljava/lang/String;)Lorg/eclipse/core/runtime/IPath;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.core.resources.IWorkspaceRoot","getContainerForLocation","(Lorg/eclipse/core/runtime/IPath;)Lorg/eclipse/core/resources/IContainer;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.eclipse.core.resources.IWorkspaceRoot","getFileForLocation","(Lorg/eclipse/core/runtime/IPath;)Lorg/eclipse/core/resources/IFile;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        // override annotation from IResource: workspace root is always available
        // XXX seems not to work at all...
        //        db.addMethodAnnotation("org.eclipse.core.resources.IWorkspaceRoot","getLocation","()Lorg/eclipse/core/runtime/IPath;",
        //                false, NullnessAnnotation.NONNULL);

        // override annotation from IResource: workspace root has no parent
        db.addMethodAnnotation("org.eclipse.core.resources.IWorkspaceRoot","getParent","()Lorg/eclipse/core/resources/IContainer;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        // XXX too high rate of false positives, due the problem with IWorkspaceRoot.getLocation() above
        //        db.addMethodAnnotation("org.eclipse.core.resources.IResource","getLocation","()Lorg/eclipse/core/runtime/IPath;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        // XXX too high rate of false positives, due the problem with IWorkspaceRoot.getLocation() above
        //        db.addMethodAnnotation("org.eclipse.core.resources.IResource","getLocationURI","()Ljava/net/URI;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.core.resources.IResource","findMarker","(J)Lorg/eclipse/core/resources/IMarker;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.eclipse.core.resources.IResource","getFileExtension","()Ljava/lang/String;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        // only true for workspace root
        //        db.addMethodAnnotation("org.eclipse.core.resources.IResource","getParent","()Lorg/eclipse/core/resources/IContainer;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.core.resources.IResource","getPersistentProperty","(Lorg/eclipse/core/runtime/QualifiedName;)Ljava/lang/String;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        //        db.addMethodAnnotation("org.eclipse.core.resources.IResource","getProject","()Lorg/eclipse/core/resources/IProject;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        //        db.addMethodAnnotation("org.eclipse.core.resources.IResource","getRawLocation","()Lorg/eclipse/core/runtime/IPath;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        //        db.addMethodAnnotation("org.eclipse.core.resources.IResource","getResourceAttributes","()Lorg/eclipse/core/resources/ResourceAttributes;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.core.resources.IResource","getSessionProperty","(Lorg/eclipse/core/runtime/QualifiedName;)Ljava/lang/Object;",
                false, NullnessAnnotation.CHECK_FOR_NULL);


        db.addMethodAnnotation("org.eclipse.core.resources.IResourceDelta","findMember","(Lorg/eclipse/core/runtime/IPath;)Lorg/eclipse/core/resources/IResourceDelta;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.eclipse.core.resources.IResourceDelta","getMovedFromPath","()Lorg/eclipse/core/runtime/IPath;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.eclipse.core.resources.IResourceDelta","getMovedToPath","()Lorg/eclipse/core/runtime/IPath;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        // javadoc collision with IFile which claims to implement interface without returning null
        //        db.addMethodAnnotation("org.eclipse.core.resources.IStorage","getFullPath","()Lorg/eclipse/core/runtime/IPath;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        // Too many false positives
        //        db.addMethodAnnotation("org.eclipse.core.resources.IMarker","getAttribute","(Ljava/lang/String;)Ljava/lang/Object;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.team.core.RepositoryProvider","getProvider","(Lorg/eclipse/core/resources/IProject;)Lorg/eclipse/team/core/RepositoryProvider;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.eclipse.team.core.RepositoryProvider","getProvider","(Lorg/eclipse/core/resources/IProject;Ljava/lang/String;)Lorg/eclipse/team/core/RepositoryProvider;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.swt.widgets.Display","getCurrent","()Lorg/eclipse/swt/widgets/Display;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        //        db.addMethodAnnotation("org.eclipse.swt.widgets.Control","getParent","()Lorg/eclipse/swt/widgets/Composite;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        // Usually checked or used only if set before
        //        db.addMethodAnnotation("org.eclipse.swt.widgets.Widget","getData","()Ljava/lang/Object;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.swt.widgets.Widget","getData","(Ljava/lang/String;)Ljava/lang/Object;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        // there is a way to ask selection before - so we can't just always warn
        //        db.addMethodAnnotation("org.eclipse.jface.viewers.IStructuredSelection","getFirstElement","()Ljava/lang/Object;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        // too many false positives
        //        db.addMethodAnnotation("org.eclipse.jface.viewers.ISelectionProvider","getSelection","()Lorg/eclipse/jface/viewers/ISelection;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.jface.viewers.ITreeContentProvider","getParent","(Ljava/lang/Object;)Ljava/lang/Object;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.jface.viewers.ILazyTreeContentProvider","getParent","(Ljava/lang/Object;)Ljava/lang/Object;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.jface.viewers.ILabelProvider","getImage","(Ljava/lang/Object;)Lorg/eclipse/swt/graphics/Image;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        // too many false positives
        //        db.addMethodAnnotation("org.eclipse.jface.viewers.ILabelProvider","getText","(Ljava/lang/Object;)Ljava/lang/String;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.jface.viewers.IFontProvider","getFont","(Ljava/lang/Object;)Lorg/eclipse/swt/graphics/Font;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.jface.viewers.IColorProvider","getForeground","(Ljava/lang/Object;)Lorg/eclipse/swt/graphics/Color;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.eclipse.jface.viewers.IColorProvider","getBackground","(Ljava/lang/Object;)Lorg/eclipse/swt/graphics/Color;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.jface.viewers.IColorDecorator","decorateForeground","(Ljava/lang/Object;)Lorg/eclipse/swt/graphics/Color;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.eclipse.jface.viewers.IColorDecorator","decorateBackground","(Ljava/lang/Object;)Lorg/eclipse/swt/graphics/Color;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.jface.viewers.IFontDecorator","decorateFont","(Ljava/lang/Object;)Lorg/eclipse/swt/graphics/Font;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.jface.viewers.ILabelDecorator","decorateImage","(Lorg/eclipse/swt/graphics/Image;Ljava/lang/Object;)Lorg/eclipse/swt/graphics/Image;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.eclipse.jface.viewers.ILabelDecorator","decorateText","(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.jface.viewers.ITableColorProvider","getForeground","(Ljava/lang/Object;I)Lorg/eclipse/swt/graphics/Color;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.eclipse.jface.viewers.ITableColorProvider","getBackground","(Ljava/lang/Object;I)Lorg/eclipse/swt/graphics/Color;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.jface.viewers.ITableLabelProvider","getColumnImage","(Ljava/lang/Object;I)Lorg/eclipse/swt/graphics/Image;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        // too many false positives
        //        db.addMethodAnnotation("org.eclipse.jface.viewers.ITableLabelProvider","getColumnText","(Ljava/lang/Object;I)Ljava/lang/String;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        db.addMethodAnnotation("org.eclipse.ui.IWorkbenchPage","findView","(Ljava/lang/String;)Lorg/eclipse/ui/IViewPart;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.eclipse.ui.IWorkbenchPage","findEditor","(Lorg/eclipse/ui/IEditorInput;)Lorg/eclipse/ui/IEditorPart;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.eclipse.ui.IWorkbenchPage","findViewReference","(Ljava/lang/String;)Lorg/eclipse/ui/IViewReference;",
                false, NullnessAnnotation.CHECK_FOR_NULL);
        db.addMethodAnnotation("org.eclipse.ui.IWorkbenchPage","findViewReference","(Ljava/lang/String;Ljava/lang/String;)Lorg/eclipse/ui/IViewReference;",
                false, NullnessAnnotation.CHECK_FOR_NULL);

        // Too many false positives if used from editor, which is active anyway
        //        db.addMethodAnnotation("org.eclipse.ui.IWorkbenchPage","getActiveEditor","()Lorg/eclipse/ui/IEditorPart;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        // too many false positives
        //        db.addMethodAnnotation("org.eclipse.ui.IWorkbenchPage","openEditor","(Lorg/eclipse/ui/IEditorInput;Ljava/lang/String;)Lorg/eclipse/ui/IEditorPart;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.ui.IWorkbenchPage","openEditor","(Lorg/eclipse/ui/IEditorInput;Ljava/lang/String;ZI)Lorg/eclipse/ui/IEditorPart;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        //        db.addMethodAnnotation("org.eclipse.ui.IWorkbenchWindow","getActivePage","()Lorg/eclipse/ui/IWorkbenchPage;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        // too much noise because if a class is used in UI context, there is an active window.
        //        db.addMethodAnnotation("org.eclipse.ui.IWorkbench","getActiveWorkbenchWindow","()Lorg/eclipse/ui/IWorkbenchWindow;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        //        db.addMethodAnnotation("org.eclipse.jface.wizard.IWizard","getContainer","()Lorg/eclipse/jface/wizard/IWizardContainer;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.jface.wizard.IWizard","getDialogSettings","()Lorg/eclipse/jface/dialogs/IDialogSettings;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.jface.wizard.IWizard","getNextPage","(Lorg/eclipse/jface/wizard/IWizardPage;)Lorg/eclipse/jface/wizard/IWizardPage;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.jface.wizard.IWizard","getPreviousPage","(Lorg/eclipse/jface/wizard/IWizardPage;)Lorg/eclipse/jface/wizard/IWizardPage;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.jface.wizard.IWizard","getWindowTitle","()Ljava/lang/String;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.jface.wizard.IWizard","getPage","(Ljava/lang/String;)Lorg/eclipse/jface/wizard/IWizardPage;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //
        //        db.addMethodAnnotation("org.eclipse.jface.wizard.IWizardPage","getNextPage","()Lorg/eclipse/jface/wizard/IWizardPage;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.jface.wizard.IWizardPage","getPreviousPage","()Lorg/eclipse/jface/wizard/IWizardPage;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.jface.wizard.IWizardPage","getWizard","()Lorg/eclipse/jface/wizard/IWizard;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        // usually statically assigned resources - so either it never worked or always ok. Too noisy
        //        db.addMethodAnnotation("org.eclipse.jface.resource.ImageRegistry","getDescriptor","(Ljava/lang/String;)Lorg/eclipse/jface/resource/ImageDescriptor;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.jface.resource.ImageRegistry","get","(Ljava/lang/String;)Lorg/eclipse/swt/graphics/Image;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //
        //        db.addMethodAnnotation("org.eclipse.jface.resource.ColorRegistry","get","(Ljava/lang/String;)Lorg/eclipse/swt/graphics/Color;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //        db.addMethodAnnotation("org.eclipse.jface.resource.ColorRegistry","getRGB","(Ljava/lang/String;)Lorg/eclipse/swt/graphics/RGB;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
        //
        //        db.addMethodAnnotation("org.eclipse.jface.resource.JFaceResources","getImage","(Ljava/lang/String;)Lorg/eclipse/swt/graphics/Image;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);

        //        db.addMethodAnnotation("org.eclipse.jface.action.IAction","getImageDescriptor","()Lorg/eclipse/jface/resource/ImageDescriptor;",
        //                false, NullnessAnnotation.CHECK_FOR_NULL);
    }
}
