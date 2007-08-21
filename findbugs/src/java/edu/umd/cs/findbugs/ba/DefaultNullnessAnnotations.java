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

/**
 * @author David Hovemeyer
 */
public class DefaultNullnessAnnotations {
	/**
	 * Add default NullnessAnnotations to given INullnessAnnotationDatabase.
	 * 
	 * @param database an INullnessAnnotationDatabase
	 */
	public static void addDefaultNullnessAnnotations(INullnessAnnotationDatabase database) {
		if (AnnotationDatabase.IGNORE_BUILTIN_ANNOTATIONS) {
			return;
		}
		
		boolean missingClassWarningsSuppressed = AnalysisContext.currentAnalysisContext().setMissingClassWarningsSuppressed(true);
		
		database.addDefaultAnnotation(AnnotationDatabase.METHOD, "java.lang.String", NullnessAnnotation.NONNULL);
		database.addFieldAnnotation("java.lang.System", "out", "Ljava/io/PrintStream;", true, NullnessAnnotation.NONNULL);
		database.addFieldAnnotation("java.lang.System", "err", "Ljava/io/PrintStream;", true, NullnessAnnotation.NONNULL);
		database.addFieldAnnotation("java.lang.System", "in", "Ljava/io/InputStream;", true, NullnessAnnotation.NONNULL);

		database.addMethodAnnotation("java.lang.Class", "newInstance", "()Ljava/lang/Object;", false, NullnessAnnotation.NONNULL);
		database.addMethodAnnotation("java.lang.Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", true, NullnessAnnotation.NONNULL);
		database.addMethodAnnotation("java.lang.reflect.Method", "getParameterTypes", "()[Ljava/lang/Class;", false, NullnessAnnotation.NONNULL);
		database.addMethodAnnotation("java.lang.Object", "clone", "()Ljava/lang/Object;", false, NullnessAnnotation.NONNULL);
		database.addMethodAnnotation("java.lang.Object", "toString", "()Ljava/lang/String;", false, NullnessAnnotation.NONNULL);
		database.addMethodAnnotation("java.lang.Object", "getClass", "()Ljava/lang/Class;", false, NullnessAnnotation.NONNULL);

		database.addMethodParameterAnnotation("java.util.Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;", true, 0, NullnessAnnotation.NONNULL);

		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.package-info", NullnessAnnotation.NONNULL);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.CopyOnWriteArrayList", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.CopyOnWriteArraySet", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.ConcurrentLinkedQueue$Node", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.Exchanger", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.FutureTask", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.LinkedBlockingQueue$Node", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.SynchronousQueue$WaitQueue", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.SynchronousQueue$Node", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.ThreadPoolExecutor$Worker", NullnessAnnotation.UNKNOWN_NULLNESS);

		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.AbstractExecutorService", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.ConcurrentSkipListMap$ConcurrentSkipListSubMap", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.ConcurrentSkipListMap$HeadIndex", NullnessAnnotation.UNKNOWN_NULLNESS);
		
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.ConcurrentSkipListMap$Index", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.ConcurrentSkipListMap$Node", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.ConcurrentSkipListMap$SubMap", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.ConcurrentSkipListSet$ConcurrentSkipListSubSet", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.LinkedBlockingDeque$Node", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.SynchronousQueue$TransferQueue", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.SynchronousQueue$TransferQueue$QNode", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.SynchronousQueue$TransferStack", NullnessAnnotation.UNKNOWN_NULLNESS);
		database.addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.SynchronousQueue$Transferer", NullnessAnnotation.UNKNOWN_NULLNESS);


		database.addMethodParameterAnnotation("java.util.concurrent.FutureTask", "<init>", "(Ljava/lang/Runnable;Ljava/lang/Object;)V", false, 1, NullnessAnnotation.CHECK_FOR_NULL);
		database.addMethodParameterAnnotation("java.util.concurrent.Executors", "callable", "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/Callable;", true, 1, NullnessAnnotation.CHECK_FOR_NULL);

		database.addMethodParameterAnnotation("java.util.concurrent.ThreadPoolExecutor", "addWorker", "(Ljava/lang/Runnable;Z)Z", false, 0, NullnessAnnotation.CHECK_FOR_NULL);

		database.addMethodParameterAnnotation("java.util.concurrent.ConcurrentHashMap$Segment", "remove", "(Ljava/lang/Object;ILjava/lang/Object;)Ljava/lang/Object;", false, 2, NullnessAnnotation.CHECK_FOR_NULL);
		database.addMethodParameterAnnotation("java.util.concurrent.CyclicBarrier", "<init>", "(ILjava/lang/Runnable;)V", false, 1, NullnessAnnotation.CHECK_FOR_NULL);
		database.addMethodParameterAnnotation("java.util.concurrent.Executors$RunnableAdapter", "<init>", "(Ljava/lang/Runnable;Ljava/lang/Object;)V", false, 1, NullnessAnnotation.CHECK_FOR_NULL);
		database.addMethodParameterAnnotation("java.util.concurrent.ConcurrentSkipListMap", "doRemove", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false, 1, NullnessAnnotation.CHECK_FOR_NULL);
		database.addMethodAnnotation("java.util.concurrent.ConcurrentHashMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false, NullnessAnnotation.CHECK_FOR_NULL);
		database.addMethodAnnotation("java.util.concurrent.ConcurrentHashMap", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", false, NullnessAnnotation.CHECK_FOR_NULL);
		database.addMethodAnnotation("java.util.concurrent.ConcurrentHashMap", "putIfAbsent", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false, NullnessAnnotation.CHECK_FOR_NULL);


		database.addMethodAnnotation("java.util.concurrent.locks.ReadWriteLock", "readLock", "()Ljava/util/concurrent/locks/Lock;", false, NullnessAnnotation.NONNULL);
		database.addMethodAnnotation("java.util.concurrent.locks.ReadWriteLock", "writeLock", "()Ljava/util/concurrent/locks/Lock;", false, NullnessAnnotation.NONNULL);
		database.addMethodAnnotation("java.util.concurrent.locks.ReentrantReadWriteLock", "readLock", "()Ljava/util/concurrent/locks/ReentrantReadWriteLock$ReadLock;", false, NullnessAnnotation.NONNULL);
		database.addMethodAnnotation("java.util.concurrent.locks.ReentrantReadWriteLock", "writeLock", "()Ljava/util/concurrent/locks/()Ljava/util/concurrent/locks/ReentrantReadWriteLock$WriteLock;", false, NullnessAnnotation.NONNULL);


		database.addMethodParameterAnnotation("java.util.concurrent.ExecutorService", "submit", "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/Future;", false, 1, NullnessAnnotation.CHECK_FOR_NULL);
		database.addMethodParameterAnnotation("java.util.concurrent.AbstractExecutorService", "submit", "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/Future;", false, 1, NullnessAnnotation.CHECK_FOR_NULL);
		database.addMethodParameterAnnotation("java.util.concurrent.ExecutorCompletionService", "submit", "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/Future;", false, 1, NullnessAnnotation.CHECK_FOR_NULL);
		database.addMethodParameterAnnotation("java.util.concurrent.AbstractExecutorServiceNullnessAnnotationDatabase", "newTaskFor", "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/Future;", false, 1, NullnessAnnotation.CHECK_FOR_NULL);
		database.addMethodParameterAnnotation("java.util.concurrent.ExecutorCompletionService", "newTaskFor", "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/RunnableFuture;", false, 1, NullnessAnnotation.CHECK_FOR_NULL);

		database.addMethodParameterAnnotation("java.util.concurrent.ThreadPoolExecutor", "addIfUnderCorePoolSize", "(Ljava/lang/Runnable;)Z", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
		database.addMethodParameterAnnotation("java.util.concurrent.ThreadPoolExecutor", "addThread", "(Ljava/lang/Runnable;)Ljava/lang/Thread;", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
		database.addMethodParameterAnnotation("java.util.concurrent.ThreadPoolExecutor", "afterExecute", "(Ljava/lang/Runnable;Ljava/lang/Throwable;)V", false, 1, NullnessAnnotation.CHECK_FOR_NULL);



		database.addMethodParameterAnnotation("java.util.EnumMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
		database.addMethodParameterAnnotation("java.util.EnumMap", "containsKey", "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
		database.addMethodParameterAnnotation("java.util.EnumMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
		database.addMethodParameterAnnotation("java.util.EnumMap", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);

		database.addMethodParameterAnnotation("java.util.SortedMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
		database.addMethodParameterAnnotation("java.util.SortedMap", "containsKey", "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
		database.addMethodParameterAnnotation("java.util.SortedMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
		database.addMethodParameterAnnotation("java.util.SortedMap", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);

		database.addMethodParameterAnnotation("java.util.SortedSet", "add", "(Ljava/lang/Object;)Z", false, 0, NullnessAnnotation.NONNULL);
		database.addMethodParameterAnnotation("java.util.SortedSet", "remove", "(Ljava/lang/Object;)Z", false, 0, NullnessAnnotation.NONNULL);
		database.addMethodParameterAnnotation("java.util.SortedSet", "cotains", "(Ljava/lang/Object;)Z", false, 0, NullnessAnnotation.NONNULL);

		// addMethodAnnotation("java.util.Queue", "poll", "()Ljava/lang/Object;", false, NullnessAnnotation.CHECK_FOR_NULL);
		database.addMethodAnnotation("java.io.BufferedReader", "readLine", "()Ljava/lang/String;", false, NullnessAnnotation.CHECK_FOR_NULL);

		AnalysisContext.currentAnalysisContext().setMissingClassWarningsSuppressed(missingClassWarningsSuppressed);
		
	}
}
