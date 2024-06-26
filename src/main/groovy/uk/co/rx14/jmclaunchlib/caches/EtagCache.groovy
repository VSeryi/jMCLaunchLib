package uk.co.rx14.jmclaunchlib.caches

import groovy.transform.Immutable
import groovy.transform.ToString
import kong.unirest.Unirest
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import uk.co.rx14.jmclaunchlib.exceptions.OfflineException

import javax.xml.ws.http.HTTPException
import java.nio.file.Path

@ToString(includePackage = false, includeNames = true)
@Immutable(knownImmutableClasses = [Path.class])
class EtagCache extends Cache {

	private final static Log LOGGER = LogFactory.getLog(EtagCache)

	final Path storage
	boolean offline

	String getLocalEtag(URL URL) {
		def path = getEtagPath(URL).toFile()
		path.exists() ? path.text : null
	}

	byte[] get(URL URL) {
		LOGGER.trace "[$storage] Getting $URL"

		def filePath = getPath(URL).toFile()
		def etagPath = getEtagPath(URL).toFile()

		if (offline) {
			if (filePath.exists()) {
				LOGGER.trace "[$storage] Offline and exists: returning file"
				return filePath.bytes
			} else {
				throw new OfflineException("[$storage] $URL does not exist in this cache.")
			}
		}

		filePath.parentFile.mkdirs()

		if (etagPath.exists() && !filePath.exists()) {
			LOGGER.debug "[$storage] Etag file $etagPath exists but file $filePath does not: Deleting etag file."
			etagPath.delete()
		}

		def localEtag = getLocalEtag(URL)
		def request = Unirest.get(URL.toString())

		if (localEtag) {
			LOGGER.trace "[$storage] Etag exists, adding header If-None-Match: $localEtag"
			request.header("If-None-Match", localEtag)
		}

		def startTime = System.nanoTime()
		int maxRetries = 5
		int retryCount = 0
		def response
		long waitTime = 1000 // initial wait time in milliseconds

		while (retryCount < maxRetries) {
			try {
				response = request.asBytes()
				break
			} catch (Exception e) {
				retryCount++
				if (retryCount == maxRetries) {
					throw new Exception("Failed after " + maxRetries + " attempts", e)
				}
				Thread.sleep(waitTime)
				waitTime *= 2 // double the wait time for the next iteration
			}
		}
		def time = System.nanoTime() - startTime

		if (response.status == 304) { //Return from cache
			LOGGER.debug "[$storage] $URL returned 304 in ${time / 1000000000}s: using cache"
		} else if (response.status == 200) { //Return from response
			LOGGER.info "Downloading $URL"
			LOGGER.debug "[$storage] $URL returned 200 in ${time / 1000000000}s: caching"
			LOGGER.trace "[$storage] Etag was ${response.headers.getFirst("etag")}"
			etagPath.text = response.headers.getFirst("etag")
			filePath.bytes = response.body
		} else {
			LOGGER.warn "[$storage] $URL returned $response.status in ${time / 1000000000}s: error"
			throw new HTTPException(response.status)
		}
		filePath.bytes
	}

	Path getPath(URL URL) {
		if (URL.file.endsWith("/")) return null
		storage.resolve(URL.path.substring(1).replace("/", "_"))
	}

	Path getEtagPath(URL URL) {
		if (URL.file.endsWith("/")) return null
		new File("${getPath(URL)}.etag").toPath()
	}
}
