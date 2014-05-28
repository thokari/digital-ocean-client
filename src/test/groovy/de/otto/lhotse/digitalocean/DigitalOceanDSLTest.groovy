package de.otto.lhotse.digitalocean

import static org.junit.Assert.*

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Test
import org.junit.Before

class DigitalOceanDSLTest {

	def defaultTestDroplets = [
		[id:437345, ip_address:"37.139.24.37", status:"active", private_ip_address:null, name:"xlt-do-160-001", region_id:2, created_at:"2013-09-06T09:19:33Z", locked:false, backups_active:false, size_id:61, image_id:724524],
		[id:437346, ip_address:"37.139.24.84", status:"active", private_ip_address:null, name:"xlt-do-160-003", region_id:2, created_at:"2013-09-06T09:19:34Z", locked:false, backups_active:false, size_id:61, image_id:724524],
		[id:437347, ip_address:"37.139.30.6", status:"active", private_ip_address:null, name:"xlt-do-160-004", region_id:2, created_at:"2013-09-06T09:19:34Z", locked:false, backups_active:false, size_id:61, image_id:724524],
		[id:437348, ip_address:"37.139.24.103", status:"active", private_ip_address:null, name:"xlt-do-160-006", region_id:2, created_at:"2013-09-06T09:19:34Z", locked:false, backups_active:false, size_id:61, image_id:724524],
		[id:477401, ip_address:null, status:"new", private_ip_address:null, name:"name-prefix-00001", region_id:2, created_at:"2013-09-19T13:31:04Z", locked:false, backups_active:false, size_id:62, image_id:724524],
		[id:477402, ip_address:null, status:"new", private_ip_address:null, name:"name-prefix-00002", region_id:2, created_at:"2013-09-19T13:31:04Z", locked:false, backups_active:false, size_id:63, image_id:724524]
	]

	def newlyCreatedDroplets = [
		[id:100000, ip_address:null, status:"new", private_ip_address:null, name:"name-prefix-00003", region_id:2, created_at:"2013-09-19T13:31:04Z", locked:false, backups_active:false, size_id:62, image_id:724524],
		[id:100001, ip_address:null, status:"new", private_ip_address:null, name:"name-prefix-00004", region_id:2, created_at:"2013-09-19T13:31:04Z", locked:false, backups_active:false, size_id:63, image_id:724524],
		[id:100002, ip_address:null, status:"new", private_ip_address:null, name:"name-prefix-00005", region_id:2, created_at:"2013-09-19T13:31:04Z", locked:false, backups_active:false, size_id:63, image_id:724524],
		[id:100003, ip_address:null, status:"new", private_ip_address:null, name:"name-prefix-00006", region_id:2, created_at:"2013-09-19T13:31:04Z", locked:false, backups_active:false, size_id:62, image_id:724524],
		[id:100004, ip_address:null, status:"new", private_ip_address:null, name:"name-prefix-00007", region_id:2, created_at:"2013-09-19T13:31:04Z", locked:false, backups_active:false, size_id:63, image_id:724524],
		[id:100005, ip_address:null, status:"new", private_ip_address:null, name:"name-prefix-00008", region_id:2, created_at:"2013-09-19T13:31:04Z", locked:false, backups_active:false, size_id:63, image_id:724524],
		[id:100006, ip_address:null, status:"new", private_ip_address:null, name:"name-prefix-00009", region_id:2, created_at:"2013-09-19T13:31:04Z", locked:false, backups_active:false, size_id:62, image_id:724524],
		[id:100007, ip_address:null, status:"new", private_ip_address:null, name:"name-prefix-00010", region_id:2, created_at:"2013-09-19T13:31:04Z", locked:false, backups_active:false, size_id:63, image_id:724524],
		[id:100008, ip_address:null, status:"new", private_ip_address:null, name:"name-prefix-00011", region_id:2, created_at:"2013-09-19T13:31:04Z", locked:false, backups_active:false, size_id:63, image_id:724524]
	]

