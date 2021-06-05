import com.google.gson.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;


import static org.junit.jupiter.api.Assertions.assertTrue;


public class jsonCheck {

    @Test
    public void sourceTest() {

        String[] fileNames;
        File jsons = new File("jsons");
        fileNames = jsons.list();

        assert fileNames != null;
        for (String fileName: fileNames) {
            JsonParser parser = new JsonParser();
            try {
                Object obj = parser.parse(new FileReader("jsons/" + fileName));
                JsonArray jsonArray = (JsonArray) obj;

                for (JsonElement jsonElement : jsonArray) {
                    JsonObject json = jsonElement.getAsJsonObject();
                    try {
                        assertTrue(json.get("source").toString().contains("pg."));
                    } catch (AssertionError e) {
                        System.out.println("Failed " + fileName + " : " + json.get("source").toString());
                        assertTrue(false);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
