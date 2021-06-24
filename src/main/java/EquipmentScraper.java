import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;

public class EquipmentScraper {

    private Elements page;
    private String pageName;
    private Document webDoc;

    /**
     * creates json files from the equipment pages on aonsrd.com.
     */

    //TODO: current errors
    //SHOULDN'T BE CALLED
    //SHOULDN'T BE CALLED
    //Red Star Plasma Kukri
    //Warclub
    //Unskilled Labor
    //Recharging Stations, 1 round/charge
    //Flechette
    public EquipmentScraper() {
        //read the archives xml for urls and pages
        try {
            InputStream is = this.getClass().getResourceAsStream("/archives.xml");
            Document doc = Jsoup.parse(is, null, "", Parser.xmlParser());
            //pulls up the equipment tag items in the xml
            Elements pages = doc.getElementsByTag("ship").get(0).children();
            //loops all the pages
            for (Element element : pages) {
                page = element.children();
                pageName = element.nodeName();
                //todo: remove after debugging
                //if (pageName.equalsIgnoreCase("baseframes"))
                read();
                System.out.println(pageName + ".json created");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * reads the page and sends the info to the correct methods for parsing and inserting.
     * @throws IOException e
     */
    private void read() throws IOException {
        if (page.get(1).text().equalsIgnoreCase("table")) {
            tableReader(page.get(0).text());
        } else {
            textReader(page.get(0).text());
        }
    }

    /**
     *  Reads the page element and gets the needed information from tables on the page.
     * @param url String of the page's url
     * @throws IOException e
     */
    private void tableReader(String url) throws IOException {
        //parse URL
        webDoc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36")
                .timeout(0).followRedirects(true).execute().parse();
        Elements tables = webDoc.getElementsByTag("table");
        //Array list that holds all the tables, pageTables.get(tableNum).get(rowNum).get(colNum)
        ArrayList<ArrayList<ArrayList<String>>> pageTables = new ArrayList<>();
        ArrayList<ArrayList<String>> tableHeadings = new ArrayList<>();
        for (Element table: tables) {
            ArrayList<ArrayList<String>> tableArray = new ArrayList<>();
            if (table.siblingElements().size() == 0) {
                String tableName;
                try {
                    tableName = table.parent().previousElementSibling().child(0).text();
                } catch (IndexOutOfBoundsException e) {
                    //This is okay just means table name is page name.
                    tableName = "";
                }
                Elements colHeadings = table.getElementsByTag("th");
                ArrayList<String> headings = new ArrayList<>();
                headings.add("source");
                //pulls the column headings from the table
                for (Element th : colHeadings) {
                    headings.add(th.text());
                }
                headings.add("type");
                tableHeadings.add(headings);
                Elements rows = table.getElementsByTag("tr");
                for (int i = 1; i < rows.size(); i++) {
                    ArrayList<String> row = new ArrayList<>();
                    Elements cols = rows.get(i).getElementsByTag("td");
                    boolean name = true;
                    String desc = "";
                    int colNum = 0;
                    for (Element col: cols) {
                        if (col.getElementsByTag("a").size() > 0 && name && colNum == 0) {
                            String link = "https://aonsrd.com/"
                                    + col.getElementsByTag("a").get(0).attributes().get("href");
                            desc = getDetails(link, col.text(), row);
                            name = false;
                        }
                        row.add(col.text());
                        colNum++;
                    }
                    //no link found for source and desc
                    if (name) {
                        Element mainContent = webDoc.getElementById("ctl00_MainContent_SectionHeader");
                        if (mainContent != null) {
                            Elements bs = mainContent.getElementsByTag("b");
                            for (Element elm : bs) {
                                if (elm.text().equalsIgnoreCase("Source")) {
                                    row.add(0,elm.nextElementSibling().text());
                                    desc = "no description found";
                                }
                            }
                        }
                    }
                    if (tableName.equalsIgnoreCase("")) {
                        row.add(pageName);
                    } else {
                        row.add(tableName);
                    }
                    if (desc.length() > 0) {
                        row.add(desc);
                        headings.add("desc");
                    }
                    tableArray.add(row);
                }
                pageTables.add(tableArray);
            }
        }
        //check to see if all table heading match.
        boolean tableCheck = true;
        ArrayList<String> tempHeadings = tableHeadings.get(0);
        for (int i = 1; i < tableHeadings.size(); i++) {
            if (tempHeadings.size() != tableHeadings.get(i).size() &&
                    !tableHeadings.get(i).get(0).equalsIgnoreCase(tempHeadings.get(0))) {
                tableCheck = false;
                break;
            }
        }
        //creates json array and entries.
        if (tableCheck) {
            StringBuilder jsonArray = new StringBuilder();
            jsonArray.append("[");
            String comma = " ";
            for (ArrayList<ArrayList<String>> arrayLists : pageTables) {
                jsonArray.append(comma);
                addToMongo(tempHeadings, arrayLists,jsonArray);
                comma = ", ";
            }
            jsonArray.append("]");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(jsonArray.toString());
            FileWriter writer = new FileWriter("jsons/" + pageName + ".json");
            writer.write(gson.toJson(element));
            writer.close();
        }
    }

    private String getDetails(String link, String item, ArrayList<String> row) throws IOException {

        //removes added amount to item names, ex: Arrows (20) -> Arrows
        if (item.contains("(")) {
            item = item.substring(0, item.lastIndexOf("(") - 1);
        }
        try {
            //parse url
            Document page = Jsoup.connect(link)
                    .userAgent("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36")
                    .timeout(0).followRedirects(true).execute().parse();
            //find main sections
            Elements tables = page.getElementsByTag("table");
            //pages without tables
            if (tables.size() == 0) {
                return extraTextRead(page, row);
            }
            int tableNum = 0;
            while (tables.get(tableNum).attributes().get("class").equalsIgnoreCase("inner")) {
                tableNum++;
            }
            Elements spans = tables.get(tableNum).getElementsByTag("span");
            Elements sections = spans.get(0).getElementsByTag("h2");
            Elements h1Sections = spans.get(0).getElementsByTag("h1");
            String desc = "";
            int spanNum = 0;
            //error tends to be on MK # items where there is a table or information throwing things off.
            while (sections.size() == 0 && h1Sections.size() == 0) {
                spanNum ++;
                sections = spans.get(spanNum).getElementsByTag("h2");
                h1Sections = spans.get(spanNum).getElementsByTag("h1");
                if (spans.size() < spanNum) {
                    spanNum = 0;
                    break;
                }
            }
            for (Element section : sections) {
                if (section.nextElementSibling().tagName().equalsIgnoreCase("h1")) {
                    section = section.nextElementSibling();
                }
                //todo: changed = to contains. need to recheck working jsons
                if (section.text().contains(item)) {
                    desc = searchInfo(section, row, spans, item, spanNum);
                    if (!desc.equalsIgnoreCase("")) {
                        break;
                    }
                }
            }
            if (desc.equalsIgnoreCase("")) {
                for (Element section : h1Sections) {
                    //todo: changed = to contains. need to recheck working jsons
                    if (section.text().contains(item)) {
                        desc = searchInfo(section, row, spans, item, spanNum);
                    }
                }
            }
            return desc;
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            System.out.println(pageName + ": " + item);
            System.exit(1);
        } catch (HttpStatusException e2) {
            System.out.println("page not found: " + link);
            System.out.println("\tNo extra details added for: " + item);
        }
        return "";
    }

    private String searchInfo(Element section, ArrayList<String> row, Elements spans, String item, int spanNum) {
        Element currentElm = section.nextElementSibling().nextElementSibling();
        String source = currentElm.text();
        if (row.size() == 0) {
            row.add(source);
        }
        currentElm = currentElm.nextElementSibling();
        //finds the items descriptions.
        while (currentElm.tagName().equalsIgnoreCase("b") || currentElm.nextElementSibling().tagName().equalsIgnoreCase("b") || currentElm.tagName().equalsIgnoreCase("a")) {
            currentElm = currentElm.nextElementSibling();
            if (currentElm == null) {
                System.out.println("error: " + item);
                return "error, looping till null";
            } else if (currentElm.tagName().equalsIgnoreCase("hr")) {
                Element heading = spans.get(spanNum).getElementsByTag("h1").get(0);
                return heading.nextSibling().toString();
            } else if (currentElm.tagName().equalsIgnoreCase("h3")) {
                //todo: get all text after breaks.
                return currentElm.nextSibling().toString();
            } else if (currentElm.tagName().equalsIgnoreCase("h2")) {
                //System.out.println("top desc: h2");
                return "top desc: h2";
            } else if (currentElm.nextElementSibling() == null) {
                System.out.println(item);
                return "No description found";
            }
        }
        //Finds descriptions that contains <i> tags and loops to get all information.
        if (currentElm.nextElementSibling() != null && currentElm.nextElementSibling().tagName().equalsIgnoreCase("i")) {
            Node node = currentElm.nextSibling();
            StringBuilder descBuilder = new StringBuilder();
            while (!node.toString().contains("<br>")) {
                descBuilder.append(node.toString().replace("<i>","").replace("</i>",""));
                node = node.nextSibling();
            }
            return descBuilder.toString();
        }
        //Looks to see if there is two <br> elements in a row which is where descriptions are stored
        else if (currentElm != null && currentElm.nextElementSibling() != null
                && currentElm.tagName().equalsIgnoreCase(currentElm.nextElementSibling().tagName())) {
            //if nothing is between the <br> tags then the desc is at the top of the span
            if (currentElm.nextSibling().toString().equalsIgnoreCase("<br>")) {
                Elements h1 = spans.get(spanNum).getElementsByTag("h1");
                Node node = h1.get(0).nextSibling();
                StringBuilder descBuilder = new StringBuilder();
                while (!node.toString().contains("<h2 ")) {
                    if (node.toString().equalsIgnoreCase("<br>")) {
                        descBuilder.append(System.getProperty("line.separator"));
                    } else {
                        descBuilder.append(node);
                    }
                    node = node.nextSibling();
                }
                return descBuilder.toString();
            }
            return currentElm.nextSibling().toString();
        }
        return "";
    }

    /**
     * gets extra information from a page that doesn't store information in a table.
     * @param page url Document
     * @param row used for source information
     * @return string of the description
     */
    private String extraTextRead(Document page, ArrayList<String> row) {

        Element mainContent = page.getElementById("ctl00_MainContent_DetailedOutput");
        if (mainContent == null) {
            row.add("No source found");
            return "No description available";
        }
        Elements content = mainContent.children();
        Element currentElem = content.first();

        StringBuilder desc = new StringBuilder();
        while (currentElem != null && !currentElem.tagName().equalsIgnoreCase("h2")) {
            if (currentElem.tagName().equalsIgnoreCase("b") && currentElem.text().equalsIgnoreCase("Source")) {
                currentElem = currentElem.nextElementSibling();
                row.add(currentElem.text());
            }
            if (currentElem.tagName().equalsIgnoreCase("br")) {
                if (!(currentElem.nextSibling() instanceof Element)) {
                    if (desc.length() != 0) {
                        desc.append("\n");
                    }
                    if (currentElem.nextSibling() != null) {
                        desc.append(currentElem.nextSibling().toString());
                    }
                }
            }
            currentElem = currentElem.nextElementSibling();
        }
        return desc.toString();
    }

    private void textReader(String url) throws IOException {
        //parse URL
        webDoc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36")
                .timeout(0).followRedirects(true).execute().parse();
        Element table = webDoc.getElementById("ctl00_MainContent_DataListAll");
        JSONArray mainArray = new JSONArray();
        if (table != null) {
            Elements items = table.getElementsByTag("span");
            for (Element item: items) {
                Elements values = item.children();
                JSONObject jsonItem = new JSONObject();
                for (Element value : values) {
                    if (value.tagName().equalsIgnoreCase("h2")) {
                        jsonItem.put("Name",value.text());
                    } else if (value.text().equalsIgnoreCase("Source")) {
                        jsonItem.put("Source",value.nextElementSibling().text());
                    } else if (value.tagName().equalsIgnoreCase("b")) {
                        jsonItem.put(value.text(),value.nextSibling().toString());
                    } else if (value.tagName().equalsIgnoreCase("h3")) {
                        JSONArray array = new JSONArray();
                        JSONObject special = new JSONObject();
                        Element elm = value.nextElementSibling();
                        while (elm != null) {
                            if (elm.tagName().equalsIgnoreCase("b")) {
                                special.put(elm.text(),elm.nextSibling().toString());
                            }
                            elm = elm.nextElementSibling();
                        }
                        array.add(special);
                        jsonItem.put(value.text(),array);
                        break;
                    } else if (value.tagName().equalsIgnoreCase("br")) {
                        if (value.nextSibling() != null && !(value.nextSibling() instanceof Element)) {
                            if (jsonItem.get("desc") == null) {
                                jsonItem.put("desc", value.nextSibling().toString());
                            } else {
                                jsonItem.put("desc", jsonItem.get("desc") + "\n" + value.nextSibling().toString());
                            }
                        }
                    }
                }
                mainArray.add(jsonItem);
            }
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(mainArray.toJSONString());
        FileWriter writer = new FileWriter("jsons/" + pageName + ".json");
        writer.write(gson.toJson(element));
        writer.close();
    }

//TODO: get working, rows need to be new json
    /**
     * creates the table in the database ships with the found column headings.
     * @param columnHeadings ArrayList
     * @return
     */
    private String addToMongo(ArrayList<String> columnHeadings, ArrayList<ArrayList<String>> rows, StringBuilder jsonArray) {
        String comma = "";
        for (ArrayList<String> row: rows) {
            jsonArray.append(comma);
            org.bson.Document doc = new org.bson.Document();
            for (int i = 0; i < row.size(); i++) {
                doc.append(columnHeadings.get(i), row.get(i));
            }
            jsonArray.append(doc.toJson());
            comma = ", ";
            //mongo.addEntry("equipment." + pageName, doc);
        }
        return null;
    }

    /**
     * Takes the list of column headings and the list containing the lists for the rows
     * and adds them to the database.
     * Used for bulk new insertions into the database.
     * @param columnHeadings ArrayList if strings
     * @param rows ArrayList of Lists of strings.
     */
    private void addEntries(ArrayList<String> columnHeadings, ArrayList<ArrayList<String>> rows) {

        ArrayList<String> inserts = new ArrayList<>();
        //String base = sql.toString();
        for (ArrayList<String> values:rows) {
            //sql = new StringBuilder();
            for (int i = 0; i < values.size(); i++) {
                String editValue = values.get(i).replace("'","_");
                //sql.append(editValue);
                if (i == values.size() - 1) {
                    //sql.append("');");
                } else {
                    //sql.append("','");
                }
            }
            //if (!sql.toString().contains("VALUES ('')")) {
           //     inserts.add(base + sql);
           // }
        }
        //SQLite.AddRecord("equipment",inserts,pageName);
    }

}
