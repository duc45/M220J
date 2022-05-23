package mflix.api.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.types.ObjectId;

public class UserComment {

    @JsonProperty("_id")
    @BsonIgnore
    private String _id;


    private int count;



    public String getId() {
        return _id;
    }

    public void setId(String id) {
        this._id = id;
    }


    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
