import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import com.opencsv.CSVWriter;

public class Main {

    public static void main(String[] args) throws Exception {
        List<CrawlItem> fetched = new CopyOnWriteArrayList();
        List<CrawlItem> discovered = new CopyOnWriteArrayList();
        List<CrawlItem> visited = new CopyOnWriteArrayList();
        AtomicInteger fetches_failed = new AtomicInteger();

        String crawlStorageFolder = "data/crawl/";
        int numberOfCrawlers = 12;

        CrawlConfig config = new CrawlConfig();

        config.setIncludeBinaryContentInCrawling(true);
        config.setIncludeHttpsPages(true);
        config.setResumableCrawling(false);
        config.setCrawlStorageFolder(crawlStorageFolder);
        config.setMaxDepthOfCrawling(16);
        config.setMaxPagesToFetch(20000);
        config.setPolitenessDelay(10);

        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        robotstxtConfig.setEnabled(false);
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);

        CrawlController.WebCrawlerFactory<MyCrawler> factory = () -> new MyCrawler(fetched, discovered, visited, fetches_failed);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
        controller.addSeed("https://www.latimes.com");
        controller.start(factory, numberOfCrawlers);

        String[] fetchedHeader = new String[] {"URL", "Status Code"};
        String[] discoveredHeader = new String[] {"URL", "Outside Indicator"};
        String[] visitedHeader = new String[] {"URL", "Size/bytes", "Number of Outlinks", "Content-type"};

        CSVWriter fetchWriter = new CSVWriter(new FileWriter("fetch_latimes.csv"));
        CSVWriter discoverWriter = new CSVWriter(new FileWriter("urls_latimes.csv"));
        CSVWriter visitWriter = new CSVWriter(new FileWriter("visit_latimes.csv"));

        fetchWriter.writeNext(fetchedHeader);
        discoverWriter.writeNext(discoveredHeader);
        visitWriter.writeNext(visitedHeader);

        HashMap<Integer, Integer> statusCount = new HashMap<>();
        for (CrawlItem item: fetched) {
            statusCount.put(item.getStatus(), statusCount.getOrDefault(item.getStatus(), 0) + 1);
            fetchWriter.writeNext(item.fetchedToArray());
        }

        HashSet<String> OKSet = new HashSet<>();
        HashSet<String> N_OKSet = new HashSet<>();
        for (CrawlItem item: discovered) {
            if (item.getOutside()) {
                OKSet.add(item.getUrl());
            } else {
                N_OKSet.add(item.getUrl());
            }
            discoverWriter.writeNext(item.discoveredToArray());
        }

        int size1 = 0, size2 = 0, size3 = 0, size4 = 0, size5 = 0;
        int total_urls = 0;
        HashMap<String, Integer> typesCount = new HashMap<>();
        for (CrawlItem item: visited) {
            int size = item.getSize();
            if (size < 1024) {
                size1 += 1;
            } else if (size < 10240) {
                size2 += 1;
            } else if (size < 102400) {
                size3 += 1;
            } else if (size < 1024 * 1024) {
                size4 += 1;
            } else {
                size5 += 1;
            }
            total_urls += item.getOutnum();
            typesCount.put(item.getType(), typesCount.getOrDefault(item.getType(), 0) + 1);
            visitWriter.writeNext(item.visitedToArray());
        }

        fetchWriter.close();
        discoverWriter.close();
        visitWriter.close();
        System.out.println("Name: Guangyuan Zhao");
        System.out.println("USC ID: 6244177719");
        System.out.println("News site crawled: latimes.com");
        System.out.println("Number of threads: " + numberOfCrawlers);
        System.out.println();

        System.out.println("Fetch Statistics:");
        System.out.println("=================");
        System.out.println("# fetches attempted: " + fetched.size());
        System.out.println("# fetches succeeded: " + visited.size());
        System.out.println("# fetches failed or aborted: " + (fetched.size() - visited.size()));
        System.out.println();

        System.out.println("Outgoing URLs:");
        System.out.println("==============");
        System.out.println("Total URLs extracted: " + total_urls);
        System.out.println("# unique URLs extracted: " + (OKSet.size() + N_OKSet.size()));
        System.out.println("# unique URLs within News Site: " + OKSet.size());
        System.out.println("# unique URLs outside News Site: " + N_OKSet.size());
        System.out.println();

        System.out.println("Status Codes:");
        System.out.println("=============");
        for (int status: statusCount.keySet()) {
            System.out.println(status + ": " + statusCount.get(status));
        }
        System.out.println();

        System.out.println("File Sizes:");
        System.out.println("===========");
        System.out.println("< 1KB: " + size1);
        System.out.println("1KB ~ <10KB: " + size2);
        System.out.println("10KB ~ <100KB: " + size3);
        System.out.println("100KB ~ <1MB: " + size4);
        System.out.println(">= 1MB: " + size5);
        System.out.println();

        System.out.println("Content Types:");
        System.out.println("==============");
        for (String type: typesCount.keySet()) {
            System.out.println(type + ": " + typesCount.get(type));
        }
    }
}