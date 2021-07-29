import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;

public class WorldBuilderScraper {

    public static void Scrape() {

        System.out.println("World Builder Scraper starting");

        Document doc = getPage("https://aonsrd.com/Rules.aspx?ID=754");
        Elements tableHeadings = doc.getElementsByTag("h3");

        JSONArray buildingTables = new JSONArray();
        //Builder tables
        for (int i = 0; i < 3; i++) {
            JSONObject table = new JSONObject();
            table.put("Name", tableHeadings.get(i).text());
            JSONArray tableValues = new JSONArray();
            Elements rows = tableHeadings.get(i).nextElementSibling().getElementsByTag("tr");
            Elements colHeadings = rows.get(0).getElementsByTag("td");
            for (int j = 1; j < rows.size(); j++) {
                JSONObject row = new JSONObject();
                Elements cols = rows.get(j).getElementsByTag("td");
                for (int k = 0; k < cols.size(); k++) {
                    row.put(colHeadings.get(k).text(),cols.get(k).text());
                }
                tableValues.add(row);
            }
            table.put("Table", tableValues);
            buildingTables.add(table);
        }
        //Biome Tables
        JSONObject bioTables = new JSONObject();
        bioTables.put("Name", "Biomes Tables");
        JSONArray tables = new JSONArray();
        for (int i = 4; i < 28; i = i + 2) {
            JSONObject bio = new JSONObject();
            bio.put("Name", tableHeadings.get(i).text().replace(" Inhabitants", ""));
            JSONArray table = new JSONArray();
            //Get table
            Elements rows = tableHeadings.get(i).nextElementSibling().getElementsByTag("tr");
            Elements colHeadings = rows.get(0).getElementsByTag("td");
            for (int j = 1; j < rows.size(); j++) {
                JSONObject row = new JSONObject();
                Elements cols = rows.get(j).getElementsByTag("td");
                for (int k = 0; k < cols.size(); k++) {
                    row.put(colHeadings.get(k).text(),cols.get(k).text());
                }
                table.add(row);
            }
            bio.put("Inhabitants", table);
            table = new JSONArray();
            rows = tableHeadings.get(i + 1).nextElementSibling().getElementsByTag("tr");
            colHeadings = rows.get(0).getElementsByTag("td");
            for (int j = 1; j < rows.size(); j++) {
                JSONObject row = new JSONObject();
                Elements cols = rows.get(j).getElementsByTag("td");
                for (int k = 0; k < cols.size(); k++) {
                    row.put(colHeadings.get(k).text(),cols.get(k).text());
                }
                table.add(row);
            }
            bio.put("Adventure Hooks", table);
            tables.add(bio);
        }
        bioTables.put("Tables", tables);
        buildingTables.add(bioTables);
        //cultural attributes
        JSONObject culture = new JSONObject();
        culture.put("Name:", "Cultures");
        tables = new JSONArray();
        JSONObject accord = new JSONObject();
        accord.put("Name", "Accord");
        for (int i = 28; i < 31 ; i++) {
            readTable(i,tableHeadings,accord);
        }
        tables.add(accord);
        JSONObject magic = new JSONObject();
        magic.put("Name","Magic");
        for (int i = 47; i < 50 ; i++) {
            readTable(i,tableHeadings,magic);
        }
        tables.add(magic);
        JSONObject religion = new JSONObject();
        religion.put("Name","Religion");
        for (int i = 50; i < 53; i++) {
            readTable(i,tableHeadings,religion);
        }
        tables.add(religion);
        JSONObject tech = new JSONObject();
        tech.put("Name","Tech");
        for (int i = 53; i < 57 ; i++) {
            readTable(i,tableHeadings,tech);
        }
        tables.add(tech);
        culture.put("Tables", tables);
        buildingTables.add(culture);
        //Orgs
        JSONObject org = new JSONObject();
        org.put("Name","Organizations");
        readTable(32,tableHeadings,org);
        buildingTables.add(org);
        //write
        writeJson(buildingTables, "world_builder.json");
        int i = 0;
        for (Element heading : tableHeadings) {
            System.out.println(i + ": " + heading.text());
            i++;
        }
        System.out.println("World Builder json created");
    }


    //Helper functions

    private static void readTable(int i, Elements tableHeadings, JSONObject json) {
        JSONArray table = new JSONArray();
        Elements rows = tableHeadings.get(i).nextElementSibling().getElementsByTag("tr");
        Elements colHeadings = rows.get(0).getElementsByTag("td");
        for (int j = 1; j < rows.size(); j++) {
            JSONObject row = new JSONObject();
            Elements cols = rows.get(j).getElementsByTag("td");
            for (int k = 0; k < cols.size(); k++) {
                row.put(colHeadings.get(k).text(),cols.get(k).text());
            }
            table.add(row);
        }
        json.put(tableHeadings.get(i).text().split("-")[0],table);
    }

    /**
     * takes an url and returns a jsoup Document or the page.
     * @param url String of url
     * @return Document
     */
    private static Document getPage(String url) {
        Document webDoc = null;
        try {
            webDoc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36")
                    .timeout(0).followRedirects(true).execute().parse();
        } catch (IOException e) {
            System.out.println("Alien url error");
            e.printStackTrace();
            System.exit(0);
        }
        return webDoc;
    }

    /**
     * writes the json file with inputted name.
     * @param jsonArray JSONArray
     * @param filename String
     */
    private static void writeJson(JSONArray jsonArray, String filename) {
        //Pretty print writer
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(jsonArray.toJSONString());
        try {
            FileWriter writer = new FileWriter("jsons/" + filename);
            writer.write(gson.toJson(element));
            writer.close();
        } catch (IOException e) {
            System.out.println("Error saving " + filename);
            //e.printStackTrace();
        }

        System.out.println(filename + " created");
    }
}
