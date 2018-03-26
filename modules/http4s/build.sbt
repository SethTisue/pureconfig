name := "pureconfig-http4s"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-core" % "0.18.4",
  Dependencies.scalaMacrosParadise,
  Dependencies.scalaTest
)

// http4s 0.18.0 isn't published for Scala 2.10
crossScalaVersions ~= { oldVersions => oldVersions.filterNot(_.startsWith("2.10")) }

developers := List(
  Developer("jcranky", "Paulo Siqueira", "paulo.siqueira@gmail.com", url("https://github.com/jcranky")))

osgiSettings

OsgiKeys.exportPackage := Seq("pureconfig.modules.http4s.*")
OsgiKeys.privatePackage := Seq()
OsgiKeys.importPackage := Seq(s"""scala.*;version="[${scalaBinaryVersion.value}.0,${scalaBinaryVersion.value}.50)"""", "*")
