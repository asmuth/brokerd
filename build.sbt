import AssemblyKeys._

name := "Fyrehose"

organization := "com.paulasmuth"

version := "0.0.1"

scalaSource in Compile <<= baseDirectory(_ / "src")

mainClass in (Compile, run) := Some("com.paulasmuth.fyrehose.Fyrehose")

assemblySettings

scalaVersion := "2.9.1"

libraryDependencies += "com.google.code.gson" % "gson" % "1.4"
