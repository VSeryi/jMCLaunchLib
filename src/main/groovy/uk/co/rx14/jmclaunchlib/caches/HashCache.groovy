package uk.co.rx14.jmclaunchlib.caches

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ImmutableOptions
import groovy.transform.ToString
import groovy.transform.TypeCheckingMode
import kong.unirest.Unirest
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import uk.co.rx14.jmclaunchlib.exceptions.OfflineException

import java.nio.file.Files
import java.nio.file.Path

/**
 * Implements a hash-addressed filesystem cache.
 *
 * The files are stored by hex-encoded in the format
 * {@code ab/ab615a912fb8ea06648836e0ec1cbeeefe117da6}
 */
@CompileStatic
@ToString(includePackage = false, includeNames = true)
@Immutable(knownImmutableClasses = [Path.class])
@ImmutableOptions(knownImmutables=['storage'])
class HashCache extends Cache {

	private final static Log LOGGER = LogFactory.getLog(HashCache)

	final Path storage
	boolean offline

	/**
	 * Stores the given data in the cache and returns it's hash.
	 *
	 * @param data the data to store.
	 * @return the sha1 hash
	 */
	String store(byte[] data) {
		def hash = DigestUtils.sha1Hex(data)
		def file = getPath(hash).toFile()


		if (!file.exists()) {
			LOGGER.trace "[$storage] Storing $hash in $file"
			file.parentFile.mkdirs()
			file.bytes = data
		} else {
			LOGGER.trace "[$storage] Not storing $hash in $file: already exists"
		}

		hash
	}

	/**
	 * Return the content of the file with the specified hash, or null if not
	 * in the cache.
	 *
	 * @param hash tha sha1 hash to look up.
	 * @return the data.
	 */
	byte[] get(String hash) {
		def file = getPath(hash).toFile()

		if (file.exists()) {
			LOGGER.trace "[$storage] Got $hash from cache"
			file.bytes
		} else {
			LOGGER.trace "[$storage] Not getting $hash from cache: doesn't exist"
			null
		}
	}

	/**
	 * Returns true if the cache has the given hash
	 *
	 * @param hash
	 * @return
	 */
	boolean has(String hash) {
		getPath(hash).toFile().exists()
	}

	/**
	 * Calculates the path to the file with the given hash. If the path is in
	 * a mirror, that path is returned. Otherwise the path in the main storage
	 * is returned.
	 *
	 * Note that this will return a {@link Path}, whether it exists or not.
	 *
	 * @param hash the sha1 hash to calculate the path for.
	 * @return the path.
	 */
	Path getPath(String hash) {
		def pre = hash.substring(0, 2)
		return storage.resolve("$pre/${hash}")
	}

	/**
	 * Downloads the given URL and places it in the
	 *
	 * @param URL
	 * @return
	 */
	@CompileStatic(TypeCheckingMode.SKIP)
	DataHashPair download(URL URL) {
		if (offline) throw new OfflineException("[$storage] Can't download $URL")

		LOGGER.info "Downloading $URL"
		LOGGER.trace "[$storage] Downloading $URL"

		byte[] data = URL.bytes
		new DataHashPair(store(data), data)
	}

	/**
	 * Gets the contents of the URL given and stores it in the cache while
	 * returning the data. If it already exists in the cache, it uses the
	 * cached value.
	 *
	 * @param hash the hash of the file to download
	 * @param URL
	 * @return the data
	 */
	byte[] download(String hash, URL URL) {
		def file = getPath(hash).toFile()
		if (file.exists()) {
			return file.bytes
		}

		LOGGER.trace "[$storage] Downloading $URL with expected hash $hash"
		_download(hash, URL)
	}