	def testRegions = [
		[id:1, name:"New York 1", slug:"nyc1"],
		[id:2, name:"Amsterdam 1", slug:"ams1"],
		[id:3, name:"San Francisco 1", slug:"sfo1"],
		[id:4, name:"New York 2", slug:"nyc2"]
	]

	def testSizes = [
		[id:66, cost_per_hour:0.00744, disk:20, name:"512MB", cpu:1, slug:null, memory:512, cost_per_month:"5.0"],
		[id:63, cost_per_hour:0.01488, disk:30, name:"1GB", cpu:1, slug:null, memory:1024, cost_per_month:"10.0"],
		[id:62, cost_per_hour:0.02976, disk:40, name:"2GB", cpu:2, slug:null, memory:2048, cost_per_month:"20.0"],
		[id:64, cost_per_hour:0.05952, disk:60, name:"4GB", cpu:2, slug:null, memory:4096, cost_per_month:"40.0"],
		[id:65, cost_per_hour:0.11905, disk:80, name:"8GB", cpu:4, slug:null, memory:8192, cost_per_month:"80.0"],
		[id:61, cost_per_hour:0.2381, disk:160, name:"16GB", cpu:8, slug:null, memory:16384, cost_per_month:"160.0"],
		[id:60, cost_per_hour:0.47619, disk:320, name:"32GB", cpu:12, slug:null, memory:32768, cost_per_month:"320.0"],
		[id:70, cost_per_hour:0.71429, disk:480, name:"48GB", cpu:16, slug:null, memory:49152, cost_per_month:"480.0"],
		[id:69, cost_per_hour:0.95238, disk:640, name:"64GB", cpu:20, slug:null, memory:65536, cost_per_month:"640.0"],
		[id:68, cost_per_hour:1.42857, disk:960, name:"96GB", cpu:24, slug:null, memory:94208, cost_per_month:"960.0"]
	]

	def testImages = [
		[id:653413, name:"XLT Agent (4.2.8)", slug:null, public:false, distribution:"Ubuntu"],
		[id:655320, name:"XLT Agent (4.2.8) agent startup", slug:null, public:false, distribution:"Ubuntu"],
		[id:685503, name:"oracle-j7u25-xlt-428-default", slug:null, public:false, distribution:"Ubuntu"],
		[id:685634, name:"oracle-j7u25-xlt-428-20", slug:null, public:false, distribution:"Ubuntu"]
	]

	def testSshKeys = [
		[id:20786, name:"some fake key"],
		[id:10786, name:"some other key"]
	]

	class MockClient extends DigitalOceanClient {

		public MockClient() {
			this.http = null
			this.client_id = "123"
			this.api_key = "XYZ"
		}

		ConcurrentLinkedQueue currentDroplets = defaultTestDroplets as ConcurrentLinkedQueue

		AtomicInteger newDropletIndex = new AtomicInteger(0)
		Integer newDropletId = 100000
		Integer newDropletEventId = 9999

		def startAddingDroplet(Map args) {
			def rnd = new Random()
			def octet = { "${rnd.nextInt(256)}" }
			def creationTime = rnd.nextInt(2000) + 1000
			Thread.start {
				it.sleep(creationTime)
				currentDroplets.find {
					it.name == args.name
				}.with {
					status = "active"
					ip_address = "37.139.${octet()}.${octet()}"
				}
				println "Droplet ${args.name} coming online"
			}
		}

		@Override
		def getDroplets() {
			currentDroplets as List
		}

		@Override
		def getRegions() {
			testRegions
		}

		@Override
		def getSizes() {
			testSizes
		}

		@Override
		def getImages() {
			testImages
		}

		@Override
		def getSshKeys() {
			testSshKeys
		}

