/***
 * @author abhirockzz
 * @author shaunsmith
 */

package com.test.fn;

import com.google.common.base.Supplier;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.functions.FunctionsInvokeClient;
import com.oracle.bmc.functions.FunctionsManagementClient;
import com.oracle.bmc.functions.model.ApplicationSummary;
import com.oracle.bmc.functions.model.FunctionSummary;
import com.oracle.bmc.functions.requests.InvokeFunctionRequest;
import com.oracle.bmc.functions.requests.ListApplicationsRequest;
import com.oracle.bmc.functions.requests.ListFunctionsRequest;
import com.oracle.bmc.functions.responses.InvokeFunctionResponse;
import com.oracle.bmc.functions.responses.ListApplicationsResponse;
import com.oracle.bmc.functions.responses.ListFunctionsResponse;
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

import org.apache.commons.io.IOUtils;

/**
 * Contains helper methods
 */
public class FunctionsUtil implements AutoCloseable {

    private String tenantOCID = null;
    private final FunctionsManagementClient fnMgtClient;
    private final IdentityClient identityClient;
    private FunctionsInvokeClient fnInvokeClient;

    /**
     * Initializes FunctionsManagementClient and IdentityClient
     * 
     * @param tenantId
     * @param userId
     * @param fingerprint
     * @param privateKeyFile
     * @param passphrase
     */
    public FunctionsUtil(String tenantId, String userId, String fingerprint, String privateKeyFile, String passphrase) {
        this.tenantOCID = tenantId;
        Supplier<InputStream> privateKeySupplier = () -> {
            try {
                return new FileInputStream(new File(privateKeyFile));
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        };

        SimpleAuthenticationDetailsProvider authDetails = SimpleAuthenticationDetailsProvider.builder()
                .tenantId(tenantId).userId(userId).fingerprint(fingerprint).privateKeySupplier(privateKeySupplier)
                .passPhrase(passphrase).region(Region.US_PHOENIX_1).build();

        this.fnMgtClient = new FunctionsManagementClient(authDetails);
        this.fnMgtClient.setRegion(Region.US_PHOENIX_1); // Oracle Functions currently only in phoenix
        this.identityClient = new IdentityClient(authDetails);
        this.fnInvokeClient = new FunctionsInvokeClient(authDetails);

    }

    /**
     * Invokes a function with the given payload
     * 
     * @param functionId
     * @param invokeEndpoint
     * @param payload
     * @throws Exception
     */
    public void invokeFunction(FunctionSummary function, String payload) throws Exception {
        try {
            System.err.println(
                    "Invoking function endpoint - " + function.getInvokeEndpoint() + " with payload " + payload);

            // the client needs to use the function invoke endpoint
            this.fnInvokeClient.setEndpoint(function.getInvokeEndpoint());

            /**
             * This is an example of sending a JSON payload as an input to the function -
             * valid for "fn init --runtime ..." generated boilerplate functions for python,
             * node, go, and ruby. The expected result is {"message":"Hello foobar"}
             *
             * For a Java boilerplate function, you can simply pass a string (not JSON)
             * e.g., foobar as the input and expect Hello foobar! as the response
             *
             * see README for note on how to send binary payload
             */
            InvokeFunctionRequest ifr = InvokeFunctionRequest.builder().functionId(function.getId())
                    .invokeFunctionBody(StreamUtils.createByteArrayInputStream(payload.getBytes())).build();

            // actual function invocation
            InvokeFunctionResponse resp = fnInvokeClient.invokeFunction(ifr);

            // parse the response - this example below simply reads a String response.
            String respString = IOUtils.toString(resp.getInputStream(), StandardCharsets.UTF_8);
            System.err.print("Response from function - " + respString + "\n");
        } catch (Exception e) {
            throw e;
        } finally {
            fnInvokeClient.close();
        }
    }

    /**
     * Gets Function information. This is an expensive operation and the results
     * should be cached.
     * 
     * @param compartmentName
     * @param appName
     * @param functionName
     * @return
     * @throws Exception
     */
    public FunctionSummary getFunction(String compartmentName, String appName, String functionName) throws Exception {
        Compartment compartment = getCompartment(compartmentName);
        return getFunction(compartment, appName, functionName);
    }

    /**
     * Gets Function information. This is an expensive operation and the results
     * should be cached.
     * 
     * @param compartment
     * @param appName
     * @param functionName
     * @return
     * @throws Exception
     */
    public FunctionSummary getFunction(Compartment compartment, String appName, String functionName) throws Exception {
        ApplicationSummary application = getApplication(compartment, appName);
        return getFunction(application, functionName);
    }

    /**
     * Gets Function information. This is an expensive operation and the results
     * should be cached.
     * 
     * @param application
     * @param functionName
     * @return
     * @throws Exception
     */
    public FunctionSummary getFunction(ApplicationSummary application, String functionName) throws Exception {
        ListFunctionsRequest lfr = ListFunctionsRequest.builder().applicationId(application.getId())
                .displayName(functionName).build();

        ListFunctionsResponse lfresp = fnMgtClient.listFunctions(lfr);

        if (lfresp.getItems().isEmpty()) {
            throw new Exception("Could not find function with name " + functionName + " in application "
                    + application.getDisplayName());
        }

        FunctionSummary function = lfresp.getItems().get(0);
        System.err.println("Found Function with OCID " + function.getId());

        return function;
    }

    /**
     * Gets application info
     * 
     * @param appName
     * @param compartmentName
     * @return
     * @throws Exception
     */
    public ApplicationSummary getApplication(String compartmentName, String appName) throws Exception {
        Compartment compartment = getCompartment(compartmentName);
        return getApplication(compartment, appName);
    }

    /**
     * Gets application info
     * 
     * @param appName
     * @param compartmentName
     * 
     * @return
     * @throws Exception
     */
    public ApplicationSummary getApplication(Compartment compartment, String appName) throws Exception {

        // find the application in a specific compartment
        ListApplicationsRequest req = ListApplicationsRequest.builder().displayName(appName)
                .compartmentId(compartment.getId()).build();
        ListApplicationsResponse resp = fnMgtClient.listApplications(req);

        if (resp.getItems().isEmpty()) {
            throw new Exception(
                    "Could not find application with name " + appName + " in compartment " + compartment.getName());
        }
        ApplicationSummary application = resp.getItems().get(0);
        return application;
    }

    /**
     * Gets compartment OCID
     * 
     * @param compartmentName
     * @return
     * @throws Exception
     */
    public Compartment getCompartment(String compartmentName) throws Exception {
        ListCompartmentsResponse listCompartmentsResponse = null;
        ListCompartmentsRequest lcr = ListCompartmentsRequest.builder().compartmentId(tenantOCID)
                .accessLevel(ListCompartmentsRequest.AccessLevel.Accessible).compartmentIdInSubtree(Boolean.TRUE)
                .build();

        listCompartmentsResponse = identityClient.listCompartments(lcr);

        for (Compartment comp : listCompartmentsResponse.getItems()) {
            if (comp.getName().equals(compartmentName)) {
                return comp;
            }
        }
        throw new Exception("Could not find compartment with name " + compartmentName + " in tenancy " + tenantOCID);
    }

    /**
     * close client instances
     */
    public void close() {
        if (fnMgtClient != null) {
            fnMgtClient.close();
        }

        if (identityClient != null) {
            identityClient.close();
        }

        if (fnInvokeClient != null) {
            fnInvokeClient.close();
        }
    }

}
