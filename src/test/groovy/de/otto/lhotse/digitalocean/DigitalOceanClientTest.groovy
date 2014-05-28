package de.otto.lhotse.digitalocean

import static org.junit.Assert.*

import org.junit.Test
import org.junit.Before

class DigitalOceanClientTest {

	DigitalOceanClient client

	@Before
	public void setUp() {
		client = new DigitalOceanClient(secureClient: false)
	}

	@Test
	public void testMakeApiRequest() {

		def data = client.makeApiRequest("/sizes")

		assertEquals "OK", data.status
		assertNotNull data.sizes[0].cost_per_month
	}

	@Test
	public void testMakeCachedApiRequest() {

		def data = client.makeCachedApiRequest "/images"

		assertEquals "OK", data.status
		assertEquals "Ubuntu", data.images[0].distribution

		def id = data.images[0].id
		data = client.makeCachedApiRequest "/images/${id}"

		assertEquals "OK", data.status
		assertEquals "Ubuntu", data.image.distribution
	}

	@Test
	public void testGetResourceData() {

		def sizes = client.getResourceData "sizes"

		assertEquals 1, sizes.find { it.cost_per_month == "5.0" }.cpu
		assertEquals "1GB", sizes.find { it.memory == 1024 }.name
		assertEquals 16384, sizes.find { it.cost_per_month == "160.0" }.memory
	}

	@Test
	public void testGetResourceDataWithPath() {

		def data = client.makeCachedApiRequest "/images"
		assertEquals "OK", data.status

		def id = data.images[0].id
		def image = client.getResourceData "/images/${id}", "image"

		assertEquals "Ubuntu", image.distribution
	}

	@Test
	public void testGetResourceDataError() {

		def data = client.getResourceData "/images/unknown", "image"
		assertEquals "No Image Found", data
	}

	@Test
	public void testGetDroplets() {

		def droplets = client.droplets
		assertNotNull droplets[0].created_at
	}

	@Test
	public void testGetRegions() {

		def regions = client.regions
		assertNotNull regions.find { it.name.contains("New York") }
	}

	@Test
	public void testGetSizes() {

		def sizes = client.sizes
		assertNotNull sizes[0].cost_per_month
	}

	@Test
	public void testGetImages() {

		def images = client.images
		assertNotNull images[0].name
	}

	@Test
	public void testGetSshKeys() {

		def keys = client.sshKeys
		assertNotNull keys[0].name
	}

	@Test
	public void testGetDomains() {
		def domains = client.domains
		assertEquals([], domains)
	}

	@Test
	public void testGetSizeId() {

		def id = client.getSizeId(5.0)
		assertTrue id > 0
	}

	@Test
	public void testGetSizeCostPerMonth() {

		def id = client.getSizeId(5.0)
		assertTrue id > 0

		def cost = client.getSizeCostPerMonth(id)
		assertEquals(5.0, cost, 0.01)
	}

	@Test
	public void testGetRegionId() {
		def id = client.getRegionId("New York 1")
		assertEquals 1, id
	}

	@Test
	public void testGetSshKeyIds() {
		def ids = client.getSshKeyIds(["createMeToMakeThisTestFail"])
		assertEquals( [], ids )
	}
}
