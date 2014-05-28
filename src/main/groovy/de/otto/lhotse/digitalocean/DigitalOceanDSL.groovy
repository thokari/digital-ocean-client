package de.otto.lhotse.digitalocean

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author Thomas Hirsch
 */
class DigitalOceanDSL {

	@Delegate
	DigitalOceanClient client

	/**
	 * A legible map of default values for droplet creation. Keys and value types are:
	 * <ul>
	 *  <li>name: String, <li/>
	 *  <li>region: String, <li/>
	 *  <li>image: String, <li/>
	 *  <li>size: String, <li/>
	 *  <li>keys: List&lt;String&gt;, <li/>
	 *  <li>networking: Boolean</li>
	 * </ul>
	 */
	Map creationDefaults

	private buildCreateQuery(args) {

		args.name = args.name ?: creationDefaults.name
		args.region = args.region ?: creationDefaults.region
		args.image = args.image ?: creationDefaults.image
		args.size = args.size ?: creationDefaults.size
		args.keys = args.keys ?: creationDefaults.keys
		args.networking = args.networking ?: false

		getArgumentsAsIds(args)
	}

	/**
	 * Utility method responsible for converting e.g. "size" into the corresponding "size_id"
	 * 
	 * @param args the arguments to convert 
	 * @return a map with keys [region, size, image, keys] converted to their respective id
	 */
	def getArgumentsAsIds(args) {

		for(field in ["region", "size", "image"]) {
			if(args."$field") {
				args."${field}_id" = "get${field.capitalize()}Id"(args."$field")
				args.remove field
			}
		}

		if(args.keys) {
			args.ssh_key_ids = getSshKeyIds(args.keys).join(",")
			args.remove "keys"
		}

		args
	}

	/**
	 * Finds new droplet names conforming to some provided pattern.
	 * 
	 * @param count how many new names do you want to find?
	 * @param prefix the prefix for droplet names
	 * @param size (optional) will be appended to the prefix
	 * @return a list of new droplet names
	 */
	List findNewDropletNames(count, prefix, size = null) {

		def candidates = getDroplets().findAll {
			it.name.contains(prefix)
		}
		def numbers = []
		candidates.each {
			def matcher
			if(size) {
				matcher = it.name =~ /$prefix-$size-(\d{3})/
			} else {
				matcher = it.name =~ /$prefix-(\d{5})/
			}
			if(matcher.matches()) {
				numbers.add(matcher.group(1))
			}
		}

		def result = []
		def inUse = numbers.collect { it as int }

		def candidate = 1
		while(result.size() < count) {
			if(!inUse.contains(candidate)) {
				result += candidate
			}
			candidate++
		}

		def format
		if(size) {
			format = {
				String.format("$prefix-$size-%03d", it)
			}
		} else {
			format = {
				String.format("$prefix-%05d", it)
			}
		}
		result.collect { format(it) }
	}

	/**
	 * Creates droplets.<br/>
	 * Use wait: &lt;seconds&gt; as a parameter to wait for all successfully<br/>
	 * successfully requested droplets to become active within x minutes.<br/>
	 * Example create count: 5, prefix: "name-prefix", wait: 10
	 * 
	 * @param params (optional) can include "count" and "wait", other parameters<br/>
	 * override creationDefaults
	 * @see #creationDefaults
	 * 
	 * @return a list of creation events/errors, or a single event in case of one droplet 
	 */
	def create(Map args = [:]) {

		def count = args.count ?: 1
		if(count) {
			args.remove "count"
		}

		def wait = args.wait
		if(wait) {
			args.remove "wait"
			createAndWait(count, args, wait)
		} else {
			doCreate(count, args)
		}
	}

	/**
	 * Internal method, used by create and createAndWait for droplet creation<br/>
	 * 
	 * @see #create
	 * @see #createAndWait
	 * @return a list of creation events/errors, or a single event in case of one droplet 
	 */
	private doCreate(Integer count = 1, Map params = [:]) {

		def newNames = findNewDropletNames(count, params.prefix, params.size)
		params.remove("prefix")

		Map args = buildCreateQuery(creationDefaults)
		args.putAll(buildCreateQuery(params))

		def result = [] as ConcurrentLinkedQueue
		def threads = []
		if(newNames) {
			newNames.each { name ->
				threads += Thread.start {
					Map thisArgs = args.clone()
					thisArgs.put("name", name)
					println "Requesting droplet creation: $thisArgs"
					result.offer(createDroplet(thisArgs))
				}
			}
		}
		threads*.join()
		count > 1 ? result as List : result.poll()
	}

