package com.test.fn;

import com.google.common.base.Supplier;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.functions.FunctionsClient;
import com.oracle.bmc.functions.model.FunctionSummary;
import com.oracle.bmc.functions.requests.InvokeFunctionRequest;
import com.oracle.bmc.functions.requests.ListApplicationsRequest;
import com.oracle.bmc.functions.requests.ListFunctionsRequest;
import com.oracle.bmc.functions.responses.InvokeFunctionResponse;
import com.oracle.bmc.functions.responses.ListApplicationsResponse;
import com.oracle.bmc.functions.responses.ListFunctionsResponse;
import com.oracle.bmc.http.DefaultConfigurator;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.requests.ListCompartmentsRequest;
import com.oracle.bmc.identity.responses.ListCompartmentsResponse;
import com.oracle.bmc.util.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.ClientBuilder;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.SslConfigurator;

public class FnInvokeExample {

    /**
     * Oracle Functions service endpoint (us-phoenix-1 corresponds to phoenix
     * region where the service is available)
     */
    public static final String FAAS_ENDPOINT = "https://functions.us-phoenix-1.oraclecloud.com/";
    private String tenantOCID = null;
    private final FunctionsClient fnClient;
    private final IdentityClient identityClient;


    /**
     * sets up authentication provider
     *
     * @param tenantId
     * @param userId
     * @param fingerprint
     * @param privateKeyFile
     * @param passphrase
     */
    public FnInvokeExample(String tenantId, String userId, String fingerprint, String privateKeyFile, String passphrase) {
        this.tenantOCID = tenantId;
        Supplier<InputStream> privateKeySupplier
                = () -> {
                    try {
                        return new FileInputStream(new File(privateKeyFile));
                    } catch (FileNotFoundException e) {
                        throw new IllegalArgumentException(e);
                    }
                };

        SimpleAuthenticationDetailsProvider authDetails = SimpleAuthenticationDetailsProvider.builder()
                .tenantId(tenantId)
                .userId(userId)
                .fingerprint(fingerprint)
                .privateKeySupplier(privateKeySupplier)
                .passPhrase(passphrase)
                .region(Region.US_PHOENIX_1)
                .build();

        this.fnClient = new FunctionsClient(authDetails, null, new TrustAllConfigurator());
        this.identityClient = new IdentityClient(authDetails);
    }

    /**
     * Invokes a function
     *
     * @param functionName
     * @param appName
     * @param compartmentName
     * @param payload
     * @throws Exception
     */
    public void invokeFunction(String functionName, String appName, String compartmentName, String payload) throws Exception {

        try {
            //get the App OCID first
            String appOCID = getAppOCID(appName, compartmentName, tenantOCID);

            //find the function details
            FunctionSummary function = getFunction(appOCID, functionName);
            String functionId = function.getId();
            String invokeEndpoint = function.getInvokeEndpoint();

            System.out.println("Invoking function endpoint - " + invokeEndpoint + " with payload " + payload);

            //the client needs to use the function invoke endpoint
            fnClient.setEndpoint(invokeEndpoint);

            /**
             * This is an example of sending a JSON payload as an input to the
             * function - valid for boilerplate functions for python, node, go,
             * ruby functions. The expected result is - {"message":"Hello
             * foobar"}
             *
             * For a Java boilerplate function, you can simply pass a string
             * (not JSON) e.g. foobar as the input and expect Hello foobar! as
             * the response
             *
             */
            InvokeFunctionRequest ifr = InvokeFunctionRequest.builder().functionId(functionId)
                    .invokeFunctionBody(StreamUtils.createByteArrayInputStream(payload.getBytes()))
                    .build();

            //actual function invocation
            InvokeFunctionResponse resp = fnClient.invokeFunction(ifr);

            //parse the response
            /**
             * the example below simply reads a String response.
             */
            String respString = IOUtils.toString(resp.getInputStream(), StandardCharsets.UTF_8);
            System.out.print("Response from function - " + respString + "\n");
        } catch (Exception e) {
            throw e;
        } finally {
            fnClient.close();
        }

    }

