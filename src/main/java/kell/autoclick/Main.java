package kell.autoclick;

import kell.autoclick.click.AutoClicker;

import java.lang.management.ManagementFactory;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        // On JDK 17+, jnativehook needs --enable-native-access=ALL-UNNAMED.
        // If not present, restart the JVM with the flag.
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        boolean hasNativeAccess = jvmArgs.stream()
                .anyMatch(a -> a.contains("enable-native-access"));

        if (!hasNativeAccess) {
            // Get current java executable and jar path, then relaunch
            String javaExe = ProcessHandle.current().info().command().orElse("java");
            String jarPath = Main.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();

            // Strip leading slash on Windows (e.g. /D:/... -> D:/...)
            if (jarPath.startsWith("/") && jarPath.length() > 2 && jarPath.charAt(2) == ':') {
                jarPath = jarPath.substring(1);
            }

            ProcessBuilder pb = new ProcessBuilder(
                    javaExe,
                    "--enable-native-access=ALL-UNNAMED",
                    "-jar", jarPath
            );
            pb.inheritIO();
            Process p = pb.start();
            System.exit(p.waitFor());
        }

        AutoClicker autoClicker = new AutoClicker();
        autoClicker.start();
    }
}
