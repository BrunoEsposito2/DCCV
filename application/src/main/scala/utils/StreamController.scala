package utils

import akka.actor.typed.ActorRef
import akka.NotUsed
import akka.stream.{KillSwitch, KillSwitches, Materializer, SinkRef}
import akka.stream.scaladsl.{BroadcastHub, Keep, Source}
import akka.util.ByteString
import message.{Message, SwitchToCamera}

import java.io.InputStream

object StreamController:
  def apply(): StreamController = new StreamController()
  private def apply(source: Option[Source[ByteString, NotUsed]], killSwitch: Option[KillSwitch], broadcastMap: Map[ActorRef[Message], KillSwitch]): StreamController =
    new StreamController(source, killSwitch, broadcastMap)

class StreamController(private val source: Option[Source[ByteString, NotUsed]] = Option.empty,
                       private val killSwitch: Option[KillSwitch] = Option.empty,
                       private val broadcastMap:Map[ActorRef[Message], KillSwitch] = Map()):

  def InitializeSource(origin: InputStream, cameraRef: ActorRef[Message])(implicit mat:Materializer): StreamController =
    killSwitch match
      case Some(stream) => closeSource().InitializeSource(origin, cameraRef)
      case None =>
        val source = Source.fromIterator(() => new Iterator[ByteString] {
          val buffer = new Array[Byte](1024)
          def hasNext: Boolean = true
          def next(): ByteString = {
            val bytesRead = origin.read(buffer)
            if (bytesRead == -1) ByteString.empty else ByteString(buffer.take(bytesRead))
          }
        }).takeWhile(_.nonEmpty)
        val (switch:KillSwitch, broadcastedSource: Source[ByteString, NotUsed]) = source.viaMat(KillSwitches.single)(Keep.right)
          .toMat(BroadcastHub.sink(bufferSize = 1))(Keep.both).run()
        
        broadcastMap.keySet.foreach(ref => ref ! SwitchToCamera(cameraRef))

        StreamController(Option(broadcastedSource), Option(switch), Map())

  def closeSource(): StreamController =
    if(killSwitch.nonEmpty)
      killSwitch.get.shutdown()
    StreamController(Option.empty, Option.empty, broadcastMap)

  def addSink(actorRef:ActorRef[Message], sinkRef: SinkRef[ByteString])(implicit materializer:Materializer): StreamController =
    source match
      case Some(stream) =>
        broadcastMap.getOrElse(actorRef, None) match
          case None =>
          case k: KillSwitch => k.shutdown()
        val newSwitch: KillSwitch = stream.viaMat(KillSwitches.single)(Keep.right).toMat(sinkRef.sink())(Keep.left).run()
        StreamController(source, killSwitch, broadcastMap + (actorRef -> newSwitch))
      case None =>
        StreamController(source, killSwitch, broadcastMap)

  def removeSink(actorRef:ActorRef[Message])(implicit materializer:Materializer): StreamController =
    broadcastMap.getOrElse(actorRef, None) match
      case None => StreamController(source, killSwitch, broadcastMap)
      case k: KillSwitch =>
        if(isStreamRunning) k.shutdown()
        StreamController(source, killSwitch, broadcastMap - actorRef)

  def isStreamRunning: Boolean =
    this.source.isDefined





