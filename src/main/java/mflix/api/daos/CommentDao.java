package mflix.api.daos;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.ReadConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import mflix.api.models.Comment;
import mflix.api.models.Critic;
import mflix.api.models.UserComment;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.print.Doc;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Component
public class CommentDao extends AbstractMFlixDao {

    public static String COMMENT_COLLECTION = "comments";
    private final Logger log;
    private MongoCollection<Comment> commentCollection;
    private CodecRegistry pojoCodecRegistry;

    @Autowired
    public CommentDao(
            MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
        super(mongoClient, databaseName);
        log = LoggerFactory.getLogger(this.getClass());
        this.db = this.mongoClient.getDatabase(MFLIX_DATABASE);
        this.pojoCodecRegistry =
                fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        this.commentCollection =
                db.getCollection(COMMENT_COLLECTION, Comment.class).withCodecRegistry(pojoCodecRegistry);
    }

    /**
     * Returns a Comment object that matches the provided id string.
     *
     * @param id - comment identifier
     * @return Comment object corresponding to the identifier value
     */
    public Comment getComment(String id) {
        return commentCollection.find(new Document("_id", new ObjectId(id))).first();
    }

    /**
     * Adds a new Comment to the collection. The equivalent instruction in the mongo shell would be:
     *
     * <p>db.comments.insertOne({comment})
     *
     * <p>
     *
     * @param comment - Comment object.
     * @throw IncorrectDaoOperation if the insert fails, otherwise
     * returns the resulting Comment object.
     */
    public Comment addComment(Comment comment) {

        // TODO> Ticket - Update User reviews: implement the functionality that enables adding a new
        // comment.
        if(!Optional.ofNullable(comment.getId()).isPresent()) {
            throw new IncorrectDaoOperation("Comment id cannot be null");
        }
        try {
            commentCollection.insertOne(comment);
        } catch (MongoException e) {
            log.error("An error ocurred while trying to insert a Comment.");
            return null;
        }
        // TODO> Ticket - Handling Errors: Implement a try catch block to
        // handle a potential write exception when given a wrong commentId.
        return comment;
    }

    /**
     * Updates the comment text matching commentId and user email. This method would be equivalent to
     * running the following mongo shell command:
     *
     * <p>db.comments.update({_id: commentId}, {$set: { "text": text, date: ISODate() }})
     *
     * <p>
     *
     * @param commentId - comment id string value.
     * @param text      - comment text to be updated.
     * @param email     - user email.
     * @return true if successfully updates the comment text.
     */
    public boolean updateComment(String commentId, String text, String email) {

        // TODO> Ticket - Update User reviews: implement the functionality that enables updating an
        // user own comments
        Bson query = Filters.and(Filters.eq("_id", new ObjectId(commentId)), Filters.eq("email", email));

        Bson updateComment = Updates.set("text", text);

        try {
            Comment result = commentCollection.findOneAndUpdate(query, updateComment);

            if (result !=null ) {
                return true;
            }

            // TODO> Ticket - Handling Errors: Implement a try catch block to
            // handle a potential write exception when given a wrong commentId.
            return false;
        } catch (MongoException e) {
            log.error("An error ocurred while trying to update a Comment.");
            return false;
        }
    }

    /**
     * Deletes comment that matches user email and commentId.
     *
     * @param commentId - commentId string value.
     * @param email     - user email value.
     * @return true if successful deletes the comment.
     */
    public boolean deleteComment(String commentId, String email) {
        // TODO> Ticket Delete Comments - Implement the method that enables the deletion of a user
        // comment
        // TIP: make sure to match only users that own the given commentId
        Bson query = Filters.and(Filters.eq("_id", new ObjectId(commentId)), Filters.eq("email", email));

        try {
            DeleteResult result = commentCollection.deleteOne(query);

            if (result.getDeletedCount() >= 1) {
                return true;
            }

            // TODO> Ticket Handling Errors - Implement a try catch block to
            // handle a potential write exception when given a wrong commentId.
            return false;
        } catch (MongoException e) {
            log.error("An error ocurred while trying to update a Comment.");
            return false;
        }

    }

    /**
     * Ticket: User Report - produce a list of users that comment the most in the website. Query the
     * `comments` collection and group the users by number of comments. The list is limited to up most
     * 20 commenter.
     *
     * @return List {@link Critic} objects.
     */
    public List<Critic> mostActiveCommenters() {
        List<Critic> mostActive = new ArrayList<>();
        // // TODO> Ticket: User Report - execute a command that returns the
        // // list of 20 users, group by number of comments. Don't forget,
        // // this report is expected to be produced with an high durability
        // // guarantee for the returned documents. Once a commenter is in the
        // // top 20 of users, they become a Critic, so mostActive is composed of
        // // Critic objects.

        BsonField sum1 = Accumulators.sum("count", 1);
        // adding both group _id and accumulators
        Bson groupStage = Aggregates.group("$email", sum1);
        Bson sortStage = Aggregates.sort(Sorts.descending("count"));
        Bson limitStage = Aggregates.limit(20);
        List<Bson> pipeline = new ArrayList<>();

        pipeline.add(groupStage);
        pipeline.add(sortStage);
        pipeline.add(limitStage);
        ArrayList<UserComment> result = new ArrayList<>();
        commentCollection.withReadConcern(ReadConcern.MAJORITY)
                .withDocumentClass(UserComment.class)
                .withCodecRegistry(pojoCodecRegistry)
                .aggregate(pipeline)
                .into(result);
        for (UserComment userComment: result
             ) {
            Critic activeUser = new Critic(userComment.getId(), userComment.getCount());
            mostActive.add(activeUser);
        }


        return mostActive;
    }
}
