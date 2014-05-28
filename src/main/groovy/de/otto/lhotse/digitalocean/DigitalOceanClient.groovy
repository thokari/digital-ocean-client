package de.otto.lhotse.digitalocean

import groovyx.net.http.RESTClient
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLSocketFactory

import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import java.security.cert.X509Certificate

/**
 * @author Thomas Hirsch
 *
 */
class DigitalOceanClient {

	static String DIGITAL_OCEAN_API_URL = "https://api.digitalocean.com"

	/**
	 * Defaults to true, if set to false the client will accept any certificate
	 */
	Boolean secureClient = true
	protected RESTClient http

	String client_id
	String api_key

	// cache for makeCachedApiRequest
	private Map digitalOceanAssets = [:]
	private Long maxAge = 3000

	// *** ACTION METHODS ***

	/**
	 * @param args map of arguments, must contain the following keys: <br/>
	 * name, size_id, image_id, region_id, ssh_key_ids (optional), private_networking (optional)
	 * 
	 * @return data object containing the response, or an error message
	 */
	def createDroplet(Map args) {
		performAction("/droplets/new", args, "droplet")
	}

	def destroyDroplet(id, args) {
		performAction("/droplets/${id}/destroy", args)
	}

	// **********************

	// *** QUERY METHODS ****
	def getSizeId(costPerMonth) {
		getSizes().find { it.cost_per_month == "${costPerMonth as Double}" }.id
	}

	def getSizeCostPerMonth(id) {
		Double.valueOf( getSizes().find { it.id == id }.cost_per_month )
	}

	def getRegionId(name) {
		try {
			getRegions().find { it.name == name }.id
		} catch (Exception e) {
			throw new Exception("Region ${name} not found!", e)
		}
	}

	def getImageId(name) {
		getImages().find { it.name == name }.id
	}

	def getImageName(id) {
		getImages().find { it.id == id }.name
	}

	def getSshKeyId(name) {
		getSshKeys().find { it.name == name }.id
	}

	def getSshKeyIds(List names) {
		if(names) {
			getSshKeys().findAll { names.contains it.name }.collect { it.id }
		} else {
			[]
		}
	}
	// **********************

	// ** RESOURCE METHODS **
	def getDroplets() {
		getResourceData "droplets"
	}

	def getImages() {
		getResourceData "images"
	}

	def getSshKeys() {
		getResourceData "ssh_keys"
	}

	def getSizes() {
		getResourceData "sizes"
	}

	def getRegions() {
		getResourceData "regions"
	}

	def getDomains() {
		getResourceData "domains"
	}
	// **********************

	/**
	 * @param path (optional) if present, this value will be used to make the request
	 * @param name will be used to request the resource and extract the correct field from the response
	 * 
	 * @return the data object containing the requested resource, or an error message
	 */
	def getResourceData(String path = null, String name) {
		path = path ?: "/${name}"
		def response  = makeCachedApiRequest path
		extractData response, name
	}

	/**
	 * @param path will be used to request action
	 * @param query (optional) map of request parameters
	 * @param name (optional) field name of the response data object
	 * 
	 * @return the response data (or object, if name is provided), or an error message
	 */
	def performAction(String path, Map query = [:], String name = null) {
		def responseData = makeApiRequest path, query
		if(name) {
			extractData responseData, name
		} else {
			responseData
		}
	}

    static def extractData(response, String name) {
		if("OK".equals(response.status)) {
			return response."${name}"
		} else {
			return response.error_message
		}
	}

	/**
	 * Basic method for using the API
	 * 
	 * @param path the request path
	 * @param query (optional) Map of request parameters
	 * @return the data object containing the response or an error message
	 * @throws RuntimeException if the request fails
	 */
	def makeApiRequest(String path, Map query = [:]) {

		Map clientParameters = [ client_id: getClientId(), api_key: getApiKey() ]
		query += clientParameters

		try {
			if(!http) {
				http = createRestClient(secureClient)
			}
			http.get(path: path, query: query).data
			
		} catch (Exception e) {
			throw new RuntimeException("Error requesting ${DIGITAL_OCEAN_API_URL}${path}: ${e.message}", e)
		}
	}

	/**
	 * Wraps a (few seconds) cache around makeApiRequest
	 * 
	 * @param path the request path
	 * @see #makeApiRequest
	 */
	def makeCachedApiRequest(String path) {

		def responseData

		// expire cache
		def now = { System.currentTimeMillis() }
		if(digitalOceanAssets."${path}_created" && (digitalOceanAssets."${path}_created" + maxAge) < now()) {
			digitalOceanAssets.remove path
		}

		// cache hit
		if(digitalOceanAssets."$path") {
			return digitalOceanAssets."$path"
		}

		// get the resource
		responseData = makeApiRequest path

		// make cache entry
		digitalOceanAssets."$path" = responseData
		digitalOceanAssets."${path}_created" = now()

		responseData
	}

	protected RESTClient createRestClient(Boolean secure) {
		if(secure) {
			return createSecureRestClient()
		} else {
			return createInsecureRestClient()
		}
	}

	protected RESTClient createSecureRestClient() {
		new RESTClient(DIGITAL_OCEAN_API_URL)
	}

	protected RESTClient createInsecureRestClient() {
		RESTClient http = createSecureRestClient()
		makeRestClientInsecure http
	}

	protected RESTClient makeRestClientInsecure(RESTClient http) {
		//=== SSL UNSECURE CERTIFICATE ===
		def sslContext = SSLContext.getInstance("SSL")
		sslContext.init(
				null,
				[
					new X509TrustManager() {
						public X509Certificate[] getAcceptedIssuers() { null }
						public void checkClientTrusted(X509Certificate[] certs, String authType) { }
						public void checkServerTrusted(X509Certificate[] certs, String authType) { }
					} ] as TrustManager[],
				new SecureRandom())
		def sf = new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
		def httpsScheme = new Scheme("https", sf, 443)
		http.client.connectionManager.schemeRegistry.register( httpsScheme )
		//================================
		http
	}

	private String getDefaultValueFromPropertyOrSystemEnv(String key) {
		if (System.properties.hasProperty(key)) {
			return System.properties.getProperty(key)
		} else if (System.getenv(key)) {
			return System.getenv(key)
		}
		throw new RuntimeException("Please make sure " + key + " is set as property or system environment variable.")
	}

	String getClientId() {
		client_id ?: getDefaultValueFromPropertyOrSystemEnv("DO_CLIENT_ID")
	}

	String getApiKey() {
		api_key ?: getDefaultValueFromPropertyOrSystemEnv("DO_API_KEY")
	}
}