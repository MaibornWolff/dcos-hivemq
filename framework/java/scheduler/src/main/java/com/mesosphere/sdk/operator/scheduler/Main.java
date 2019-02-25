
package com.mesosphere.sdk.operator.scheduler;

import com.mesosphere.sdk.scheduler.SchedulerConfig;
import com.mesosphere.sdk.specification.DefaultServiceSpec;
import com.mesosphere.sdk.specification.yaml.RawServiceSpec;

import java.io.File;
import java.util.Arrays;

/**
 * Main entry point for the Scheduler.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected one file argument, got: " + Arrays.toString(args));
        }
        File yamlSpecFile = new File(args[0]);
        CustomizedSchedulerRunner.fromSchedulerBuilder(createSchedulerBuilder(yamlSpecFile))
                .run();
    }

    private static CustomizedSchedulerBuilder createSchedulerBuilder(File yamlSpecFile) throws Exception {

        SchedulerConfig schedulerConfig = SchedulerConfig.fromEnv();
        RawServiceSpec rawServiceSpec = RawServiceSpec.newBuilder(yamlSpecFile).build();
        File configDir = yamlSpecFile.getParentFile();

        DefaultServiceSpec serviceSpec = DefaultServiceSpec
                .newGenerator(rawServiceSpec, schedulerConfig, configDir)
                .build();

        return CustomizedDefaultScheduler.newBuilder(serviceSpec, schedulerConfig)
                .setPlansFrom(rawServiceSpec);


    }

}
