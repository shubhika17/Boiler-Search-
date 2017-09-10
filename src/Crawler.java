import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.sql.Connection;
import java.util.*;
import org.jsoup.*;
import org.jsoup.nodes.Element;



public class Crawler
{
	//int MAX_URLS = 0;
	int count = 0;
	Connection connection;
	int urlID;
	public Properties props;
	Queue<String> urlsToVisit = new LinkedList<>();
	Queue<String> urlsVisited = new LinkedList<>();
	String StopWordurl ="http://ir.dcs.gla.ac.uk/resources/linguistic_utils/stop_words";
	ArrayList<String> stopWordList = new ArrayList<>();
	Crawler() {
		urlID = 2887;
	}
	public void stopwords(){
		org.jsoup.Connection connection = Jsoup.connect(StopWordurl);
		String text = null;
		try {
			org.jsoup.nodes.Document document = connection.get();
			text = document.body().text();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] words = text.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
		for(int i = 0; i < words.length; i++){
			stopWordList.add(words[i]);
		}
	}

	public void readProperties() throws IOException {
      		props = new Properties();
      		FileInputStream in = new FileInputStream("database.properties");
      		props.load(in);
      		in.close();
	}

	public void openConnection() throws SQLException, IOException
	{
		String drivers = props.getProperty("jdbc.drivers");
		if (drivers != null) System.setProperty("jdbc.drivers", drivers);
      	String url = props.getProperty("jdbc.url");
      	String username = props.getProperty("jdbc.username");
      	String password = props.getProperty("jdbc.password");
		connection = DriverManager.getConnection( url, username, password);
   	}

