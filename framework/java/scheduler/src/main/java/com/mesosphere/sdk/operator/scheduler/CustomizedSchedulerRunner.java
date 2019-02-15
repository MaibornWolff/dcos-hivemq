package com.mesosphere.sdk.operator.scheduler;

import com.mesosphere.sdk.config.validate.PodSpecsCannotUseUnsupportedFeatures;
import com.mesosphere.sdk.framework.FrameworkConfig;
import com.mesosphere.sdk.framework.FrameworkRunner;
import com.mesosphere.sdk.metrics.Metrics;
import com.mesosphere.sdk.scheduler.AbstractScheduler;
import com.mesosphere.sdk.scheduler.SchedulerConfig;
import com.mesosphere.sdk.specification.DefaultServiceSpec;
import com.mesosphere.sdk.specification.ServiceSpec;
import com.mesosphere.sdk.specification.yaml.RawServiceSpec;
import com.mesosphere.sdk.state.SchemaVersionStore;
import com.mesosphere.sdk.state.SchemaVersionStore.SchemaVersion;
import com.mesosphere.sdk.storage.Persister;
import com.mesosphere.sdk.storage.PersisterException;

import java.io.File;

/**
 * Sets up and executes the {@link AbstractScheduler} instance.
 */
public final class CustomizedSchedulerRunner implements Runnable {

    private final CustomizedSchedulerBuilder schedulerBuilder;

    private CustomizedSchedulerRunner(CustomizedSchedulerBuilder schedulerBuilder) {
        this.schedulerBuilder = schedulerBuilder;
    }

    /**
     * Builds a new instance using a {@link RawServiceSpec} representing the raw object model of a YAML service
     * specification file.
     *
     * @param rawServiceSpec    the object model of a YAML service specification file
     * @param schedulerConfig   the scheduler configuration to use (usually based on process environment)
     * @param configTemplateDir the directory where any configuration templates are located (usually the parent
     *                          directory of the YAML service specification file)
     * @return a new {@link CustomizedSchedulerRunner} instance, which may be launched with {@link #run()}
     */
    public static CustomizedSchedulerRunner fromRawServiceSpec(
            RawServiceSpec rawServiceSpec,
            SchedulerConfig schedulerConfig,
            File configTemplateDir)
            throws Exception
    {
        return fromSchedulerBuilder(CustomizedDefaultScheduler.newBuilder(
                DefaultServiceSpec.newGenerator(rawServiceSpec, schedulerConfig, configTemplateDir).build(),
                schedulerConfig)
                .setPlansFrom(rawServiceSpec));
    }

    /**
     * Builds a new instance using a {@link ServiceSpec} representing the serializable Java representation of a service
     * specification.
     *
     * @param serviceSpec     the service specification converted to be used by the config store
     * @param schedulerConfig the scheduler configuration to use (usually based on process environment)
     * @return a new {@link CustomizedSchedulerRunner} instance, which may be launched with {@link #run()}
     */
    public static CustomizedSchedulerRunner fromServiceSpec(
            ServiceSpec serviceSpec,
            SchedulerConfig schedulerConfig)
            throws PersisterException
    {
        return fromSchedulerBuilder(CustomizedDefaultScheduler.newBuilder(serviceSpec, schedulerConfig));
    }

    /**
     * Builds a new instance using a {@link CustomizedSchedulerBuilder} instance representing the scheduler logic to be executed.
     *
     * @param schedulerBuilder the (likely customized) scheduler object to be run by the runner
     * @return a new {@link CustomizedSchedulerRunner} instance, which may be launched with {@link #run()}
     */
    public static CustomizedSchedulerRunner fromSchedulerBuilder(CustomizedSchedulerBuilder schedulerBuilder) {
        return new CustomizedSchedulerRunner(schedulerBuilder);
    }

    /**
     * Runs the scheduler. Don't forget to call this!
     * This should never exit, instead the entire process will be terminated internally.
     */
    @Override
    public void run() {
        SchedulerConfig schedulerConfig = schedulerBuilder.getSchedulerConfig();
        ServiceSpec serviceSpec = schedulerBuilder.getServiceSpec();
        Persister persister = schedulerBuilder.getPersister();

        // Check and/or initialize schema version before doing any other storage access:
        new SchemaVersionStore(persister).check(SchemaVersion.SINGLE_SERVICE);

        Metrics.configureStatsd(schedulerConfig);

        FrameworkRunner frameworkRunner = new FrameworkRunner(
                schedulerConfig,
                FrameworkConfig.fromServiceSpec(serviceSpec),
                PodSpecsCannotUseUnsupportedFeatures.serviceRequestsGpuResources(serviceSpec),
                schedulerBuilder.isRegionAwarenessEnabled());

        AbstractScheduler scheduler = schedulerBuilder.build();
        frameworkRunner.registerAndRunFramework(persister, scheduler);
    }
}
