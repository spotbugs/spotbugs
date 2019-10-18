Annotations
===========

SpotBugs supports several annotations to express the developer's intent so that SpotBugs can issue warnings more appropriately.
Annotations for SpotBugs (mostly deprecated except for SuppressFBWarnings).

edu.umd.cs.findbugs.annotations.CheckForNull
--------------------------------------------
The annotated element might be null, and uses of the element should check for null.

edu.umd.cs.findbugs.annotations.CheckReturnValue
-------------------------------------------------
This annotation is used to denote a method whose return value should always be checked when invoking the method.

edu.umd.cs.findbugs.annotations.CleanupObligation
-------------------------------------------------
Mark a class or interface as a resource type requiring cleanup.

edu.umd.cs.findbugs.annotations.CreatesObligation
-------------------------------------------------
Mark a constructor or method as creating a resource which requires cleanup.

edu.umd.cs.findbugs.annotations.DefaultAnnotation
-------------------------------------------------
Indicates that all members of the class or package should be annotated with the default value of the supplied annotation class.

edu.umd.cs.findbugs.annotations.DefaultAnnotationForFields
----------------------------------------------------------
Indicates that all members of the class or package should be annotated with the default value of the supplied annotation class.

edu.umd.cs.findbugs.annotations.DefaultAnnotationForMethods
-----------------------------------------------------------
Indicates that all members of the class or package should be annotated with the default value of the supplied annotation class.

edu.umd.cs.findbugs.annotations.DefaultAnnotationForParameters
--------------------------------------------------------------
Indicates that all members of the class or package should be annotated with the default value of the supplied annotation class.

edu.umd.cs.findbugs.annotations.DesireNoWarning (Deprecated)
------------------------------------------------------------
The annotation based approach is useless for lambdas.

edu.umd.cs.findbugs.annotations.DesireWarning (Deprecated)
----------------------------------------------------------
The annotation based approach is useless for lambdas.

edu.umd.cs.findbugs.annotations.DischargesObligation
----------------------------------------------------
Mark a method as cleaning up a resource.

edu.umd.cs.findbugs.annotations.ExpectWarning (Deprecated)
----------------------------------------------------------
The annotation based approach is useless for lambdas.

edu.umd.cs.findbugs.annotations.NonNull
---------------------------------------
The annotated element must not be null.

edu.umd.cs.findbugs.annotations.NoWarning (Deprecated)
------------------------------------------------------
The annotation based approach is useless for lambdas.

edu.umd.cs.findbugs.annotations.Nullable
----------------------------------------
The annotated element could be null under some circumstances.
This is treated the same way as not being annotated.

edu.umd.cs.findbugs.annotations.OverrideMustInvoke
--------------------------------------------------
Used to annotate a method that, if overridden, must (or should) be invoked by an invocation on super in the overriding method.

edu.umd.cs.findbugs.annotations.PossiblyNull (Deprecated)
---------------------------------------------------------
Use `CheckForNull` instead. 
The name of which more clearly indicates that not only could the value be null, 
but that good coding practice requires that the value be checked for null.

edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault
---------------------------------------------------------------
This annotation can be applied to a package, class or method to indicate that the methods in that element have nonnull return 
values by default unless there is: An explicit nullness annotation The method overrides a method in a superclass 
(in which case the annotation of the corresponding parameter in the superclass applies) there is a default annotation applied 
to a more tightly nested element.

edu.umd.cs.findbugs.annotations.SuppressFBWarnings
--------------------------------------------------
Used to suppress SpotBugs warnings.

edu.umd.cs.findbugs.annotations.SuppressWarnings (Deprecated)
-------------------------------------------------------------
Use `SuppressFBWarnings` instead.

edu.umd.cs.findbugs.annotations.UnknownNullness
-----------------------------------------------
Used to indicate that the nullness of element is unknown, or may vary in unknown ways in subclasses.


edu.umd.cs.findbugs.annotations.CleanupObligation (Deprecated)
--------------------------------------------------------------
Mark a class or interface as a resource type requiring cleanup.

edu.umd.cs.findbugs.annotations.CreatesObligation (Deprecated)
--------------------------------------------------------------
Mark a constructor or method as creating a resource which requires cleanup. 
The marked method must be a member of a class marked with the CleanupObligation annotation.

edu.umd.cs.findbugs.annotations.DischargesObligation (Deprecated)
-----------------------------------------------------------------
Mark a method as cleaning up a resource. The marked method must be a member of a class marked with the CleanupObligation annotation.
