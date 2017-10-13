package com.github.spotbugs.internal.spotbugs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.reporting.internal.CustomizableHtmlReportImpl;
import org.gradle.util.CollectionUtils;

import com.github.spotbugs.SpotBugsReports;
import com.github.spotbugs.internal.SpotBugsReportsImpl;

public class SpotBugsSpecBuilder
{
    private static final Set<String> VALID_EFFORTS = createImmutableSet("min", "default", "max");
    private static final Set<String> VALID_REPORT_LEVELS = createImmutableSet("experimental", "low", "medium", "high");

    private FileCollection pluginsList;
    private FileCollection sources;
    private FileCollection classpath;
    private final FileCollection classes;
    private SpotBugsReports reports;

    private String effort;
    private String reportLevel;
    private String maxHeapSize;
    private Collection<String> visitors;
    private Collection<String> omitVisitors;
    private File excludeFilter;
    private File includeFilter;
    private File excludeBugsFilter;
    private Collection<String> extraArgs;
    private boolean debugEnabled;

    public SpotBugsSpecBuilder(final FileCollection classes)
    {
        if (classes == null || classes.isEmpty())
        {
            throw new InvalidUserDataException("No classes configured for SpotBugs analysis.");
        }
        this.classes = classes;
    }

    public SpotBugsSpecBuilder withPluginsList(final FileCollection pluginsClasspath)
    {
        pluginsList = pluginsClasspath;
        return this;
    }

    public SpotBugsSpecBuilder withSources(final FileCollection sources)
    {
        this.sources = sources;
        return this;
    }

    public SpotBugsSpecBuilder withClasspath(final FileCollection classpath)
    {
        this.classpath = classpath;
        return this;
    }

    public SpotBugsSpecBuilder configureReports(final SpotBugsReports reports)
    {
        this.reports = reports;
        return this;
    }

    public SpotBugsSpecBuilder withEffort(final String effort)
    {
        if (effort != null && !VALID_EFFORTS.contains(effort))
        {
            throw new InvalidUserDataException("Invalid value for SpotBugs 'effort' property: " + effort);
        }
        this.effort = effort;
        return this;
    }

    public SpotBugsSpecBuilder withReportLevel(final String reportLevel)
    {
        if (reportLevel != null && !VALID_REPORT_LEVELS.contains(reportLevel))
        {
            throw new InvalidUserDataException("Invalid value for SpotBugs 'reportLevel' property: " + reportLevel);
        }
        this.reportLevel = reportLevel;
        return this;
    }

    public SpotBugsSpecBuilder withMaxHeapSize(final String maxHeapSize)
    {
        this.maxHeapSize = maxHeapSize;
        return this;
    }

    public SpotBugsSpecBuilder withVisitors(final Collection<String> visitors)
    {
        this.visitors = visitors;
        return this;
    }

    public SpotBugsSpecBuilder withOmitVisitors(final Collection<String> omitVisitors)
    {
        this.omitVisitors = omitVisitors;
        return this;
    }

    public SpotBugsSpecBuilder withExcludeFilter(final File excludeFilter)
    {
        if (excludeFilter != null && !excludeFilter.canRead())
        {
            final String errorStr =
                    String.format("Cannot read file specified for SpotBugs 'excludeFilter' property: %s", excludeFilter);
            throw new InvalidUserDataException(errorStr);
        }

        this.excludeFilter = excludeFilter;
        return this;
    }

    public SpotBugsSpecBuilder withIncludeFilter(final File includeFilter)
    {
        if (includeFilter != null && !includeFilter.canRead())
        {
            final String errorStr =
                    String.format("Cannot read file specified for SpotBugs 'includeFilter' property: %s", includeFilter);
            throw new InvalidUserDataException(errorStr);
        }

        this.includeFilter = includeFilter;
        return this;
    }

    public SpotBugsSpecBuilder withExcludeBugsFilter(final File excludeBugsFilter)
    {
        if (excludeBugsFilter != null && !excludeBugsFilter.canRead())
        {
            final String errorStr =
                    String.format("Cannot read file specified for SpotBugs 'excludeBugsFilter' property: %s", excludeBugsFilter);
            throw new InvalidUserDataException(errorStr);
        }

        this.excludeBugsFilter = excludeBugsFilter;

        return this;
    }

