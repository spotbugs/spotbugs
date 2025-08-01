plugins {
  id ("com.diffplug.eclipse.mavencentral")
}

val pdeTool by configurations.creating {
  setTransitive(false)
}

eclipseMavenCentral {
  silenceEquoIDE()
  release("4.24.0") {
    compileOnly("org.eclipse.ant.core")
    compileOnly("org.eclipse.core.resources")
    compileOnly("org.eclipse.core.runtime")
    compileOnly("org.eclipse.jdt.core")
    compileOnly("org.eclipse.jdt.ui")
    compileOnly("org.eclipse.jface")
    compileOnly("org.eclipse.pde")
    compileOnly("org.eclipse.ui.workbench")
    testImplementation("org.eclipse.core.runtime")

    dep("pdeTool", "org.eclipse.pde.build")

    // TODO these packages are not listed in the manifest
    compileOnly("org.eclipse.pde.ui")
    compileOnly("org.eclipse.swt")

    // necessary to build with the org.eclipse.swt module
    useNativesForRunningPlatform()

    constrainTransitivesToThisRelease()
  }
}

/**
 * Unzip "org.eclipse.pde.build" package into the outputDir.
 */
val pdeToolDir = layout.buildDirectory.dir("pdeTool")
val unzipPdeTool = tasks.register<Copy>("unzipPdeTool") {
  from(zipTree(pdeTool.singleFile))
  into(pdeToolDir)
}

dependencies {
  compileOnly(files(pdeToolDir.map { it.file("pdebuild.jar") }){
    builtBy(unzipPdeTool)
  })
}
