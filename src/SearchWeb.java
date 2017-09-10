/**
 * Created by Shubhika on 11/6/2016.
 */
import org.jsoup.Jsoup;
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class Search
 */

public class SearchWeb extends HttpServlet
{
    Connection connection = null;
    String StopWordurl ="http://ir.dcs.gla.ac.uk/resources/linguistic_utils/stop_words";
    ArrayList<String> stopWordList = new ArrayList<>();
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
    //public Properties props;
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        stopwords();
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>" + "Search CS at Purdue" + "</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h3>Search CS at Purdue</h3>");
        out.println("Keywords:<br>");
        out.println("<P>");
        out.print("<form action=\"");
        out.print("search\" ");
        String keyword = request.getParameter("keyword");
        out.println("method=POST>");
        out.println("<input type=text size=48 name=keyword>");
        out.println("<br>");
        out.println("<input type=submit name=submit>");
        out.println("<br>");
        out.println("</form>");
        if (keyword != null) {
            try {
                //String url = "here!!!!";
                //out.println(keyword);
                openConnection(keyword, out);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }else{

        }
        out.println("</p>");
        out.println("</body>");
        out.println("</html>");
    }
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        doGet(request, response);
    }
    public void openConnection(String keyword,PrintWriter out) throws SQLException, IOException
    {
        try {
            if (connection == null) {
                Class.forName("com.mysql.jdbc.Driver");
                String url = "jdbc:mysql://localhost:3310/CRAWLER";
                String username = "root";
                String password = "17Sb2417!";
                connection = DriverManager.getConnection(url, username, password);
            }
            wordlist(keyword, out);
        } catch(Exception e) {
            out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    public void postURL(int urlID, PrintWriter out) {
        try {
            Statement stat = connection.createStatement();
            ResultSet result = stat.executeQuery("SELECT * FROM urls WHERE urlid = " + urlID + ";");
            if (result.next()) {
                String url = result.getString("url");
                //out.print("here!in post Url");
                out.println("<font size=\"4\"><a href=\"" + url + "\">" + result.getString("title") + "</a></font>");
                out.println("<br>");
                out.println("<font color=\"red\">" + url + "</font>");
                out.println("<br>");
                out.println("<font size=\"4\"><a href=\"" + url + "\">" + result.getString("description") + "</a></font>");
                out.println("<br>");
                if(result.getString("image") == null){
                    out.println("<a href="+url+">");
                    out.println("<img src="+"https://bigtennetworks.files.wordpress.com/2012/04/purold.jpeg"+ " style="+"width:100px;height:120px;"+">");
                    out.println("</a>");
                    //out.println("https://bigtennetworks.files.wordpress.com/2012/04/purold.jpeg");
                }else{
                    out.println("<a href="+url+">");
                    out.println("<img src="+result.getString("image")+" + style="+"width:100px;height:120px;"+">");
                    out.println("</a>");
                    //out.println(result.getString("image"));
                }
                out.println("<br>");
                out.println("<br>");
            }
        } catch(Exception e) {
            //out.println(e.getErrorCode());
            out.println(e.getMessage());
            //out.println("query");
            //out.println("Sorry!");
        }
    }

    public void wordlist(String keyword, PrintWriter out) {
        try {
            Hashtable<Integer, Integer> urlToWord = new Hashtable<>();
            //out.println("herererer!!!");
            String[] word_list = keyword.toLowerCase().split("\\s+");
            int wordCnt = 0;
            for (int i = 0; i < word_list.length; i++) {
                if (!word_list[i].equals("") && !stopWordList.contains(word_list[i])) {
                    //out.println(wordCnt);
                    //out.println(word_list[i]);
                    wordCnt++;
                    Statement stat = null;
                    try {
                        stat = connection.createStatement();
                    } catch (SQLException e) {
                        out.println(e.getMessage());
                    }
                    ResultSet result = null;

                    try {
                        result = stat.executeQuery("SELECT * FROM words WHERE word = '" + word_list[i] + "';");
                    } catch (SQLException e) {
                        out.println(e.getErrorCode());
                        out.println(e.getMessage());
                        out.println(e.getMessage());
                    }
                    try {
                        while (result.next()) {
                            int urlID = Integer.parseInt(result.getString("urlid"));
                            if (urlToWord.containsKey(urlID)) {
                                //out.println(urlID);
                                urlToWord.put(urlID, urlToWord.get(urlID) + 1);
                            } else {
                                urlToWord.put(urlID, 1);
                            }
                        }
                    } catch (SQLException e) {
                        out.print(e.getMessage());
                    }
                }
            }
            for (Integer urlID : urlToWord.keySet()) {
                if (urlToWord.containsKey(urlID)) {
                    if (urlToWord.get(urlID) == wordCnt) {
                        //out.println(urlID);
                        //out.println(urlToWord.get(urlID));
                        postURL(urlID, out);
                    }
                }
            }
        }catch (Exception e){
            out.println(e.getMessage());
            out.println("wordlist");

        }
    }
}