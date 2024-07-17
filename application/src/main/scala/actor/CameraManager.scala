package actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import message.{ConfigMsg, Message}
import utils.{CVLauncher, ExitApp, Info}

object CameraManager:
  def apply(info:Info, args:List[String]): CameraManager = new CameraManager(info, args)

private class CameraManager(info:Info, val args:List[String])(implicit val exe:CVLauncher = CVLauncher(args), val stopCmd:ExitApp = ExitApp()) extends ReachableActor(info):
  def getManagingBehavior: PartialFunction[Message, Behavior[Message]] =
    case ConfigMsg(args) => stopCmd.execute()
      CVLauncher(args).execute()
      Behaviors.same

  override def behavior(): Behavior[Message] = Behaviors.setup { context =>
    Behaviors.receiveMessagePartial(getReachableBehavior.orElse(getManagingBehavior))
  }
