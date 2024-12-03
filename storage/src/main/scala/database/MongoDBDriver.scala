package database

import com.mongodb.{ConnectionString, MongoClientSettings, MongoException, ServerApi, ServerApiVersion}
import com.mongodb.client.{MongoClient, MongoClients, MongoCollection, MongoDatabase}
import org.bson.{BsonDocument, BsonInt64, BsonObjectId, Document}
import org.bson.conversions.Bson
import org.bson.types.ObjectId

import scala.util.Properties
import scala.collection.mutable.ListBuffer

object MongoDBDriver:
  def apply(): MongoDBDriver = new MongoDBDriver()

private class MongoDBDriver:
  val DB_NAME: String = "DCCV"
  val DB_COLLECTION: String = "tracking"
  var mongoClient: MongoClient = _

  def connect(): Option[MongoCollection[Document]] =
    val serverAPI: ServerApi = ServerApi.builder().version(ServerApiVersion.V1).build()

    val settings: MongoClientSettings = MongoClientSettings.builder()
      .applyConnectionString(ConnectionString(scala.sys.env.getOrElse("DATABASE_URI", "mongodb://localhost:27017/")))
      .serverApi(serverAPI)
      .build()

    mongoClient = MongoClients.create(settings)
    val database: MongoDatabase = mongoClient.getDatabase(DB_NAME)

    /* Send a ping to confirm a successful connection */
    try {
      val oid = ObjectId("5f8d6b2b9d3b2a1b1c9d1e1f")
      val doc = Document("_id", oid).append("ping", 1)
      val collection = database.getCollection(DB_COLLECTION)
      collection.insertOne(doc)
      Option(collection)
    } catch {
      case mongodbException: MongoException =>
        System.err.println(mongodbException);
        Option.empty
    }

  def getTrackingData(): ListBuffer[Document] =
    val collection: MongoCollection[Document] = mongoClient.getDatabase(DB_NAME).getCollection(DB_COLLECTION)
    val resultData: ListBuffer[Document] = ListBuffer()
    collection.find().forEach(data => resultData += data)
    resultData


