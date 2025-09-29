## Java SDK for GTN Embed
<img src="https://img.shields.io/badge/Java-17+-green"/>

This is a lightweight SDK which wraps the REST APIs of the GTN Embed set as documented in the [API Portal](https://developer.globaltradingnetwork.com/rest-api-reference)

### Importing packages

The GTN Embed SDK is available
on [maven central repository](https://central.sonatype.com/artifact/com.gtngroup/gtn-embed-sdk)

Maven:

```xml
<dependency>
    <groupId>com.gtngroup</groupId>
    <artifactId>gtn-embed-sdk</artifactId>
    <version>0.9.1</version>
</dependency>
```

Gradle:

```text
implementation group: 'com.gtngroup', name: 'gtn-embed-sdk', version: '0.4.0'
```

Gradle Short:

```text
implementation 'com.gtngroup:gtn-embed-sdk:0.4.0'
```

Gradle Kotlin:

```kotlin
implementation("com.gtngroup:gtn-embed-sdk:0.4.0")
```

### API Authentication
The GTN Embed uses the notion of Institutions, which represent customers that build their platform upon the GTN Embed.
The end-users of the institution, are referred to as customers.
An institution is able to manage their customers directly and is also able to initiate actions on the user's behalf.

As describe in the [API Portal](https://developer.globaltradingnetwork.com/rest-api-reference) you are required to authenticate
first to the Institution and then as a customer. And resulting keys expire in a certain period, which require
renewing using authentication APIs. However, when using the SDK, key renewing is not required since it is
handled by the SDK in background.

The <code>api_url</code> is the API hub where customers are connected to access the GTN Embed. This URL can change depending on
customer's registered country.

#### Initiating API connection

For a connection to be established, it is required to have the following information

* `API URL`, provided by GTN. Can vary depending on the environment (Sandbox, UAT, Production)
* `App Key`, provided by GTN
* `App Secret`, provided by GTN
* `Institution` Code, provided by GTN
* `Private Key` of the institution, provided by GTN

Following code snippet is to authenticate the **Institution**.<br>
By authenticating the Institution, all endpoints authorised to the Institution token can me accessed  
```java
import com.gtngroup.GTNAPI;
import com.gtngroup.util.Params;
import org.json.JSONObject;


Params params=new Params()
        .setURL("https://sandbox.globaltradingnetwork.com")
        .setAppKey("my-app-key")
        .setAppSecret("my-app-secret")
        .setInstitution("MY-INST-CODE")
        .setInstitutionId(MY-INST-ID-NUMBER)    // for Micro Invest customers only
        .setChannel("DWM")                      // for Micro Invest customers only
        .setPrivateKey("MY-PRIVATE-KEY");

        GTNAPI api=new GTNAPI(params);
        JSONObject status=api.init();
```

authentication **status** is in the format

```json
{
    "http_status": 200, 
    "auth_status": "SUCCESS"
}
```

Once the _**gtnapi.init()**_ is success (i.e. <code>http_code == 200</code>), it is possible to access any **REST** and **Streaming** endpoints (authorised to the Institution) by using the SDK.
Request, response parameter and formats are as per the [API Documentation](https://developer.globaltradingnetwork.com/rest-api-reference)

Since the SDK is just a wrapper to the **REST API**, only following methods are available for API endpoints

* `api.get()` - for HTTP GET endpoints
* `api.post()` - for HTTP POST endpoints
* `api.patch()` - for HTTP UPDATE endpoints
* `api.delete()` - for HTTP DELETE endpoints

and for streaming data

* `api.getMarketDataStreamingService()` - for receiving streaming Trade Data
* `api.Streaming.MarkgetTradeStreamingService()` - for receiving streaming Market Data

### initialising a customer
To initialise (login) a customer, following code snippet can be used (after the Institution login)

```java
response = api.initCustomer("customerNumber");
```

authentication **status** is in the format

```json
{
    "http_status": 200,
    "auth_status": "SUCCESS"
}
```
When HTTP status is 200, the customer is ready to perform any REST endpoint authorised to the customer token.<br>
When calling endpoints on behalf of a customer, all `get()`, `post()`, `patch()` and `delete()` methods should contain the `customerNumber` as the last parameter.

See <u>**Getting customer details**</u> example below

> [!IMPORTANT]
> SDK does not provide anything not supported by the REST API. See [API Documentation](https://developer.globaltradingnetwork.com/rest-api-reference)

## Examples

#### Creating a customer <img src="https://img.shields.io/badge/REST-blue"/>

Call the `HTTP POST` method to the endpoint `customer/account` to create a customer

```java
Params params = new Params()
    .add("referenceNumber", "546446546")
    .add("firstName", "Kevin")
    .add("lastName", "Smith")
    .add("passportNumber", "123456")
    ...
    
JSONObject response = api.post('/trade/bo/v1.2.1/customer/account', params)
System.out.println(response.toString(4));
```

#### Getting customer details <img src="https://img.shields.io/badge/REST-blue"/>

Call the `HTTP Get` method to the endpoint `customer/account` to get customer details

```java
// using the institution token
JSONObject response = api.get("/trade/bo/v1.2.1/customer/account")
System.out.println(response.toString(4));

// using the customer token
JSONObject response = api.get("/trade/bo/v1.2.1/customer/account", customerNumber)
System.out.println(response.toString(4));
```

Response is in the format

```text
{
    "http_status" : 200,  # http status of the api call as per the API documentation
    "response" : {data map}  # response data of the api as per the API documentation
}
```

### Initiate the Trade Data streaming connection <img src="https://img.shields.io/badge/HTTP Streaming-blue"/>

Can initiate the session by passing endpoint, event type and call-back method references

```java
StreamingService ts = api.getTradeStreamingService(new MessageListener() {
    @Override
    public void onOpen() {
                System.out.println("Session opened");
            }
    
    @Override
    public void onMessage(JSONObject message) {
                System.out.println("Message: " + message.toString(4));
            }
    
    @Override
    public void onClose(String closeMessage) {
                System.out.println("Session closed");
            }
    
    @Override
    public void onError(JSONObject message) {
            System.out.println("Error: " + message.toString(4));
    
            }
    });
ts.connect("/trade/sse/v1.2.1", "ORDER");
```

Above connection will respond with Order related events when available.
The sdk will call onMessage and other relevant methods when required with relevant data

### Close the streaming connection

can close the Trade Streaming session by calling

```java
ts.disconnect();
```

### Getting market data

```java
Params search_params = new Params()
    .add("source-id", "DFM")
    .add("keys", "DFM~EMAAR");

    JSONObject response = api.get("/market-data/realtime/keys/data", search_params);
    System.out.println(resp.toString());
```

### Initiate the market data websocket connection

Can initiate the WS session by passing call-back method references

```java
final StreamingService ms = api.getMarketDataStreamingService(new MessageListener() {
    @Override
    public void onOpen() {
        System.out.println("Open");
    }
    
    @Override
    public void onMessage(JSONObject message) {
        System.out.println(message);
    }
    
    @Override
    public void onClose(String closeMessage) {
        System.out.println("On CLose");
    }
    
    @Override
    public void onError(JSONObject message) {
        System.out.println("Error: " + message);
    }
});
ms.connect("/market-data/websocket/price");
```

### close the websocket connection

can close the WS session by calling

```java
ms.disconnect();
```
### terminate the session

The while GTN Embed session will be terminated by calling the following

```java
api.stop();
```
