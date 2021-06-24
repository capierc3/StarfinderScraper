import com.google.gson.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class AlienScrape {

    private static final String alienUrl = "https://aonsrd.com/Aliens.aspx?Letter=All";
    private static final String urlPrefix = "https://aonsrd.com/";


    /**
     * runs the scrape for the Alien information from the site.
     */
    public static void Scrape() {

        System.out.println("Starting Alien Json Scraping");
        alienScrape();
        arrayScrape();
        graftScrape();
        rulesScrape();
        System.out.println("Alien Jsons all done!\n");

    }

    //rules/ ability to json

    private static void rulesScrape() {
        System.out.println("Scraping monster rules");

        Document doc = getPage("https://aonsrd.com/UniversalMonsterRules.aspx?ItemName=All");
        Element mainTable = doc.getElementById("ctl00_MainContent_DataListRulesAll");
        Elements rows = mainTable.getElementsByTag("tr");

        JSONArray rulesJson = new JSONArray() ;
        Element current;
        for (Element row : rows) {
            JSONObject rule = new JSONObject();
            current = row.getElementsByTag("h1").get(0);
            rule.put("Name", current.text());
            current = current.nextElementSibling().nextElementSibling();
            StringBuilder value = new StringBuilder();
            while (current.tagName().equalsIgnoreCase("a")) {
                value.append(current.text());
                if (current.nextSibling().toString().equalsIgnoreCase(", ")) {
                    value.append(", ");
                } else {
                    break;
                }
                current = current.nextElementSibling();
            }
            rule.put("Source", value.toString());
            value = new StringBuilder();
            current = current.nextElementSibling();
            while (true) {
                if (!(current.nextSibling() instanceof Element)) {
                    if (!current.tagName().equalsIgnoreCase("br")) {
                        value.append(current.text()).append(" ");
                    }
                    value.append(current.nextSibling());
                } else if (current.nextElementSibling().tagName().equalsIgnoreCase("br")) {
                    break;
                }
                current = current.nextElementSibling();
                if (current == null) {
                    break;
                }
            }
            if (current != null && current.tagName().equalsIgnoreCase("ul")) {
                Elements items = current.getElementsByTag("li");
                for (Element item: items) {
                    value.append(item.text());
                }
                current = current.nextElementSibling();
            }
            while (current != null && current.tagName().equalsIgnoreCase("br")) {
                current = current.nextElementSibling();
            }
            if (current == null) {
                break;
            }
            rule.put("Desc", value.toString());
            value = new StringBuilder();
            if (current.tagName().equalsIgnoreCase("i")) {
                if (current.nextElementSibling() != null && current.nextElementSibling().tagName().equalsIgnoreCase("b")) {
                    current = current.nextElementSibling();
                    value.append(current.text()).append(": ");
                    value.append(current.nextSibling().toString());
                }
                rule.put("Format", value.toString());
            }
            current = current.nextElementSibling();
            if (current != null && current.nextElementSibling() != null) {
                current = current.nextElementSibling();
            }
            if (current.tagName().equalsIgnoreCase("i")) {
                rule.put("Guidelines", current.nextSibling().toString().replace(": ", ""));
            }
            rulesJson.add(rule);
        }
        writeJson(rulesJson,"monster_rules.json");
    }

    //graft scraper

    private static void graftScrape() {

        System.out.println("Scraping grafts");

        Document doc = getPage("https://aonsrd.com/TemplateGrafts.aspx?ItemName=All&Family=None");
        Element mainSpan = doc.getElementById("ctl00_MainContent_DataListExamples_ctl00_LabelName");
        Elements headings = mainSpan.getElementsByTag("h1");

        JSONArray jsonArray = new JSONArray();
        for (Element heading: headings) {
            JSONObject graftJson = new JSONObject();
            graftJson.put("Type", heading.text().replace(" Grafts", ""));
            JSONArray grafts = new JSONArray();
            Element current = heading;
            while (current.nextElementSibling() != null && current.nextElementSibling().tagName().equalsIgnoreCase("a")) {
                current = current.nextElementSibling();
                grafts.add(getGraftJson(current.attr("href"), current.text()));
            }
            graftJson.put("grafts", grafts);
            jsonArray.add(graftJson);
        }
        writeJson(jsonArray, "grafts.json");
    }

    private static JSONObject getGraftJson(String url, String name) {

        Document doc = getPage("https://aonsrd.com/" + url);
        Elements headings = doc.getElementsByTag("h2");
        Element graftHeading = headings.last();

        JSONObject graft = new JSONObject();
        if (name.contains(" (")) {
            graft.put("Subtype", name.substring(name.indexOf("(") + 1, name.lastIndexOf(")") - 1));
        }
        //graft with no information on page.
        if (headings.size() < 4) {
            graft.put("Name", name.substring(0, name.indexOf("(") - 1));
            return graft;
        }
        //if more then on graft type is in a page
        if (headings.size() > 4) {
            for (Element heading : headings) {
                String foundName = heading.text();
                if (heading.text().contains("(CR ")) {
                    foundName = foundName.substring(0,foundName.indexOf("(") -1);
                }
                if (foundName.equalsIgnoreCase(name.substring(0,name.indexOf("(") - 1))) {
                    graftHeading = heading;
                    break;
                }
            }
        }
        //get information
        graft.put("Name", graftHeading.text());
        Element current = graftHeading.nextElementSibling();
        Elements keys = new Elements();
        while (current != null && !current.tagName().equalsIgnoreCase("h2")) {
            if (current.tagName().equalsIgnoreCase("b")) {
                keys.add(current);
            }
            current = current.nextElementSibling();
        }

        for (Element key : keys) {
            current = key;
            if (key.text().equalsIgnoreCase("Source")) {
                current = current.nextElementSibling();
                StringBuilder source = new StringBuilder();
                while (current.tagName().equalsIgnoreCase("a")) {
                    if (source.length() != 0) {
                        source.append(", ");
                    }
                    source.append(current.text());
                    current = current.nextElementSibling();
                }
                graft.put("Source", source.toString());
                StringBuilder desc = new StringBuilder();
                while (current != null && !(current.nextSibling() instanceof Element)) {
                    desc.append(current.nextSibling().toString());
                    current = current.nextElementSibling();
                }
                graft.put("Desc", desc.toString());
            }
            else if (key.text().equalsIgnoreCase("Abilities by CR")) {
                JSONObject abilities = new JSONObject();
                Element temp = key.nextElementSibling();
                while (temp != null) {
                    if (!temp.text().equalsIgnoreCase("")) {
                        abilities.put(temp.text(), temp.nextSibling().toString().replace(": ",""));
                    }
                    temp = temp.nextElementSibling();
                }
                graft.put("Abilities by CR", abilities);
                break;
            }
            else if (key.text().equalsIgnoreCase("Traits")) {
                JSONObject trait = new JSONObject();
                current = current.nextElementSibling();

                String keyValue = "Main";
                StringBuilder value = new StringBuilder();
                value.append(key.nextSibling().toString());

                while (current != null && !(current.tagName().equalsIgnoreCase("b"))) {
                    if (current.tagName().equalsIgnoreCase("br")) {
                        if (!(current.nextSibling() instanceof  Element)) {
                            value.append(current.nextSibling().toString());
                        } else {
                            trait.put(keyValue, value.toString());
                        }
                    } else if (current.tagName().equalsIgnoreCase("i") && current.text().contains(": ")) {
                        keyValue = current.text();
                        value = new StringBuilder(current.nextSibling().toString());
                    } else {
                        value.append(current.text()).append(" ").append(current.nextSibling());
                    }
                    current = current.nextElementSibling();
                }
                trait.put(keyValue, value.toString());
                graft.put(key.text(), trait);
            }
            else if (key.text().equalsIgnoreCase("Attack")) {
                //TODO add attack and check other grafts type
                JSONObject attack = new JSONObject();
                StringBuilder value = new StringBuilder();
                value.append(key.nextSibling().toString());
                current = key.nextElementSibling();
                while (current != null) {
                    if (current.tagName().equalsIgnoreCase("br")) {
                        value.append(" ");
                        if (current.nextSibling() != null && current.nextElementSibling().tagName().equalsIgnoreCase("br")) {
                            current = current.nextElementSibling();
                        }
                    } else {
                        value.append(current.text()).append(" ");
                        if (current.nextSibling() != null) {
                            value.append(current.nextSibling().toString());
                        }
                    }
                    current = current.nextElementSibling();
                }
                graft.put("Attack", value.toString());
                break;
            }
            else {
                StringBuilder value = new StringBuilder();
                value.append(key.nextSibling().toString());
                Element temp = key.nextElementSibling();
                while (temp != null && !temp.tagName().equalsIgnoreCase("b")) {
                    if (temp.tagName().equalsIgnoreCase("br")) {
                        if (temp.nextSibling() != null && !(temp.nextSibling() instanceof Element)) {
                            value.append(temp.nextSibling().toString());
                        }
                    }
                    else {
                        value.append(temp.text()).append(" ").append(temp.nextSibling());
                    }
                    temp = temp.nextElementSibling();
                }
                graft.put(key.text(), value.toString());
            }
        }
        return  graft;
    }

    //Creature arrays to json

    /**
     * Scraps the site for creature type arrays
     */
    private static void arrayScrape() {

        System.out.println("Scraping creature arrays");

        Document doc = getPage("https://www.aonsrd.com/Rules.aspx?ID=318");
        Elements tables = doc.getElementsByTag("table");
        Elements headings = doc.getElementsByTag("h1");
        headings.removeIf(heading -> !heading.text().contains("Table "));
        assert(tables.size() == headings.size());
        JSONArray arrays = new JSONArray();
        int tableNum = 0;
        for (int i = 0; i < headings.size(); i++) {
            JSONObject creatureJson = new JSONObject();
            String type = headings.get(i).text().substring(9).split(" - ")[0].split(" ")[0];
            creatureJson.put("Type", type);
            creatureJson.put("Main Array", readTable(tables.get(tableNum)));
            tableNum++;
            if (tableNum < tables.size()) {
                creatureJson.put("Attack Array", readTable(tables.get(tableNum)));
                tableNum++;
            }
            i++;
            arrays.add(creatureJson);
        }

        writeJson(arrays, "creature_arrays.json");
    }

    /**
     * reads the tables for the type arrays
     * @param table Element of the table to read
     * @return JSONArray
     */
    private static JSONArray readTable (Element table) {
        JSONArray mainArray = new JSONArray();
        Elements rows = table.getElementsByTag("tr");
        Elements headings = rows.get(0).getElementsByTag("td");
        if (rows.get(0).getElementsByTag("td").size() != rows.get(1).getElementsByTag("td").size()) {
            headings = rows.get(1).getElementsByTag("td");
        }
        for (int i = 1; i < rows.size(); i++) {
            JSONObject row = new JSONObject();
            int colNum = 0;
            for (Element col : rows.get(i).getElementsByTag("td")) {
                row.put(headings.get(colNum).text(),col.text());
                colNum++;
            }
            mainArray.add(row);
        }
        return mainArray;
    }

    //Alien scraping functions

    /**
     * Scrapes the entire list of aliens and saves as json.
     */
    private static void alienScrape() {

        System.out.println("Scraping Aliens");

        Document webDoc = getPage(alienUrl);
        Element mainTable = webDoc.getElementById("ctl00_MainContent_GridViewAliens");
        Elements tableRows = mainTable.getElementsByTag("tr");
        JSONArray alienArray = new JSONArray();

        for (int i = 1; i < tableRows.size(); i++) {
            Element row = tableRows.get(i);
            Elements cols = row.getElementsByTag("td");
            Element link = cols.get(0).getElementsByTag("a").first();
            if (link != null) {
                alienArray.add(alienRead(link.attr("href")));
            } else {
                System.out.println("Link not found");
            }
        }

        writeJson(alienArray, "aliens.json");
    }

    /**
     * Parses and saves the needed information.
     * @param link Url of the Alien page
     * @return JsonObject of the Alien
     */
    private static JSONObject alienRead(String link) {

        //Get url document
        Document alienPage = getPage(urlPrefix + link);
        Element mainInfo = alienPage.getElementById("ctl00_MainContent_DataListTalentsAll_ctl00_LabelName");
        Elements headings = mainInfo.getElementsByTag("h2");

        JSONObject alien = new JSONObject();
        //get name section and descriptions
        for (Element heading: headings) {
            //Get Name and CR/Tier
            String splitWord = " ";
            if (heading.text().contains("CR")) {
                alien.put("Scale", "normal");
                splitWord = "CR";
            } else if (heading.text().contains("Tier")){
                splitWord = "Tier";
                alien.put("Scale", "Starship");
            }
            //Separate name from CR/tier if name heading
            if (!splitWord.equalsIgnoreCase(" ")) {
                String[] nameCR = heading.text().split(splitWord);
                alien.put("Name", nameCR[0]);
                if (nameCR.length >= 2) {
                    String cr = nameCR[1].replaceAll(" ", "");
                    if (cr.contains("/")) {
                        double crNum = Double.parseDouble(cr.split("/")[0]) / Double.parseDouble(cr.split("/")[1]);
                        cr = Double.toString(crNum);
                    }
                    alien.put(splitWord, cr);
                } else {
                    alien.put(splitWord, 0);
                }
                //get rest of name block
                Element current = heading.nextElementSibling();
                while (current != null && !current.tagName().equalsIgnoreCase("h3")) {
                    if (current.tagName().equalsIgnoreCase("b")) {
                        if (current.text().contains("XP")) {
                            alien.put(current.text().split(" ")[0], current.text().split(" ")[1]);
                            current = current.nextElementSibling();
                            if (current.nextElementSibling().nextElementSibling().tagName().equalsIgnoreCase("br")) {
                                alien.put("Race", current.nextSibling().toString());
                                current = current.nextElementSibling();
                                alien.put("Graft", current.nextSibling().toString());
                            } else {
                                alien.put("Race", "none");
                                alien.put("Graft", current.nextSibling().toString());
                            }
                        } else {
                            alien.put(current.text(), current.nextSibling().toString());
                        }
                    }
                    current = current.nextElementSibling();
                }
            }
            //Read description
            else if (heading.text().equalsIgnoreCase("Description")) {
                StringBuilder desc = new StringBuilder();
                desc.append(heading.nextSibling().toString()).append("\n");
                Element current = heading.nextElementSibling();
                while (current != null && !current.tagName().equalsIgnoreCase("h1")
                        && current.nextSibling() != null) {
                    if (current.nextSibling().toString().equalsIgnoreCase("<br>")) {
                        desc.append("\n");
                    } else {
                        desc.append(current.nextSibling().toString());
                    }
                    current = current.nextElementSibling();
                }
                alien.put("Desc", desc.toString());
            }
        }
        //find subsections
        Elements subHeadings = mainInfo.getElementsByTag("h3");
        for (Element subHeading : subHeadings) {
            Element current = subHeading.nextElementSibling();
            Elements sectionEntries = new Elements();
            while (current != null && !current.tagName().equalsIgnoreCase("h3")) {
                if (current.tagName().equalsIgnoreCase("b")) {
                    sectionEntries.add(current);
                }
                current = current.nextElementSibling();
            }
            //get subsection data
            JSONObject data = new JSONObject();
            for (Element entry : sectionEntries) {
                String key = entry.text();
                StringBuilder value = new StringBuilder();
                current = entry;
                while (current.nextSibling() != null) {
                    if (current.tagName().equalsIgnoreCase("i") || current.tagName().equalsIgnoreCase("a")) {
                        value.append(current.text());
                    }
                    value.append(current.nextSibling().toString());
                    current = current.nextElementSibling();
                    if (current == null || current.tagName().equalsIgnoreCase("b")
                            || current.nextSibling() instanceof Element || current.tagName().contains("h")) {
                        break;
                    }
                }
                data.put(key, value.toString());
            }
            //Add to subsection to json
            alien.put(subHeading.text(), data);
        }
        return alien;
    }

    //Helper functions

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
