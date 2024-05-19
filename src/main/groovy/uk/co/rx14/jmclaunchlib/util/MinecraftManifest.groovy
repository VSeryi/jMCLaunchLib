package uk.co.rx14.jmclaunchlib.util


import uk.co.rx14.jmclaunchlib.caches.EtagCache

class MinecraftManifest {

    final EtagCache cache

    MinecraftManifest(String MCVersion, EtagCache versionsCache) {
        this.MCVersion = MCVersion
        this.uniqueVersion = MCVersion
        this.cache = versionsCache
    }


    def obtenerUrlClientJar(String version) {
        def manifestResponse = Unirest.get("https://launchermeta.mojang.com/mc/game/version_manifest.json").asJson()

        if (manifestResponse.isSuccess()) {
            def versionData = manifestResponse.getBody().getObject().getJSONArray("versions")

            for (int i = 0; i < versionData.length(); i++) {
                def versionInfo = versionData.getJSONObject(i)
                if (versionInfo.getString("id").equals(version)) {
                    def versionUrl = versionInfo.getString("url")
                    def versionResponse = Unirest.get(versionUrl).asJson()

                    if (versionResponse.isSuccess()) {
                        return versionResponse.getBody().getObject().getJSONObject("downloads").getJSONObject("client").getString("url")
                    } else {
                        println("Error al obtener los detalles de la versión: ${versionResponse.getStatus()}")
                    }
                    break
                }
            }
        } else {
            println("Error al obtener el manifiesto de versiones: ${manifestResponse.getStatus()}")
        }
        return null
    }
}
