package uk.co.rx14.jmclaunchlib;

import uk.co.rx14.jmclaunchlib.util.ChangePrinter;

import java.io.*;
import java.util.function.Supplier;

public class JavaTest {
	public static void main(String[] args) {
		System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "info");
		final LaunchTask task = new LaunchTaskBuilder()
				.setCachesDir("data")
//			.setMinecraftVersion("1.7.10")
				.setForgeVersion("1.7.10", "1.7.10-10.13.4.1614-1.7.10")
				.setInstanceDir("minecraft")
			.setUsername("RX14")
			.setOffline()
			.build();

		new ChangePrinter(
			new Supplier<String>() {
				public String get() {return "" + task.getCompletedPercentage();}
			}
			, 100
		).start();

		LaunchSpec spec = task.getSpec();

		//Process run = spec.run(new File("C:/Program Files/Java/jdk-21.0.2/bin/javaw.exe").toPath());
		Process run = spec.run(new File("C:/Program Files/Java/jre1.8.0_341/bin/javaw.exe").toPath());
		try {
			InputStream mergedStream = new SequenceInputStream(run.getInputStream(), run.getErrorStream());
			BufferedReader reader = new BufferedReader(new InputStreamReader(mergedStream));
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
