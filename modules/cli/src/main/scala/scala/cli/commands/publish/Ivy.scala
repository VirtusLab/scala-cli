package scala.cli.commands.publish

import coursier.core.{Configuration, ModuleName, Organization, Type}
import coursier.publish.Pom
import coursier.publish.Pom.{Developer, License, Scm}

import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.{LocalDateTime, ZoneOffset}

import scala.collection.mutable
import scala.xml.NodeSeq

object Ivy {

  private lazy val dateFormatter = new DateTimeFormatterBuilder()
    .appendValue(ChronoField.YEAR, 4)
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .appendValue(ChronoField.DAY_OF_MONTH, 2)
    .appendValue(ChronoField.HOUR_OF_DAY, 2)
    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
    .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
    .toFormatter

  def create(
    organization: Organization,
    moduleName: ModuleName,
    version: String,
    packaging: Option[Type] = None,
    description: Option[String] = None,
    url: Option[String] = None,
    name: Option[String] = None,
    // TODO Accept full-fledged coursier.Dependency
    dependencies: Seq[(Organization, ModuleName, String, Option[Configuration])] = Nil,
    license: Option[License] = None,
    scm: Option[Scm] = None,
    developers: Seq[Developer] = Nil,
    time: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    hasPom: Boolean = true,
    hasDoc: Boolean = true,
    hasSources: Boolean = true
  ): String = {

    val nodes = new mutable.ListBuffer[NodeSeq]

    nodes += {
      val desc = (description, url) match {
        case (Some(d), Some(u)) =>
          Seq(<description homepage={u}>{d}</description>)
        case (Some(d), None) =>
          Seq(<description>{d}</description>)
        case (None, Some(u)) =>
          Seq(<description homepage={u}></description>)
        case (None, None) =>
          Nil
      }
      <info organisation={organization.value} module={moduleName.value} revision={
        version
      } status="integration" publication={time.format(dateFormatter)}>
        {desc}
      </info>
    }

    nodes += {

      val docConf =
        if (hasDoc) Seq(<conf name="docs" visibility="public" description=""/>)
        else Nil
      val sourcesConf =
        if (hasSources) Seq(<conf name="sources" visibility="public" description=""/>)
        else Nil
      val pomConf =
        if (hasPom) Seq(<conf name="pom" visibility="public" description=""/>)
        else Nil

      <configurations>
        <conf name="compile" visibility="public" description=""/>
        <conf name="runtime" extends="compile" visibility="public" description=""/>
        <conf name="test" extends="runtime" visibility="public" description=""/>
        <conf name="provided" visibility="public" description=""/>
        {docConf}
        {sourcesConf}
        {pomConf}
      </configurations>
    }

    nodes += {

      val docPub =
        if (hasDoc) Seq(<artifact e:classifier="javadoc" name={
          moduleName.value
        } type="doc" ext="jar" conf="docs"/>)
        else Nil
      val sourcesPub =
        if (hasSources) Seq(<artifact e:classifier="sources" name={
          moduleName.value
        } type="src" ext="jar" conf="sources"/>)
        else Nil
      val pomPub =
        if (hasPom) Seq(<artifact name={moduleName.value} type="pom" ext="pom" conf="pom"/>)
        else Nil

      <publications>
        <artifact name={moduleName.value} type="jar" ext="jar" conf="compile"/>
        {docPub}
        {sourcesPub}
        {pomPub}
      </publications>
    }

    nodes += {
      val depNodes = dependencies.map {
        case (org, name, ver, confOpt) =>
          val conf     = confOpt.map(_.value).getOrElse("compile")
          val confSpec = s"$conf->default(compile)"
          <dependency org={org.value} name={name.value} rev={ver} conf={confSpec}></dependency>
      }
      <dependencies>
        {depNodes}
      </dependencies>
    }

    Pom.print(
      <ivy-module version="2.0" xmlns:e="http://ant.apache.org/ivy/extra">
        {nodes.result()}
      </ivy-module>
    )
  }

}
