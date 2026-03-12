package sdd.validate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DomainModelParser {

    public record DomainBehavior(String id, String given, String when, String then) {}

    public List<DomainBehavior> parse(Path domainJsonPath) throws IOException {
        String json = Files.readString(domainJsonPath);
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        JsonArray behaviors = root.getAsJsonArray("behaviors");

        List<DomainBehavior> result = new ArrayList<>();
        for (JsonElement el : behaviors) {
            JsonObject b = el.getAsJsonObject();
            result.add(new DomainBehavior(
                    b.get("id").getAsString(),
                    b.get("given").getAsString(),
                    b.get("when").getAsString(),
                    b.get("then").getAsString()
            ));
        }
        return result;
    }
}
