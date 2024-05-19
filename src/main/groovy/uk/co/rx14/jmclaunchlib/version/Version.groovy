package uk.co.rx14.jmclaunchlib.version

import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic
import groovy.transform.Synchronized
import uk.co.rx14.jmclaunchlib.Constants
import uk.co.rx14.jmclaunchlib.caches.EtagCache

abstract class Version {

    public String MCVersion;
    protected String uniqueVersion;

    protected Map MCJson
    protected Map uniqueJson

    final EtagCache cache

    protected Version(String mcVersion, EtagCache versionsCache) {
        MCVersion = mcVersion;
        cache = versionsCache;
    }

    @CompileDynamic
    //Groovy bugs
    List getLibs() {
        ensureJson();
        uniqueJson.libraries.clone() as List
    }

    String getAssetsVersion() {
        ensureJson();
        uniqueJson.assets
    }

    String getMinecraftArguments() {
        ensureJson();
        if(uniqueJson.minecraftArguments)   uniqueJson.minecraftArguments
        else uniqueJson.arguments.game.join(" ")

    }

    String getJVMArguments() {
        ensureJson();
        if(uniqueJson.minecraftArguments)   ""
        else uniqueJson.arguments.jvm.join(" ")
    }

    String getMainClass() {
        ensureJson();
        uniqueJson.mainClass
    }

    protected abstract Object ensureJson();

    URL getJarDownloadUrl() {
        ensureMCJson()
        (((MCJson['downloads'] as Map)['client'] as Map).url as String).toURL()
    }

    @Synchronized
    protected ensureMCJson() {
        if (MCJson) return

        this.MCJson = Versions.applyParent(
                new JsonSlurper().parse(new String(
                        cache.get(getJsonURL(MCVersion).toURL())
                ).chars) as Map,
                cache
        )
    }

    URL getAssetsUrl() {
        ensureMCJson()
        return ((MCJson['assetIndex'] as Map).url as String).toURL()
    }

    def String getJsonURL(String version) {
        def manifestResponse = new JsonSlurper().parseText(new String(cache.get("$Constants.MinecraftManifest".toURL()))) as Map
        def versionData = manifestResponse.versions

        return (versionData.find { (it as Map).id == version } as Map)?.url
    }
}
