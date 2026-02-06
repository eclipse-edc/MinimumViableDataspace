package org.eclipse.edc.demo.otel;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import org.eclipse.edc.spi.monitor.ConsoleColor;
import org.eclipse.edc.spi.monitor.Monitor;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Implementación de {@link Monitor} que envía logs a OpenTelemetry via OTLP.
 *
 * Esta clase reemplaza el ConsoleMonitor por defecto de EDC.
 * Cuando cualquier componente de EDC hace:
 *   monitor.info("mensaje");
 *   monitor.warning("algo pasó");
 *   monitor.severe("error crítico", exception);
 *
 * Este OtelMonitor:
 * 1. Convierte el mensaje a un LogRecord de OpenTelemetry
 * 2. Lo envía via OTLP al collector
 * 3. También lo imprime en consola (para kubectl logs)
 *
 * El formato de consola es IDÉNTICO al ConsoleMonitor original de EDC
 * para mantener compatibilidad visual.
 */
public class OtelMonitor implements Monitor {

    // Constantes para los niveles de log (texto que se muestra en consola)
    private static final String SEVERE = "SEVERE";
    private static final String WARNING = "WARNING";
    private static final String INFO = "INFO";
    private static final String DEBUG = "DEBUG";

    /**
     * Logger de OpenTelemetry para crear y enviar LogRecords.
     * Este logger viene configurado por OtelMonitorExtension con:
     * - Endpoint OTLP (ej: http://otel-collector:4317)
     * - Resource con service.name
     * - BatchLogRecordProcessor para envío eficiente
     */
    private final Logger otelLogger;

    /**
     * Nombre del servicio (ej: "consumer-controlplane", "madrid-dataplane").
     * Se incluye como atributo en cada log para poder filtrar.
     */
    private final String serviceName;

    /**
     * Prefijo para la salida de consola (ej: "[consumer-controlplane] ").
     * Igual que el ConsoleMonitor de EDC.
     */
    private final String prefix;

    public OtelMonitor(Logger otelLogger, String serviceName) {
        this.otelLogger = otelLogger;
        this.serviceName = serviceName;
        this.prefix = "[%s] ".formatted(serviceName);
    }

    // ========================================================================
    // Métodos de la interfaz Monitor
    // ========================================================================
    // Cada método delega a log() con el nivel de severidad apropiado.
    // El Supplier<String> permite lazy evaluation del mensaje (solo se evalúa si se va a loguear).

    @Override
    public void severe(Supplier<String> supplier, Throwable... errors) {
        log(Severity.ERROR, SEVERE, supplier, errors);  // severity_number = 17
    }

    @Override
    public void warning(Supplier<String> supplier, Throwable... errors) {
        log(Severity.WARN, WARNING, supplier, errors);  // severity_number = 13
    }

    @Override
    public void info(Supplier<String> supplier, Throwable... errors) {
        log(Severity.INFO, INFO, supplier, errors);     // severity_number = 9
    }

    @Override
    public void debug(Supplier<String> supplier, Throwable... errors) {
        log(Severity.DEBUG, DEBUG, supplier, errors);   // severity_number = 5
    }

    /**
     * Método central que procesa todos los logs.
     *
     * @param severity Nivel de severidad de OpenTelemetry (ERROR, WARN, INFO, DEBUG)
     * @param level    Texto del nivel para la consola ("SEVERE", "WARNING", etc.)
     * @param supplier Función que genera el mensaje (lazy evaluation)
     * @param errors   Excepciones opcionales a incluir
     */
    private void log(Severity severity, String level, Supplier<String> supplier, Throwable[] errors) {
        // sanitizeMessage viene de la interfaz Monitor - reemplaza newlines por espacios
        var message = sanitizeMessage(supplier);

        // ====================================================================
        // PARTE 1: Enviar a OpenTelemetry
        // ====================================================================
        // Construimos un LogRecord con toda la información necesaria.
        // Este LogRecord se serializa a Protobuf y se envía via gRPC al collector.
        LogRecordBuilder builder = otelLogger.logRecordBuilder()
                .setSeverity(severity)                                              // Nivel (ERROR=17, WARN=13, INFO=9, DEBUG=5)
                .setBody(message)                                                   // El mensaje de log
                .setTimestamp(Instant.now())                                        // Cuándo ocurrió
                .setAttribute(AttributeKey.stringKey("service.name"), serviceName); // Quién lo generó

        // Si hay una excepción, añadirla como atributos
        if (errors != null && errors.length > 0 && errors[0] != null) {
            builder.setAttribute(AttributeKey.stringKey("exception.message"), errors[0].getMessage());
            builder.setAttribute(AttributeKey.stringKey("exception.type"), errors[0].getClass().getName());
        }

        // emit() serializa el LogRecord y lo añade al batch para enviar
        builder.emit();

        // ====================================================================
        // PARTE 2: Imprimir en consola (para kubectl logs)
        // ====================================================================
        // Mantenemos el mismo formato que el ConsoleMonitor de EDC:
        // [color][prefix]LEVEL TIMESTAMP mensaje[reset]
        // Ejemplo: [verde][consumer-controlplane] INFO 2024-01-28T10:30:00.123 Starting transfer[reset]
        var time = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        var colorCode = getColorCode(level);

        System.out.println(colorCode + prefix + level + " " + time + " " + message + ConsoleColor.RESET);

        // Si hay excepciones, imprimir el stack trace en color
        if (errors != null) {
            for (var error : errors) {
                if (error != null) {
                    System.out.print(colorCode);
                    error.printStackTrace(System.out);
                    System.out.print(ConsoleColor.RESET);
                }
            }
        }
    }

    /**
     * Devuelve el código de color ANSI para cada nivel de log.
     * Estos colores son los mismos que usa el ConsoleMonitor de EDC.
     *
     * @param level Nivel de log (SEVERE, WARNING, INFO, DEBUG)
     * @return Código ANSI de color
     */
    private String getColorCode(String level) {
        return switch (level) {
            case SEVERE -> ConsoleColor.RED;      // Rojo para errores
            case WARNING -> ConsoleColor.YELLOW;  // Amarillo para warnings
            case INFO -> ConsoleColor.GREEN;      // Verde para info
            case DEBUG -> ConsoleColor.BLUE;      // Azul para debug
            default -> "";
        };
    }
}
