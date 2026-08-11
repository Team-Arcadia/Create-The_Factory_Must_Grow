package com.drmangotea.tfmg.content.electricity.connection.cables;

import com.drmangotea.tfmg.base.TFMGUtils;
import com.drmangotea.tfmg.content.electricity.connection.cable_type.CableType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class CableConnection {

    public final CablePos pos1;
    public final CablePos pos2;
    public final BlockPos blockPos1;
    public final boolean visible;
    public final CableType type;

    public CableConnection(CablePos pos1, CablePos pos2,BlockPos blockPos1,CableType type, boolean visible){
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.blockPos1 = blockPos1;
        this.visible = visible;
        this.type = type;
    }

    /**
     * Two connections describe the same wire when they join the same pair of
     * cable positions with the same cable type, whichever end is listed first.
     *
     * Without this the duplicate check in SpoolItem fell back to identity and
     * therefore never matched: re-linking two connectors that were already
     * wired added a second connection and charged the spool a second time.
     *
     * visible and blockPos1 are deliberately excluded. The two halves of a
     * single wire disagree on both and still describe the same wire.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CableConnection other))
            return false;
        if (!java.util.Objects.equals(type, other.type))
            return false;
        return (samePos(pos1, other.pos1) && samePos(pos2, other.pos2))
                || (samePos(pos1, other.pos2) && samePos(pos2, other.pos1));
    }

    /**
     * Endpoint-only comparison, ignoring the cable type.
     *
     * Two connectors carry at most one wire between them whatever it is made
     * of, so the "already wired" check has to ask this and not equals: a copper
     * wire and an aluminum wire between the same pair are different by equals,
     * which let a player stack a second and a third cable on one pair.
     */
    public boolean linksSameEndpoints(CableConnection other) {
        if (other == null)
            return false;
        return (samePos(pos1, other.pos1) && samePos(pos2, other.pos2))
                || (samePos(pos1, other.pos2) && samePos(pos2, other.pos1));
    }

    private static boolean samePos(CablePos a, CablePos b) {
        if (a == b)
            return true;
        if (a == null || b == null)
            return false;
        return a.x() == b.x() && a.y() == b.y() && a.z() == b.z();
    }

    @Override
    public int hashCode() {
        // Summed rather than ordered so both endpoint orderings hash alike.
        return java.util.Objects.hashCode(type) + posHash(pos1) + posHash(pos2);
    }

    private static int posHash(CablePos p) {
        if (p == null)
            return 0;
        return Double.hashCode(p.x()) ^ Double.hashCode(p.y()) ^ Double.hashCode(p.z());
    }

    public CompoundTag saveConnection(){
        CompoundTag compoundTag = new CompoundTag();

        compoundTag.putDouble("X1", pos1.x());
        compoundTag.putDouble("Y1", pos1.y());
        compoundTag.putDouble("Z1", pos1.z());

        compoundTag.putDouble("X2", pos2.x());
        compoundTag.putDouble("Y2", pos2.y());
        compoundTag.putDouble("Z2", pos2.z());


        compoundTag.putLong("Pos", blockPos1.asLong());




        compoundTag.putBoolean("Visible", visible);

        compoundTag.putString("CableType", type.getKey().toString());

        return compoundTag;
    }
    public static CableConnection loadConnection(CompoundTag compoundTag){



        CablePos pos1  = new CablePos(compoundTag.getDouble("X1"),compoundTag.getDouble("Y1"),compoundTag.getDouble("Z1"));
        CablePos pos2 = new CablePos(compoundTag.getDouble("X2"),compoundTag.getDouble("Y2"),compoundTag.getDouble("Z2"));


        BlockPos blockPos1 = BlockPos.of(compoundTag.getLong("Pos"));

        boolean visible = compoundTag.getBoolean("Visible");
        CableType type = TFMGUtils.getCableType(ResourceLocation.parse(compoundTag.getString("CableType")));
        return new CableConnection(pos1,pos2,blockPos1,type,visible);
    }
    public float getLength(){
        return TFMGUtils.getDistance(new BlockPos((int) pos1.x(), (int) pos1.y(), (int) pos1.z()),new BlockPos((int) pos2.x(), (int) pos2.y(), (int) pos2.z()), false);
    }

}
