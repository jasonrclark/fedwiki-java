import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.regex.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
  // https://stackoverflow.com/questions/5762491/how-to-print-color-in-console-using-system-out-println
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

  // commands
    static Scanner scanner = new Scanner(System.in);
    static int lineno = 0;
  // pages
    static String slug = "welcome-visitors";
    static String origin = "ward.dojo.fed.wiki";
    static Page page;
    static int itemno = 0;

  public static void main(String... args) throws URISyntaxException, IOException, InterruptedException {
    if (args.length > 0) origin = args[1];
    List<String> context = new ArrayList<String>();
    page = fetch(context,slug);
    Item shown = page.story.get(itemno);
    while (true) {
      Item item = page.story.get(itemno);
      if(item != shown) {item.println(); shown = item;}
      var cmd = scanner.nextLine();
      lineno++;
      if (cmd.length() != 0) System.out.println(" <<" + String.valueOf(lineno) + " " + cmd + ">>");
      if (cmd.startsWith("e")) System.exit(0);
      if (cmd.startsWith("l")) {page = fetch(context,item.links().get(0)); context = page.context(); itemno = 0;}
      if (cmd.startsWith("t")) test(cmd,item);
      if (cmd.startsWith("f")) find(cmd);
      if (cmd.startsWith("n")) next();
    }
  }

  static int next () {
    itemno = (itemno+1) % page.story.size();
    return itemno;
  }

  static void trouble(String msg) {
    System.out.println(ANSI_RED + " <<" + msg + ">>" + ANSI_RESET);
    System.exit(1);
  }

  static void test (String cmd, Item item) {
    // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/regex/Pattern.html
    Pattern want = Pattern.compile(cmd.split(" ")[1]);
    Matcher have = want.matcher(item.text);
    boolean pass = have.find();
    if (!pass) trouble("no match");
  }

  static void find (String cmd) {
    Pattern want = Pattern.compile(cmd.split(" ")[1]);
    int last = itemno;
    while(next() != last) {
      Matcher have = want.matcher(page.story.get(itemno).text);
      if(have.find()) return;
    }
    trouble("not found");
  }

  static Page fetch(List<String> context, String slug) throws URISyntaxException, IOException, InterruptedException  {
    String url = String.format("http://%s/%s.json", origin, slug);
    while(true) {
      HttpRequest request = HttpRequest.newBuilder()
              .uri(new URI(url))
              .header("User-Agent", "fedwiki-java")
              .version(HttpClient.Version.HTTP_1_1)
              .GET()
              .build();
      HttpResponse<String> response = HttpClient
              .newBuilder()
              .build()
              .send(request, HttpResponse.BodyHandlers.ofString());
      var code = response.statusCode();
      if (code == 200) {
        var mapper = new ObjectMapper();
        Page result = mapper.readValue(response.body(), Page.class);
        itemno = 0;
        System.out.println("");
        System.out.println(result.title);
        System.out.println("==========================================");
        return result;
      }
      if(!context.isEmpty()) {
        url = String.format("http://%s/%s.json", context.get(0), slug);
        System.out.println(ANSI_YELLOW + url + ANSI_RESET );
        context.remove(0);
      } else
        trouble("can't find page in current context");
    }
  }


  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Page {
    public String title = "Empty";
    public List<Item> story = List.of();
    public List<Action> journal = List.of();

    public List<String> context() {
      List<String> sites = new ArrayList<String>();
      for (int i=journal.size()-1; i>=0; i--)
        if (journal.get(i).site != null && !sites.contains(journal.get(i).site))
          sites.add(journal.get(i).site);
      return sites;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Item {
    public String type;
    public String id;
    public String title;
    public String text = "";

    private static final Pattern linkPattern = Pattern.compile("\\[\\[(.*?)]]");

    public List<String> links() {
      var matcher = linkPattern.matcher(text);
      if (matcher.find()) {
        var slug = matcher.group(1)
          .replaceAll("\\s", "-")
          .replaceAll("[^A-Za-z0-9-]", "")
          .toLowerCase(Locale.getDefault());
        return List.of(slug);
      }
      return List.of();
    }

    public void println() {
      System.out.println(text);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Action {
    public String type;
    public String id;
    public Long date;
    public Item item;
    public String site;
  }
}