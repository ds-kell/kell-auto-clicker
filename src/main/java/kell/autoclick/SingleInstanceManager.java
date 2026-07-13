package kell.autoclick;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Prevents multiple app processes and lets a later launch activate the first.
 */
public final class SingleInstanceManager implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(SingleInstanceManager.class.getName());

    private static final String ACTIVATE_COMMAND = "ACTIVATE";
    private static final int LOOPBACK_AUTO_ASSIGN_PORT = 0;
    private static final int ACTIVATION_ATTEMPTS = 10;
    private static final int CONNECT_TIMEOUT_MS = 250;
    private static final int RETRY_DELAY_MS = 100;
    private static final Path LOCK_PATH = Path.of(
            System.getProperty("java.io.tmpdir"),
            "kell-auto-clicker.lock"
    );

    private final FileChannel lockChannel;
    private final FileLock lock;
    private ServerSocket serverSocket;

    private SingleInstanceManager(FileChannel lockChannel, FileLock lock) {
        this.lockChannel = lockChannel;
        this.lock = lock;
    }

    public static SingleInstanceManager tryAcquire() throws IOException {
        FileChannel channel = FileChannel.open(
                LOCK_PATH,
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
        );

        try {
            FileLock lock = channel.tryLock();
            if (lock == null) {
                channel.close();
                return null;
            }
            return new SingleInstanceManager(channel, lock);
        } catch (OverlappingFileLockException _) {
            channel.close();
            return null;
        } catch (IOException e) {
            closeQuietly(channel, "Could not close single-instance lock channel after lock failure");
            throw e;
        }
    }

    public void listenForActivation(Runnable onActivate) throws IOException {
        Objects.requireNonNull(onActivate, "onActivate");

        serverSocket = new ServerSocket();
        try {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(
                    InetAddress.getLoopbackAddress(),
                    LOOPBACK_AUTO_ASSIGN_PORT
            ));

            byte[] port = Integer.toString(serverSocket.getLocalPort()).getBytes(StandardCharsets.UTF_8);
            lockChannel.truncate(0);
            lockChannel.position(0);
            writeFully(lockChannel, ByteBuffer.wrap(port));
            lockChannel.force(true);

            Thread listener = new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try (Socket socket = serverSocket.accept();
                         BufferedReader reader = new BufferedReader(new InputStreamReader(
                                 socket.getInputStream(), StandardCharsets.UTF_8))) {
                        if (ACTIVATE_COMMAND.equals(reader.readLine())) {
                            onActivate.run();
                        }
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            LOG.log(Level.WARNING, "Single-instance listener stopped", e);
                        }
                    } catch (RuntimeException e) {
                        LOG.log(Level.WARNING, "Single-instance activation handler failed", e);
                    }
                }
            }, "single-instance-listener");
            listener.setDaemon(true);
            listener.start();
        } catch (IOException e) {
            closeQuietly(serverSocket, "Could not close single-instance server socket after setup failure");
            throw e;
        }
    }

    public static void activateRunningInstance() {
        // The first process may still be creating its UI and listener.
        for (int attempt = 0; attempt < ACTIVATION_ATTEMPTS; attempt++) {
            if (sendActivationCommand()) {
                return;
            }
            if (!waitBeforeRetry()) {
                return;
            }
        }
    }

    private static boolean sendActivationCommand() {
        try {
            String value = Files.readString(LOCK_PATH, StandardCharsets.UTF_8).trim();
            int port = Integer.parseInt(value);
            InetSocketAddress address = new InetSocketAddress(
                    InetAddress.getLoopbackAddress(),
                    port
            );

            try (Socket socket = new Socket()) {
                socket.connect(address, CONNECT_TIMEOUT_MS);
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        socket.getOutputStream(), StandardCharsets.UTF_8))) {
                    writer.write(ACTIVATE_COMMAND);
                    writer.newLine();
                    writer.flush();
                }
            }
            return true;
        } catch (IOException | NumberFormatException e) {
            LOG.log(Level.FINE, "Waiting for the running app instance to become reachable", e);
            return false;
        }
    }

    private static boolean waitBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
            return true;
        } catch (InterruptedException interrupted) {
            LOG.log(Level.INFO, "InterruptedException", interrupted);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void close() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // The process is already shutting down.
                LOG.log(Level.FINE, "Could not close single-instance server socket", e);
            }
        }
        try {
            lock.release();
        } catch (IOException e) {
            // The process is already shutting down.
            LOG.log(Level.FINE, "Could not release single-instance lock", e);
        }
        try {
            lockChannel.close();
        } catch (IOException e) {
            // The process is already shutting down.
            LOG.log(Level.FINE, "Could not close single-instance lock channel", e);
        }
    }

    private static void closeQuietly(Closeable closeable, String message) {
        try {
            closeable.close();
        } catch (IOException e) {
            LOG.log(Level.FINE, message, e);
        }
    }

    private static void writeFully(FileChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            int bytesWritten = channel.write(buffer);
            if (bytesWritten == 0) {
                throw new IOException("Could not write single-instance activation port");
            }
        }
    }
}
