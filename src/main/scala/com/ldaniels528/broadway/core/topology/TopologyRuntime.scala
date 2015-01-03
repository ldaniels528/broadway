package com.ldaniels528.broadway.core.topology

import java.util.Properties

import com.ldaniels528.broadway.server.etl.BroadwayTopology
import com.ldaniels528.trifecta.util.OptionHelper._

import scala.collection.concurrent.TrieMap
import scala.util.Try

/**
 * Represents a topology runtime
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class TopologyRuntime() {
  private val feeds = TrieMap[String, Feed]()
  private val topologies = TrieMap[String, BroadwayTopology]()
  private val properties = TrieMap[String, Properties]()

  def getFeed(fd: FeedDescriptor): Feed = {
    feeds.getOrElseUpdate(fd.uuid, Feed(fd.uuid, fd.name, fd.dependencies map (_.toFeed(this)), fd.topology))
  }

  def getPropertiesByID(id: String): Option[Properties] = properties.get(id)

  def getTopology(td: TopologyDescriptor): Try[BroadwayTopology] = Try {
    topologies.getOrElseUpdate(td.id, instantiateTopology(td.className))
  }

  def getTopologyByID(id: String): BroadwayTopology = {
    topologies.get(id) orDie s"Topology ID '$id' not found"
  }

  private def instantiateTopology(className: String) = {
    Class.forName(className).newInstance().asInstanceOf[BroadwayTopology]
  }

}
