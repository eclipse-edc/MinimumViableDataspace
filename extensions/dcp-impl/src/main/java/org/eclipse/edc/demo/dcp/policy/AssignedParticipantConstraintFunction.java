/*
 *  Copyright (c) 2025 MinimumViableDataspace Contributors
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       MinimumViableDataspace Contributors - initial API and implementation
 *
 */

package org.eclipse.edc.demo.dcp.policy;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Constraint function que valida que el participante que intenta acceder
 * sea el participante asignado en la política.
 *
 * Esta función permite crear políticas donde solo ciertos participantes
 * específicos pueden ver y negociar contratos para un asset.
 *
 * Ejemplo de uso en política con un solo participante asignado:
 *
 * {
 *   "leftOperand": "AssignedParticipant",
 *   "operator": "eq",
 *   "rightOperand": "did:web:consumer-identityhub%3A7083:consumer"
 * }
 *
 *
 * Ejemplo con múltiples participantes asignados:
 *
 * {
 *   "leftOperand": "AssignedParticipant",
 *   "operator": "isAnyOf",
 *   "rightOperand": "did:web:consumer1-identityhub%3A7083:consumer1,did:web:consumer2-identityhub%3A7083:consumer2"
 * }
 *
 *
 * Ejemplo de blacklist (todos excepto estos):
 *
 * {
 *   "leftOperand": "AssignedParticipant",
 *   "operator": "isNoneOf",
 *   "rightOperand": "ddid:web:consumer-identityhub%3A7083:consumer"
 * }
 *
 */

public class AssignedParticipantConstraintFunction<C extends ParticipantAgentPolicyContext>
        implements AtomicConstraintRuleFunction<Permission, C>  {

    public static final String ASSIGNED_PARTICIPANT_KEY = "AssignedParticipant";

    private AssignedParticipantConstraintFunction() {
    }

    public static <C extends ParticipantAgentPolicyContext> AssignedParticipantConstraintFunction<C> create() {
        return new AssignedParticipantConstraintFunction<>();
    }

    /**
     * Evalúa si el participante actual está asignado a esta política.
     *
     * @param operator El operador de comparación (soporta: eq, isAnyOf, isNoneOf)
     * @param rightOperand El DID del participante asignado o lista separada por comas
     * @param permission El permiso que se está evaluando
     * @param context El contexto de la política con información del participante
     * @return true si el participante está autorizado según la asignación, false en caso contrario
     */

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Permission permission, C context) {
        // Valida que el operador sea soportado
        if (!isSupportedOperator(operator)) {
            context.reportProblem("Unsupported operator '%s' for AssignedParticipant. Supported: eq, isAnyOf, isNoneOf".formatted(operator));
            return false;
        }

        // Obtener participante del contexto
        var participantAgent = context.participantAgent();
        if (participantAgent == null) {
            context.reportProblem("No ParticipantAgent found in policy context");
            return false;
        }

        // Obtener la identidad del participante (su DID)
        var participantId = participantAgent.getIdentity();
        if (participantId == null || participantId.isBlank()) {
            context.reportProblem("ParticipantAgent does not have an identity (DID)");
            return false;
        }

        // Parsear los participantes asignados del rightOperand
        var assignedParticipants = parseRightOperand(rightOperand);
        if (assignedParticipants.isEmpty()) {
            context.reportProblem("No assigned participants found in rightOperand");
            return false;
        }

        // Evaluar según el operador
        return switch (operator) {
            case EQ -> evaluateEquals(participantId, assignedParticipants, context);
            case IS_ANY_OF -> evaluateIsAnyOf(participantId, assignedParticipants, context);
            case IS_NONE_OF -> evaluateIsNoneOf(participantId, assignedParticipants, context);
            default -> {
                context.reportProblem("Unsupported operator: %s".formatted(operator));
                yield false;
            }
        };
    }

    /**
     * Evalúa IS_NONE_OF: el participante NO debe estar en la lista de bloqueados.
     * Permite a todos los participantes EXCEPTO los que están en la lista.
     * El participante actual NO debe coincidir con ninguno de los DIDs en la lista.
     */
    private boolean evaluateIsNoneOf(String participantId, List<String> blockedParticipants, C context) {
        boolean matches = blockedParticipants.stream()
                .noneMatch(blocked -> Objects.equals(participantId, blocked));

        if (!matches) {
            context.reportProblem(
                    "Participant '%s' is in the blocked participants list: %s"
                            .formatted(participantId, blockedParticipants)
            );
        }
        return matches;
    }

    /**
     * Evalúa IS_ANY_OF: el participante debe estar en la lista de asignados.
     * Permite múltiples participantes asignados. El participante actual debe
     * coincidir con al menos uno de los DIDs en la lista.
     */
    private boolean evaluateIsAnyOf(String participantId, List<String>  assignedParticipants, C context) {
        boolean matches = assignedParticipants.stream()
                .anyMatch(assigned -> Objects.equals(participantId, assigned)
                );

        if (!matches) {
            context.reportProblem(
                    "Participant '%s' is not in the list of assigned participants: %s"
                            .formatted(participantId, assignedParticipants)
            );
        }
        return matches;
    }

    /**
     * Evalúa EQ: el participante debe ser exactamente el asignado.
     * Este operador espera un único participante en el rightOperand.
     */
    private boolean evaluateEquals(String participantId, List<String> assignedParticipants, C context) {
        // Para eq, solo debería haber un participante asignado
        if (assignedParticipants.size() > 1) {
            context.reportProblem("Operator 'eq' expects a single assigned participant, but got %d. Use 'isAnyOf' for multiple participants.");
            return false;
        }

        String assignedParticipant = assignedParticipants.get(0);
        boolean matches = Objects.equals(participantId, assignedParticipant);

        if (!matches) {
            context.reportProblem("Participant '%s' is not the assigned participant. Expected: '%s'".formatted(participantId, assignedParticipant)
            );
        }

        return matches;

    }

    /**
     * Parsea el rightOperand a una lista de DIDs de participantes asignados.
     * Acepta formatos:
     *   - Un solo DID: "did:web:localhost%3A7083"
     *   - Múltiples DIDs separados por coma: "did:web:consumer1,did:web:consumer2"
     */
    private List<String> parseRightOperand(Object rightOperand) {
        if (rightOperand == null) {
            return List.of();
        }

        String rightOpStr = rightOperand.toString();
        return Arrays.stream(rightOpStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Verifica si el operador es soportado por esta función.
     */
    private boolean isSupportedOperator(Operator operator) {
        return operator == Operator.EQ ||
                operator == Operator.IS_ANY_OF ||
                operator == Operator.IS_NONE_OF;
    }
}