	/**
	 * Gets the contents of the URL given and stores it in the cache. This
	 * method does not return the data.
	 *
	 * For data already in the cache this is significantly faster because we
	 * do not need to read in the file contents.
	 *
	 * @param hash
	 * @param URL
	 */
	void preDownload(String hash, URL URL) {
		def file = getPath(hash).toFile()
		if (!file.exists())  {
			LOGGER.trace "[$storage] Predownloading $URL with expected hash $hash"
			_download(hash, URL)
		}
	}

	/**
	 * Copies all files from the other cache to the current cache. This method
	 * does not trust the contents of the cache therefore simply uses
	 * {@link #store} to save the contents of each file into the cache.
	 *
	 * @param otherCache the path to the cache to copy
	 */
	@Override
	void copyFrom(Path otherCache) {
		LOGGER.debug "[$storage] Copying from $otherCache"
		def startTime = System.nanoTime()
		Files.walk(otherCache)
		     .filter(Files.&isRegularFile)
		     .forEach { Path path ->
		         store(path.bytes)
		     }
		def time = System.nanoTime() - startTime
		LOGGER.debug "[$storage] Copy finished in ${time / 1000000000}s"
	}

	/**
	 * Copies all files from another cache to this one. This variant is faster
	 * because it trusts the cache to be correct and simply copies the files directory.
	 *
	 * @param trustedCache the path to the cache to copy
	 */
	@Override
	void copyFromTrusted(Path trustedCache) {
		if (!storage.toFile().exists()) {
			LOGGER.debug "[$storage] Not verifying cache: storage does not exist"
			return
		}
		LOGGER.debug "[$storage] Copying from trusted cache $trustedCache"
		def startTime = System.nanoTime()
		Files.walk(trustedCache)
		     .filter(Files.&isRegularFile)
		     .filter { !has(it.toFile().name) }
		     .forEach { Path path ->
		         def destination = getPath(path.toFile().name)
		         destination.toFile().parentFile.mkdirs()
		         Files.copy(path, destination)
		     }
		def time = System.nanoTime() - startTime
		LOGGER.debug "[$storage] Trusted copy finished in ${time / 1000000000}s"

	}

	void verify(VerificationAction action) {
		if (!storage.toFile().exists()) {
			LOGGER.debug "[$storage] Not verifying cache: storage does not exist"
			return
		}
		LOGGER.debug "[$storage] Verifying cache"
		def startTime = System.nanoTime()
		Files.walk(storage)
		     .filter(Files.&isRegularFile)
		     .filter { DigestUtils.sha1Hex(it.bytes) != it.toFile().name }
		     .forEach { Path path ->
		         LOGGER.warn "[$storage] File $path did not match expected hash: ${action.action} file."
		         switch (action) {
		             case VerificationAction.REHASH:
		                 store(path.bytes)
		             case VerificationAction.DELETE:
		                 path.toFile().delete()
		         }
		     }
		def time = System.nanoTime() - startTime
		LOGGER.debug "[$storage] Verified cache in ${time / 1000000000}s"
	}

	private byte[] _download(String hash, URL URL) {
		if (offline) throw new OfflineException("[$storage] Can't download $URL")
		LOGGER.info "Downloading $URL"
		byte[] data = Unirest.get(URL.toString())
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3").asBytes().body

		String downloadedHash = store(data)

		if (downloadedHash != hash) {
			getPath(downloadedHash).toFile().delete()
			throw new InvalidResponseException("$URL did not match hash \"$hash\" but instead \"$downloadedHash\"")
		}

		data
	}

	/**
	 * Indicates that the response received from an external source did not
	 * match the expected hash.
	 */
	static class InvalidResponseException extends RuntimeException {
		InvalidResponseException() {
			super()
		}

		InvalidResponseException(String message) {
			super(message)
		}
	}

	@Immutable
	static class DataHashPair {
		String hash
		byte[] data

		boolean verify() {
			DigestUtils.sha1Hex(data) == hash
		}
	}

	enum VerificationAction {
		REHASH("rehashing"), DELETE("deleting")

		String action

		VerificationAction(String action) {
			this.action = action
		}
	}
}