	public void createDB() throws SQLException {
		try {
			openConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//Statement stat = connection.createStatement();
		try {
			//stat.executeUpdate("DROP TABLE URLS");
			//stat.executeUpdate("DROP TABLE WORDS");
		}
		catch (Exception e) {
		}
       	//stat.executeUpdate("CREATE TABLE URLS (urlid INT, url VARCHAR(512), description VARCHAR(200), Title VARCHAR(50), Image VARCHAR(512))");
		//stat.executeUpdate("CREATE TABLE WORDS (word VARCHAR(500), urlid INT)");
	}
	public void tokenize(org.jsoup.nodes.Document document) throws SQLException {
		Statement stat = connection.createStatement();
		String text;
		try {
			text = document.body().text();
		}catch (Exception e){
			return;
		}
		String[] words = text.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
		ArrayList<String> wordsVisited = new ArrayList<>();
		for(int i = 0; i < words.length; i++){
			if(!stopWordList.contains(words[i]) && !wordsVisited.contains(words[i])){
				//System.out.println(words[i]);
				if(words[i].length() > 500){
					continue;
				}
				String query = "INSERT INTO words VALUES ('"+words[i]+"','"+urlID+"')";
				stat.executeUpdate( query );
				wordsVisited.add(words[i]);
			}
		}
	}

	public boolean urlInDB(String urlFound) throws SQLException, IOException {
       	Statement stat = connection.createStatement();
		try {
			ResultSet result = stat.executeQuery("SELECT * FROM urls WHERE url LIKE '" + urlFound + "'");
			if (result.next()) {
				//System.out.println("URL "+urlFound+" already in DB");
				return true;
			}
		}catch (Exception e){
			System.out.println("ERRRRRRORRR INDB");
			return true;

		}
	    return false;
	}

	public void insertURLInDB( String url) throws SQLException, IOException {
        Statement stat = connection.createStatement();
		//System.out.println(url);
		if(urlInDB(url)){
			return;
		}

		org.jsoup.nodes.Document doc;
		try {
			doc = Jsoup.connect(url).userAgent("Mozilla").get();
		}catch (Exception e){
			return;
		}

		org.jsoup.select.Elements allTags = null;
		allTags = doc.select("h1, h2, h3");
		//System.out.println("here!");

		String str = ""; //stores the description

	//now check what tags you do have to get the description
		if(allTags == null || allTags.isEmpty()){
			str = doc.text().replaceAll("[^a-zA-Z ]", ""); //just get the anchor text
			if(str.length() > 200)
				str = str.substring(0, 199);
		} else {
			int k = 0;
			while (k < allTags.size()) {
				//System.out.println("sorry");
				if (allTags.get(k) != null) {
					if (allTags.get(k).text().replaceAll("[^a-zA-Z ]", "").length() >= 200) {
						str = allTags.get(k).text().replaceAll("[^a-zA-Z ]", "").substring(0, 199);
					} else {
						str = allTags.get(k).text().replaceAll("[^a-zA-Z ]", "");
					}
				}
				k++;
			}
		}
		String heading = doc.title().replaceAll("[^a-zA-Z0-9 ]", "");
		if(heading.length() > 50){
			heading = heading.substring(0, 49);
		}
		//doc = Jsoup.connect(url).userAgent("Mozilla").get();
		org.jsoup.select.Elements media = doc.getElementsByTag("img");
		String img = null;
		for (Element src : media) {
				img = src.absUrl("src");
			if(img.length() <= 512){
				break;
			}
		}
		//System.out.println(img);
		String query = "INSERT INTO urls VALUES ('"+urlID+"','"+url+"',' "+str+"', '"+heading+"', '"+img+"')";
		try{
			stat.executeUpdate( query );
		}catch (Exception e){
			System.out.println(e.getMessage());
			return;
		}
		tokenize(doc);
		urlID++;
	}
	public int crawl(String url) throws SQLException, IOException {
		if(url.length() < 5 || 'h'!= url.charAt(0) || 't'!= url.charAt(1) || 't'!= url.charAt(2) || 'p'!= url.charAt(3) ) {

		}
		else {
			if('s'== url.charAt(4)){
				String curr = url.substring(0, 3);
				String curr2 = url.substring(5);
				url.equals(curr.concat(curr2));
			}
			org.jsoup.select.Elements links;
			//System.out.println(url);
			org.jsoup.Connection connection;
			org.jsoup.nodes.Document document;
			try{
				connection = Jsoup.connect(url);
				 //links = document.select("a[href]");
			}catch (Exception e){
				System.out.println("Errrrroorrrr!!!");
				return 0;
			}
			try{
				document = connection.userAgent("Mozilla").get();
			}catch (Exception e){
				System.out.println("Errrrroorrrr!!!");
				return 0;
			}
			links = document.select("a[href]");
			for (org.jsoup.nodes.Element link : links) {
				if(urlsToVisit.size() > 2000){
					break;
				}
				if (!urlsToVisit.contains(link.absUrl("href")) && !urlsVisited.contains(link.absUrl("href"))) {
					if((link.absUrl("href")).length() < 5 || 'h'!= link.absUrl("href").charAt(0) || 't'!= (link.absUrl("href")).charAt(1) || 't'!= (link.absUrl("href")).charAt(2) || 'p'!= (link.absUrl("href")).charAt(3) ) {

					}else if(link.absUrl("href").equals("https://www.linkedin.com/edu/purdue-university-18357")){

					}else if(link.absUrl("href").length() > 512){

					}else {
						try {
							URI uri = new URI(link.absUrl("href"));
							String domain = uri.getHost();
							//System.out.println(domain);
							if(domain == null){
								continue;
							}
							if(domain.equals("www.cs.purdue.edu")){
								System.out.println(domain);
								urlsToVisit.add(link.absUrl("href"));
								//System.out.println();
							}else {
								continue;
							}
						} catch (URISyntaxException e) {
							System.out.println("error!");
							continue;
						}

					}
				}
			}
			return 1;
		}
		return 0;
	}

	public void iterator(String url) throws SQLException, IOException {
		String curr;
		crawl(url);
		this.insertURLInDB(url);
		urlsVisited.add(url);
		count++;
		while(!urlsToVisit.isEmpty()){
			System.out.println(urlID);
			//System.out.println(urlsToVisit.size());
			if(urlID >= 10100){
				break;
			}
			curr = urlsToVisit.remove();
			urlsVisited.add(curr);
			System.out.println(curr);
			int num = crawl(curr);
			if(num == 1 && !urlInDB(curr)){
				this.insertURLInDB(curr);
				count++;
			}
		}
	}

   	public static void main(String[] args)
   	{
		Crawler crawler = new Crawler();
		try {
			crawler.readProperties();
			String root = "http://www.cs.purdue.edu";
			crawler.createDB();
			crawler.stopwords();
			crawler.iterator(root);
		}
		catch( Exception e) {
         		e.printStackTrace();
		}
   	}
}

