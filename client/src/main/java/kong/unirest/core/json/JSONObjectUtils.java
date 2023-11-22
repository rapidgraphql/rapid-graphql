package kong.unirest.core.json;

public class JSONObjectUtils {
    public static boolean isNull(JSONObject jsonObject, String fieldName) {
        return ((JsonEngine.Object)jsonObject.asElement()).get(fieldName).isJsonNull();
    }
}
