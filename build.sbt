ThisBuild / scalaVersion := "3.3.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "edu.efrei"

lazy val root = (project in file("."))
  .settings(
    name := "library-management-system",
    
    // Coverage configuration
    coverageExcludedPackages := ".*CLI.*;.*DataLoader.*;.*RecommendationEngine.*;.*ValidationSystem.*;.*AuthSession.*;.*DataTransformation.*",
    coverageExcludedFiles := ".*Main.*;.*DataTransformation.*;.*AuthSession.*;.*FileManager.*;.*BackupRestore.*;.*utils.*",
    coverageMinimumStmtTotal := 30,
    coverageFailOnMinimum := false,
    
    // ScalaDoc configuration
    Compile / doc / scalacOptions ++= Seq(
      "-doc-title", "Library Management System - Scala 3",
      "-doc-version", version.value,
      "-sourcepath", (Compile / sourceDirectories).value.head.getAbsolutePath,
      "-doc-source-url", "https://github.com/MarcoSrhl/library-management-system-scala3",
      "-social-links:github::https://github.com/MarcoSrhl/library-management-system-scala3"
    ),
    
    libraryDependencies ++= Seq(
      // Testing
      "org.scalatest" %% "scalatest" % "3.2.17" % Test,
      "org.scalacheck" %% "scalacheck" % "1.17.0" % Test,
      "org.scalatestplus" %% "scalacheck-1-17" % "3.2.17.0" % Test,
      // Functional Programming
      "org.typelevel" %% "cats-core" % "2.10.0",
      // JSON
      "io.circe" %% "circe-core" % "0.14.6",
      "io.circe" %% "circe-generic" % "0.14.6",
      "io.circe" %% "circe-parser" % "0.14.6",
      "com.lihaoyi" %% "upickle" % "3.1.0"
    )
  )
