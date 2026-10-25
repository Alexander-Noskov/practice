package academy.tochkavhoda.competition.util;

import academy.tochkavhoda.competition.server.dto.ServerResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder().create();

    private JsonUtil() {}

    public static <T> T fromJson(String json, Class<T> type) {
        T result = GSON.fromJson(json, type);
        if (result == null) {
            throw new JsonSyntaxException("empty body");
        }
        return result;
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    public static ServerResponse ok(Object data) {
        return new ServerResponse(200, GSON.toJson(data));
    }

    public static ServerResponse okEmpty() {
        return new ServerResponse(200, "");
    }

    public static ServerResponse error(int code, String message) {
        return new ServerResponse(code, GSON.toJson(new ErrorResponse(message)));
    }

    public record ErrorResponse(String error) {}
}
