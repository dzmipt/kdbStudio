package studio.kdb.config;

import com.google.gson.JsonObject;

public interface JsonConverter {

    boolean convert(JsonObject json);
}