		@Override
		def createDroplet(Map args) {
			currentDroplets.add(newlyCreatedDroplets[newDropletIndex.getAndIncrement()])
			startAddingDroplet(args)
			[
				"id": newDropletId++,
				"name": args.name,
				"image_id": args.image_id,
				"size_id": args.size_id,
				"event_id": newDropletEventId++
			]
		}

		@Override
		def destroyDroplet(id, args) {
			// fake destroy, normally this would return status and event_id only
			getDroplets().find { it.id == id }
		}
	}

	def creationDefaults = [
		region: /Amsterdam 1/,
		image: /oracle-j7u25-xlt-428-default/,
		size: /160/,
		keys: [
			/some fake key/,
			/some other key/
		],
	]

	DigitalOceanDSL digitalOcean

	@Before
	public void setUp() {
		digitalOcean = new DigitalOceanDSL(client: new MockClient(), creationDefaults: creationDefaults)
	}

	@Test
	public void testClientDelegate() {
		def sizes = digitalOcean.sizes
		assertTrue sizes.collect { it.name }.contains("512MB")
	}

	@Test
	public void testCreateQuery() {

		def args = [
			name : "newDroplet",
			size : "5",
			image : "XLT Agent (4.2.8)",
			region : "New York 1",
			keys : [
				"some fake key",
				"some other key"
			],
			networking : false
		]
		def query = digitalOcean.buildCreateQuery(args)

		assertEquals([name:"newDroplet", region_id:1, image_id:653413, size_id:66, ssh_key_ids:"20786,10786", networking:false], query)
	}

	@Test
	public void testGetArgumentsAsIds() {

		def result = digitalOcean.getArgumentsAsIds( [
			region: "New York 1",
			image: "oracle-j7u25-xlt-428-default",
			size: 160,
			keys: ["some fake key"]] )

		assertEquals 1, result.region_id
		assertEquals 685503, result.image_id
		assertEquals 61, result.size_id
		assertEquals( "20786", result.ssh_key_ids )
	}

	@Test
	public void testCreationDefaults() {

		def args = [
			name : "newDropletName"
		]
		def query = digitalOcean.buildCreateQuery(args)

		assertEquals([name:"newDropletName", region_id:2, image_id:685503, size_id:61, ssh_key_ids:"20786,10786", networking:false], query)
	}

	@Test
	public void testFindNewDropletNamesWithSize() {

		def newNames = digitalOcean.findNewDropletNames(3, "xlt-do", "160")

		assertEquals( [
			"xlt-do-160-002",
			"xlt-do-160-005",
			"xlt-do-160-007"
		], newNames )
	}

	@Test
	public void testFindNewDropletNamesWithoutSize() {

		def newNames = digitalOcean.findNewDropletNames(3, "name-prefix")

		assertEquals( [
			"name-prefix-00003",
			"name-prefix-00004",
			"name-prefix-00005"
		], newNames )
	}

	@Test
	public void testFindSingleRegex() {

		def filteredList = digitalOcean.find ip_address : /.139.\d{2}.\d{1}$/
		def ips = filteredList.collect { it.ip_address }

		assertEquals( ["37.139.30.6"], ips )
	}

	@Test
	public void testFindWithDropletArgument() {

		def droplet = digitalOcean.getDroplets()[0]

		def filteredList = digitalOcean.find droplet

		assertEquals( droplet, filteredList[0] )
	}

	@Test
	public void testFindMultipleFilters() {

		def filteredList = digitalOcean.find ip_address : "^37", size: 160
		def ips = filteredList.collect { it.ip_address }

		assertEquals( [
			"37.139.24.37",
			"37.139.24.84",
			"37.139.30.6",
			"37.139.24.103",
		], ips )
	}

	@Test
	public void testFindAll() {

		def filteredList = digitalOcean.find name : ""
		def ids = filteredList.collect { it.id }

		assertEquals( [
			437345,
			437346,
			437347,
			437348,
			477401,
			477402,
		], ids )
	}

