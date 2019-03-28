/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items-purge-tool
 *
 * holdings-items-purge-tool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items-purge-tool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.purge;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public final class Arguments {

    private final Option agencyId =
            Option.builder("a")
                    .longOpt("agency-id")
                    .hasArg()
                    .required()
                    .argName("AGENCY")
                    .desc("Agency ID to purge for")
                    .build();
    private final Option openAgency =
            Option.builder("o")
                    .longOpt("open-agency-url")
                    .hasArg()
                    .required()
                    .argName("URL")
                    .desc("OpenAgency URL to connect to. E.g. http://openagency.addi.dk/<version>/")
                    .build();
    private final Option database =
            Option.builder("d")
                    .longOpt("database")
                    .hasArg()
                    .required()
                    .argName("DB")
                    .desc("Connectstring for database. E.g jdbc:postgresql://user:password@host:port/database")
                    .build();
    private final Option queue =
            Option.builder("q")
                    .longOpt("queue")
                    .hasArg()
                    .required()
                    .argName("QUEUE")
                    .desc("Name of queue that should process deletes")
                    .build();
    private final Option verbose =
            Option.builder("v")
                    .longOpt("verbose")
                    .desc("Enable debug log")
                    .build();
    private final Option help =
            Option.builder("h")
                    .longOpt("help")
                    .desc("This help")
                    .build();
    private final Option commit =
            Option.builder("c")
                    .longOpt("commit-every")
                    .hasArg()
                    .argName("NUM")
                    .desc("How often to commit")
                    .build();
    private final Option dryRun =
            Option.builder("n")
                    .longOpt("dry-run")
                    .desc("Only simulate")
                    .build();
    private final Options options = new Options()
            .addOption(agencyId)
            .addOption(openAgency)
            .addOption(database)
            .addOption(queue)
            .addOption(verbose)
            .addOption(dryRun)
            .addOption(help)
            .addOption(commit);

    private final CommandLine commandLine;

    public Arguments(String... args) throws ExitException {
        List<Option> required = options.getOptions().stream()
                .filter(Option::isRequired)
                .collect(Collectors.toList());

        this.commandLine = parse(options, required, args);

        if (commandLine.hasOption(help.getOpt()))
            throw new ExitException(usage(null));

        String missing = required.stream()
                .map(Option::getOpt)
                .filter(o -> !commandLine.hasOption(o))
                .sorted()
                .collect(Collectors.joining(", "));
        if (!missing.isEmpty()) {
            // subst last , with &
            int i = missing.lastIndexOf(", ");
            if (i > 0)
                missing = missing.substring(0, i) + " & " + missing.substring(i + 2);
            missing = "Required options: " + missing + " are missing";
            throw new ExitException(usage(missing));
        }
    }

    private CommandLine parse(Options options, List<Option> required, String[] args) throws ExitException {
        try {
            required.forEach(o -> o.setRequired(false));
            Options optionsWitlAllAsOptional = new Options();
            options.getOptions().forEach(optionsWitlAllAsOptional::addOption);
            return new DefaultParser().parse(optionsWitlAllAsOptional, args);
        } catch (ParseException ex) {
            System.err.println(ex.getMessage());
            throw new ExitException(1);
        } finally {
            required.forEach(o -> o.setRequired(true));
        }
    }

    /**
     * Print usage
     *
     * @param error An optional error
     * @return system exit code
     */
    public int usage(String error) {
        boolean hasError = error != null && !error.isEmpty();
        try (OutputStream os = hasError ? System.err : System.out ;
             Writer osWriter = new OutputStreamWriter(os, StandardCharsets.UTF_8) ;
             PrintWriter writer = new PrintWriter(osWriter)) {
            HelpFormatter formatter = new HelpFormatter();
            if (hasError) {
                formatter.printWrapped(writer, 76, error);
                formatter.printWrapped(writer, 76, "");
            }
            formatter.printUsage(writer, 76, "java -jar holdings-items-purge-tool.jar", options);
            formatter.printWrapped(writer, 76, "");
            formatter.printOptions(writer, 76, options, 4, 4);
            formatter.printWrapped(writer, 76, "");
        } catch (IOException ex) {
            System.err.println(ex);
        }
        return hasError ? 1 : 0;
    }

    public boolean hasDryRun() {
        return commandLine.hasOption(dryRun.getOpt());
    }

    public boolean hasHelp() {
        return commandLine.hasOption(help.getOpt());
    }

    public boolean hasVerbose() {
        return commandLine.hasOption(verbose.getOpt());
    }

    public int getAgencyId() throws ExitException {
        try {
            return Integer.parseInt(commandLine.getOptionValue(agencyId.getOpt()));
        } catch (NumberFormatException ex) {
            throw new ExitException(usage("Invalid number in agency-id"));
        }
    }

    OptionalInt getCommit() {
        if (commandLine.hasOption(commit.getOpt()))
            return OptionalInt.of(Integer.parseInt(commandLine.getOptionValue(commit.getOpt())));
        return OptionalInt.empty();
    }

    public String getDatabase() {
        return commandLine.getOptionValue(database.getOpt());
    }

    public String getOpenAgencyUrl() {
        return commandLine.getOptionValue(openAgency.getOpt());
    }

    public String getQueue() {
        return commandLine.getOptionValue(queue.getOpt());
    }

}