	/**
	 * Internal method.
	 * Requests droplet creation and waits (i.e. blocks) until all created droplets are active
	 * 
	 * @param count (optional) amount of droplets to be created, defaults to 1
	 * @param params (optional) overrides any creationDefaults
	 * @param maxWaitMinutes (optional) defaults to 10 
	 * @return a Boolean indicating whether all droplets became active within the timeout
	 * @see #create
	 * @see #creationDefaults
	 */
	private createAndWait(Integer count = 1, Map params = [:], Integer maxWaitMinutes = 10) {

		def creationResult = doCreate(count, params)
		List newIds = (count == 1) ? creationResult.id : creationResult.collect { it.id }

		if(newIds.size() < count) {
			println "WARNING: Only ${newIds.size()} of ${count} droplets were scheduled for creation"
		}

		waitForDropletsProperty(maxWaitMinutes, newIds, "status", "active")
	}

	/**
	 * @param maxWaitSeconds
	 * @param ids the droplets which are to be observed
	 * @param key the droplet property key to observe, e.g. "status"
	 * @param value the value to wait for, e.g. "active"
	 * @return a Boolean indicating whether waiting was successful
	 */
	private waitForDropletsProperty(Integer maxWaitSeconds, List ids, String key, String value) {

		def now = { System.currentTimeMillis() }
		Long start = now()
		Long deadline = start + maxWaitSeconds * 1000
		Long waitingInterval = maxWaitSeconds * 1000
		Boolean success = false

		WAIT: while(true) {

			List observedDroplets = client.getDroplets().findAll { it.id in ids }
			List properties = observedDroplets.collect { it."$key" }

			def readyCount = properties.findAll { it == value }.size()

			if(readyCount == ids.size()) {
				println "Wait successful: $readyCount droplets had $key $value within ${Math.round((now() - start) / 1000)} seconds"
				success = true
				break WAIT
			}

			if(now() > deadline) {
				println "WARNING: Only $readyCount of ${ids.size()} droplets went active within $maxWaitSeconds seconds"
				success = false
				break WAIT
			}

			Thread.sleep waitingInterval
		}
		success
	}

	/**
	 * Wait for droplet properties to have a specified value
	 * 
	 * @param timeout (optional) timeout in seconds, defaults to 60
	 * @param filter look at the find method
	 * @param key the key to look for
	 * @param value the value to compare
	 * @see #find
	 * @return a Boolean indicating whether waiting was successful
	 * 
	 */
	Boolean doWait(Integer timeout = 60, Map filter = [name:""], String key, String value) {

		def ids = find(filter).collect { it.id }
		waitForDropletsProperty(timeout, ids, key, value)
	}

	/**
	 * A method for filtering droplets.
	 * The droplet is added to the result list, when a filter value<br/>
	 * 1. is equal to a droplet property<br/>
	 * 2. is contained in a droplet property<br/>
	 * Negative filters or patterns are not yet implemented.
	 * 
	 * @param filter
	 * @return a list of droplets
	 */
	List find(Map filter = [:]) {

		filter = getArgumentsAsIds(filter)

		def droplets = getDroplets()
		if(filter) {
			filter.each { filterKey, filterValue ->
				def filtered = getDroplets().findAll { (it."$filterKey" =~ filterValue).find() }
				droplets = droplets.intersect(filtered)
			}
		}
		droplets
	}

	/**
	 * Shortcut for find(filter).each { action.call(it) } 
	 * 
	 * @param filter (optional)
	 * @param action
	 */
	void eachDroplet(Map filter = [:], Closure action) {
		def droplets = filter ? find(filter) : getDroplets()
		droplets.each { action.call(it) }
	}

	/**
	 * @param filter the droplets to destroy, can also pass a single droplet as argument
	 * @param scrub Boolean indicating whether to scrub the droplets data, defaults to false
	 * @return the destruction event or an error message, or a list of them
	 */
	def destroy(filter, Boolean scrub = false) {

		if(!filter) {
			throw new RuntimeException("Please sepcify a droplet or droplet filter for safety reasons")
		}
		def filtered = find filter

		def result = [] as ConcurrentLinkedQueue
		def threads = []
		if(filtered) {
			filtered.each { droplet ->
				threads += Thread.start {
					println "Requesting droplet destruction: ${droplet.name}"
					result.offer(destroyDroplet(droplet.id, ["scrub_data": scrub]))
				}
			}
		}
		threads*.join()
		filtered.size() > 1 ? result as List : result.poll()
	}

	private Map dslBinding = [

		create: this.&create,
		doWait: this.&doWait,
		find: this.&find,
		eachDroplet: this.&eachDroplet,
		destroy: this.&destroy,

		client: client,

		getDroplets: { getDroplets() },
		getImages: { getImages() },
		getSizes: { getSizes() },
		getRegions: { getRegions() },
		getKeys: { getSshKeys() },
	]

	/**
	 * Runs a DSL script.
	 * 
	 * @param script as a plain string
	 * @return the evaluation result
	 */
	def runScript(script) {

		def binding = new Binding(dslBinding)
		def shell = new GroovyShell(binding)
		shell.evaluate(script)
	}
}