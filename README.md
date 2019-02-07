# Invoke Oracle Functions using the OCI Java SDK

This example demonstrates how to invoke a function on Oracle Functions using (preview version of) the Oracle Cloud Infrastructure Java SDK

## Pre-requisites

Start by cloning this repository - 

`git clone https://github.com/abhirockzz/fn-java-sdk-invoke`

Install latest Fn CLI - 

`curl -LSs https://raw.githubusercontent.com/fnproject/cli/master/install | sh`

A function that you want to invoke - 

Create a function using [Go Hello World Function](https://github.com/abhirockzz/oracle-functions-hello-worlds/blob/master/golang-hello-world.md)

### Install preview OCI Java SDK

You need to install the OCI SDK JAR to your local Maven repository first.

Download the preview version of the OCI Java SDK

Unzip - 

`unzip oci-java-sdk-preview.zip`

Set the name of the SDK JAR as an environment variable - 

`export OCI_SDK_JAR=oci-java-sdk-full-1.3.6-preview1-SNAPSHOT.jar`

Change into the correct directory - 

`cd oci-java-sdk-preview`

Install the JAR to local Maven repo - 

`mvn install:install-file -Dfile=lib/$OCI_SDK_JAR -DgroupId=com.oracle.oci.sdk -DartifactId=oci-java-sdk -Dversion=1.3.6-preview1-20190125.203329-3 -Dpackaging=jar`

### Build the JAR and configure environment variables

Change to the correct directory where you cloned the example: 

`cd fn-java-sdk-invoke` 

Then build the JAR using 

`mvn clean install`

Set environment variables

	export TENANT_OCID=<OCID of your tenancy>
	export USER_OCID=<OCID of the OCI user>
	export PUBLIC_KEY_FINGERPRINT=<public key fingerprint>
	export PRIVATE_KEY_LOCATION=<location of the private key on your machine>

> please note that `PASSPHRASE` is optional i.e. only required if your private key has one

	export PASSPHRASE=<private key passphrase>

e.g. 

	export TENANT_OCID=ocid1.tenancy.oc1..aaaaaaaaydrjd77otncda2xn7qrv7l3hqnd3zxn2u4siwdhniibwfv4wwhtz
	export USER_OCID=ocid1.user.oc1..aaaaaaaavz5efd7jwjjipbvm536plgylg7rfr53obvtghpi2vbg3qyrnrtfa
	export PUBLIC_KEY_FINGERPRINT=42:42:5f:42:ca:a1:2e:58:d2:63:6a:af:42:d5:3d:42
	export PRIVATE_KEY_LOCATION=/Users/foobar/oci_api_key.pem
	
> and only if your private key has a passphrase:

	export PASSPHRASE=4242


### You can now invoke your function!

`java -jar target/<jar-name>.jar <compartment-name> <app-name> <function-name> <optional-payload>`

> Payload is optional. If your function doesn't expect any input parameters, you can omit the <optional-payload>

e.g. with payload:

`java -jar target/fn-java-sdk-invoke-1.0-SNAPSHOT.jar mycompartment helloworld-app helloworld-func-go {\"name\":\"foobar\"}`

e.g. without payload:

`java -jar target/fn-java-sdk-invoke-1.0-SNAPSHOT.jar mycompartment helloworld-app helloworld-func-go`

## Troubleshooting

### If you fail to set the required environment variables like TENANT_OCID etc.

You will see the following error - `Exception in thread "main" java.lang.Exception: Please ensure you have set the mandatory environment variables - TENANT_OCID, USER_OCID, PUBLIC_KEY_FINGERPRINT, PRIVATE_KEY_LOCATION`

### If you do not provide required arguments i.e. function name etc.

You will see the following error - `Exception in thread "main" java.lang.Exception: Usage: java -jar <jar-name>.jar <function name> <app name> <compartment name> <function invoke payload>`

### If you provide an invalid value for function name etc.

You will see something similar to - `Exception in thread "main" java.lang.Exception: Could not find function with name test-function in application test-app`

### If you provide an incorrect TENANT_OCID or USER_OCID or PUBLIC_KEY_FINGERPRINT

You will get this error - `Exception in thread "main" com.oracle.bmc.model.BmcException: (401, NotAuthenticated, false) The required information to complete authentication was not provided or was incorrect. (opc-request-id: 974452A5243XXXXX77194672D650/37DFE2AEXXXXXXX20ADFEB2E43/48B235F1D7XXXXXX273CFB889)`

### If your key has a passphrase but you failed to set the environment variable PASSPHRASE

You will get this error - `Exception in thread "main" java.lang.NullPointerException: The provided private key requires a passphrase`

### If your key has a passphrase but you set an incorrect value in the environment variable PASSPHRASE

You will get this error - `Exception in thread "main" java.lang.IllegalArgumentException: The provided passphrase is incorrect.`

