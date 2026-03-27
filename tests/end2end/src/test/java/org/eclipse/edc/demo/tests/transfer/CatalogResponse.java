/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.demo.tests.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogResponse {

    @JsonProperty("@id")
    private String id;

    @JsonProperty("participantId")
    private String participantId;

    @JsonProperty("dataset")
    private List<Dataset> datasets;

    @JsonProperty("service")
    private List<DataService> services;

    public String getId() { return id; }

    public String getParticipantId() { return participantId; }

    public List<Dataset> getDatasets() { return datasets; }

    public List<DataService> getServices() { return services; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dataset {

        @JsonProperty("@id")
        private String id;

        @JsonProperty("description")
        private String description;

        @JsonProperty("hasPolicy")
        private List<Offer> policies;

        @JsonProperty("distribution")
        private List<Object> distributions;

        public String getId() { return id; }

        public String getDescription() { return description; }

        public List<Offer> getPolicies() { return policies; }

        public List<Object> getDistributions() { return distributions; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Offer {

        @JsonProperty("@id")
        private String id;

        @JsonProperty("obligation")
        private List<Obligation> obligations;

        public String getId() { return id; }

        public List<Obligation> getObligations() { return obligations; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Obligation {

        @JsonProperty("action")
        private String action;

        @JsonProperty("constraint")
        private List<Constraint> constraints;

        public String getAction() { return action; }

        public List<Constraint> getConstraints() { return constraints; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Constraint {

        @JsonProperty("leftOperand")
        private String leftOperand;

        @JsonProperty("operator")
        private String operator;

        @JsonProperty("rightOperand")
        private String rightOperand;

        public String getLeftOperand() { return leftOperand; }

        public String getOperator() { return operator; }

        public String getRightOperand() { return rightOperand; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataService {

        @JsonProperty("@id")
        private String id;

        @JsonProperty("endpointDescription")
        private String endpointDescription;

        @JsonProperty("endpointURL")
        private String endpointUrl;

        public String getId() { return id; }

        public String getEndpointDescription() { return endpointDescription; }

        public String getEndpointUrl() { return endpointUrl; }
    }
}