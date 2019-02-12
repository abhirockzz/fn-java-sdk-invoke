# Invoke Oracle Functions using the OCI Java SDK

This example demonstrates how to invoke a function on Oracle Functions using (preview version of) the Oracle Cloud Infrastructure Java SDK. 


## Introduction

To be specifc, it shows how you can invoke a function by its name given that you also provide the application name (to which the function belongs), the OCI compartment (for which your Oracle Functions service is configured) and the OCID of your tenancy

The function invocation API requires two key inputs - the function OCID and the function invoke endpoint. The most important function related API object is the `FunctionsClient`

We start off with the initial information i.e. function name, application name, the compartment name and the tenant OCID

- The first step extracts the Compartment OCID from the name using the `IdentityClient.listCompartments` method - it looks for compartments in the tenancy and matches the one with the provided name
- The compartment OCID is then used to find the Application OCID from the name using `FunctionsClient.listApplications`
- Once we have the application OCID, the function information (in the form of a `FunctionSummary` object) is extracted using `FunctionsClient.listFunctions` - this allows us to get both the function OCID as well as its invoke endpoint

Now that we have the function OCID and invoke enpoint at our disposal

- we build the `InvokeFunctionRequest` object with the function OCID and the (optional) payload which we want to send to our function, and,
- call `setEndpoint` in our `FunctionsClient` object to point it towards the invoke endpoint
- finally, we call `invokeFunction` and extract the String response from the `InvokeFunctionResponse` object

### Authentication

The client program needs to authenticate to OCI before being able to make service calls. The standard OCI authenitcation is used, which accepts the following inputs (details below) - tenant OCID, user OCID, fingerprint, private key and passphrase (optional). These details are required to instantiate a `SimpleAuthenticationDetailsProvider` object which is subsequently used by the service client objects (`FunctionsClient`, `IdentityClient`). 

This example does not assume the presence of an OCI config file on the machine from where this is being executed. However, if you have one present as per the standard OCI practices i.e. a config file in your home directory, you can use the `ConfigFileAuthenticationDetailsProvider` for convenience

## Pre-requisites

1. Clone this repository

   `git clone https://github.com/abhirockzz/fn-java-sdk-invoke`

2. Install the latest Fn CLI

   `curl -LSs https://raw.githubusercontent.com/fnproject/cli/master/install | sh`