    /**
     * Returns application OCID for an application
     *
     * @param appName Name of the application whose OCID is required
     * @param compartmentName Name of the compartment where the Oracle Functions
     * service is configured
     * @param tenantOCID OCID of the tenancy i.e. root compartment
     *
     * @return Application OCID
     *
     * @throws Exception
     */
    public String getAppOCID(String appName, String compartmentName, String tenantOCID) throws Exception {

        //start by finding the compartment OCID from the name
        String compOCID = getCompartmentOCID(compartmentName, tenantOCID);
        System.out.println("Finding OCID for App " + appName);
        
        fnClient.setEndpoint(FnInvokeExample.FAAS_ENDPOINT);

        //find the application in a specific compartment
        ListApplicationsRequest req = ListApplicationsRequest.builder()
                .displayName(appName)
                .compartmentId(compOCID)
                .build();
        ListApplicationsResponse resp = fnClient.listApplications(req);

        if (resp.getItems().isEmpty()) {
            throw new Exception("Could not find App with  name " + appName + " in compartment " + compartmentName);

        }
        String appOCID = resp.getItems().get(0).getId();

        System.out.println("Application OCID " + appOCID);

        return appOCID;

    }

    /**
     * Gets compartment OCID
     *
     * @param compartmentName
     * @param tenantOCID
     * @return compartment OCID
     * @throws Exception
     */
    public String getCompartmentOCID(String compartmentName, String tenantOCID) throws Exception {
        System.out.println("Finding OCID for Compartment " + compartmentName);
        String compOCID = null;
        ListCompartmentsResponse listCompartmentsResponse = null;
        try {
            ListCompartmentsRequest lcr = ListCompartmentsRequest.builder().compartmentId(tenantOCID).build();
            listCompartmentsResponse = identityClient.listCompartments(lcr);

            for (Compartment comp : listCompartmentsResponse.getItems()) {
                if (comp.getName().equals(compartmentName)) {
                    compOCID = comp.getId();
                    break;
                }
            }

            if (compOCID == null) {
                throw new Exception("Could not find compartment with  name " + compartmentName + " in tenancy " + tenantOCID);
            }
            System.out.println("Compartment OCID " + compOCID);
        } catch (Exception e) {
            throw e;
        } finally {
            identityClient.close();
        }
        return compOCID;
    }

    /**
     * Gets function details in form of a FunctionSummary object
     *
     * @param appOCID
     * @param functionName
     * @return FunctionSummary object
     * @throws Exception
     */
    public FunctionSummary getFunction(String appOCID, String functionName) throws Exception {

        System.out.println("Finding OCID for function " + functionName);

        //search for the function in the app
        ListFunctionsRequest lfr = ListFunctionsRequest.builder().applicationId(appOCID).displayName(functionName).build();

        fnClient.setEndpoint(FnInvokeExample.FAAS_ENDPOINT);

        ListFunctionsResponse lfresp = fnClient.listFunctions(lfr);

        if (lfresp.getItems().isEmpty()) {
            throw new Exception("Could not find function with  name " + functionName + " for application " + appOCID);

        }
        FunctionSummary function = lfresp.getItems().get(0);
        System.out.println("Found Function with OCID " + function.getId());

        return function;
    }

    public static class TrustAllConfigurator extends DefaultConfigurator {

        @Override
        protected void setSslContext(ClientBuilder builder) {
            SSLContext sslContext
                    = SslConfigurator.newInstance(true).securityProtocol("TLSv1.2").createSSLContext();
            builder.sslContext(sslContext);
            try {
                sslContext.init(
                        null,
                        new TrustManager[]{
                            new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
                        },
                        new SecureRandom());
            } catch (KeyManagementException e) {
                System.err.println(e);
            }

            builder.hostnameVerifier(
                    new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }
    }

}
