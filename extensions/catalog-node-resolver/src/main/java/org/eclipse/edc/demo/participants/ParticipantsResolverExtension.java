/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.demo.participants;

import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.crawler.spi.TargetNodeFilter;
import org.eclipse.edc.demo.participants.resolver.LazyLoadNodeDirectory;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.io.File;

import static org.eclipse.edc.demo.participants.ParticipantsResolverExtension.NAME;

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
    public TargetNodeDirectory createLazyTargetNodeDirectory() {
        if (nodeDirectory == null) {
            nodeDirectory = new LazyLoadNodeDirectory(typeManager.getMapper(), participantListFile, didResolverRegistry, monitor);
        }
        return nodeDirectory;
    }

    @Provider
    public TargetNodeFilter skipSelfNodeFilter(ServiceExtensionContext context) {
        return targetNode -> {
            var predicateTest = !targetNode.id().equals(context.getParticipantId());
            if (!predicateTest) {
                monitor.debug("Node filter: skipping node '%s' for participant '%s'".formatted(targetNode.id(), context.getParticipantId()));
            }
            return predicateTest;
        };
    }


}