3. Create a function to invoke

   Create a function using [Go Hello World Function](https://github.com/abhirockzz/oracle-functions-hello-worlds/blob/master/golang-hello-world.md)

### Install preview OCI Java SDK

As this example uses Maven, you need to install the OCI SDK JAR to your local Maven repository.

1. Download and unzip the preview version of the OCI Java SDK

   `unzip oci-java-sdk-dist-1.3.6-preview1-20190125.203329-3.zip`

2. Set the name of the SDK JAR as an environment variable

   `export OCI_SDK_JAR=oci-java-sdk-full-1.3.6-preview1-SNAPSHOT.jar`

3. Change into the correct directory

   `cd oci-java-sdk-dist-1.3.6-preview1-20190125.203329-3`

4. Install the JAR to local Maven repo

   `mvn install:install-file -Dfile=lib/$OCI_SDK_JAR -DgroupId=com.oracle.oci.sdk -DartifactId=oci-java-sdk -Dversion=1.3.6-preview1-20190125.203329-3 -Dpackaging=jar`

### Build the JAR and configure environment variables

1. Change to the correct directory where you cloned the example: 

   `cd fn-java-sdk-invoke` 

2. Then build the JAR using 

   `mvn clean install`

3. Set environment variables

   ```shell
   export TENANT_OCID=<OCID of your tenancy>
   export USER_OCID=<OCID of the OCI user>
   export PUBLIC_KEY_FINGERPRINT=<public key fingerprint>
   export PRIVATE_KEY_LOCATION=<location of the private key on your machine>
   ```

   > please note that `PASSPHRASE` is optional i.e. only required if your private key has one

   ```shell
   export PASSPHRASE=<private key passphrase>
   ```
   
   e.g.

   ```shell
   export TENANT_OCID=ocid1.tenancy.oc1..aaaaaaaaydrjd77otncda2xn7qrv7l3hqnd3zxn2u4siwdhniibwfv4wwhtz
   export USER_OCID=ocid1.user.oc1..aaaaaaaavz5efd7jwjjipbvm536plgylg7rfr53obvtghpi2vbg3qyrnrtfa
   export PUBLIC_KEY_FINGERPRINT=42:42:5f:42:ca:a1:2e:58:d2:63:6a:af:42:d5:3d:42
   export PRIVATE_KEY_LOCATION=/Users/foobar/oci_api_key.pem
   ```

   > and only if your private key has a passphrase:

   ```shell
   export PASSPHRASE=4242
   ```

## You can now invoke your function!

`java -jar target/<jar-name>.jar <compartment-name> <app-name> <function-name> <optional-payload>`

> Payload is optional. If your function doesn't expect any input parameters, you can omit the <optional-payload>

e.g. with payload:

`java -jar target/fn-java-sdk-invoke-1.0-SNAPSHOT.jar mycompartment helloworld-app helloworld-func-go {\"name\":\"foobar\"}`

e.g. without payload:

`java -jar target/fn-java-sdk-invoke-1.0-SNAPSHOT.jar mycompartment helloworld-app helloworld-func-go`


## What if my function needs input in binary form ?

This example demonstrates how to invoke a boilerplate function which accepts (an optional) string payload (JSON data). But, it is possible to send binary payload as well.

You can use this Tensorflow based function as an example to explore the possibility of invoking a function using binary content - https://github.com/abhirockzz/fn-hello-tensorflow. This function expects the image data (in binary form) as an input and returns what object that image resembles along with the percentage accuracy

If you were to deploy the Tensorflow function, the command to invoke it using Fn CLI would be something like this - `cat /home/foo/cat.jpeg | fn invoke fn-tensorflow-app classify`. In this case, the `cat.jpeg` image is being passed as an input to the function. The programmatic (using Java SDK) equivalent of this would look something like the below snippet, where the function invocation request (`InvokeFunctionRequest`) is being built along with the binary input (image file content)

```java
InvokeFunctionRequest invokeFunctionRequest = 

InvokeFunctionRequest.builder()
                     .functionId(function.getFunction().getId())
                     .invokeFunctionBody(StreamUtils.toInputStream(new File("/home/foo/cat.jpeg")))
                     .build();
```

Pay attention to the following line `invokeFunctionBody(StreamUtils.toInputStream(new File("/home/foo/cat.jpeg")))`. The `toInputStream` helper method from `com.oracle.bmc.util.StreamUtils` is being used to send the binary contents of file `/home/foo/cat.jpeg`

## Troubleshooting

### If you fail to set the required environment variables like `TENANT_OCID` etc.

You will see the following error - `Exception in thread "main" java.lang.Exception: Please ensure you have set the mandatory environment variables - TENANT_OCID, USER_OCID, PUBLIC_KEY_FINGERPRINT, PRIVATE_KEY_LOCATION`

### If you do not provide required arguments i.e. function name etc.

You will see the following error - `Exception in thread "main" java.lang.Exception: Usage: java -jar <jar-name>.jar <function name> <app name> <compartment name> <function invoke payload>`

### If you provide an invalid value for function name etc.

You will see something similar to - `Exception in thread "main" java.lang.Exception: Could not find function with name test-function in application test-app`

### If you provide an incorrect `TENANT_OCID` or `USER_OCID` or `PUBLIC_KEY_FINGERPRINT`

You will get this error - `Exception in thread "main" com.oracle.bmc.model.BmcException: (401, NotAuthenticated, false) The required information to complete authentication was not provided or was incorrect. (opc-request-id: 974452A5243XXXXX77194672D650/37DFE2AEXXXXXXX20ADFEB2E43/48B235F1D7XXXXXX273CFB889)`

### If your key has a passphrase but you failed to set the environment variable PASSPHRASE

You will get this error - `Exception in thread "main" java.lang.NullPointerException: The provided private key requires a passphrase`

### If your key has a passphrase but you set an incorrect value in the environment variable PASSPHRASE

You will get this error - `Exception in thread "main" java.lang.IllegalArgumentException: The provided passphrase is incorrect.`

