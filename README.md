digital-ocean-client
====================

Groovy class that wraps the Digital Ocean API.
Also includes a simple DSL with some enhanced features, e.g. wait droplet status.

It's not uploaded to maven yet, so you have to check out this repository and run ./gradlew jar.
This will also produce the groovydoc.
For the tests to pass, set DO_CLIENT_ID and DO_API_KEY as environment variables

Use it like this in a groovy script.

<code>
    def client = new DigitalOceanClient(clientId: "your-client-id", apiKey: "your_api_key", secureClient: false)
    
    def digitalOcean = new DigitalOceanDSL(client: client)
    
    digitalOcean.creationDefaults = [
        size: 20,
        image: "some-image-name",
        region: "New York 1"
        keys: ["some-ssh-key-name"]
    ]
    
    digitalOcean.create count: 2, prefix: "name-prefix", wait: 2 // minutes for droplets to come online
    digitalOcean.eachDroplet( [name: /name-prefix-\d{5}/], { 
        println "${it.name} + ${it.status}"
    } )
</code>
