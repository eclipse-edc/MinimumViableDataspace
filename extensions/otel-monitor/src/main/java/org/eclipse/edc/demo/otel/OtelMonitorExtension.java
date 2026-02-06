package org.eclipse.edc.demo.otel;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * Extensión EDC que configura OpenTelemetry para capturar logs.
 *
 * EDC usa su propia interfaz {@link Monitor} para logging, no SLF4J/Logback.
 * Por eso el Java Agent de OpenTelemetry no puede capturar logs automáticamente.
 *
 * Esta extensión:
 * 1. Configura el SDK de OpenTelemetry para enviar logs via OTLP
 * 2. Proporciona un {@link OtelMonitor} que reemplaza el ConsoleMonitor por defecto
 * 3. Así todos los logs de EDC se envían al collector de OpenTelemetry
 *
 * Flujo: EDC components -> Monitor.info() -> OtelMonitor -> OTLP -> Collector -> CloudWatch
 */
@Extension(value = OtelMonitorExtension.NAME)
public class OtelMonitorExtension implements ServiceExtension {

    public static final String NAME = "OpenTelemetry Monitor Extension";

    /**
     * Configuración del endpoint OTLP (dónde enviar los logs).
     * Se puede configurar via variable de entorno: EDC_OTEL_LOGS_ENDPOINT
     * Ejemplo: http://otel-collector.mvd.svc.cluster.local:4317
     */
    @Setting(value = "OTLP endpoint for logs", defaultValue = "http://localhost:4317")
    private static final String OTEL_ENDPOINT = "edc.otel.logs.endpoint";

    /**
     * Nombre del servicio que aparecerá en los logs.
     * Se puede configurar via variable de entorno: EDC_OTEL_LOGS_SERVICE_NAME
     * Ejemplo: consumer-controlplane, madrid-dataplane
     */
    @Setting(value = "Service name for logs", defaultValue = "edc-connector")
    private static final String SERVICE_NAME = "edc.otel.logs.service.name";

    // Referencia al logger provider para poder cerrarlo en shutdown()
    private SdkLoggerProvider loggerProvider;

    // Nuestra implementación de Monitor
    private OtelMonitor otelMonitor;

    @Override
    public String name() {
        return NAME;
    }

    /**
     * Método llamado automáticamente por EDC gracias a @Provider.
     *
     * Cuando EDC necesita un Monitor, busca extensiones con @Provider que devuelvan Monitor.
     * Este método se llama UNA vez durante el arranque, y el Monitor devuelto
     * se usa para TODOS los logs de la aplicación.
     *
     * @param context Contexto de EDC para leer configuración
     * @return OtelMonitor que reemplaza al ConsoleMonitor por defecto
     */
    @Provider
    public Monitor createMonitor(ServiceExtensionContext context) {
        // Leer configuración de variables de entorno
        var endpoint = context.getSetting(OTEL_ENDPOINT, "http://localhost:4317");
        var serviceName = context.getSetting(SERVICE_NAME, context.getRuntimeId());

        // ====================================================================
        // PASO 1: Crear el exportador OTLP
        // ====================================================================
        // Este componente envía los logs via gRPC al collector de OpenTelemetry.
        // Puerto 4317 es el estándar para OTLP/gRPC.
        var logExporter = OtlpGrpcLogRecordExporter.builder()
                .setEndpoint(endpoint)
                .build();

        // ====================================================================
        // PASO 2: Crear el Resource (metadatos del servicio)
        // ====================================================================
        // El Resource contiene información sobre quién genera los logs.
        // "service.name" es el atributo más importante - identifica el servicio.
        var resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), serviceName
                )));

        // ====================================================================
        // PASO 3: Crear el LoggerProvider
        // ====================================================================
        // El LoggerProvider gestiona la creación de loggers y el envío de logs.
        // BatchLogRecordProcessor agrupa logs en batches para enviarlos eficientemente
        // (no envía cada log individualmente, sino en grupos).
        loggerProvider = SdkLoggerProvider.builder()
                .setResource(resource)
                .addLogRecordProcessor(
                        BatchLogRecordProcessor.builder(logExporter).build()
                )
                .build();

        // ====================================================================
        // PASO 4: Crear el SDK de OpenTelemetry
        // ====================================================================
        // El SDK es el punto de entrada principal de OpenTelemetry.
        // Aquí solo configuramos logs, pero también podría tener traces y metrics.
        var openTelemetry = OpenTelemetrySdk.builder()
                .setLoggerProvider(loggerProvider)
                .build();

        // ====================================================================
        // PASO 5: Obtener el Logger de OpenTelemetry
        // ====================================================================
        // Este logger es el que usará OtelMonitor para crear LogRecords.
        // "edc-monitor" es solo un nombre identificativo.
        Logger logger = openTelemetry.getLogsBridge().loggerBuilder("edc-monitor").build();

        // ====================================================================
        // PASO 6: Crear nuestro Monitor personalizado
        // ====================================================================
        // OtelMonitor implementa la interfaz Monitor de EDC.
        // Convierte llamadas como monitor.info("msg") a LogRecords de OpenTelemetry.
        otelMonitor = new OtelMonitor(logger, serviceName);

        // Mensaje informativo (se imprime antes de que el monitor esté listo)
        System.out.println("\u001B[32mINFO OpenTelemetry Monitor configured - sending logs to: " + endpoint + "\u001B[0m");

        return otelMonitor;
    }

    /**
     * Llamado cuando EDC se apaga.
     * Cierra el LoggerProvider para asegurar que todos los logs pendientes se envíen.
     */
    @Override
    public void shutdown() {
        if (loggerProvider != null) {
            loggerProvider.close();
        }
    }
}
