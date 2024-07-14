package database

import com.mongodb.{ConnectionString, MongoClientSettings, MongoException, ServerApi, ServerApiVersion}
import com.mongodb.client.{MongoClient, MongoClients, MongoCollection, MongoDatabase}
import org.bson.{BsonDocument, BsonInt64, Document}
import org.bson.conversions.Bson

import scala.collection.mutable.ListBuffer

object MongoDBDriver:
  def apply(): MongoDBDriver = new MongoDBDriver()

private class MongoDBDriver:
  val DB_NAME: String = "DB_NAME"

  val serverAPI: ServerApi = ServerApi.builder().version(ServerApiVersion.V1).build()

  val settings: MongoClientSettings = MongoClientSettings.builder()
    .applyConnectionString(new ConnectionString(System.getenv("DATABASE_URI")))
    .serverApi(serverAPI)
    .build()

  def getData(): ListBuffer[Document] =
    val mongoClient: MongoClient = MongoClients.create(settings)
    // TODO: change DB_NAME
    val database: MongoDatabase = mongoClient.getDatabase(DB_NAME)

    /* Send a ping to confirm a successful connection */
    try {
      val command: Bson = new BsonDocument("ping", new BsonInt64(1))
      val commandResult: Document = database.runCommand(command)
      println("MongoDB: Connection established")
    } catch {
      case mongodbException: MongoException => System.err.println(mongodbException)
    }

    val collection: MongoCollection[Document] = mongoClient.getDatabase(DB_NAME).getCollection("DB_COLLECTION")
    val resultData: ListBuffer[Document] = ListBuffer()
    collection.find().limit(5).forEach(data => resultData += data)
    resultData


