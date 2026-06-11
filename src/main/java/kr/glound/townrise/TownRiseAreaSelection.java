package kr.glound.townrise;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class TownRiseAreaSelection {
    private BlockPos first;
    private BlockPos second;
    private ResourceKey<Level> dimension;

    public void setFirst(ResourceKey<Level> dimension, BlockPos pos) {
        setDimensionOrReset(dimension);
        this.first = pos.immutable();
    }

    public void setSecond(ResourceKey<Level> dimension, BlockPos pos) {
        setDimensionOrReset(dimension);
        this.second = pos.immutable();
    }

    public Optional<TownRiseArea> completeArea() {
        if (first == null || second == null || dimension == null) {
            return Optional.empty();
        }
        return Optional.of(TownRiseArea.from(dimension, first, second));
    }

    public boolean hasAnyPoint() {
        return first != null || second != null;
    }

    private void setDimensionOrReset(ResourceKey<Level> newDimension) {
        if (dimension != null && !dimension.equals(newDimension)) {
            first = null;
            second = null;
        }
        dimension = newDimension;
    }

    public record TownRiseArea(ResourceKey<Level> dimension, BlockPos min, BlockPos max) {
        public static TownRiseArea from(ResourceKey<Level> dimension, BlockPos a, BlockPos b) {
            BlockPos min = new BlockPos(
                    Math.min(a.getX(), b.getX()),
                    Math.min(a.getY(), b.getY()),
                    Math.min(a.getZ(), b.getZ())
            );
            BlockPos max = new BlockPos(
                    Math.max(a.getX(), b.getX()),
                    Math.max(a.getY(), b.getY()),
                    Math.max(a.getZ(), b.getZ())
            );
            return new TownRiseArea(dimension, min, max);
        }

        public int blockVolume() {
            return (max.getX() - min.getX() + 1)
                    * (max.getY() - min.getY() + 1)
                    * (max.getZ() - min.getZ() + 1);
        }

        public String compactDescription() {
            return "(" + min.getX() + ", " + min.getY() + ", " + min.getZ() + ") ~ ("
                    + max.getX() + ", " + max.getY() + ", " + max.getZ() + ")";
        }
    }
}
