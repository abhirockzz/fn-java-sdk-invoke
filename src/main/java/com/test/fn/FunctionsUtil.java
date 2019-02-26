package com.test.fn;

import com.google.common.base.Supplier;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.functions.FunctionsManagementClient;
import com.oracle.bmc.functions.model.FunctionSummary;
import com.oracle.bmc.functions.requests.ListApplicationsRequest;
import com.oracle.bmc.functions.requests.ListFunctionsRequest;
import com.oracle.bmc.functions.responses.ListApplicationsResponse;
import com.oracle.bmc.functions.responses.ListFunctionsResponse;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.requests.ListCompartmentsRequest;
import com.oracle.bmc.identity.responses.ListCompartmentsResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Contains helper methods
 */
public class FunctionsUtil {

    private String tenantOCID = null;
    private final FunctionsManagementClient fnMgtClient;
    private final IdentityClient identityClient;

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

        this.fnMgtClient = new FunctionsManagementClient(authDetails);
        this.fnMgtClient.setRegion(Region.US_PHOENIX_1); //Oracle Functions currently only in phoenix
        this.identityClient = new IdentityClient(authDetails);
    }

    /**
     * Gets Function information 
     * 
     * @param compartmentName
     * @param appName
     * @param functionName
     * @return
     * @throws Exception 
     */
    public FunctionSummary getFunction(String compartmentName, String appName, String functionName) throws Exception {

        System.out.println("Finding OCID for function " + functionName);

        String appOCID = getAppOCID(appName, compartmentName);

        //search for the function in the app
        ListFunctionsRequest lfr = ListFunctionsRequest.builder().applicationId(appOCID).displayName(functionName).build();

        //fnMgtClient.setEndpoint(FnInvokeExample.FAAS_ENDPOINT);
        ListFunctionsResponse lfresp = fnMgtClient.listFunctions(lfr);

        if (lfresp.getItems().isEmpty()) {
            throw new Exception("Could not find function with  name " + functionName + " for application " + appOCID);

        }
        FunctionSummary function = lfresp.getItems().get(0);
        System.out.println("Found Function with OCID " + function.getId());

        return function;
    }

    /**
     * Gets application OCID
     * 
     * @param appName
     * @param compartmentName
     * @return
     * @throws Exception 
     */
    public String getAppOCID(String appName, String compartmentName) throws Exception {

        //start by finding the compartment OCID from the name
        String compOCID = getCompartmentOCID(compartmentName);
        System.out.println("Finding OCID for App " + appName);

        //find the application in a specific compartment
        ListApplicationsRequest req = ListApplicationsRequest.builder()
                .displayName(appName)
                .compartmentId(compOCID)
                .build();
        ListApplicationsResponse resp = fnMgtClient.listApplications(req);

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
     * @return
     * @throws Exception 
     */
    public String getCompartmentOCID(String compartmentName) throws Exception {
        System.out.println("Finding OCID for Compartment " + compartmentName);
        String compOCID = null;
        ListCompartmentsResponse listCompartmentsResponse = null;
        try {
            ListCompartmentsRequest lcr = ListCompartmentsRequest.builder()
                    .compartmentId(tenantOCID)
                    .accessLevel(ListCompartmentsRequest.AccessLevel.Accessible)
                    .compartmentIdInSubtree(Boolean.TRUE)
                    .build();

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
        }
        return compOCID;
    }

    /**
     * closes FunctionsManagementClient and IdentityClient instances
     */
    public void close() {
        if (fnMgtClient != null) {
            fnMgtClient.close();
        }

        if (identityClient != null) {
            identityClient.close();
        }
    }

}
