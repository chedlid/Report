/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javaapplication4;

import Business.Test;
import XmlFileParsing.XmlFileParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 * @author cdhouibi
 */
public class JavaApplication4 {

    /**
     * @param args the command line arguments
     * @throws java.sql.SQLException
     * @throws java.lang.ClassNotFoundException
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws org.xml.sax.SAXException
     */
    public static void main(String[] args) throws SQLException, SAXException, ParserConfigurationException, ClassNotFoundException {

        List<String> job_urls = new ArrayList<>();
        job_urls.add("https://jenkins.vistaprint.net/view/+Default/view/Fulfill%20Demand/view/FDQE/job/FD.Quality.Performance");
        job_urls.add("https://jenkins.vistaprint.net/view/+Default/view/Fulfill%20Demand/view/FDQE/job/FD.Quality.NonUITests");
        job_urls.add("https://jenkins.vistaprint.net/view/+Default/view/Fulfill%20Demand/view/FDQE/job/FD.Quality.UITests/");
        job_urls.add("https://jenkins.vistaprint.net/view/+Default/view/Fulfill%20Demand/view/FDQE/job/FD.Quality.UITestsLongRunning");
        job_urls.add("https://jenkins.vistaprint.net/job/FD.plant.integration.tests");
        job_urls.add("https://jenkins.vistaprint.net/view/+Default/view/Fulfill%20Demand/view/FDLQP/job/FD.DeliveryOptions.run_servicetests_and_promote");
        job_urls.add("https://jenkins.vistaprint.net/view/+Default/view/Fulfill%20Demand/view/OrderManager/job/FD.OrderManager.IntegrationTests");
        job_urls.add("https://jenkins.vistaprint.net/job/MSW.AssetRetrieval.ServiceLevelTest");

     //   job_urls.add("https://vbujenkins.vistaprint.net/job/QualityPlatform.AllInOne.Master.Commit");
        //   job_urls.add("https://jenkins.vistaprint.net/job/MSW.ProductAvailability.Acceptance"); 
        //job_urls.add("https://jenkins.vistaprint.net/job/VP.ShippingSystem.acceptance");
        TestPersistence.TestPersistenceManager.spreadConnectionString("jdbc:sqlserver://dbservices.vptest.com;databaseName=NunitTestResultDB;user=qawriter;password=updateqa");
        Connection con = DataBaseConnection.DataBaseConnectionFactory.getInstance().getConnection();
        Statement stm = con.createStatement();

        for (String job_url : job_urls) {

            try {
                String apiXml = "/api/xml";
                String[] url_parts = job_url.split("/");
                String job_name = url_parts[url_parts.length - 1];
                System.out.println("/////////////////////////////////////////////////////////////////////////////////////Job: " + job_name);
                String query = "select ID from Job where job_name='" + job_name + "' and job_url='" + job_url + "'";
                ResultSet rs = stm.executeQuery(query);
                int job_id = 0;
                if (rs.next()) {
                    job_id = rs.getInt("ID");
                } else {
                    //go fetch job's project, team, and insert all in the database, then return job_id
                }

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document jobDoc = db.parse(new URL(job_url + apiXml).openStream());
                Element jobElt = (Element) jobDoc.getDocumentElement();
                Set<String> set = new HashSet<>();
                List<String> list = new ArrayList<>();
                for (int i = 0; i < jobElt.getElementsByTagName("number").getLength(); i++) {
                    String buildNumber = jobElt.getElementsByTagName("number").item(i).getTextContent();
                    list.add(buildNumber);
                }

                set.addAll(list);
                list.clear();
                list.addAll(set);
                //we have a list of all build numbers available
                System.out.println("Available builds: " + list.size());

                query = "select BUILD_NUMBER from Build where ID_job='" + job_id + "'";
                rs = stm.executeQuery(query);
                List<String> list_existing_nbuilds = new ArrayList<>();
                while (rs.next()) {
                    String number = rs.getString("BUILD_NUMBER");
                    list_existing_nbuilds.add(number);
                }
                //we have a list of existing build numbers
                list.removeAll(list_existing_nbuilds);
                //we have a list of new not saved builds
                System.out.println("New unsaved builds: " + list.size());
                for (int i = 0; i < list.size(); i++) {
                    try {
                        System.out.print("Build " + list.get(i) + "  =>  ");
                        String buildUrl = job_url + "/" + list.get(i);
                        Document buildDoc;

                        buildDoc = db.parse(new URL(buildUrl + apiXml).openStream());

                        Element buildElt = (Element) buildDoc.getDocumentElement();
                        String buildId = buildElt.getElementsByTagName("id").item(0).getTextContent();
                        String[] parts = buildId.split("_");
                        String date = parts[0];
                        String time = parts[1].replace("-", ":");
                        String buildNumber = list.get(i);

                        int artifact = buildElt.getElementsByTagName("artifact").getLength();
                        int testID = -1;
                        if (artifact > 0) {
                            String filePath = buildElt.getElementsByTagName("relativePath").item(0).getTextContent();
                            String fileUrl = buildUrl + "/artifact/" + filePath;
//////////////////////////////////////////////////////////////TO TEST ///////////////////////////////////////////////////////////////////////////////////////////////:

                            InputStream resultInput = new URL(fileUrl).openStream();

                            if (resultInput.available() > 0) {
                                Document fileDoc = db.parse(resultInput);
                                Element fileElt = (Element) fileDoc.getDocumentElement();
                                if (!fileElt.getNodeName().equals("result")) {
                                    System.out.print("Proper result file found  =>  ");
                                    String testName = fileElt.getAttribute("name");
                                    String test_date = fileElt.getAttribute("date");
                                    String test_time = fileElt.getAttribute("time");
                                    //save test and get ID
                                    XmlFileParsing.XmlFileParser parser = new XmlFileParser(fileDoc);
                                    Test test = parser.getGeneratedTest();
                                    TestPersistence.TestPersistenceManager.saveTest(test);
                                    query = "select ID from GlobalTest where name='" + testName + "' and date='" + test_date + "' and time='" + test_time + "'";
                                    rs = stm.executeQuery(query);
                                    rs.next();
                                    testID = rs.getInt("ID");
                                    System.out.print("Test saved (ID:" + testID + ")  =>  ");

                                } else {
                                    System.out.print("Deployment didn't complete correctly  =>  ");
                                }
                            } else {
                                System.out.println(buildNumber + ": Empty result file =>");
                            }
                        } else {
                            System.out.print(buildNumber + ": No artifact found  =>  ");
                        }

                        if (testID != -1) {
                            query = "insert into Build(BUILD_NUMBER,BUILD_ID,BUILD_URL,ID_global_test,ID_job,date,time) values('" + buildNumber + "','" + buildId + "','" + buildUrl + "','" + testID + "','" + job_id + "','" + date + "','" + time + "')";
                        } else {
                            query = "insert into Build(BUILD_NUMBER,BUILD_ID,BUILD_URL,ID_job,date,time) values('" + buildNumber + "','" + buildId + "','" + buildUrl + "','" + job_id + "','" + date + "','" + time + "')";
                        }
                        stm.execute(query);
                        System.out.println("");
                        System.out.println("Build " + i + " is done.");
                    } catch (SAXException ex) {
                        System.out.println("");
                        System.out.println("Error: SAXException");
                    } catch (IOException ex) {
                        System.out.println("");
                        System.out.println("Error: IOException");
                    } catch (SQLException ex) {
                        System.out.println("");
                        System.out.println("Error: SQLException");
                    } catch (ClassNotFoundException ex) {
                        System.out.println("");
                        System.out.println("Error: ClassNotFoundException");
                    } catch (ParserConfigurationException ex) {
                        System.out.println("");
                        System.out.println("Error: ParserException");
                    } catch (XPathExpressionException ex) {
                        System.out.println("");
                        System.out.println("Error: XPathException");
                    }

                }
                System.out.println(job_name + " is Done.");
            } catch (IOException ex) {
                System.out.println("Error: IOException");;
            }

        }

    }

    static class PreemptiveAuth implements HttpRequestInterceptor {

        /*
         * (non-Javadoc)
         *
         * @see org.apache.http.HttpRequestInterceptor#process(org.apache.http.HttpRequest,
         * org.apache.http.protocol.HttpContext)
         */
        @Override
        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
            // Get the AuthState
            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

            // If no auth scheme available yet, try to initialize it preemptively
            if (authState.getAuthScheme() == null) {
                AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
                CredentialsProvider credsProvider = (CredentialsProvider) context
                        .getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (authScheme != null) {
                    Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost
                            .getPort()));
                    if (creds == null) {
                        throw new HttpException("No credentials for preemptive authentication");
                    }
                    authState.setAuthScheme(authScheme);
                    authState.setCredentials(creds);
                }
            }

        }

    }

}
