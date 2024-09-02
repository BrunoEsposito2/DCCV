package actor

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, Behavior, TypedActorContext}
import akka.actor.typed.scaladsl.Behaviors
import message.{ConfigMsg, InputMsg, Message, OutputServiceMsg, SetOutputRef}
import utils.{ClientLauncher, Info}

import java.io.PrintWriter
import scala.collection.immutable.Queue

type Ref = ActorRef[OutputServiceMsg]
type ArgsList2 = Queue[String]

type Behavior = akka.actor.typed.Behavior[Message]
type Context = TypedActorContext[Message]

object CameraManager:
  def apply(info:Info, child:ClientLauncher, childStdin: Option[PrintWriter], port:Integer, outputRef:Ref): CameraManager = new CameraManager(info, child, childStdin, port, outputRef)

private class CameraManager(info:Info, child:ClientLauncher, childStdin:Option[PrintWriter], port:Integer, outputRef:Ref) extends ReachableActor(info):

  private def getManagingBehavior(ctx:Context): PartialFunction[Message, Behavior] =
    case ConfigMsg(args) =>
      //on receiving new cv configuration, become an updated version of the CameraManager actor
      if(childStdin.nonEmpty) {
        childStdin.get.println("k")
        Thread.sleep(5000)
      }
      val newChild = ClientLauncher(port, args, ctx.asScala.self, outputRef)
      Thread(newChild).start()
      Thread.sleep(2000)
      val stdin = newChild.getChildProcessStdin
      CameraManager(info, newChild, stdin, port, outputRef).behavior()

    case InputMsg(arg) =>
      if(childStdin.nonEmpty) 
        childStdin.get.println(arg)
        Behaviors.same
      else
        var newChildStdin: Option[PrintWriter] = Option.empty
        while(newChildStdin.isEmpty)
          Thread.sleep(5000)
          newChildStdin = child.getChildProcessStdin
          childStdin.get.println(arg)
        CameraManager(info, child, newChildStdin, port, outputRef).behavior()

    case SetOutputRef(ref) =>
      CameraManager(info, child, childStdin, port, ref).behavior()

  override def behavior(): Behavior = Behaviors.setup { context =>{
      //context.system.receptionist.tell(Receptionist.register(ServiceKey[Message]("inputs"), context.self))
      Behaviors.receiveMessagePartial(getReachableBehavior.orElse(getManagingBehavior(context)))
    }
  }
