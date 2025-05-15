package usernode.swarmnode

import actor.GUIBackEnd

import scala.jdk.CollectionConverters.*
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import com.typesafe.config.ConfigFactory
import message.Message

import java.util.logging.LogManager
import scala.collection.immutable.HashMap

object App:
  private def initBasicConfig =
    var settings = new HashMap[String, Object]
  
    settings += ("akka.remote.artery.canonical.hostname" -> "guibackend")
    settings += ("akka.remote.artery.canonical.port" -> "2551")

    settings += ("akka.remote.artery.bind.hostname" -> "0.0.0.0")
    settings += ("akka.remote.artery.bind.port" -> "2551")

    settings += ("akka.actor.allow-java-serialization" -> "on")
    settings += ("akka.remote.artery.transport" -> "tcp")
    settings += ("akka.cluster.seed-nodes" ->
      List("akka://akka-cluster-system@cameranode:2553",
        "akka://akka-cluster-system@utilitynode:2552",
        "akka://akka-cluster-system@guibackend:2551").asJava)

    settings += ("akka.cluster.downing-provider-class" -> "akka.cluster.sbr.SplitBrainResolverProvider")
    settings += ("akka.cluster.jmx.multi-mbeans-in-same-jvm" -> "on")

    settings += ("akka.cluster.retry-unsuccessful-join-after" -> "3s")
    settings += ("akka.cluster.shutdown-after-unsuccessful-join-seed-nodes" -> "off")
    settings += ("akka.cluster.joining-timeout" -> "60s")

    settings += ("akka.actor.provider" -> "cluster")

    ConfigFactory.parseMap(settings.asJava).withFallback(ConfigFactory.load())

  def main(args: Array[String]): Unit =
    val system = ActorSystem(GUIBackEnd().create(), "akka-cluster-system", initBasicConfig)
    val cluster = Cluster(system)
    println("GUIBackEnd (server) cluster created")
