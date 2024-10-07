package utils

import akka.NotUsed
import akka.stream.javadsl.StreamRefs
import akka.stream.{KillSwitch, KillSwitches, Materializer, SourceRef}
import akka.stream.scaladsl.{BroadcastHub, Keep, Sink, Source}
import akka.util.ByteString

import java.io.InputStream

object StreamController:
  def apply(): StreamController = new StreamController()
  private def apply(source: Option[Source[ByteString, NotUsed]], ref: Option[SourceRef[ByteString]], killSwitch: Option[KillSwitch]): StreamController =
    new StreamController(source, ref, killSwitch)

class StreamController(private val source: Option[Source[ByteString, NotUsed]] = Option.empty,
                       private val ref: Option[SourceRef[ByteString]] = Option.empty,
                       private val killSwitch: Option[KillSwitch] = Option.empty):

  def InitializeSource(origin: InputStream)(implicit mat:Materializer): StreamController =
    killSwitch match
      case Some(stream) => closeSource().InitializeSource(origin)
      case None =>
        val source = Source.fromIterator(() => new Iterator[ByteString] {
          val buffer = new Array[Byte](1024)
          def hasNext: Boolean = true
          def next(): ByteString = {
            val bytesRead = origin.read(buffer)
            if (bytesRead == -1) ByteString.empty else ByteString(buffer.take(bytesRead))
          }
        }).takeWhile(_.nonEmpty)
        val (switch: KillSwitch, broadcastedSource: Source[ByteString, NotUsed]) = source.viaMat(KillSwitches.single)(Keep.right)
          .toMat(BroadcastHub.sink(bufferSize = 1))(Keep.both).run()

        broadcastedSource.runForeach(f => println("SOURCE RECEIVED "+ f.utf8String.strip()))

        val sourceRef = broadcastedSource.toMat(StreamRefs.sourceRef())(Keep.right).run()
        StreamController(Option(broadcastedSource), Option(sourceRef), Option(switch))

  def closeSource(): StreamController =
    if(killSwitch.nonEmpty)
      killSwitch.get.shutdown()
    StreamController(Option.empty, Option.empty, Option.empty)

  def getSourceRef: Option[SourceRef[ByteString]] =
    this.ref





