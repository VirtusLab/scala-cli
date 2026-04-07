package scala.cli.commands.publish

import coursier.core.{Configuration, MinimizedExclusions, ModuleName, Organization, Type}
import coursier.publish.Pom

import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.{LocalDateTime, ZoneOffset}

import scala.collection.mutable
import scala.xml.{Elem, NodeSeq}

object Ivy {

  private val mavenPomNs = "http://maven.apache.org/POM/4.0.0"

  private def ivyLicenseNodes(license: Option[Pom.License]): NodeSeq =
    license match {
      case None    => NodeSeq.Empty
      case Some(l) =>
        val u = l.url.trim
        if u.nonEmpty then
          scala.xml.NodeSeq.fromSeq(Seq(<license name={l.name} url={u}/>))
        else
          scala.xml.NodeSeq.fromSeq(Seq(<license name={l.name}/>))
    }

  private def mavenScmNodes(scm: Option[Pom.Scm]): NodeSeq =
    scm match {
      case None    => NodeSeq.Empty
      case Some(s) =>
        val url        = s.url.trim
        val connection = s.connection.trim
        val devConn    = s.developerConnection.trim
        val children   = Seq(
          Option.when(url.nonEmpty)(<m:url>{url}</m:url>),
          Option.when(connection.nonEmpty)(<m:connection>{connection}</m:connection>),
          Option.when(devConn.nonEmpty)(
            <m:developerConnection>{devConn}</m:developerConnection>
          )
        ).flatten
        if children.isEmpty then NodeSeq.Empty
        else scala.xml.NodeSeq.fromSeq(Seq(<m:scm>{children}</m:scm>))
    }

  private def mavenDeveloperNodes(developers: Seq[Pom.Developer]): NodeSeq =
    if (developers.isEmpty) NodeSeq.Empty
    else {
      val devElems = developers.map { d =>
        val url   = d.url.trim
        val parts = Seq(
          Some(<m:id>{d.id}</m:id>),
          Some(<m:name>{d.name}</m:name>),
          d.mail.map(m => <m:email>{m}</m:email>),
          Option.when(url.nonEmpty)(<m:url>{url}</m:url>)
        ).flatten
        <m:developer>{parts}</m:developer>
      }
      scala.xml.NodeSeq.fromSeq(Seq(<m:developers>{devElems}</m:developers>))
    }

  private def mavenProjectNamePackagingNodes(
    pomProjectName: Option[String],
    packaging: Option[Type]
  ): NodeSeq =
    val namePart = pomProjectName.flatMap { n =>
      val t = n.trim
      Option.when(t.nonEmpty)(<m:name>{t}</m:name>)
    }
    val packagingPart = packaging.map(p => <m:packaging>{p.value}</m:packaging>)
    val parts         = namePart.toSeq ++ packagingPart.toSeq
    if parts.isEmpty then NodeSeq.Empty
    else scala.xml.NodeSeq.fromSeq(parts)

  private lazy val dateFormatter = new DateTimeFormatterBuilder()
    .appendValue(ChronoField.YEAR, 4)
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .appendValue(ChronoField.DAY_OF_MONTH, 2)
    .appendValue(ChronoField.HOUR_OF_DAY, 2)
    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
    .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
    .toFormatter

  /** Ivy descriptor aligned with coursier `Pom.create` metadata (license, SCM, developers, optional
    * name/packaging).
    */
  def create(
    organization: Organization,
    moduleName: ModuleName,
    version: String,
    description: Option[String] = None,
    url: Option[String] = None,
    pomProjectName: Option[String] = None,
    packaging: Option[Type] = None,
    // TODO Accept full-fledged coursier.Dependency
    dependencies: Seq[(
      Organization,
      ModuleName,
      String,
      Option[Configuration],
      MinimizedExclusions
    )] = Nil,
    license: Option[Pom.License] = None,
    scm: Option[Pom.Scm] = None,
    developers: Seq[Pom.Developer] = Nil,
    time: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    hasPom: Boolean = true,
    hasDoc: Boolean = true,
    hasSources: Boolean = true
  ): String = {

    val licenseXml       = ivyLicenseNodes(license)
    val scmXml           = mavenScmNodes(scm)
    val devXml           = mavenDeveloperNodes(developers)
    val projectMetaXml   = mavenProjectNamePackagingNodes(pomProjectName, packaging)
    val hasMavenMetadata = scmXml.nonEmpty || devXml.nonEmpty || projectMetaXml.nonEmpty

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
        {licenseXml}
        {desc}
        {projectMetaXml}
        {scmXml}
        {devXml}
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
        case (org, name, ver, confOpt, exclusions) =>
          val conf           = confOpt.map(_.value).getOrElse("compile")
          val confSpec       = s"$conf->default(compile)"
          val exclusionNodes =
            exclusions.data.toSet().map { case (org, module) =>
              <exclude org={org.value} module={module.value}/>
            }
          <dependency org={org.value} name={name.value} rev={ver} conf={confSpec}>
            {exclusionNodes}
          </dependency>
      }
      <dependencies>
        {depNodes}
      </dependencies>
    }

    val root: Elem =
      if (hasMavenMetadata)
        <ivy-module version="2.0" xmlns:e="http://ant.apache.org/ivy/extra" xmlns:m={
          mavenPomNs
        }>
          {nodes.result()}
        </ivy-module>
      else
        <ivy-module version="2.0" xmlns:e="http://ant.apache.org/ivy/extra">
          {nodes.result()}
        </ivy-module>

    Pom.print(root)
  }

}
