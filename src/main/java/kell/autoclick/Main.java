package kell.autoclick;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;

import kell.autoclick.click.AutoClicker;

public class Main {

    public static void main(String[] args) throws Exception {
        // jnativehook calls native code. On JDK 17+ this prints warnings;
        // on JDK 24+ (JEP 472) it requires --enable-native-access=ALL-UNNAMED.
        // The packaged JAR sets `Enable-Native-Access: ALL-UNNAMED` in its manifest,
        // so this relaunch is only needed when running from the IDE/classes dir
        // without that VM flag.
        var jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        boolean hasNativeAccess = jvmArgs.stream()
                .anyMatch(a -> a.contains("enable-native-access"));

        if (!hasNativeAccess && shouldRelaunch()) {
            relaunchWithNativeAccess(args);
            return;
        }

        new AutoClicker().start();
    }

    /**
     * Only relaunch on JDK 17+. Older JVMs don't know the flag and would fail to start.
     */
    private static boolean shouldRelaunch() {
        try {
            var version = System.getProperty("java.specification.version", "0");
            // "11", "17", "21", "25", ... (no "1." prefix on modern JDKs)
            int major = Integer.parseInt(version.split("\\.")[0]);
            return major >= 17;
        } catch (Exception e) {
            return false;
        }
    }

    private static void relaunchWithNativeAccess(String[] args) throws Exception {
        var javaExe = ProcessHandle.current().info().command().orElse("java");
        var codeSource = new File(
                Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()
        );

        var cmd = new ArrayList<String>();
        cmd.add(javaExe);
        cmd.add("--enable-native-access=ALL-UNNAMED");

        if (codeSource.isDirectory()) {
            // Running from IDE / classes directory: use the full classpath.
            var classpath = ManagementFactory.getRuntimeMXBean().getClassPath();
            if (classpath == null || classpath.isBlank()) {
                classpath = codeSource.getAbsolutePath();
            }
            cmd.add("-cp");
            cmd.add(classpath);
            cmd.add(Main.class.getName());
        } else {
            // Running from a packaged JAR.
            cmd.add("-jar");
            cmd.add(codeSource.getAbsolutePath());
        }

        Collections.addAll(cmd, args);

        var pb = new ProcessBuilder(cmd).inheritIO();
        var process = pb.start();
        System.exit(process.waitFor());
    }
}
