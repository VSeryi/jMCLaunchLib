package uk.co.rx14.jmclaunchlib.version


import groovy.transform.CompileStatic
import groovy.transform.Synchronized
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import uk.co.rx14.jmclaunchlib.caches.EtagCache

@CompileStatic
class MinecraftVersion extends Version {

	private static final Log LOGGER = LogFactory.getLog(MinecraftVersion)

	MinecraftVersion(String MCVersion, EtagCache versionsCache) {
		super(MCVersion, versionsCache)
		this.uniqueJson = MCJson
		this.uniqueVersion = MCVersion
	}

	@Synchronized
	protected ensureJson() {
		if (uniqueJson) return
		ensureMCJson()
		this.uniqueJson = MCJson
	}

}
