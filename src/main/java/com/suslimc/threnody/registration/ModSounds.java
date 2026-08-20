package com.suslimc.threnody.registration;

import com.suslimc.threnody.ThrenodyMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ThrenodyMod.MODID);

    /** Low detuned sub tones that sit under everything while it is nearby. */
    public static final RegistryObject<SoundEvent> DRONE = register("threnody.drone");
    /** The player's own pulse, rising as the distance closes. */
    public static final RegistryObject<SoundEvent> HEARTBEAT = register("threnody.heartbeat");
    /** Breath-like noise that never resolves into words. */
    public static final RegistryObject<SoundEvent> WHISPER = register("threnody.whisper");
    /** The sting for the moment it notices a player. */
    public static final RegistryObject<SoundEvent> SEEN = register("threnody.seen");
    /** A dissonant cluster torn apart by distortion. */
    public static final RegistryObject<SoundEvent> SHRIEK = register("threnody.shriek");
    /** Pulsing rumble that means it has stopped pretending. */
    public static final RegistryObject<SoundEvent> CHASE = register("threnody.chase");
    /** A heavy body meeting the ground. */
    public static final RegistryObject<SoundEvent> STEP = register("threnody.step");
    /** The space it leaves behind. */
    public static final RegistryObject<SoundEvent> VANISH = register("threnody.vanish");
    /** A rare cue that something moved just out of sight. */
    public static final RegistryObject<SoundEvent> KNOCK = register("threnody.knock");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(ThrenodyMod.MODID, name)
        ));
    }
}
