import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoUtils {

    private MongoClient mongo;
    private MongoDatabase database;

    public MongoUtils(String host, int port) {
        mongo = new MongoClient(host, port);
        database = mongo.getDatabase("starfinder");
    }

    public void creatCollection(String name) {
        database.createCollection(name);
        System.out.println(name + "collection created successfully");
    }


    public void addEntry(String collectionName, Document doc) {
        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.insertOne(doc);
    }


}
