package com.test.fn;

import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.functions.FunctionsClient;
import com.oracle.bmc.functions.requests.GetFunctionRequest;
import com.oracle.bmc.functions.requests.ListApplicationsRequest;
import com.oracle.bmc.functions.requests.ListFunctionsRequest;
import com.oracle.bmc.functions.responses.GetFunctionResponse;
import com.oracle.bmc.functions.responses.ListApplicationsResponse;
import com.oracle.bmc.functions.responses.ListFunctionsResponse;
import com.oracle.bmc.http.DefaultConfigurator;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.requests.ListCompartmentsRequest;
import com.oracle.bmc.identity.responses.ListCompartmentsResponse;

import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.SslConfigurator;

/**
 * Contains helper methods
 */
public class Util {

    private SimpleAuthenticationDetailsProvider authDetails;

    public Util(SimpleAuthenticationDetailsProvider authDetails) {
        this.authDetails = authDetails;
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
        try (FunctionsClient fnClient = new FunctionsClient(authDetails, null, new TrustAllConfigurator())) {
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

        return compOCID;
    }

    /**
     * Get function OCID
     *
     * @param appOCID
     * @param functionName
     * @return function OCID
     * @throws Exception
     */
    public String getFunctionOCID(String appOCID, String functionName) throws Exception {

        System.out.println("Finding OCID for function " + functionName);

        //search for the function in the app
        ListFunctionsRequest lfr = ListFunctionsRequest.builder().applicationId(appOCID).displayName(functionName).build();

        try (FunctionsClient fnClient = new FunctionsClient(authDetails, null, new TrustAllConfigurator())) {
            fnClient.setEndpoint(FnInvokeExample.FAAS_ENDPOINT);

            ListFunctionsResponse lfresp = fnClient.listFunctions(lfr);

            if (lfresp.getItems().isEmpty()) {
                throw new Exception("Could not find function with  name " + functionName + " for application " + appOCID);

            }
            String functionId = lfresp.getItems().get(0).getId();
            System.out.println("Function OCID " + functionId);

            return functionId;
        }
    }

    /**
     * Get function invoke endpoint
     *
     * @param functionId
     * @return function invoke endpoint
     */
    public String getFunctionInvokeEndpoint(String functionId) {
        try (FunctionsClient fnClient = new FunctionsClient(authDetails, null, new TrustAllConfigurator())) {
            fnClient.setEndpoint(FnInvokeExample.FAAS_ENDPOINT);

            System.out.println("Finding invoke endpoint for function " + functionId);

            //get function details
            GetFunctionRequest gfr = GetFunctionRequest.builder().functionId(functionId).build();

            GetFunctionResponse function = fnClient.getFunction(gfr);
            String invokeEndpoint = function.getFunction().getInvokeEndpoint();
            return invokeEndpoint;
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
