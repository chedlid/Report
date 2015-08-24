
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author cdhouibi
 */
public class RestApiFetcher {
    
    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException{
        //A working example
        String job_url= "https://jenkins.vistaprint.net/view/+Default/view/Fulfill%20Demand/view/FDQE/job/FD.Quality.Performance";
        String username= "sltReportingUser";
        String password= "9o@PFYkvE2l3";
        Map<String,String> results = get_stash_information_through_job_url(job_url, username, password);
        System.out.println("-------------------------------");
        System.out.println("- Stash Project key: " + results.get("project_key"));
        System.out.println("- Stash Repository name: " + results.get("project_name"));
        System.out.println("- Stash Project name: " + results.get("squad_name"));
    }

    //this method returns a dictionnary (map) with three properties :
    //project_key: The Stash Project Key
    //project_name: The Stash Repository 
    //squad_name:  The Stash project
    //PS: You have to provide the job url, the username of password of an account that accesses both Jenkins and Stash
    public static Map<String, String> get_stash_information_through_job_url(String upstreamJobUrl, String username, String password) throws IOException, ParserConfigurationException, SAXException {

        //REST API only accepts SSL requests
        if (!upstreamJobUrl.contains("http")) {
            upstreamJobUrl += "https://" + upstreamJobUrl;
        }
        //start authentication for Jenkins to access config.xml file
        DefaultHttpClient client = new DefaultHttpClient();
        client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), new UsernamePasswordCredentials(username, password));
        BasicScheme basicAuth = new BasicScheme();
        BasicHttpContext context = new BasicHttpContext();
        context.setAttribute("preemptive-auth", basicAuth);
        client.addRequestInterceptor(new PreemptiveAuth(), 0);
        String getUrl = upstreamJobUrl + "/config.xml";
        HttpGet get = new HttpGet(getUrl);
        HttpResponse response = client.execute(get, context);
        HttpEntity entity = response.getEntity();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document testDoc = db.parse(entity.getContent());
        Element testElt = (Element) testDoc.getDocumentElement();
        String stash_repo = "NULL";
        System.out.println("Exploring Job configuration...");
        //Searching for Stash repository url in config.xml ( <hudson.plugins.git.UserRemoteConfig> )
        if (testElt.getElementsByTagName("hudson.plugins.git.UserRemoteConfig").getLength() > 0) {
            Node UserRemoteConfig = testElt.getElementsByTagName("hudson.plugins.git.UserRemoteConfig").item(0);
            for (int i = 0; i < UserRemoteConfig.getChildNodes().getLength(); i++) {
                if (UserRemoteConfig.getChildNodes().item(i).getNodeName().equals("url")) {
                    stash_repo = UserRemoteConfig.getChildNodes().item(i).getTextContent();
                    System.out.println("Repository URL: " + stash_repo);
                }
            }
            if (stash_repo.equals("NULL")) {
                System.out.println("Warning: URL is not specified in the User Remote Configuration.");
            }
        } else {
            System.out.println("Warning: Repository not specified in job configuration.");
        }

        String project_key = "NO_PROJECT_KEY_ASSOCIATED";
        String project_name = "NO_PROJECT_ASSOCIATED";
        String squad_name = "NO_SQUAD_ASSOCIATED";
        if (!stash_repo.equals("NULL")) {
            //At this point, we have successfully retrieved the stash repository url. We need to find the key, repository name and project name.
            //removing possible url security extensions
            if (stash_repo.contains("http") || stash_repo.contains("ssh:")) {
                stash_repo = stash_repo.substring(stash_repo.indexOf("//") + 2);
            }
            //Getting Stash base url
            String stash_base_url = stash_repo.substring(0, stash_repo.indexOf("/"));
            //Removing port indication in Stash url
            if (stash_base_url.contains(":")) {
                stash_base_url = stash_base_url.substring(0, stash_base_url.indexOf(":"));
            }
            //Stash only accepts SSL requests
            stash_base_url = "https://" + stash_base_url;
            System.out.println("Base repo: " + stash_base_url);
            //Stash REST API url extension
            String stash_rest_api = "/rest/api/1.0/projects";
            String project_potential_key = stash_repo.split("/")[stash_repo.split("/").length - 2];
            String project_name_git_ext = stash_repo.split("/")[stash_repo.split("/").length - 1];
            String project_potential_name = project_name_git_ext.split(".git")[0];
            String final_stash_repo = stash_base_url + stash_rest_api + "/" + project_potential_key + "/repos/" + project_potential_name;
            //Authenticating to Stash and calling REST API
            HttpGet get_stash = new HttpGet(final_stash_repo);
            HttpResponse response_stash = client.execute(get_stash, context);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response_stash.getEntity().getContent(), "UTF-8"));
            String json = reader.readLine();
            if (json.contains("errors")) {
                //REST API return error message
                System.out.println("Authentification failed!");
            } else {
                json = json.substring(json.indexOf("\"name\""));
                //Getting Stash repository name
                project_name = json.substring(json.indexOf(":\"") + 2, json.indexOf(",") - 1);
                json = json.substring(json.indexOf("\"project\""));
                json = json.substring(json.indexOf("\"key\""));
                //Getting Stash project key
                project_key = json.substring(json.indexOf(":\"") + 2, json.indexOf(",") - 1);
                json = json.substring(json.indexOf("\"name\""));
                //Getting Stash project name
                squad_name = json.substring(json.indexOf(":\"") + 2, json.indexOf(",") - 1);
                System.out.println("Done figuring out project's name and key.");
            }
        } else {
            System.out.println("Project Stash repository not found in Job configuration. Skipping project saving.");
        }

        HashMap<String, String> key_name = new HashMap<>();
        key_name.put("project_key", project_key);
        key_name.put("project_name", project_name);
        key_name.put("squad_name", squad_name);
        return key_name;
    }

    static class PreemptiveAuth implements HttpRequestInterceptor {

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
