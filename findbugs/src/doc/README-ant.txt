To use the find bugs task with ant (version 1.5 or later)

1) Copy findbugs-ant.jar, findbugs.jar and bcel.jar into your $ANT_HOME/lib direcotry or add these to your class path.

2) For ant to recognise the findbugs task add the following line to the top
 of your project. 
  <taskdef classname="edu.umd.cs.findbugs.anttask.FindBugsTask" name="findbugs"/>

 
