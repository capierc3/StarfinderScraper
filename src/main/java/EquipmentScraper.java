import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class EquipmentScraper {

    private Elements page;
    private String pageName;
    private Document webDoc;
    private MongoUtils mongo;

    /**
     * creates json files from the equipment pages on aonsrd.com.
     */
    public EquipmentScraper() {
        try {
            InputStream is = this.getClass().getResourceAsStream("/archives.xml");
            Document doc = Jsoup.parse(is, null, "", Parser.xmlParser());
            Elements pages = doc.getElementsByTag("equipment").get(0).children();
            mongo = new MongoUtils("localhost", 27017);
            for (Element element : pages) {
                page = element.children();
                pageName = element.nodeName();
                read();
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
            //textReader(page.get(0).text());
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
                String tableName = "";
                try {
                    tableName = table.parent().previousElementSibling().child(0).text();
                } catch (IndexOutOfBoundsException e) {
                    System.out.println("table name not found in " + pageName);
                }
                Elements colHeadings = table.getElementsByTag("th");
                ArrayList<String> headings = new ArrayList<>();
                for (Element th : colHeadings) {
                    headings.add(th.text());
                }
                headings.add("type");
                tableHeadings.add(headings);
                Elements rows = table.getElementsByTag("tr");
                for (int i = 1; i < rows.size(); i++) {
                    ArrayList<String> row = new ArrayList<>();
                    Elements cols = rows.get(i).getElementsByTag("td");
                    for (Element col: cols) {
                        row.add(col.text());
                    }
                    row.add(tableName);
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
            for (ArrayList<ArrayList<String>> arrayLists : pageTables) {
                System.out.println(pageName);
                addToMongo(tempHeadings, arrayLists);
            }
        }
    }

    private void textReader(String url) throws IOException {
        //parse URL
        webDoc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36")
                .timeout(0).followRedirects(true).execute().parse();
        Elements tables = webDoc.getElementsByTag("table");
        ArrayList<ArrayList<String>> pageHeadings = new ArrayList<>();
        ArrayList<ArrayList<ArrayList<String>>> pageTables = new ArrayList<>();
        String type;
        for (Element table: tables) {
            if (table.className().equalsIgnoreCase("inner")) {
                Element parent = table.parent();
                Elements h2 = parent.getElementsByClass("title");
                type = h2.get(0).text();
                Elements rows = table.getElementsByTag("tr");
                ArrayList<ArrayList<String>> tableArray = new ArrayList<>();
                ArrayList<String> tableHeadings = new ArrayList<>();
                for (int i = 0; i < rows.size(); i++) {
                    Elements cols = rows.get(i).getElementsByTag("td");
                    ArrayList<String> row = new ArrayList<>();
                    for (Element col : cols) {
                        if (i == 0) {
                            tableHeadings.add(col.text());
                        } else {
                            row.add(col.text());
                        }
                    }
                    if (i > 0) {
                        row.add(type);
                        tableArray.add(row);
                    }
                }
                tableHeadings.add("type");
                pageHeadings.add(tableHeadings);
                pageTables.add(tableArray);
            }
        }
        //todo: test all headings match
        //buildJson(pageHeadings.get(0));
        for (ArrayList<ArrayList<String>> table: pageTables) {
            addEntries(pageHeadings.get(0),table);
        }

    }

//TODO: get working, rows need to be new json
    /**
     * creates the table in the database ships with the found column headings.
     * @param columnHeadings ArrayList
     */
    private void addToMongo(ArrayList<String> columnHeadings, ArrayList<ArrayList<String>> rows) {
        for (ArrayList<String> row: rows) {
            org.bson.Document doc = new org.bson.Document();
            for (int i = 0; i < row.size(); i++) {
                if (columnHeadings.get(i).equalsIgnoreCase("type")) {
                    doc.append("type", pageName);
                } else {
                    doc.append(columnHeadings.get(i), row.get(i));
                }
            }
            mongo.addEntry("equipment." + pageName, doc);
        }
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
