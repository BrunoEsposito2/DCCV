package database

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.flatspec.AnyFlatSpec

class MongoDBDriverTest extends AnyFlatSpec:

  "A Database connection" should "be opened" in testDBConnection()
  //"A Database getData function" should "get data from the database" in testDBGetTrackingData()

  val testKit: ActorTestKit = ActorTestKit()
  val mongodbDriver: MongoDBDriver = MongoDBDriver()

  def testDBConnection(): Unit = assert(mongodbDriver.connect().isDefined)

  //def testDBGetTrackingData(): Unit = assert(mongodbDriver.getTrackingData().nonEmpty)

