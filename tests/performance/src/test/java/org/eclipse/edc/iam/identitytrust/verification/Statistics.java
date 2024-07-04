/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.verification;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

public class Statistics {
    public static void runWithStatistics(int numTestruns, Runnable runnable, int sampleSize){
        var clock = Clock.systemUTC();
        var start = clock.millis();
        var runTimes = new ArrayList<Long>();
        for (int i = 0; i < numTestruns; i++) {
            var runStart = clock.millis();
            runnable.run();
            var runEnd = clock.millis();
            runTimes.add(runEnd - runStart);
        }
        var end = clock.millis();
        var duration = end - start;
        System.out.printf("Signing %s VCs %s times%n", sampleSize, numTestruns);
        System.out.println("***************************");
        System.out.println("Overall test run time: " + duration);
        System.out.println("Average test run time: " + mean(runTimes));
        System.out.println("std-dev: " + stdev(runTimes));
        System.out.println("var: " + variance(runTimes));
    }

    public static double mean(List<Long> dataset) {
        var l = dataset.size();
        return dataset.stream().mapToDouble(v -> (double) v).sum() / l;
    }

    public static double variance(List<Long> dataset) {
        return Math.sqrt(stdev(dataset));
    }

    public static double stdev(List<Long> dataset) {
        var mean = mean(dataset);
        var l = dataset.size();
        // calculate the standard deviation
        double standardDeviation = 0.0;
        for (double num : dataset) {
            standardDeviation += Math.pow(num - mean, 2);
        }
        return Math.sqrt(standardDeviation / l);
    }
}
