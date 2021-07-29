public class run {

    public static void main(String[] args) {
        System.out.println("Scraper Starting");
        new EquipmentScraper();
        AlienScrape.Scrape();
        WorldBuilderScraper.Scrape();
        System.out.println("DONE!");
    }

}
