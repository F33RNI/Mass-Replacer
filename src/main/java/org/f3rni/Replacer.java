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

import br.com.gamemods.nbtmanipulator.NbtCompound;
import br.com.gamemods.nbtmanipulator.NbtList;
import br.com.gamemods.regionmanipulator.Chunk;
import br.com.gamemods.regionmanipulator.ChunkPos;
import br.com.gamemods.regionmanipulator.Region;
import br.com.gamemods.regionmanipulator.RegionIO;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Replacer {
    public static final Logger LOGGER = Logger.getLogger(Replacer.class.getName());

    private static final int X_POS_MAX = 32;
    private static final int Z_POS_MAX = 32;

    /**
     * Replaces one block with other in given MCA file
     * @param mcaFile .mca file
     * @param blocksReplace List of BlockReplace classes
     * @return entries replaced
     */
    public static int replace(File mcaFile, List<BlockReplace> blocksReplace) {
        // Log data
        LOGGER.log(Level.INFO, "Trying to replace blocks in " + mcaFile.getName() + "...");

        try {
            // Load file
            Region region = RegionIO.readRegion(mcaFile);

            // Entries counter
            int entriesCounter = 0;

            // List all chunks
            for (int xPos = 0; xPos <= X_POS_MAX; xPos++) {
                for (int zPos = 0; zPos <= Z_POS_MAX; zPos++) {
                    try {
                        Chunk chunk = region.get(new ChunkPos(region.getPosition().getXPos() * 32 + xPos,
                                region.getPosition().getZPos() * 32 + zPos));
                        if (chunk != null && chunk.getLevel().containsKey("sections")) {
                            // List all sections
                            NbtList<NbtCompound> sections = chunk.getLevel().getCompoundList("sections");
                            for (NbtCompound section : sections) {
                                if (section.containsKey("block_states")) {
                                    NbtCompound blockStates = section.getCompound("block_states");

                                    // List all palettes
                                    if (blockStates.containsKey("palette")) {
                                        NbtList<NbtCompound> palettes = blockStates.getCompoundList("palette");
                                        for (NbtCompound palette : palettes) {
                                            if (palette.containsKey("Name")) {
                                                // List all blocks
                                                for (BlockReplace blockReplace : blocksReplace) {
                                                    if (blockReplace != null && blockReplace.getFrom() != null
                                                            && blockReplace.getTo() != null) {
                                                        // Replace with target block if equals to source block
                                                        if (palette.getString("Name")
                                                                .equals(blockReplace.getFrom())) {
                                                            palette.set("Name", blockReplace.getTo());
                                                            entriesCounter++;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error reading chunk at " + xPos + " " + zPos + "!", e);
                    }
                }
            }

            // Save file
            RegionIO.writeRegion(mcaFile, region);

            // Return how many entries replaced
            LOGGER.log(Level.INFO, "Replaced " + entriesCounter + " entries");
            return entriesCounter;
        }

        // Error
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error replacing blocks!", e);
        }

        return 0;
    }
}
