package uk.co.rx14.jmclaunchlib.util;

import groovy.transform.CompileStatic;

import java.util.Locale;

@CompileStatic
enum OS
{
	LINUX("linux", "bsd", "unix"),
	WINDOWS("windows", "win"),
	OSX("osx", "mac"),
	UNKNOWN("unknown");

	private String name;
	private String[] aliases;

	private OS(String name, String... aliases) {
		this.name = name;
		this.aliases = aliases;
	}

	public static String getVERSION() {
		return System.getProperty("os.version");
	}

	public static OS getCURRENT() {
		return fromString(System.getProperty("os.name").toLowerCase(Locale.US));
	}

	public static OS fromString(String osName) {
		for (OS os : values()) {
			if (osName.contains(os.name)) return os;
			for (String alias : os.aliases) {
				if (osName.contains(alias)) return os;
			}
		}
		return UNKNOWN;
	}

	@Override
	public String toString() {
		return name;
	}
}
