/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import uk.gov.hmrc.DefaultBuildSettings.{ defaultSettings, scalaSettings }
import play.twirl.sbt.Import.TwirlKeys
import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys

val appName = "secure-message-frontend"

Global / majorVersion := 1
Global / scalaVersion := "3.3.6"

val excludedPackages: Seq[String] = Seq(
  "<empty>",
  "Reverse.*",
  ".*Routes.*",
  ".*BuildInfo.*",
  ".*\\$anon.*",
  "testOnlyDoNotUseInAppConf.*",
  "views.viewmodels.*"
)

lazy val scoverageSettings =
  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(","),
    ScoverageKeys.coverageMinimumStmtTotal := 67.90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, BuildInfoPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(scalaSettings *)
  .settings(defaultSettings() *)
  .settings(
    name := appName,
    RoutesKeys.routesImport ++= Seq(
      "models._",
      "controllers.generic.models._",
      "controllers.binders._",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
    ),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    TwirlKeys.templateImports ++= Seq(
      "config.AppConfig",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
    ),
    PlayKeys.playDefaultPort := 9055,
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator
  )
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)
  )
  .settings(
    scalacOptions := scalacOptions.value.diff(Seq("-Wunused:all")),
    scalacOptions ++= Seq(
      // Silence unused imports in template files
      "-Wconf:msg=unused import&src=.*:s",
      // Silence "Flag -XXX set repeatedly"
      "-Wconf:msg=Flag.*repeatedly:s",
      // Silence unused warnings on Play `routes` files
      "-Wconf:src=routes/.*:s"
    )
  )
  .settings(scoverageSettings.settings *)

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(`microservice` % "test->test")
  .settings(
    scalacOptions ++= Seq(
      // Silence "Flag -XXX set repeatedly"
      "-Wconf:msg=Flag.*repeatedly:s"
    )
  )

Test / test := (Test / test)
  .dependsOn(scalafmtCheckAll)
  .value

it / test := (it / Test / test)
  .dependsOn(scalafmtCheckAll, it / scalafmtCheckAll)
  .value
