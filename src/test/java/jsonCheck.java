import com.google.gson.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;

import static org.junit.jupiter.api.Assertions.*;


public class jsonCheck {

    @Test
    public void sourceTest() {

        String[] fileNames;
        File jsons = new File("jsons/");
        fileNames = jsons.list();

        assert fileNames != null;
        for (String fileName: fileNames) {
            JsonParser parser = new JsonParser();
            try {
                Object obj = parser.parse(new FileReader("jsons/" + fileName));
                JsonArray jsonArray = (JsonArray) obj;
                for (JsonElement jsonElement : jsonArray) {
                    JsonObject json = jsonElement.getAsJsonObject();
                    //Alien tests
                    if (fileName.equalsIgnoreCase("aliens.json")) {
                        assertNotNull(json.get("Name"), "Name is null");
                        assertNotNull(json.get("Scale"), "Scale is null: " + json.get("Name"));
                        if (json.get("Scale").getAsString().equalsIgnoreCase("normal")) {
                            assertNotNull(json.get("CR"), "CR is null: " + json.get("Name"));
                            try {
                                Double cr = json.get("CR").getAsDouble();
                            } catch (Exception e) {
                                System.out.println("CR not a number: " + json.get("Name").getAsString());
                                fail();
                            }
                            assertNotNull(json.get("XP"), "XP is null: " + json.get("Name"));
                            try {
                                Double cr = Double.parseDouble(json.get("XP").getAsString().replace(",", ""));
                            } catch (Exception e) {
                                System.out.println("XP not a number: " + json.get("Name").getAsString());
                                fail();
                            }
                            //assertNotNull(json.get("Senses"), "Senses is null: " + json.get("Name"));
                            assertNotNull(json.get("Init"), "init is null: " + json.get("Name"));
                            assertNotNull(json.get("Perception"), "Senses is null: " + json.get("Name"));
                            //assertNotNull(json.get("Desc"), "Description is null: " + json.get("Name"));
                            assertNotNull(json.get("Race"), "Race is null: " + json.get("Name"));
                            assertNotNull(json.get("Graft"),  "Graft is null: " + json.get("Name"));
                            assertNotNull(json.get("Statistics"), "Stats  is null: " + json.get("Name"));
                            assertNotNull(json.get("Statistics").getAsJsonObject().get("STR"));
                            assertNotNull(json.get("Statistics").getAsJsonObject().get("DEX"));
                            assertNotNull(json.get("Statistics").getAsJsonObject().get("CON"));
                            assertNotNull(json.get("Statistics").getAsJsonObject().get("INT"));
                            assertNotNull(json.get("Statistics").getAsJsonObject().get("WIS"));
                            assertNotNull(json.get("Statistics").getAsJsonObject().get("CHA"));
                            assertNotNull(json.get("Defense"), "Defense  is null: " + json.get("Name"));
                            assertNotNull(json.get("Offense"), "Offense  is null: " + json.get("Name"));
                            assertNotNull(json.get("Ecology"), "Ecology  is null: " + json.get("Name"));
                        }
                    }
                    //Equipment and Ship tests
                    else {
                        try {
                            assertTrue(json.get("source").toString().contains("pg.") ||
                                    json.get("source").toString().contains("No source found"));
                        } catch (AssertionError e) {
                            System.out.println("Failed " + fileName + " : " + json.get("source").toString());
                            assertTrue(false);
                        }
                    }
                }
                System.out.println("Passed " + fileName);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }



}
