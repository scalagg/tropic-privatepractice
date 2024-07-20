package land.esta.plugin.ssb.utils.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import gg.scala.store.spigot.ScalaDataStoreSpigotImpl;
import lombok.Getter;
import org.bson.Document;

@Getter
public class Mongo {
    private final MongoDatabase database;
    private final MongoCollection<Document> players;
    private final MongoCollection<Document> difficulties;
    private final MongoCollection<Document> unranked;
    private final MongoCollection<Document> ranked;
    private final MongoCollection<Document> tournament;
    private final MongoCollection<Document> profiles;

    public Mongo() {
        this.database = ScalaDataStoreSpigotImpl.INSTANCE.getNewMongoConnection().getAppliedResource();

        players = database.getCollection("players");
        difficulties = database.getCollection("difficulties");
        unranked = database.getCollection("unranked");
        ranked = database.getCollection("ranked");
        profiles = database.getCollection("profiles");
        tournament = database.getCollection("tournament");
    }
}
