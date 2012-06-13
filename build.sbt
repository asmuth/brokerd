import AssemblyKeys._

name := "Fyrehose"

organization := "com.paulasmuth"

version := "0.0.4"

scalaSource in Compile <<= baseDirectory(_ / "src")

mainClass in (Compile, run) := Some("com.paulasmuth.fyrehose.Fyrehose")

assemblySettings

jarName in assembly <<= (version) { v => "fyrehose_" + v + ".jar" }

scalaVersion := "2.9.1"

libraryDependencies += "com.google.code.gson" % "gson" % "1.4"
