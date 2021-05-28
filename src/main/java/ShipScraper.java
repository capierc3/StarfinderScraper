import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Class that reads the Website aonsrd.com and turns the ship info into a database file.
 *
 * @author Chase
 */
public class ShipScraper {

    private Elements page;
    private String pageName;
    private Document webDoc;

    /**
     * Builds a new database of the Ship components from aonsrd.com.
     */
    public ShipScraper() {
        //SQLite.build("ships");
        try {
            InputStream is = this.getClass().getResourceAsStream("/archives.xml");
            Document doc = Jsoup.parse(is, null, "", Parser.xmlParser());
            Elements pages = doc.getElementsByTag("ship").get(0).children();
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

        //main list is if there is more then one table on the page,
        //2nd list is the whole row
        //3rd list is the item in the row.
        ArrayList<ArrayList<ArrayList<String>>> tablesArray = new ArrayList<>();
        ArrayList<String> columnsHeadings = new ArrayList<>();
        for (Element e:tables) {
            ArrayList<ArrayList<String>> table = new ArrayList<>();
            Elements tr = e.getElementsByTag("tr");
            for (Element element: tr) {
                Elements td = element.getElementsByTag("td");
                ArrayList<String> row = new ArrayList<>();
                for (Element e2 : td) {
                    row.add(e2.text());
                }
                if (row.size() > 0) {
                    table.add(row);
                }
                Elements th = element.getElementsByTag("th");
                for (Element e3:th) {
                    if (columnsHeadings.isEmpty()) {
                        columnsHeadings.add(e3.text());
                    } else if (columnsHeadings.get(0).equalsIgnoreCase(e3.text())) {
                        break;
                    } else {
                        String heading = e3.text().replace("(","");
                        heading = heading.replace(")","");
                        heading = heading.replace(" ","_");
                        columnsHeadings.add(heading);
                    }
                }
            }
            tablesArray.add(table);
        }
        createTable(columnsHeadings);
        for (ArrayList<ArrayList<String>> arrayLists : tablesArray) {
            addEntries(columnsHeadings, arrayLists);
        }
    }

    /**
     * Reads the page element and adds the info to the database for text based pages.
     * @param url String of the page's url
     * @throws IOException e
     */
    private void textReader(String url) throws IOException {
        webDoc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36")
                .timeout(0).followRedirects(true).execute().parse();
        ArrayList<ArrayList<String>> allHeadings = new ArrayList<>();
        ArrayList<ArrayList<String>> allValues = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();
        //Finds the information and stores it in arrays.
        Elements blocks = webDoc.getElementsByTag("body");
        Element block = blocks.get(0).getElementById("main");
        block = block.getElementById("ctl00_MainContent_DataListAll");
        blocks = block.getElementsByTag("tr");
        for (Element value : blocks) {
            ArrayList<String> headings = new ArrayList<>();
            ArrayList<String> values = new ArrayList<>();
            titles.add(value.getElementsByClass("title").get(0).text());
            if (value.getElementsByTag("b").size() == 1) {
                headings.add(value.getElementsByTag("b").get(0).text());
                Element a = value.getElementsByTag("b").get(0).nextElementSibling();
                values.add(a.getElementsByTag("i").text());
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < value.getElementsByTag("br").size(); j++) {
                    Element elm = value.getElementsByTag("br").get(j);
                    while (elm.nextElementSibling() != null) {
                        String s = elm.nextSibling().toString().replace("'","/");
                        sb.append(s);
                        if (elm.nextElementSibling().tag().toString().equalsIgnoreCase("a")) {
                            sb.append(elm.nextElementSibling().text());
                            sb.append(elm.nextElementSibling().getElementsByTag("i").text());
                            sb.append("\n");
                        }
                        elm = elm.nextElementSibling();
                    }
                }
                values.add(sb.toString());
            } else {
                Elements elms = value.getElementsByTag("b");
                for (Element element : elms) {
                    headings.add(element.text());
                    if (element.nextSibling().toString()
                            .equalsIgnoreCase(" ")) {
                        values.add(value.getElementsByTag("i").text());
                    } else {
                        values.add(element.nextSibling().toString());
                    }
                }
                Element elm = elms.get(elms.size() - 1).nextElementSibling();
                while (elm.nextSibling() != null) {
                    if (!elm.nextSibling().toString().contains("<br>")) {
                        String s = elm.nextElementSibling().toString().replace("'","/");
                        values.add(s);
                    }
                    elm = elm.nextElementSibling();
                }
            }
            allHeadings.add(headings);
            allValues.add(values);
        }
        cleanAndInsert(allHeadings,allValues,titles);
    }


