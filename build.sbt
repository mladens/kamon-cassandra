/* =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */


val kamonCore           = "io.kamon"            %% "kamon-core"               % "1.1.3"
val kamonTestkit        = "io.kamon"            %% "kamon-testkit"            % "1.1.3"
val scalaExtension      = "io.kamon"            %% "kanela-scala-extension"   % "0.0.14"

val cassandraDriver     = "com.datastax.cassandra"    % "cassandra-driver-core"	    % "3.6.0"
val cassandraAll        = "org.apache.cassandra"      % "cassandra-all"        	    % "3.11.2"
val cassandraUnit	      = "org.cassandraunit"         % "cassandra-unit"       	    % "3.5.0.1"
val lombok              = "org.projectlombok"         % "lombok"                    % "1.18.0"


lazy val root = (project in file("."))
  .settings(noPublishing: _*)
  .aggregate(cassandraClient, cassandraServerPublishing)


lazy val cassandraClient = (project in file("kamon-cassandra-client"))
  .enablePlugins(JavaAgent)
  .settings(bintrayPackage := "kamon-cassandra")
  .settings(name := "kamon-cassandra-client")
  .settings(javaAgents += "io.kamon"    % "kanela-agent"   % "0.0.15"  % "compile;test")
  .settings(resolvers += Resolver.bintrayRepo("kamon-io", "snapshots"))
  .settings(resolvers += Resolver.mavenLocal)
  .settings(
      libraryDependencies ++=
        compileScope(kamonCore, cassandraDriver, scalaExtension) ++
        providedScope(lombok) ++
        testScope(cassandraUnit, kamonTestkit, scalatest, slf4jApi, logbackClassic))

lazy val cassandraServer = (project in file("kamon-cassandra-server"))
  .enablePlugins(AssemblyPlugin)
  .settings(bintrayPackage := "kamon-cassandra")
  .settings(name := "kamon-cassandra-server")
  .settings(skip in publish := true)
  .settings(resolvers += Resolver.bintrayRepo("kamon-io", "snapshots"))
  .settings(resolvers += Resolver.mavenLocal)
  .settings(fork in Test := true)
  .settings(javaOptions in Test := Seq("-Dcassandra.custom_tracing_class=kamon.cassandra.server.KamonTracing"))
  .settings( assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*)  => MergeStrategy.discard
    case PathList("library.properties", xs @ _*)  => MergeStrategy.discard
    case PathList("rootdoc.txt", xs @ _*)  => MergeStrategy.discard
    case _                              => MergeStrategy.first
  })
  .settings(assemblyOption in assembly := (assemblyOption in assembly).value.copy(
    includeScala = true,
    includeDependency = true,
    includeBin = true
  ))
  .settings(assemblyShadeRules in assembly := Seq(
    ShadeRule.rename("org.HdrHistogram.**"-> "shaded.@0").inAll,
    ShadeRule.rename("ch.qos.logback.**"-> "shaded.@0").inAll,
    ShadeRule.rename("io.netty.**"-> "shaded.@0").inAll,
    ShadeRule.rename("com.typesafe.**"-> "shaded.@0").inAll,
    ShadeRule.rename("scala.**"-> "shaded.@0").inAll,
    ShadeRule.rename("org.objectweb**"-> "shaded.@0").inAll,
    ShadeRule.rename("org.slf4j**"-> "shaded.@0").inAll,
    ShadeRule.rename("com.codahale**"-> "shaded.@0").inAll,
    ShadeRule.rename("com.datastax**"-> "shaded.@0").inAll,
    ShadeRule.rename("com.google**"-> "shaded.@0").inAll,
    ShadeRule.rename("com.kenai**"-> "shaded.@0").inAll,
    ShadeRule.zap("sourcecode.**").inAll,
    ShadeRule.zap("kanela.**").inAll,
    ShadeRule.zap("fansi.**").inAll,
    ShadeRule.zap("io.vavr.**").inAll
  ))
  .settings(
        libraryDependencies ++=
          compileScope(kamonCore, cassandraDriver, scalaExtension) ++
            providedScope(lombok, cassandraAll) ++
            testScope(cassandraUnit, kamonTestkit, scalatest, slf4jApi, logbackClassic))


lazy val cassandraServerPublishing = project
  .settings(
    name := "kamon-cassandra-server",
    packageBin in Compile := (assembly in (cassandraServer, Compile)).value,
    packageSrc in Compile := (packageSrc in (cassandraServer, Compile)).value
  )