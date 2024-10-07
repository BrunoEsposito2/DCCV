package actor

import akka.actor.typed
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.stream.Materializer
import akka.util.ByteString
import message.{CameraOutputStreamSource, Config, GetSourceRef, Input, InputServiceFailure, InputServiceMsg, InputServiceSuccess, Message, OutputServiceMsg}
import utils.{ConnectionController, Info, InputServiceErrors, StandardChildProcessCommands, StreamController}

import java.io.{OutputStreamWriter, PrintWriter}
import scala.sys.process.*
import java.net.SocketTimeoutException
import scala.collection.immutable.Queue

type Ref = ActorRef[OutputServiceMsg]
type ArgsList2 = Queue[String]

type Behavior = akka.actor.typed.Behavior[Message]
type Context = ActorContext[Message]

object CameraManager:
  def apply(port:Integer): CameraManager = new CameraManager(Info(), Option.empty,  StreamController(), ConnectionController(port))
  private def apply(info:Info,
                    childStdin:Option[PrintWriter] = Option.empty,
                    childOutputStream: StreamController = StreamController(),
                    socket: ConnectionController): CameraManager = new CameraManager(info, childStdin, childOutputStream, socket)

private class CameraManager(info:Info, childStdin:Option[PrintWriter], childOutputStream: StreamController, socket:ConnectionController) extends ReachableActor(info):

  override def create(): Behavior =
    Behaviors.setup { context =>
      context.system.receptionist.tell(Receptionist.register(ServiceKey[InputServiceMsg]("inputs"), context.self))
      implicit val ctx: ActorContext[Message] = context
      implicit val mat: Materializer = Materializer(ctx.system)
      CameraManager(setActorInfo(context), childStdin, childOutputStream, socket).behavior
    }

  override def setActorInfo(ctx: Context): Info =
    super.setActorInfo(ctx).setActorType("CameraManager")
     
  override def behavior(implicit materializer: Materializer, ctx:ActorContext[Message]): Behavior =
    Behaviors.setup { ctx =>
      Behaviors.receiveMessagePartial(getReachableBehavior.orElse(getManagingBehavior))
    }

  private def launchNewChildProcess(command: Queue[String])(implicit materializer: Materializer): (PrintWriter, ConnectionController) =
    val bashScript = command.foldLeft("")((acc, arg) => if (acc.isEmpty) arg else acc + " " + arg)
    var processStdin: Option[PrintWriter] = Option.empty
    val processIO = ProcessIO(stdin => processStdin = Option(PrintWriter(OutputStreamWriter(stdin), true)), stdout => {}, stderr => {})
    //launch the bash script and wait for the launched program to reach the socket
    Process(bashScript).run(processIO)
    val newConnection = socket.enstablishConnection()
    (processStdin.get, newConnection)

  private def getManagingBehavior(implicit materializer: Materializer, ctx:ActorContext[Message]): PartialFunction[Message, Behavior] =
    case Config(replyTo, args) =>
      //on receiving new cv configuration, stop previous child if present and become an updated version of the CameraManager actor
      if(childStdin.nonEmpty) childStdin.get.println(StandardChildProcessCommands.Kill.command)
      val newSource = childOutputStream.closeSource()
      try
        val (newStdin, newConnection) = launchNewChildProcess(args)
        val ns = newSource.InitializeSource(newConnection.getClientInput.get)
        replyTo ! InputServiceSuccess(info)
        CameraManager(info, Option(newStdin), ns, newConnection).behavior
      catch
        case e: SocketTimeoutException =>
          replyTo ! InputServiceFailure(InputServiceErrors.MissingConnection)
          CameraManager(info, childStdin, childOutputStream, socket).behavior

    case Input(replyTo, arg) =>
      (childOutputStream.getSourceRef, childStdin, arg) match
        case (None, _, _) =>
          replyTo ! InputServiceFailure(InputServiceErrors.MissingChild)
          CameraManager(info, childStdin, childOutputStream, socket).behavior
        case (Some(ref), None, _) =>
          replyTo ! InputServiceFailure(InputServiceErrors.MissingStdin)
          CameraManager(info, childStdin, childOutputStream, socket).behavior
        case (Some(ref), Some(inputStream), StandardChildProcessCommands.Kill.command) =>
          inputStream.println(arg)
          replyTo ! InputServiceSuccess(info)
          CameraManager(info, Option.empty, childOutputStream.closeSource(), socket.closeConnection()).behavior
        case (Some(ref), Some(inputStream), _) =>
          inputStream.println(arg)
          replyTo ! InputServiceSuccess(info)
          CameraManager(info, childStdin, childOutputStream, socket).behavior

    case GetSourceRef(replyTo) =>
      childOutputStream.getSourceRef match
        case Some(source) =>
          replyTo ! CameraOutputStreamSource(info, source)
        case None => replyTo ! InputServiceFailure(InputServiceErrors.MissingChild)
      CameraManager(info, childStdin, childOutputStream, socket).behavior