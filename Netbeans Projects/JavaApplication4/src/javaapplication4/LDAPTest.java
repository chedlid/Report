package javaapplication4;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

/**
 * Example code for retrieving a Users Primary Group from Microsoft Active
 * Directory via. its LDAP API
 *
 * @author Adam Retter <adam.retter@googlemail.com>
 */
public class LDAPTest {

    static DirContext ldapContext;

    /**
     * @param args the command line arguments
     * @throws javax.naming.NamingException
     */
    public static void main(String[] args) throws NamingException {

        String username = "ooueslati";
        String password = "Happiness11";
        String requested_group = "SLT Admins";
        try {
            System.out.println("LDAP: Starting LDAP verification...");

            Hashtable<String, String> ldapEnv = new Hashtable<String, String>(11);
            ldapEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            ldapEnv.put(Context.PROVIDER_URL, "ldap://lexusdc03.vistaprint.net:389");
            ldapEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
            ldapEnv.put(Context.SECURITY_PRINCIPAL, "CN=Internaltools,OU=_Service Accounts,OU=Office,OU=Corporate,DC=vistaprint,DC=net");
            ldapEnv.put(Context.SECURITY_CREDENTIALS, "internaltools");
            ldapContext = new InitialDirContext(ldapEnv);
            
            SearchControls searchCtls = new SearchControls();
            String returnedAtts[] = {"cn","memberof", "distinguishedname", "samAccountName"};
            searchCtls.setReturningAttributes(returnedAtts);
            searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            String searchFilter = "(&(objectClass=user)(samAccountName=" + username + "))";

            String searchBase = "dc=vistaprint,dc=net";

            NamingEnumeration<SearchResult> answer = ldapContext.search(searchBase, searchFilter, searchCtls);

            //Loop through the search results
            if (answer.hasMoreElements()) {
                System.out.println("LDAP: User found. Verifying group... ");
                SearchResult sr = (SearchResult) answer.next();
                Attributes attrs = sr.getAttributes();
                String user_DN = attrs.get("distinguishedname").toString().split(":")[1];
                String user_CN = attrs.get("cn").toString().split(":")[1];
                List<String> user_groups = new ArrayList<>();
                
                String user_group_chain = attrs.get("memberof").toString();
                String[] groups = user_group_chain.split(", ");
                for (String group : groups) {
                    String group_CN = (group.split(",")[0]).split("=")[1];
                    user_groups.add(group_CN);
                }
                
                if(user_groups.contains(requested_group)){
                    System.out.println("LDAP: User belongs to the correct group. Verifying password...");
                } else {
                    System.out.println("LDAP: User doesn't belong to the correct group.");
                    return;
                }
                
                ldapEnv.put(Context.SECURITY_PRINCIPAL, user_DN);
                ldapEnv.put(Context.SECURITY_CREDENTIALS, password);

                ldapContext = new InitialDirContext(ldapEnv);
                System.out.println("LDAP: Password verified.");
            } else {
                System.out.println("LDAP: User not found.");
                ldapContext.close();
                return;
            }
            ldapContext.close();
        } catch (Exception e) {
            System.out.println("LDAP: User password is incorrect.");
            return ;
        }
        System.out.println("LDAP: Log in approved.");
    }

}
