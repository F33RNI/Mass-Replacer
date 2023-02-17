/*
 * Copyright (C) 2023 Fern Lane, Mass-Replacer Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.f3rni;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Main {
    public static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static final String BLOCKS_FILE_DEFAULT = "blocks.json";
    public static final String MCA_FILE_EXTENSION = ".mca";
    public static final String DIM_FOLDERS_BASE_NAME = "DIM";
    public static final String REGION_FOLDERS_BASE_NAME = "region";

    public static void main(String[] args) {
        // Initialize logging
        LOGGER.setLevel(Level.FINE);

        // Parse the arguments given from the console
        Options options = new Options();
        options.addOption(Option.builder("world")
                .longOpt("world_dir")
                .hasArg(true)
                .desc("Source world directory")
                .required(true)
                .build());
        options.addOption(Option.builder("out")
                .longOpt("output_dir")
                .hasArg(true)
                .desc("World output directory")
                .required(true)
                .build());
        options.addOption(Option.builder("blocks")
                .longOpt("blocks_file")
                .hasArg(true)
                .desc("blocks.json file")
                .required(false)
                .build());
        CommandLineParser parser = new DefaultParser();

        try {
            // Parse arguments
            CommandLine cmd = parser.parse(options, args);
            File worldSource = new File(cmd.getOptionValue("world"));
            File worldDestination = new File(cmd.getOptionValue("out"));
            String blocksFile = BLOCKS_FILE_DEFAULT;
            if (cmd.hasOption("blocks"))
                blocksFile = cmd.getOptionValue("blocks");

            // Read blocks.json
            Gson gson = new Gson();
            Reader reader = Files.newBufferedReader(Paths.get(blocksFile));
            List<BlockReplace> blocksReplace = gson.fromJson(reader, new TypeToken<List<BlockReplace>>() {}.getType());
            LOGGER.log(Level.INFO, "Blocks to replace: " + blocksReplace.size());

            // Copy to output directory
            LOGGER.log(Level.INFO, "Copying " + worldSource.getAbsolutePath()
                    + " to " + worldDestination.getAbsolutePath());
            copyFolder(worldSource.toPath(), worldDestination.toPath());

            // List all files in world directory
            File[] worldFiles = worldDestination.listFiles();
            assert worldFiles != null;

            // List all .mca files
            List<File> mcaFiles = new ArrayList<>();
            for (File worldFile: worldFiles) {
                // Dimension found
                if (worldFile.isDirectory() && worldFile.getName().contains(DIM_FOLDERS_BASE_NAME)) {

                    // List all files in dimension
                    File[] dimFiles = worldFile.listFiles();
                    assert dimFiles != null;
                    for (File dimFile: dimFiles) {
                        // Region folder found
                        if (dimFile.isDirectory() && dimFile.getName().contains(REGION_FOLDERS_BASE_NAME)) {
                            LOGGER.log(Level.INFO, "Adding region files from " + worldFile.getName());

                            // List all mca files
                            for (File regionFile: Objects.requireNonNull(dimFile.listFiles())) {
                                if (!regionFile.isDirectory() && regionFile.getName().endsWith(MCA_FILE_EXTENSION)) {
                                    LOGGER.log(Level.INFO, "Adding file: " + regionFile.getName());
                                    mcaFiles.add(regionFile);
                                }
                            }
                        }
                    }
                }

                // Overworld region folder
                else if (worldFile.isDirectory() && worldFile.getName().contains(REGION_FOLDERS_BASE_NAME)) {
                    // List all mca files
                    for (File regionFile: Objects.requireNonNull(worldFile.listFiles())) {
                        if (!regionFile.isDirectory() && regionFile.getName().endsWith(MCA_FILE_EXTENSION)) {
                            LOGGER.log(Level.INFO, "Adding file: " + regionFile.getName());
                            mcaFiles.add(regionFile);
                        }
                    }
                }
            }

            // Replace blocks and count entries
            int entriesCounter = 0;
            for (File mcaFile : mcaFiles) {
                int replacedEntries = Replacer.replace(mcaFile, blocksReplace);
                entriesCounter += replacedEntries;
            }

            // Log result
            LOGGER.log(Level.INFO, "Replaced total " + entriesCounter + " entries");
        }

        // Parsing error
        catch (ParseException e) {
            LOGGER.log(Level.SEVERE, "Error parsing arguments!", e);

            // Print help message if wrong arguments provided
            new HelpFormatter().printHelp(
                    "java -jar Mass-Replacer-X.X-SNAPSHOT.jar", options);
        }

        // Other error
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error!", e);
        }
    }

    /**
     * Copies folder from src to dest
     * @param src copy from Path
     * @param dest copy to Path
     * @throws IOException error copying directory
     */
    public static void copyFolder(Path src, Path dest) throws IOException {
        try (Stream<Path> stream = Files.walk(src)) {
            stream.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
        }
    }

    /**
     * Copies source path to dest
     * @param source source Path
     * @param dest destination Path
     */
    private static void copy(Path source, Path dest) {
        try {
            Files.copy(source, dest, REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}