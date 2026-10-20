package dev.ricodevvv.riven.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.UUID;

/**
 * Envelope of everything that travels through Redis: type, who sent it, who it's for (null for everyone),
 * an id to answer requests and the JSON payload.
 */
public class NetworkMessage {

    private final String id;
    private final String type;
    private final String source;
    private final String target;
    private final String replyTo;
    private final long timestamp;
    private final JsonObject payload;

    public NetworkMessage(String type, String source, String target, String replyTo, JsonObject payload) {
        this(UUID.randomUUID().toString(), type, source, target, replyTo, System.currentTimeMillis(), payload);
    }

    private NetworkMessage(String id, String type, String source, String target, String replyTo, long timestamp, JsonObject payload) {
        this.id = id;
        this.type = type;
        this.source = source;
        this.target = target;
        this.replyTo = replyTo;
        this.timestamp = timestamp;
        this.payload = payload == null ? new JsonObject() : payload;
    }

    public String serialize() {
        final JsonObject json = new JsonObject();

        json.addProperty("id", this.id);
        json.addProperty("type", this.type);
        json.addProperty("source", this.source);

        if (this.target != null) {
            json.addProperty("target", this.target);
        }

        if (this.replyTo != null) {
            json.addProperty("replyTo", this.replyTo);
        }

        json.addProperty("timestamp", this.timestamp);
        json.add("payload", this.payload);
        return json.toString();
    }

    /**
     * @param text serialized message
     * @return the message, or null if it's broken
     */
    public static NetworkMessage deserialize(String text) {
        try {
            final JsonObject json = new JsonParser().parse(text).getAsJsonObject();

            return new NetworkMessage(json.get("id").getAsString(), json.get("type").getAsString(), json.get("source").getAsString(),
                    json.has("target") ? json.get("target").getAsString() : null,
                    json.has("replyTo") ? json.get("replyTo").getAsString() : null,
                    json.get("timestamp").getAsLong(),
                    json.getAsJsonObject("payload"));
        } catch (Exception e) {
            return null;
        }
    }

    public String getId() {
        return this.id;
    }

    public String getType() {
        return this.type;
    }

    public String getSource() {
        return this.source;
    }

    public String getTarget() {
        return this.target;
    }

    public String getReplyTo() {
        return this.replyTo;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public JsonObject getPayload() {
        return this.payload;
    }

}
