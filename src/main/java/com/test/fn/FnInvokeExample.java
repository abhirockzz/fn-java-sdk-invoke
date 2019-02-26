package com.test.fn;

import com.google.common.base.Supplier;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.functions.FunctionsInvokeClient;
import com.oracle.bmc.functions.requests.InvokeFunctionRequest;
import com.oracle.bmc.functions.responses.InvokeFunctionResponse;
import com.oracle.bmc.util.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

public class FnInvokeExample {

    private String tenantOCID = null;
    private final FunctionsInvokeClient fnInvokeClient;

    /**
     * Initializes FunctionsInvokeClient
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

        this.fnInvokeClient = new FunctionsInvokeClient(authDetails);
    }

    /**
     * Invokes a function
     * 
     * @param functionId
     * @param invokeEndpoint
     * @param payload
     * @throws Exception
     */
    public void invokeFunction(String functionId, String invokeEndpoint, String payload) throws Exception {
        try {
            System.out.println("Invoking function endpoint - " + invokeEndpoint + " with payload " + payload);

            //the client needs to use the function invoke endpoint
            fnInvokeClient.setEndpoint(invokeEndpoint);

            /**
             * This is an example of sending a JSON payload as an input to the
             * function - valid for "fn init --runtime ..." generated
             * boilerplate functions for python, node, go, and ruby. The
             * expected result is {"message":"Hello foobar"}
             *
             * For a Java boilerplate function, you can simply pass a string
             * (not JSON) e.g., foobar as the input and expect Hello foobar! as
             * the response
             *
             * see README for note on how to send binary payload
             */
            InvokeFunctionRequest ifr = InvokeFunctionRequest.builder().functionId(functionId)
                    .invokeFunctionBody(StreamUtils.createByteArrayInputStream(payload.getBytes()))
                    .build();

            //actual function invocation
            InvokeFunctionResponse resp = fnInvokeClient.invokeFunction(ifr);

            //parse the response - this example below simply reads a String response.
            String respString = IOUtils.toString(resp.getInputStream(), StandardCharsets.UTF_8);
            System.out.print("Response from function - " + respString + "\n");
        } catch (Exception e) {
            throw e;
        } finally {
            fnInvokeClient.close();
        }
    }
}
