import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.util.Scanner;

public class textToJson {


    public static void main(String[] args) {

        File dir = new File("textfiles");
        if (dir.isDirectory() && dir.listFiles() != null) {
            for (File file : dir.listFiles()) {
                convertFile(file.getPath(), file.getName());
            }
        }
    }

    private static void convertFile(String path, String name) {
        try {
            Scanner in = new Scanner(new FileReader(path));
            JSONObject json = new JSONObject();
            JSONArray array = new JSONArray();
            while (in.hasNext()) {
                String value = in.nextLine().split(" ")[1];
                array.add(value);
            }
            json.put("list", array);
            writeJson(json,name.replace(".txt",".json"));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void writeJson(JSONObject json, String filename) {
        //Pretty print writer
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(json.toJSONString());
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
