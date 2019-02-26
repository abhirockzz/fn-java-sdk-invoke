package com.test.fn;

import com.oracle.bmc.functions.model.FunctionSummary;

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

        if (tenantOCID == null || userId == null || fingerprint == null || privateKeyFile == null /*|| passphrase == null*/) {
            throw new Exception("Please ensure you have set the mandatory environment variables - TENANT_OCID, USER_OCID, PUBLIC_KEY_FINGERPRINT, PRIVATE_KEY_LOCATION");
        }

        String compartmentName = args[0];
        String appName = args[1];
        String funcName = args[2];
        String invokePayload = args.length == 4 ? args[3] : "";

        FunctionsUtil util = null;
        try {
            util = new FunctionsUtil(tenantOCID, userId, fingerprint, privateKeyFile, passphrase);
            FnInvokeExample test = new FnInvokeExample(tenantOCID, userId, fingerprint, privateKeyFile, passphrase);

            FunctionSummary function = util.getFunction(compartmentName, appName, funcName);
            String functionId = function.getId();
            String invokeEndpoint = function.getInvokeEndpoint();
            System.out.println("Invoking function " + funcName + " from app " + appName + " in compartment " + compartmentName + " from tenancy " + tenantOCID);

            test.invokeFunction(functionId, invokeEndpoint, invokePayload);
        } catch (Exception e) {
            throw e;
        } finally {
            if (util != null) {
                util.close();
            }
        }

    }

}
