package com.test.fn;

import com.google.common.base.Supplier;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.functions.FunctionsClient;
import com.oracle.bmc.functions.requests.InvokeFunctionRequest;
import com.oracle.bmc.functions.responses.InvokeFunctionResponse;
import com.oracle.bmc.util.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

public class FnInvokeExample {

    /**
     * Oracle Functions service endpoint (us-phoenix-1 corresponds to phoenix
     * region where the service is available)
     */
    public static final String FAAS_ENDPOINT = "https://functions.us-phoenix-1.oraclecloud.com/";
    static String tenantOCID = null;
    private SimpleAuthenticationDetailsProvider authDetails;
    private Util ociUtil;

    static String ERR_MSG = "Usage: java -jar <jar-name>.jar <compartment name> <app name> <function name> <(optional) function invoke payload>";

    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            throw new Exception(ERR_MSG);
        }

        tenantOCID = System.getenv("TENANT_OCID");
        String userId = System.getenv("USER_OCID");
        String fingerprint = System.getenv("PUBLIC_KEY_FINGERPRINT");
        String privateKeyFile = System.getenv("PRIVATE_KEY_LOCATION");
        String passphrase = System.getenv("PASSPHRASE");

        if (tenantOCID == null || userId == null || fingerprint == null || privateKeyFile == null /*|| passphrase == null*/) {
            throw new Exception("Please ensure you have set the mandatory environment variables - TENANT_OCID, USER_OCID, PUBLIC_KEY_FINGERPRINT, PRIVATE_KEY_LOCATION");

        }
        FnInvokeExample test = new FnInvokeExample(tenantOCID, userId, fingerprint, privateKeyFile, passphrase);

        String compartmentName = args[0];
        String appName = args[1];
        String funcName = args[2];
        String invokePayload = args.length == 4 ? args[3] : "";

        System.out.println("Invoking function " + funcName + " from app " + appName + " in compartment " + compartmentName + " from tenancy " + tenantOCID);
        test.invokeFunction(funcName, appName, compartmentName, invokePayload);

    }

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

        this.ociUtil = new Util(authDetails);

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
        String appOCID = ociUtil.getAppOCID(appName, compartmentName, tenantOCID);

        //find the function OCID
        String functionId = ociUtil.getFunctionOCID(appOCID, functionName);

        //find the function invoke endpoint
        String invokeEndpoint = ociUtil.getFunctionInvokeEndpoint(functionId);

        try (FunctionsClient fnClient = new FunctionsClient(authDetails, null, new Util.TrustAllConfigurator())) {

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

}
