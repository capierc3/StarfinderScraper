import com.google.gson.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.print.Doc;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class AlienScrape {

    private static final String url = "https://aonsrd.com/Aliens.aspx?Letter=All";
    private static final String urlPrefix = "https://aonsrd.com/";


    /**
     * runs the scrape for the Alien information from the site.
     */
    public static void Scrape() {

        System.out.println("--Starting Alien Scrape--");

        Document webDoc = null;

        try {
            //get document
            webDoc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36")
                    .timeout(0).followRedirects(true).execute().parse();
            Element mainTable = webDoc.getElementById("ctl00_MainContent_GridViewAliens");

            Elements tableRows = mainTable.getElementsByTag("tr");
            JSONArray alienArray = new JSONArray();
            JSONArray starScaleArray = new JSONArray();
            //TODO: change i back to full array
            for (int i = 1; i < tableRows.size(); i++) {
                Element row = tableRows.get(i);
                Elements cols = row.getElementsByTag("td");
                Element link = cols.get(0).getElementsByTag("a").first();
                if (link != null) {
                    alienArray.add(read(link.attr("href")));
                } else {
                    System.out.println("Link not found");
                }
            }

            //Pretty print writer
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(alienArray.toJSONString());
            FileWriter writer = new FileWriter("jsons/aliens.json");
            writer.write(gson.toJson(element));
            writer.close();

            System.out.println("Alien json created");



        } catch (IOException e) {
            System.out.println("Alien url error");
            e.printStackTrace();
        }
    }

    /**
     * Parses and saves the needed information.
     * @param link Url of the Alien page
     * @return JsonObject of the Alien
     * @throws IOException error
     */
    private static JSONObject read(String link) throws IOException {

        //Get url document
        Document alienPage = Jsoup.connect(urlPrefix + link)
                .userAgent("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36")
                .timeout(0).followRedirects(true).execute().parse();

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


        //System.out.println(alien.toJSONString());

        return alien;
    }
}
