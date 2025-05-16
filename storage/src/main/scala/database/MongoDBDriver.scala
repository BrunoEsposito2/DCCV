package database

import com.mongodb.client.model.{Filters, UpdateOptions, Updates}
import com.mongodb.{ConnectionString, MongoClientSettings, MongoCredential, MongoException, ServerAddress, ServerApi, ServerApiVersion}
import com.mongodb.client.{MongoClient, MongoClients, MongoCollection, MongoDatabase}
import org.bson.{BsonDocument, BsonInt64, BsonObjectId, Document}
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import scala.jdk.CollectionConverters.*

import scala.util.Properties
import scala.collection.mutable.ListBuffer

object MongoDBDriver:
  def apply(): MongoDBDriver = new MongoDBDriver()

private class MongoDBDriver:
  val DB_NAME: String = "DCCV"
  val DB_COLLECTION: String = "tracking"
  var mongoClient: MongoClient = _

  private val user: String = sys.env.getOrElse("MONGO_USER", "APP-USERNAME")
  private val password: Array[Char] = sys.env.getOrElse("MONGO_PASSWORD", "APP-PASSWORD").toCharArray
  private val source: String = DB_NAME

  private val credential = MongoCredential.createCredential(user, source, password)

  def connect(): Option[MongoCollection[Document]] =
    val settings: MongoClientSettings = MongoClientSettings.builder()
      applyConnectionString(ConnectionString(scala.sys.env.getOrElse("DATABASE_URI", "mongodb://localhost:27017/")))
      .credential(credential)
      .build()

    mongoClient = MongoClients.create(settings)
    val database: MongoDatabase = mongoClient.getDatabase(DB_NAME)

    println("DATABASE CONNECTED SUCCESSFULLY")
    /* Send a ping to confirm a successful connection */
    try {
      val oid = ObjectId("5f8d6b2b9d3b2a1b1c9d1e1f")
      val updateOptions = UpdateOptions().upsert(true)
     
      val collection = database.getCollection(DB_COLLECTION)
      collection.updateOne(
        Filters.eq("_id", oid), // Filter by specific ObjectId
        Updates.combine(
          Updates.set("ping", 1),
        ),
        updateOptions
      )
      Option(collection)
    } catch {
      case mongodbException: MongoException =>
        System.err.println(mongodbException)
        Option.empty
    }