    /**
     * Takes the list of column headings and the list containing the lists for the rows
     * and adds them to the database.
     * Used for bulk new insertions into the database.
     * @param columnHeadings ArrayList if strings
     * @param rows ArrayList of Lists of strings.
     */
    private void addEntries(ArrayList<String> columnHeadings, ArrayList<ArrayList<String>> rows) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(pageName).append("(");
        for (int i = 0; i < columnHeadings.size(); i++) {
            sql.append(columnHeadings.get(i).replace(" ","_"));
            if (i == columnHeadings.size() - 1) {
                sql.append(")");
            } else {
                sql.append(",");
            }
        }
        sql.append("VALUES ('");
        ArrayList<String> inserts = new ArrayList<>();
        String base = sql.toString();
        for (ArrayList<String> values:rows) {
            sql = new StringBuilder();
            for (int i = 0; i < values.size(); i++) {
                sql.append(values.get(i));
                if (i == values.size() - 1) {
                    sql.append("');");
                } else {
                    sql.append("','");
                }
            }
            inserts.add(base + sql.toString());
        }
        //SQLite.AddRecord("ships",inserts,pageName);
    }

    /**
     * creates the table in the database ships with the found column headings.
     * @param columnHeadings ArrayList
     */
    private void createTable(ArrayList<String> columnHeadings) {
        StringBuilder table = new StringBuilder();
        table.append("CREATE TABLE ")
                .append(pageName)
                .append("(");
        for (int i = 0; i < columnHeadings.size(); i++) {
            table.append(columnHeadings.get(i).replace(" ","_"));
            if (i == 0) {
                table.append("     TEXT NOT NULL, ");
            } else if (i == columnHeadings.size() - 1) {
                table.append("     TEXT)");
            } else {
                table.append("     TEXT, ");
            }
        }
        //SQLite.createTable("ships",pageName,table.toString());
    }

    /**
     * Takes the arrays from the text reader and turns them into arrays for SQL stings.
     * @param headings List of all the heading list found for the page.
     * @param values List of all the value lists for the page.
     * @param titles String of the titles from the page.
     */
    private void cleanAndInsert(ArrayList<ArrayList<String>> headings,
                               ArrayList<ArrayList<String>> values, ArrayList<String> titles) {
        int headingHigh = headings.get(0).size();
        int valuesHigh = values.get(0).size();
        int headingLow = headings.get(0).size();
        int headingLowI = 0;
        int valuesLow = values.get(0).size();
        int headingDiff = 1;
        int valuesDiff = 1;
        for (int i = 1; i < headings.size(); i++) {
            if (values.get(i).size() < valuesLow) {
                valuesLow = values.get(i).size();
                valuesDiff++;
            } else if (values.get(i).size() > valuesHigh) {
                valuesHigh = values.get(i).size();
                valuesDiff++;
            }
            if (headings.get(i).size() < headingLow) {
                headingLow = headings.get(i).size();
                headingDiff++;
            } else if (headings.get(i).size() > headingHigh) {
                headingHigh = headings.get(i).size();
                headingDiff++;
            }
        }
        ArrayList<String> finalHeadings = new ArrayList<>();
        finalHeadings.add("Name");
        finalHeadings.addAll(headings.get(headingLowI));
        if (headingDiff > 1) {
            finalHeadings.add("Other");
        }
        if (valuesHigh > headingHigh) {
            finalHeadings.add("info");
        }
        ArrayList<ArrayList<String>> finalValues = new ArrayList<>();
        ArrayList<String> tempValues;
        for (int i = 0; i < titles.size(); i++) {
            tempValues = new ArrayList<>();
            tempValues.add(titles.get(i));
            for (int j = 0; j < finalHeadings.size() - 1; j++) {
                if (values.get(i).size() > j) {
                    tempValues.add(values.get(i).get(j));
                } else {
                    tempValues.add("N/A");
                }
            }
            finalValues.add(tempValues);
        }
        createTable(finalHeadings);
        addEntries(finalHeadings,finalValues);
    }
}
