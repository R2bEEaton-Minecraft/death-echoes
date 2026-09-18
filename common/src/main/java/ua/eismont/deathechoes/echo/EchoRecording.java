package ua.eismont.deathechoes.echo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import ua.eismont.deathechoes.config.DeathEchoesConfig;

/**
 * A ring buffer of recorded {@link EchoFrame}s for a player, used to
 * replay a ghost's final moments after death.
 *
 * <p>Main server thread only; not thread-safe.
 */
public class EchoRecording {

    public static final int DEFAULT_MAX_FRAMES = 200;
    public static final int MAX_FRAMES = DEFAULT_MAX_FRAMES;

    private final int maxFrames;
    private final ArrayDeque<EchoFrame> frames;

    // Cached snapshot for O(1) indexed reads; invalidated on every push since replay reads
    // frame(int) every tick and a fresh ArrayList per push would be wasteful otherwise.
    private List<EchoFrame> cache;

    public EchoRecording() {
        this(DeathEchoesConfig.get().getMaxFrames());
    }

    public EchoRecording(int maxFrames) {
        this.maxFrames = Math.max(20, maxFrames);
        this.frames = new ArrayDeque<>(this.maxFrames);
    }

    public void push(EchoFrame frame) {
        frames.addLast(frame);
        while (frames.size() > maxFrames) {
            frames.removeFirst();
        }
        cache = null;
    }

    public int size() {
        return frames.size();
    }

    public int getMaxFrames() {
        return maxFrames;
    }

    public EchoFrame frame(int index) {
        if (cache == null) {
            cache = new ArrayList<>(frames);
        }
        return cache.get(index);
    }

    public CompoundTag toTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (EchoFrame frame : frames) {
            list.add(frame.toTag(registries));
        }
        tag.put("frames", list);
        return tag;
    }

    public static EchoRecording fromTag(CompoundTag tag, HolderLookup.Provider registries) {
        EchoRecording recording = new EchoRecording();
        ListTag list = tag.getListOrEmpty("frames");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag frameTag = list.getCompoundOrEmpty(i);
            recording.push(EchoFrame.fromTag(frameTag, registries));
        }
        return recording;
    }
}
