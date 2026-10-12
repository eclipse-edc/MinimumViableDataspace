package eu.europeana.dsp.connector.controlplane.catalog.spi.service;

import eu.europeana.dsp.connector.controlplane.catalog.spi.definitions.EuropeanaDcatDistribution;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// this class extracts the distribution metadata from the asset properties.
// so distribution.distributionId:title will become title
public class DistributionMetadataExtractor {

    private static final Map<String, String> DISTRIBUTION_FORMAT_BY_TYPE = Map.of(
            "HttpData", "HttpData-PULL"
    );

    private static final String DISTRIBUTION_PREFIX   = "distribution.";
    private static final Pattern DISTRIBUTION_PATTERN =
            Pattern.compile("distribution\\.(\\d+)\\.");

    public static int getDistributionCount(Map<String, Object> properties) {
        return properties.keySet().stream()
                .map(DISTRIBUTION_PATTERN::matcher)
                .filter(Matcher::find)
                .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                .max()
                .orElse(0);
    }

    public static EuropeanaDcatDistribution buildDistribution(Map<String, Object> properties, String distributionId) {
        if (distributionId == null || properties == null) {
            return null;
        }
        var prefix = DISTRIBUTION_PREFIX + distributionId + ".";
        Map<String, Object> distributionProperties = new HashMap<>();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String propertyName = entry.getKey();
            Object value = entry.getValue();
            if (!propertyName.contains(prefix)) {
                continue;
            }
            var metadataProperty = StringUtils.substringAfter(propertyName, prefix);
            distributionProperties.put(metadataProperty, value);
        }
        return new EuropeanaDcatDistribution(distributionId, distributionProperties);
    }


    /**
     * Retrieves the distribution format based on the type of the given {@link DataAddress}.
     *
     * @param dataAddress the {@link DataAddress} containing the type information. Can be null.
     * @return the distribution format associated with the type of the {@link DataAddress}, or an empty string if
     *         the type is null, not found, or no corresponding format exists.
     */
    public static String getDistributionFormat(DataAddress dataAddress) {
        return Optional.ofNullable(dataAddress)
                .map(DataAddress::getType)
                .map(DISTRIBUTION_FORMAT_BY_TYPE::get)
                .orElse("");
    }

}