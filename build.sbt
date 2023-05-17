// Basic facts
name := "jackson-module-scala"

organization := "com.fasterxml.jackson.module"

scalaVersion := "2.13.10"

crossScalaVersions := Seq("2.10.4", "2.11.2", "2.13.10")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

//scalacOptions in (Compile, compile) += "-Xfatal-warnings"

// Ensure jvm 1.6 for java
// javacOptions ++= Seq("-source", "1.6", "-target", "1.6")

// Try to future-proof scala jvm targets, in case some future scala version makes 1.7 a default
// scalacOptions += "-target:jvm-1.6"

libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "com.fasterxml.jackson.core" % "jackson-core" % "2.4.5",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.4.5",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.5",
    "com.thoughtworks.paranamer" % "paranamer" % "2.6",
    "com.google.code.findbugs" % "jsr305" % "2.0.1",
    "com.google.guava" % "guava" % "15.0",
    // test dependencies
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.4.5" % "test",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % "2.4.5" % "test",
    "com.fasterxml.jackson.module" % "jackson-module-jsonSchema" % "2.4.5" % "test",
    "org.scalatest" %% "scalatest" % "3.0.8" % "test",
    "junit" % "junit" % "4.11" % "test"
)

// build.properties
resourceGenerators in Compile <+=
  (resourceManaged in Compile, version, organization, name) map { (dir, v, o, n) =>
    val file = dir / "com" / "fasterxml" / "jackson" / "module" / "scala" / "build.properties"
    val contents = "version=%s\ngroupId=%s\nartifactId=%s\n".format(v, o, n)
    IO.write(file, contents)
    Seq(file)
  }

// site
//site.settings

//site.includeScaladoc()

//ghpages.settings

//git.remoteRepo := "git@github.com:FasterXML/jackson-module-scala.git"
