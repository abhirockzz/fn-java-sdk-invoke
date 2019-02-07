# Invoke Oracle Functions using the OCI Java SDK

This example demonstrates how to invoke a function on Oracle Functions using (preview version of) the Oracle Cloud Infrastructure Java SDK

## Pre-requisites

Start by cloning this repository - `git clone https://github.com/abhirockzz/fn-java-sdk-invoke`

Install latest Fn CLI - `curl -LSs https://raw.githubusercontent.com/fnproject/cli/master/install | sh`

### Install preview OCI Java SDK

You need to install the OCI SDK JAR to your local Maven repository first

- Download the preview version of the OCI Java SDK
- Unzip - `unzip oci-java-sdk-preview.zip`
- change into the correct directory - `cd oci-java-sdk-preview`
- Check the name of the SDK JAR in `/lib` directory and configure it as environment variable using `export OCI_SDK_JAR=<SDK_JAR_name>.jar` e.g. `export OCI_SDK_JAR=oci-java-sdk-full-1.3.6-preview1-SNAPSHOT.jar`
- Install the JAR to local Maven repo - `mvn install:install-file -Dfile=lib/$OCI_SDK_JAR -DgroupId=com.oracle.oci.sdk -DartifactId=oci-java-sdk -Dversion=1.3.6-preview1-20190125.203329-3 -Dpackaging=jar`

## Test

### Application and function creation

> For details, please refer [this link](https://github.com/abhirockzz/oracle-functions-hello-worlds/blob/master/golang-hello-world.md)

Create an application if you haven't already - you can use the Oracle Functions console or the Fn CLI e.g. `fn create app mytestapp --annotation oracle.com/oci/subnetIds=<SUBNETS_OCIDs>`

> For the subsequent steps, let's assume your application name is `mytestapp`

Create boilerplate function using `fn init --runtime go --name mytestfunc` and deploy it to Oracle Functions with `fn -v deploy --app mytestapp`

Confirm successful deployment

- check using Fn CLI `fn inspect fn <app-name> <function-name>` e.g. `fn inspect fn mytestapp mytestfunc`
- invoke via Fn CLI - `fn invoke <app-name> <function-name>` e.g. `fn invoke mytestapp mytestfunc`

### Build the JAR and configure environment variables

Change to the correct directory (where you cloned the example) `cd fn-java-sdk-invoke` and then build the JAR using `mvn clean install`

Set environment variables

	export TENANT_OCID=<OCID of your tenancy>
	export USER_OCID=<OCID of the OCI user>
	export PUBLIC_KEY_FINGERPRINT=<public key fingerprint>
	export PRIVATE_KEY_LOCATION=<location of the private key on your machine>
	export PASSPHRASE=<private key passphrase>

> please note that `PASSPHRASE` is optional i.e. only required if your private key has one

e.g. 

	export TENANT_OCID=ocid1.tenancy.oc1..aaaaaaaaydrjd77otncda2xn7qrv7l3hqnd3zxn2u4siwdhniibwfv4wwhtz
	export USER_OCID=ocid1.user.oc1..aaaaaaaavz5efd7jwjjipbvm536plgylg7rfr53obvtghpi2vbg3qyrnrtfa
	export PUBLIC_KEY_FINGERPRINT=42:42:5f:42:ca:a1:2e:58:d2:63:6a:af:42:d5:3d:42
	export PRIVATE_KEY_LOCATION=/Users/foobar/oci_api_key.pem
	export PASSPHRASE=4242


### You can now invoke your function!

`java -jar target/<jar-name>.jar <compartment name> <app name> <function name> <(optional) function invoke payload>`

> the function payload is optional, you can leave it blank (empty payload)

e.g.

`java -jar target/fn-java-sdk-invoke-1.0-SNAPSHOT.jar mycompartment mytestapp mytestfunc {\"name\":\"foobar\"}`

To simulate empty payload, just omit the last argument e.g. `java -jar target/fn-java-sdk-invoke-1.0-SNAPSHOT.jar mycompartment mytestapp mytestfunc`

## Troubleshooting

### If you fail to set the required environment variables like TENANT_OCID etc.

You will see the following error - `Exception in thread "main" java.lang.Exception: Please ensure you have set the mandatory environment variables - TENANT_OCID, USER_OCID, PUBLIC_KEY_FINGERPRINT, PRIVATE_KEY_LOCATION`

### If you do not provide required arguments i.e. function name etc.

You will see the following error - `Exception in thread "main" java.lang.Exception: Usage: java -jar <jar-name>.jar <function name> <app name> <compartment name> <function invoke payload>`

### If you provide an invalid value for function name etc.

You will see something similar to - `Exception in thread "main" java.lang.Exception: Could not find function with name test-function in application test-app`
