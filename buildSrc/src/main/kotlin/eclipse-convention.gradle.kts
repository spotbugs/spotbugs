plugins {
  id ("com.diffplug.oomph.ide")
  id ("com.diffplug.eclipse.mavencentral")
}

val pdeTool by configurations.creating {
  setTransitive(false)
}
val updateSitePublisher by configurations.creating {
  setTransitive(false)
}

eclipseMavenCentral {
  // TODO: it should include what is required in the MANIFEST.MF, and nothing else
  release("4.6.3") {
    dep("pdeTool", "org.eclipse.pde.build")
    dep("updateSitePublisher", "org.eclipse.equinox.launcher")
  }
}

/**
 * Unzip "org.eclipse.pde.build" package into the outputDir.
 */
val unzipPdeTool = tasks.register<Copy>("unzipPdeTool") {
  from(zipTree(pdeTool.singleFile))
  into("$buildDir/pdeTool")
}

fun createGenerateP2MetadataTask(classifier: String): TaskProvider<JavaExec> {
  val dirName: String = if (classifier.isEmpty()) "eclipse" else "eclipse-$classifier"
  val dir = file("$buildDir/site/$dirName")

  return tasks.register<JavaExec>("generate${ classifier.capitalize() }P2Metadata") {
    doFirst {
      project.delete(dir.resolve("artifacts.xml"))
      project.delete(dir.resolve("content.xml"))
      files(updateSitePublisher.singleFile).forEach { println(it) }
    }
    classpath = files(updateSitePublisher)
    args = listOf(
      "-application", "org.eclipse.equinox.p2.publisher.UpdateSitePublisher",
      "-metadataRepository", dir.toURI().toString(),
      "-artifactRepository", dir.toURI().toString(),
      "-source", dir.absolutePath)
  }
}

listOf("").forEach {
  createGenerateP2MetadataTask(it)
}

dependencies {
  compileOnly(files("$buildDir/pdeTool/pdebuild.jar"){
    builtBy(unzipPdeTool)
  })
}
