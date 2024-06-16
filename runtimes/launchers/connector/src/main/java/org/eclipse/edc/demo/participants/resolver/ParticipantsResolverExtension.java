package org.eclipse.edc.demo.participants.resolver;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.demo.participants.resolver.ParticipantsResolverExtension.NAME;

@Extension(value = NAME)
public class ParticipantsResolverExtension implements ServiceExtension {
    public static final String NAME = "MVD Participant Resolver Extension";

    public static final String PARTICIPANT_LIST_FILE_PATH = "edc.mvd.participants.list.file";


    @Inject
    private TypeManager typeManager;

    @Inject
    private DidResolverRegistry didResolverRegistry;

    private File participantListFile;
    private Monitor monitor;
    private TargetNodeDirectory nodeDirectory;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var participantsPath = context.getConfig().getString(PARTICIPANT_LIST_FILE_PATH);
        monitor = context.getMonitor().withPrefix("DEMO");

        participantListFile = new File(participantsPath).getAbsoluteFile();
        if (!participantListFile.exists()) {
            monitor.warning("Path '%s' does not exist. It must be a resolvable path with read access. Will not add any VCs.".formatted(participantsPath));
        }
    }

    @Provider
    public TargetNodeDirectory createLazyTargetNodeDirectory(ServiceExtensionContext context) {
        if (nodeDirectory == null) {
            nodeDirectory = new LazyLoadNodeDirectory(typeManager.getMapper(), participantListFile, didResolverRegistry, monitor);
        }
        return nodeDirectory;
    }


}
