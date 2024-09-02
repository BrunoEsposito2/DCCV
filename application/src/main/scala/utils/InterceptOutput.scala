package utils
import message.{InputServiceMsg, Output, OutputServiceMsg}
import akka.actor.typed.ActorRef

import java.net.ServerSocket
import java.io.{BufferedReader, InputStreamReader, OutputStreamWriter, PrintWriter}
import scala.collection.immutable.Queue
import scala.sys.process.*

type InputRef = ActorRef[InputServiceMsg]
type OutputRef = ActorRef[OutputServiceMsg]
type ArgsList = Queue[String]

object Receiver:
  def main(args: Array[String]): Unit =
    Thread(ClientLauncher(9999, Queue("sender.bat"), null, null)).start()

object ClientLauncher:
  def apply(port: Int, command: ArgsList, overseer: InputRef, receiver: OutputRef) = new ClientLauncher(port, command, overseer, receiver)

class ClientLauncher(port: Int, command: ArgsList, overseer: InputRef, receiver: OutputRef) extends Runnable:
  private var processStdin: Option[PrintWriter] = Option.empty

  def run(): Unit =
    val server = new ServerSocket(port)
    println(s"Listening on port $port")

    // Launch the batch script
    val batchScript = command.foldLeft("")((acc,arg)=>{if (acc.isEmpty) arg else acc + " " + arg})

    val processIO = ProcessIO(
      stdin => {
        processStdin = Option(PrintWriter(OutputStreamWriter(stdin), true))
      }, stdout => {}, stderr => {}
    )
    Process(batchScript).run(processIO)
    val client = server.accept()
    val in = BufferedReader(InputStreamReader(client.getInputStream))
    manageChildOutput(in, receiver)
    client.close()
    server.close()

  def manageChildOutput(in:BufferedReader, receiver:OutputRef): Unit =
    LazyList.continually(in.readLine())
      .takeWhile(_ != null)
      .foreach( str =>
          println("Received: "+ str)
          receiver ! Output(str)
      )

  def getChildProcessStdin: Option[PrintWriter] =
    processStdin