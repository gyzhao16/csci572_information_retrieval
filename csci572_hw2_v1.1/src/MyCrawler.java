import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicInteger;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.url.WebURL;

public class MyCrawler extends WebCrawler{

    private List<CrawlItem> fetched;
    private List<CrawlItem> discovered;
    private List<CrawlItem> visited;
    private AtomicInteger fetches_failed;
    private List<String> allowTypes = new ArrayList<>();

    public MyCrawler(List<CrawlItem> _fetched, List<CrawlItem> _discovered, List<CrawlItem> _visited,
                     AtomicInteger _fetches_failed){
        allowTypes.add("text/html");
        allowTypes.add("image/");
        allowTypes.add("application/pdf");
        this.fetched = _fetched;
        this.discovered = _discovered;
        this.visited = _visited;
        this.fetches_failed = _fetches_failed;
    }

    public String getContentType(Page referringPage) {
        String type = referringPage.getContentType();
        if (type.contains(";")) {
            type = type.substring(0, type.indexOf(';'));
        }
        return type;
    }

    public boolean startWithDomain(WebURL url) {
        String domain = url.getDomain();
        String subdomain = url.getSubDomain();
        if (subdomain == null) {
            subdomain = "www";
        }
        String address = String.format("%s.%s", subdomain, domain);
        return address.equals("www.latimes.com");
    }

    private static final Pattern FILTERS = Pattern.compile(
            ".*(\\.(css|js" + "|mid|mp2|mp3|mp4" +
                    "|wav|avi|mov|mpeg|ram|m4v" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");

    @Override
    protected void handlePageStatusCode(WebURL url, int statusCode, String statusDescription) {
        CrawlItem temp = new CrawlItem(url.getURL(), statusCode);
        if (statusCode > 299) {
            fetches_failed.incrementAndGet();
        }
        fetched.add(temp);
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();

        boolean flag = startWithDomain(url);
        CrawlItem temp = new CrawlItem(href, flag);
        discovered.add(temp);

        if (!flag) return false;

        if (FILTERS.matcher(href).matches()) {
            return false;
        }

        if (referringPage.getContentType() == null) {
            return false;
        } else {
            boolean res = false;
            String type = getContentType(referringPage);
            for (String t : allowTypes) {
                res = res || type.contains(t);
            }
            return res;
        }
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        boolean res = false;
        String type = getContentType(page);
        for (String t : allowTypes) {
            res = res || type.contains(t);
        }

        if (!res) return;

        int size = page.getContentData().length;
        int outNum = page.getParseData().getOutgoingUrls().size();
        CrawlItem temp = new CrawlItem(url, size, outNum, type);
        visited.add(temp);
    }
}

class CrawlItem {

    private String url;
    private boolean outside;
    private int size;
    private int outnum;
    private int status;
    private String type;

    public String getUrl() {return url;}
    public boolean getOutside() {return outside;}
    public int getSize() {return size;}
    public int getOutnum() {return outnum;}
    public int getStatus() {return status;}
    public String getType() {return type;}

    public CrawlItem (String _url, boolean _outside) {
        this.url = _url;
        this.outside = _outside;
    }

    public CrawlItem (String _url, int _size, int _outnum, String _type) {
        this.url  = _url;
        this.size = _size;
        this.type = _type;
        this.outnum = _outnum;
    }

    public CrawlItem (String _url, int _status) {
        this.url = _url;
        this.status = _status;
    }

    public String[] fetchedToArray() {
        String[] res = new String[2];
        res[0] = url;
        res[1] = String.valueOf(status);
        return res;
    }

    public String[] discoveredToArray() {
        String[] res = new String[2];
        res[0] = url;
        if (outside) {
            res[1] = "OK";
        } else {
            res[1] = "N_OK";
        }
        return res;
    }

    public String[] visitedToArray() {
        String[] res = new String[4];
        res[0] = url;
        res[1] = String.valueOf(size);
        res[2] = String.valueOf(outnum);
        res[3] = type;
        return res;
    }
}