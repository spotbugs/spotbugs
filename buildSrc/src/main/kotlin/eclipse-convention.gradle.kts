plugins {
  id ("com.diffplug.oomph.ide")
  id ("com.diffplug.eclipse.mavencentral")
}

val pdeTool by configurations.creating {
  setTransitive(false)
}

eclipseMavenCentral {
  // TODO: it should include what is required in the MANIFEST.MF, and nothing else
  release("4.6.3") {
    dep("pdeTool", "org.eclipse.pde.build")
  }
}

/**
 * Unzip "org.eclipse.pde.build" package into the outputDir.
 */
val unzipPdeTool = tasks.register<Copy>("unzipPdeTool") {
  from(zipTree(pdeTool.singleFile))
  into("$buildDir/pdeTool")
}

dependencies {
  implementation(files("$buildDir/pdeTool/pdebuild.jar"){
    builtBy(unzipPdeTool)
  })
}
