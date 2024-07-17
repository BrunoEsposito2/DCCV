package database

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.flatspec.AnyFlatSpec

class MongoDBDriverTest extends AnyFlatSpec:

  "A Database connection" should "be opened" in testDBConnection()
  "A Database getData function" should "get data from the database" in testDBGetTrackingData()

  val testKit: ActorTestKit = ActorTestKit()

  def testDBConnection(): Unit =
    val mongodbDriver: MongoDBDriver = MongoDBDriver()
    assert(mongodbDriver.connect() == "MongoDB: Connection established")

  def testDBGetTrackingData(): Unit =
    val mongodbDriver: MongoDBDriver = MongoDBDriver()
    mongodbDriver.connect() // Connect to the MongoDB server
    assert(mongodbDriver.getTrackingData().nonEmpty)

