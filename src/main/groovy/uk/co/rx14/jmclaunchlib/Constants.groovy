package uk.co.rx14.jmclaunchlib

import groovy.transform.CompileStatic
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import uk.co.rx14.jmclaunchlib.util.NamedThreadFactory
import uk.co.rx14.jmclaunchlib.util.Task

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@CompileStatic
class Constants {
	public static final String MinecraftManifest = "https://launchermeta.mojang.com/mc/game/version_manifest.json"
	public static final String MinecraftAssetsBase = "https://resources.download.minecraft.net"
	public static final String MinecraftLibsBase = "https://libraries.minecraft.net/"
	public static final String MinecraftForgeBase = "https://maven.minecraftforge.net"

	public static final String[] XZLibs = []

	public static final ExecutorService executor = Executors.newFixedThreadPool(10, new NamedThreadFactory("jMCLaunchLib task thread"))

	public static final Log TaskLogger = LogFactory.getLog(Task)
}
