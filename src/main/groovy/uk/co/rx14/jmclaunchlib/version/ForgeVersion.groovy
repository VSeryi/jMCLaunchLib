package uk.co.rx14.jmclaunchlib.version

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.Synchronized
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import uk.co.rx14.jmclaunchlib.Constants
import uk.co.rx14.jmclaunchlib.caches.MinecraftCaches
import uk.co.rx14.jmclaunchlib.util.Compression

@CompileStatic
class ForgeVersion extends Version {

	private static final Log LOGGER = LogFactory.getLog(ForgeVersion)

	final MinecraftCaches caches

	ForgeVersion(String MCVersion, String ForgeVersion, MinecraftCaches caches) {
        super(MCVersion, caches.versions)
		this.uniqueVersion = ForgeVersion
		this.caches = caches
	}

	@Synchronized
    protected ensureJson() {
        if (uniqueJson) return

        def data
        def paths = [
                "net.minecraftforge:forge:jar:universal:$uniqueVersion",
                "net.minecraftforge:forge:jar:installer:$uniqueVersion"
        ]

        paths.any { path ->
            try {
                def resolvedPath = caches.libs.resolve(path, "$Constants.MinecraftForgeBase")
                data = Compression.extractZipSingleFile(resolvedPath, "version.json")
                return true // Si se extrae con éxito, termina la iteración
            } catch (Exception e) {
            }
        }

        if (!data) {
            throw "The $uniqueVersion version is not supported at the moment."
        }


        this.uniqueJson = Versions.applyParent(new JsonSlurper().parse(new String(data).replaceAll('\\.pack\\.xz$', "").replaceAll("http:", "https:").chars) as Map, caches.versions)
    }

}
