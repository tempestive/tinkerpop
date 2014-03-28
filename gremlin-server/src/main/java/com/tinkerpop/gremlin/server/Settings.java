package com.tinkerpop.gremlin.server;

import info.ganglia.gmetric4j.gmetric.GMetric;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Server settings as configured by a YAML file.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class Settings {

    /**
     * Host to bind the server to. Defaults to localhost.
     */
    public String host = "localhost";

    /**
     * Port to bind the server to.  Defaults to 8182.
     */
    public int port = 8182;

    /**
     * Size of the worker thread pool.   Defaults to 1 and should generally not exceed 2 * number of cores. A
     * worker thread performs non-blocking read and write for one or more Channels in a non-blocking mode.
     */
    public int threadPoolWorker = 1;

    /**
     * Size of the Gremlin thread pool. This pool handles Gremlin script execution and other related "long-run"
     * processing.  This setting should be sufficiently large to ensure that requests processed by the non-blocking
     * worker threads are processed with limited queuing.  Defaults to 8.
     */
    public int gremlinPool = 8;

    /**
     * Size of the boss thread pool.  Defaults to 1 and should likely stay at 1.  The bossy thread accepts incoming
     * connections on a port until it is unbound. Once a connection is accepted successfully, the boss thread
     * passes processing to the worker threads.
     */
    public int threadPoolBoss = 1;

    /**
     * Time in milliseconds to wait for a script to complete execution.  Defaults to 30000.
     */
    public long scriptEvaluationTimeout = 30000l;

    /**
     * Time in milliseconds to wait while an evaluated script serializes its results. This value represents the
     * total serialization time for the request.  Defaults to 30000.
     */
    public long serializedResponseTimeout = 30000l;

    /**
     * Time in milliseconds to wait while an individual item in a result set is serialized.  Defaults to 200.  If
     * this value is exceeded then the iteration of the result set is aborted with error and no additional results
     * are returned for that request.
     */
    public long serializeResultTimeout = 200l;

    /**
     * The size of the frame queue which contains an individual serialized result (a frame) to be written back
     * to the client on each request. If the frame queue fills (i.e. the server serializes results far faster than
     * it can write them back to the client) then serialization will cease until there is space in the queue.  Note
     * that if there is excessive blocking then the serializedResponseTimeout will be exceeded and the response
     * will produce an error.
     */
    public int frameQueueSize = 256;

    /**
     * The maximum length of the initial line (e.g. {@code "GET / HTTP/1.0"}). Can reduce excessive memory consumption.
     * This setting ties to the Netty {@code HttpRequestDecoder}
     */
    public int maxInitialLineLength = 4096;

    /**
     * The maximum length of all headers.  If the sum of the length of each header exceeds this value. Can reduce
     * excessive memory consumption. This setting ties to the Netty {@code HttpRequestDecoder}
     */
    public int maxHeaderSize = 8192;

    /**
     * The maximum length of the content or each chunk.  If the content length exceeds this value, the transfer
     * encoding of the decoded request will be converted to 'chunked' and the content will be split into multiple
     * {@code HttpContent}s.  If the transfer encoding of the HTTP request is 'chunked' already, each chunk will be
     * split into smaller chunks if the length of the chunk exceeds this value. Can reduce excessive memory
     * consumption. This setting ties to the Netty {@code HttpRequestDecoder}
     */
    public int maxChunkSize = 8192;

    /**
     * The maximum length of the aggregated content for a message.  Works in concert with {@link #maxChunkSize} where
     * chunked requests are accumulated back into a single message.  A request exceeding this size will
     * return a 413 - Request Entity Too Large status code.  A response exceeding this size will raise an internal
     * exception.
     */
    public int maxContentLength = 65536;

    public int maxAccumulationBufferComponents = 1024;

    /**
     * Configured metrics for Gremlin Server.
     */
    public ServerMetrics metrics = null;

    /**
     * {@link Map} of {@link com.tinkerpop.gremlin.structure.Graph} objects keyed by their binding name.
     */
    public Map<String, String> graphs;

    /**
     * {@link Map} of settings for {@code ScriptEngine} setting objects keyed by the name of the {@code ScriptEngine}
     * implementation.
     */
    public Map<String, ScriptEngineSettings> scriptEngines;

    /**
     * {@link List} of maven coordinates stored as a {@link List} of {@link String} objects to be used with the
     * {@code ScriptEngine} instances.
     */
    public List<List<String>> use;

    /**
     * Settings must be instantiated from {@link #read(String)} or {@link #read(java.io.InputStream)} methods.
     */
    private Settings() {
    }

    public Optional<ServerMetrics> optionalMetrics() {
        return Optional.ofNullable(metrics);
    }

    /**
     * Read configuration from a file into a new {@link Settings} object.
     *
     * @param file the location of a Gremlin Server YAML configuration file
     * @return a new {@link Optional} object wrapping the created {@link Settings}
     */
    public static Settings read(final String file) throws Exception {
        final InputStream input = new FileInputStream(new File(file));
        return read(input);
    }

    /**
     * Read configuration from a file into a new {@link Settings} object.
     *
     * @param stream an input stream containing a Gremlin Server YAML configuration
     * @return a new {@link Optional} object wrapping the created {@link Settings}
     */
    public static Settings read(final InputStream stream) {
        Objects.requireNonNull(stream);

        final Constructor constructor = new Constructor(Settings.class);
        final TypeDescription settingsDescription = new TypeDescription(Settings.class);
        settingsDescription.putMapPropertyType("graphs", String.class, String.class);
        settingsDescription.putMapPropertyType("scriptEngines", String.class, ScriptEngineSettings.class);
        settingsDescription.putListPropertyType("use", List.class);
        constructor.addTypeDescription(settingsDescription);

        final TypeDescription scriptEngineSettingsDescription = new TypeDescription(ScriptEngineSettings.class);
        scriptEngineSettingsDescription.putListPropertyType("imports", String.class);
        scriptEngineSettingsDescription.putListPropertyType("staticImports", String.class);
        constructor.addTypeDescription(scriptEngineSettingsDescription);

        final TypeDescription serverMetricsDescription = new TypeDescription(ServerMetrics.class);
        constructor.addTypeDescription(serverMetricsDescription);

        final TypeDescription consoleReporterDescription = new TypeDescription(ConsoleReporterMetrics.class);
        constructor.addTypeDescription(consoleReporterDescription);

        final TypeDescription csvReporterDescription = new TypeDescription(CsvReporterMetrics.class);
        constructor.addTypeDescription(csvReporterDescription);

        final TypeDescription jmxReporterDescription = new TypeDescription(JmxReporterMetrics.class);
        constructor.addTypeDescription(jmxReporterDescription);

        final TypeDescription slf4jReporterDescription = new TypeDescription(Slf4jReporterMetrics.class);
        constructor.addTypeDescription(slf4jReporterDescription);

        final TypeDescription gangliaReporterDescription = new TypeDescription(GangliaReporterMetrics.class);
        constructor.addTypeDescription(gangliaReporterDescription);

        final TypeDescription graphiteReporterDescription = new TypeDescription(GraphiteReporterMetrics.class);
        constructor.addTypeDescription(graphiteReporterDescription);

        final Yaml yaml = new Yaml(constructor);
        return yaml.loadAs(stream, Settings.class);
    }

    /**
     * Settings for the {@code ScriptEngine}.
     */
    public static class ScriptEngineSettings {
        public List<String> imports;
        public List<String> staticImports;
    }

    /**
     * Settings for {@code Metrics} recorded by Gremlin Server.
     */
    public static class ServerMetrics {
        public ConsoleReporterMetrics consoleReporter = null;
        public CsvReporterMetrics csvReporter = null;
        public JmxReporterMetrics jmxReporter = null;
        public Slf4jReporterMetrics slf4jReporter = null;
        public GangliaReporterMetrics gangliaReporter = null;
        public GraphiteReporterMetrics graphiteReporter = null;

        public Optional<ConsoleReporterMetrics> optionalConsoleReporter() {
            return Optional.ofNullable(consoleReporter);
        }

        public Optional<CsvReporterMetrics> optionalCsvReporter() {
            return Optional.ofNullable(csvReporter);
        }

        public Optional<JmxReporterMetrics> optionalJmxReporter() {
            return Optional.ofNullable(jmxReporter);
        }

        public Optional<Slf4jReporterMetrics> optionalSlf4jReporter() {
            return Optional.ofNullable(slf4jReporter);
        }

        public Optional<GangliaReporterMetrics> optionalGangliaReporter() {
            return Optional.ofNullable(gangliaReporter);
        }

        public Optional<GraphiteReporterMetrics> optionalGraphiteReporter() {
            return Optional.ofNullable(graphiteReporter);
        }
    }

    /**
     * Settings for a {@code Metrics} reporter that writes to the console.
     */
    public static class ConsoleReporterMetrics extends IntervalMetrics {
    }


    /**
     * Settings for a {@code Metrics} reporter that writes to a CSV file.
     */
    public static class CsvReporterMetrics extends IntervalMetrics {
        public String fileName = "metrics.csv";
    }

    /**
     * Settings for a {@code Metrics} reporter that writes to JMX.
     */
    public static class JmxReporterMetrics extends BaseMetrics {
        public String domain = null;
        public String agentId = null;
    }

    /**
     * Settings for a {@code Metrics} reporter that writes to the SL4J output.
     */
    public static class Slf4jReporterMetrics extends IntervalMetrics {
        public String loggerName = Slf4jReporterMetrics.class.getName();
    }

    /**
     * Settings for a {@code Metrics} reporter that writes to Ganglia.
     */
    public static class GangliaReporterMetrics extends HostPortIntervalMetrics {
        public String addressingMode = null;
        public int ttl = 1;
        public boolean protocol31 = true;
        public UUID hostUUID = null;
        public String spoof = null;

        public GangliaReporterMetrics() {
            // default ganglia port
            this.port = 8649;
        }

        public GMetric.UDPAddressingMode optionalAddressingMode() {
            if (null == addressingMode)
                return null;

            try {
                return GMetric.UDPAddressingMode.valueOf(addressingMode);
            } catch (IllegalArgumentException iae) {
                return null;
            }
        }
    }

    /**
     * Settings for a {@code Metrics} reporter that writes to Graphite.
     */
    public static class GraphiteReporterMetrics extends HostPortIntervalMetrics {
        public String prefix = "";

        public GraphiteReporterMetrics() {
            // default graphite port
            this.port = 2003;
        }
    }

    public static abstract class HostPortIntervalMetrics extends IntervalMetrics {
        public String host = "localhost";
        public int port;
    }

    public static abstract class IntervalMetrics extends BaseMetrics {
        public long interval = 60000;
    }

    public static abstract class BaseMetrics {
        public boolean enabled = false;
    }
}
