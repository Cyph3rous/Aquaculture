package com.teammetallurgy.aquaculture.loot;

import com.google.common.collect.Lists;
import com.google.gson.*;
import com.teammetallurgy.aquaculture.Aquaculture;
import com.teammetallurgy.aquaculture.init.AquaLootTables;
import com.teammetallurgy.aquaculture.init.FishRegistry;
import com.teammetallurgy.aquaculture.misc.AquaConfig;
import com.teammetallurgy.aquaculture.misc.BiomeDictionaryHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class FishReadFromJson {
    public static HashMap<EntityType<?>, List<ResourceLocation>> FISH_BIOME_MAP = new HashMap<>();
    public static HashMap<EntityType<?>, Integer> FISH_WEIGHT_MAP = new HashMap<>();
    private static final Gson GSON_INSTANCE = new GsonBuilder().setPrettyPrinting().create();
    public static boolean hasRunFirstTime;

    public static void read() {
        try {
            String filePath = Aquaculture.MOD_ID + "/loot_tables/" + AquaLootTables.FISH.getPath() + ".json";
            InputStreamReader fileReader;
            if (Aquaculture.IS_DEV) {
                IModFile modFile = ModList.get().getModFileById(Aquaculture.MOD_ID).getFile();
                Path root = modFile.findResource(PackType.SERVER_DATA.getDirectory()).toAbsolutePath();
                fileReader = new FileReader(root.resolve(root.getFileSystem().getPath(filePath)).toFile());
            } else {
                fileReader = new InputStreamReader(Aquaculture.instance.getClass().getResourceAsStream("/data/" + filePath));
            }

            BufferedReader reader = new BufferedReader(fileReader);
            JsonElement json = GSON_INSTANCE.fromJson(reader, JsonElement.class);
            if (json != null && !json.isJsonNull()) {
                JsonObject jsonObject = json.getAsJsonObject();
                JsonArray pools = jsonObject.getAsJsonArray("pools");
                JsonObject poolsObject = pools.get(0).getAsJsonObject();
                JsonArray entries = poolsObject.getAsJsonArray("entries");
                for (JsonElement entry : entries) {
                    JsonArray conditions = entry.getAsJsonObject().getAsJsonArray("conditions");
                    EntityType<?> fish = getEntityFromString(entry.getAsJsonObject().get("name").toString());
                    for (JsonElement conditionElement : conditions) {
                        JsonObject condition = conditionElement.getAsJsonObject();
                        if (condition.get("condition").getAsString().equals("aquaculture:biome_tag_check")) {
                            FISH_BIOME_MAP.put(fish, getSpawnableBiomes(condition.get("predicate")));
                        } else if (condition.get("condition").getAsString().equals("minecraft:alternative")) {
                            for (JsonElement term : condition.getAsJsonObject().getAsJsonArray("terms")) {
                                List<ResourceLocation> spawnableBiomes = getSpawnableBiomes(term.getAsJsonObject().get("predicate"));
                                if (!FISH_BIOME_MAP.containsKey(fish)) {
                                    FISH_BIOME_MAP.put(fish, spawnableBiomes);
                                } else {
                                    spawnableBiomes.forEach(biome -> FISH_BIOME_MAP.get(fish).add(biome));
                                }
                            }
                        }
                    }
                    FISH_WEIGHT_MAP.put(fish, entry.getAsJsonObject().get("weight").getAsInt());
                }
                //Remove loot entries that does not have an entity
                FISH_BIOME_MAP.keySet().retainAll(FishRegistry.fishEntities);
                FISH_WEIGHT_MAP.keySet().retainAll(FishRegistry.fishEntities);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static EntityType<?> getEntityFromString(String name) {
        name = name.replace("\"", "");
        return ForgeRegistries.ENTITIES.getValue(new ResourceLocation(name));
    }

    private static List<ResourceLocation> getSpawnableBiomes(JsonElement predicate) {
        List<ResourceLocation> biomes = Lists.newArrayList();
        List<BiomeDictionary.Type> includeList = Lists.newArrayList();
        List<BiomeDictionary.Type> excludeList = Lists.newArrayList();
        boolean and = false;

        if (predicate.getAsJsonObject().has("include")) {
            JsonArray include = predicate.getAsJsonObject().get("include").getAsJsonArray();
            for (int entry = 0; entry < include.size(); entry++) {
                includeList.add(BiomeDictionaryHelper.getType(include.get(entry).getAsString().toLowerCase(Locale.ROOT)));
            }
        }
        if (predicate.getAsJsonObject().has("exclude")) {
            JsonArray exclude = predicate.getAsJsonObject().get("exclude").getAsJsonArray();
            for (int entry = 0; entry < exclude.size(); entry++) {
                excludeList.add(BiomeDictionaryHelper.getType(exclude.get(entry).getAsString().toLowerCase(Locale.ROOT)));
            }
        }
        if (predicate.getAsJsonObject().has("and")) {
            and = predicate.getAsJsonObject().get("and").getAsBoolean();
        }

        biomes.addAll(BiomeTagPredicate.getValidBiomes(includeList, excludeList, and));

        return biomes;
    }

    public static void addFishSpawns(BiomeLoadingEvent event) {
        if (AquaConfig.BASIC_OPTIONS.enableFishSpawning.get()) {
            //Biome debug
            if (!FISH_BIOME_MAP.isEmpty()) {
                for (EntityType<?> fish : FISH_BIOME_MAP.keySet()) {
                    if (AquaConfig.BASIC_OPTIONS.debugMode.get() && !hasRunFirstTime) {
                        List<String> strings = new ArrayList<>();
                        for (ResourceLocation biome : FISH_BIOME_MAP.get(fish)) {
                            if (biome != null) {
                                strings.add(biome.getPath());
                            }
                        }
                        Aquaculture.LOG.info(fish.getRegistryName() + " Biomes: " + strings);
                    }

                    int weight = FISH_WEIGHT_MAP.get(fish) / 3;
                    int maxGroupSize = Mth.clamp((FISH_WEIGHT_MAP.get(fish) / 10), 1, 8);
                    if (weight < 1) weight = 1;
                    if (AquaConfig.BASIC_OPTIONS.debugMode.get() && !hasRunFirstTime) {
                        Aquaculture.LOG.info(fish.getRegistryName() + " spawn debug = loottable weight: " + FISH_WEIGHT_MAP.get(fish) + " | weight : " + weight + " | maxGroupSize: " + maxGroupSize);
                    }
                    ResourceLocation name = event.getName();
                    if (name != null) {
                        for (ResourceLocation biome : FISH_BIOME_MAP.get(fish)) {
                            if (name.equals(biome)) {
                                event.getSpawns().getSpawner(MobCategory.WATER_AMBIENT).add(new MobSpawnSettings.SpawnerData(fish, weight, 1, maxGroupSize));
                            }
                        }
                    }
                }
                hasRunFirstTime = true;
            }
        }
    }
}