	@Test
	public void testEachDroplet() {

		def ips = []
		digitalOcean.eachDroplet { droplet ->
			ips.add(droplet.ip_address)
		}

		assertEquals( [
			"37.139.24.37",
			"37.139.24.84",
			"37.139.30.6",
			"37.139.24.103",
			null,
			null
		], ips )
	}

	@Test
	public void testEachDropletWithFilter() {

		def ips = []
		digitalOcean.eachDroplet( [ip_address : "24"] , { droplet ->
			ips.add(droplet.ip_address)
		})

		assertEquals( [
			"37.139.24.37",
			"37.139.24.84",
			"37.139.24.103"
		], ips )
	}

	@Test
	public void testCreateDroplet() {

		def result = digitalOcean.createDroplet( [
			name:
			"newDropletName",
			region_id:2,
			image_id:685503,
			size_id:61,
			ssh_key_ids:"30786,10786"
		] )

		assertEquals( [
			id: 100000,
			name: "newDropletName",
			image_id: 685503,
			size_id: 61,
			event_id: 9999
		], result )
	}

	@Test
	public void testCreateSingleDropletWithPrefix() {

		def result = digitalOcean.create count: 1, prefix:"name-prefix"

		assertEquals( [
			id: 100000,
			name: "name-prefix-00003",
			image_id: 685503,
			size_id: 61,
			event_id: 9999
		], result )
	}

	@Test
	public void testCreateSingleDropletWithPrefixAndSize() {

		def result = digitalOcean.create count: 1, prefix: "xlt-do", size: "160"

		assertEquals( [
			id: 100000,
			name: "xlt-do-160-002",
			image_id: 685503,
			size_id: 61,
			event_id: 9999
		], result )
	}

	@Test
	public void testCreateManyDropletsWithPrefix() {

		def result = digitalOcean.create count: 3, prefix:"name-prefix"

		def newNames = result*.name.sort()
		def newIds = result*.id.sort()
		def events = result*.event_id.sort()

		assertEquals([
			"name-prefix-00003",
			"name-prefix-00004",
			"name-prefix-00005"
		], newNames)
		assertEquals([100000, 100001, 100002], newIds)
		assertEquals([9999, 10000, 10001], events)
	}

	@Test
	public void testRunScript() {

		def script = """
			getDroplets().collect { it.name }
		"""
		def result = digitalOcean.runScript script
		assertEquals([
			"xlt-do-160-001",
			"xlt-do-160-003",
			"xlt-do-160-004",
			"xlt-do-160-006",
			"name-prefix-00001",
			"name-prefix-00002"
		], result)
	}

	@Test
	public void testCreateAndWait() {

		digitalOcean.create count: 5, prefix: "name-prefix", wait: 4 // seconds

		def states = digitalOcean.droplets*.status

		assertEquals 9, states.groupBy().active.size()
	}

	@Test
	public void testWait() {

		digitalOcean.create count: 5, prefix: "name-prefix"

		Boolean result = digitalOcean.doWait 1, "status", "active"
		assertFalse "for this test to pass make sure the first call to wait does not succeed", result.value

		result = digitalOcean.doWait 5, [ip_address: "37.139"], "status", "active"
		assertTrue "droplet filter did not match within timeout", result
	}

	@Test
	public void testDestroySingleDroplet() {

		def droplet = digitalOcean.find(id: 437345)[0]

		// this is kind of fake, see the mock destroy method
		// normally the method would just return status and event_id
		def destroyedDroplet = digitalOcean.destroy droplet

		assertEquals droplet, destroyedDroplet
	}

	@Test
	public void testDestroyWithFilter() {

		// kind of fake test, see mock destroy method
		def result = digitalOcean.destroy ip_address: "37.139.24"
		assertEquals( [437345, 437346, 437348], result.collect { it.id }.sort() )
	}
}