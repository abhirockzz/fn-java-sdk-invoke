package com.test.fn;

import com.google.common.base.Supplier;
import com.oracle.bmc.Region;

import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.functions.FunctionsClient;
import com.oracle.bmc.functions.requests.GetFunctionRequest;
import com.oracle.bmc.functions.requests.InvokeFunctionRequest;
import com.oracle.bmc.functions.requests.ListApplicationsRequest;
import com.oracle.bmc.functions.requests.ListFunctionsRequest;
import com.oracle.bmc.functions.responses.GetFunctionResponse;
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
import java.io.IOException;
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
    private static final String FAAS_ENDPOINT = "https://functions.us-phoenix-1.oraclecloud.com/";
    static String tenantOCID = null;
    private SimpleAuthenticationDetailsProvider authDetails;

    static String ERR_MSG = "Usage: java -jar <jar-name>.jar <compartment name> <app name> <function name> <function invoke payload>";

    public static void main(String[] args) throws Exception {

        if (args.length < 4) {
            throw new Exception(ERR_MSG);
        }

        tenantOCID = System.getenv("TENANT_OCID");
        String userId = System.getenv("USER_OCID");
        String fingerprint = System.getenv("PUBLIC_KEY_FINGERPRINT");
        String privateKeyFile = System.getenv("PRIVATE_KEY_LOCATION");
        String passphrase = System.getenv("PASSPHRASE");

        if (tenantOCID == null || userId == null || fingerprint == null || privateKeyFile == null || passphrase == null) {
            throw new Exception("Please ensure you have set the following environment variables - TENANT_OCID, USER_OCID, PUBLIC_KEY_FINGERPRINT, PRIVATE_KEY_LOCATION, PASSPHRASE");

        }
        FnInvokeExample test = new FnInvokeExample(tenantOCID, userId, fingerprint, privateKeyFile, passphrase);

        String compartmentName = args[0];
        String appName = args[1];
        String funcName = args[2];
        String invokePayload = args[3];

        System.out.println("Invoking function " + funcName + " from app " + appName + " in compartment " + compartmentName + " from tenancy " + tenantOCID);
        test.invokeFunction(funcName, appName, compartmentName, invokePayload);

    }

    /**
     * Invokes a function
     *
     * @param tenantId
     * @param userId
     * @param fingerprint
     * @param privateKeyFile
     * @param passphrase
     */
    public FnInvokeExample(String tenantId, String userId, String fingerprint, String privateKeyFile, String passphrase) {
        Supplier<InputStream> privateKeySupplier
                = () -> {
                    try {
                        return new FileInputStream(new File(privateKeyFile));
                    } catch (FileNotFoundException e) {
                        throw new IllegalArgumentException(e);
                    }
                };

        this.authDetails = SimpleAuthenticationDetailsProvider.builder()
                .tenantId(tenantId)
                .userId(userId)
                .fingerprint(fingerprint)
                .privateKeySupplier(privateKeySupplier)
                .passPhrase(passphrase)
                .region(Region.US_PHOENIX_1)
                .build();

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

        //get the App OCID first
        String appOCID = getAppOCID(appName, compartmentName, tenantOCID);

        System.out.println("Finding OCID for function " + functionName);

        //search for the function in the app
        ListFunctionsRequest lfr = ListFunctionsRequest.builder().applicationId(appOCID).displayName(functionName).build();

        try (FunctionsClient fnClient = new FunctionsClient(authDetails, null, new TrustAllConfigurator())) {
            fnClient.setEndpoint(FAAS_ENDPOINT);

            ListFunctionsResponse lfresp = fnClient.listFunctions(lfr);

            if (lfresp.getItems().isEmpty()) {
                throw new Exception("Could not find function with  name " + functionName + " in application " + appName);

            }
            String functionId = lfresp.getItems().get(0).getId();
            System.out.println("Function OCID " + functionId);

            System.out.println("Finding invoke endpoint for function " + functionName);

            //get function details
            GetFunctionRequest gfr = GetFunctionRequest.builder().functionId(functionId).build();

            GetFunctionResponse function = fnClient.getFunction(gfr);
            String invokeEndpoint = function.getFunction().getInvokeEndpoint();
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
            InvokeFunctionRequest ifr = InvokeFunctionRequest.builder().functionId(function.getFunction().getId())
                    .invokeFunctionBody(StreamUtils.createByteArrayInputStream(payload.getBytes()))
                    .build();

            //actual function invocation
            InvokeFunctionResponse resp = fnClient.invokeFunction(ifr);

            try {
                //parse the response

                /**
                 * the example below simply reads a String response.
                 */
                String respString = IOUtils.toString(resp.getInputStream(), StandardCharsets.UTF_8);
                System.out.print("Response from function - " + respString + "\n");
            } catch (IOException e) {
                System.err.println(e);
            }

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

        System.out.println("Finding OCID for Compartment " + compartmentName);

        //start by finding the compartment OCID from the name
        ListCompartmentsRequest lcr = ListCompartmentsRequest.builder().compartmentId(tenantOCID).build();
        ListCompartmentsResponse listCompartmentsResponse = null;
        try (IdentityClient idc = new IdentityClient(authDetails)) {
            listCompartmentsResponse = idc.listCompartments(lcr);
        }
        String compOCID = null;
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

        System.out.println("Finding OCID for App " + appName);
        try (FunctionsClient fnClient = new FunctionsClient(authDetails, null, new TrustAllConfigurator())) {
            fnClient.setEndpoint(FAAS_ENDPOINT);

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