    public SpotBugsSpecBuilder withExtraArgs(final Collection<String> extraArgs)
    {
        this.extraArgs = extraArgs;
        return this;
    }

    public SpotBugsSpecBuilder withDebugging(final boolean debugEnabled)
    {
        this.debugEnabled = debugEnabled;
        return this;
    }

    public SpotBugsSpec build()
    {
        final ArrayList<String> args = new ArrayList<>();
        args.add("-pluginList");
        args.add(pluginsList == null ? "" : pluginsList.getAsPath());
        args.add("-sortByClass");
        args.add("-timestampNow");
        args.add("-progress");

        if (reports != null && !reports.getEnabled().isEmpty())
        {
            if (reports.getEnabled().size() == 1)
            {
                final SpotBugsReportsImpl reportsImpl = (SpotBugsReportsImpl) reports;
                String outputArg = "-" + reportsImpl.getFirstEnabled().getName();
                if (reportsImpl.getFirstEnabled() instanceof SpotBugsXmlReportImpl)
                {
                    final SpotBugsXmlReportImpl r = (SpotBugsXmlReportImpl) reportsImpl.getFirstEnabled();
                    if (r.isWithMessages())
                    {
                        outputArg += ":withMessages";
                    }
                }
                else
                    if (reportsImpl.getFirstEnabled() instanceof CustomizableHtmlReportImpl)
                    {
                        final CustomizableHtmlReportImpl r = (CustomizableHtmlReportImpl) reportsImpl.getFirstEnabled();
                        if (r.getStylesheet() != null)
                        {
                            outputArg += ':' + r.getStylesheet().asFile().getAbsolutePath();
                        }
                    }
                args.add(outputArg);
                args.add("-outputFile");
                args.add(reportsImpl.getFirstEnabled().getDestination().getAbsolutePath());
            }
            else
            {
                throw new InvalidUserDataException("SpotBugs tasks can only have one report enabled, however more than one report was enabled. You need to disable all but one of them.");
            }
        }

        if (has(sources))
        {
            args.add("-sourcepath");
            args.add(sources.getAsPath());
        }

        if (has(classpath))
        {
            args.add("-auxclasspath");

            // Filter unexisting files as SpotBugs can't handle them.
            args.add(classpath.filter(element -> element.exists()).getAsPath());
        }

        if (has(effort))
        {
            args.add(String.format("-effort:%s", effort));
        }

        if (has(reportLevel))
        {
            args.add(String.format("-%s", reportLevel));
        }

        if (has(visitors))
        {
            args.add("-visitors");
            args.add(CollectionUtils.join(",", visitors));
        }

        if (has(omitVisitors))
        {
            args.add("-omitVisitors");
            args.add(CollectionUtils.join(",", omitVisitors));
        }

        if (has(excludeFilter))
        {
            args.add("-exclude");
            args.add(excludeFilter.getPath());
        }

        if (has(includeFilter))
        {
            args.add("-include");
            args.add(includeFilter.getPath());
        }

        if (has(excludeBugsFilter))
        {
            args.add("-excludeBugs");
            args.add(excludeBugsFilter.getPath());
        }

        if (has(extraArgs))
        {
            args.addAll(extraArgs);
        }

        for (final File classFile : classes.getFiles())
        {
            args.add(classFile.getAbsolutePath());
        }

        return new SpotBugsSpec(args, maxHeapSize, debugEnabled);
    }

    private boolean has(final String str)
    {
        return str != null && str.length() > 0;
    }

    private boolean has(final File file)
    {
        return file != null && file.canRead();
    }

    private boolean has(final Collection<?> collection)
    {
        return collection != null && !collection.isEmpty();
    }

    private boolean has(final FileCollection fileCollection)
    {
        return fileCollection != null && !fileCollection.isEmpty();
    }

    private static final Set<String> createImmutableSet(final String... strings)
    {
        final Set<String> set = Arrays.asList(strings).stream().collect(Collectors.toSet());
        return Collections.unmodifiableSet(set);
    }
}