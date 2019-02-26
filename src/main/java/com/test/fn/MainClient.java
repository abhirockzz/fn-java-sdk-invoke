/***
 * @author abhirockzz
 * @author shaunsmith
 */

package com.test.fn;

import com.oracle.bmc.functions.model.ApplicationSummary;
import com.oracle.bmc.functions.model.FunctionSummary;
import com.oracle.bmc.identity.model.Compartment;

public class MainClient {

    static String ERR_MSG = "Usage: java -jar <jar-name>.jar <compartment name> <app name> <function name> <(optional) function invoke payload>";

    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            throw new Exception(ERR_MSG);
        }

        String tenantOCID = System.getenv("TENANT_OCID");
        String userId = System.getenv("USER_OCID");
        String fingerprint = System.getenv("PUBLIC_KEY_FINGERPRINT");
        String privateKeyFile = System.getenv("PRIVATE_KEY_LOCATION");
        String passphrase = System.getenv("PASSPHRASE");

        if (tenantOCID == null || userId == null || fingerprint == null
                || privateKeyFile == null /* || passphrase == null */) {
            throw new Exception(
                    "Please ensure you have set the mandatory environment variables - TENANT_OCID, USER_OCID, PUBLIC_KEY_FINGERPRINT, PRIVATE_KEY_LOCATION");
        }

        String compartmentName = args[0];
        String appName = args[1];
        String funcName = args[2];
        String invokePayload = args.length == 4 ? args[3] : "";

        try (FunctionsUtil functionsUtil = new FunctionsUtil(tenantOCID, userId, fingerprint, privateKeyFile,
                passphrase);) {
            Compartment compartment = functionsUtil.getCompartment(compartmentName);
            ApplicationSummary application = functionsUtil.getApplication(compartment, appName);
            FunctionSummary function = functionsUtil.getFunction(application, funcName);
            System.err.println("Invoking function " + funcName + " from app " + appName + " in compartment "
                    + compartmentName + " from tenancy " + tenantOCID);
            functionsUtil.invokeFunction(function, invokePayload);
        }

    }

}
