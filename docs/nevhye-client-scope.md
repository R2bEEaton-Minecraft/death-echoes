# nevhye — DeathEchoes Fabric Customization

## Project context

- **Client:** nevhye
- **Target:** the Fabric build of DeathEchoes only; do not modify the NeoForge build unless separately agreed.
- **Requested Minecraft version:** 1.21.11 (client called it “backdate”; confirm the precise target version before final delivery because upstream currently targets 26.1.2+).
- **Requested timeline:** within one week, as soon as practical.
- **Use case:** private play with the client’s friends.

## Requested work

1. **Smooth ghost replay playback**
   - Resolve the current choppy/skipping replay movement.
   - Use smooth interpolation between recorded positions/rotations so the replay is visually smoother than a raw 20 TPS sequence.

2. **Record and replay player actions**
   - Capture attack/swing state, including unarmed fist swings.
   - Display the appropriate arm/swing animation during ghost playback.

3. **Replay equipment visually**
   - Record the equipment state needed to show worn armor on the ghost.
   - Render armor correctly during playback.
   - Preserve the mod’s intended translucent ghost presentation for equipment where feasible.

4. **Configurable replay duration**
   - Replace the fixed ten-second recording/replay duration with a player/server-configurable value.
   - The setting controls how much gameplay is retained before death and consequently how long the ghost loop runs.
   - Choose a straightforward configuration format and document its location and supported range/default.

5. **Obfuscated death messages**
   - Add an option/behavior that hides player names and item/weapon names in death messages.
   - The replaced text should use Minecraft’s animated “magic”/obfuscated text effect (formatting code `§k`), e.g. names and weapon references are unreadable rather than merely removed.
   - Preserve the rest of the vanilla death-message structure when possible.

6. **Delivery**
   - Provide a tested Fabric JAR for the agreed target Minecraft version.
   - Include brief installation/configuration notes and source changes in the fork.

## Existing upstream behavior to preserve

- Echoes replay the final ten seconds at the death location and loop.
- The owning player can reclaim 50% of lost XP by interacting with their echo.
- The mod limits each player to three echoes.
- Echoes remain safe from lava/void placement and work across dimensions.

## Open items to confirm before implementation

- Whether “1.21.11” refers to the exact Minecraft release requested; upstream’s published Fabric build is for 26.1.2+.
- Whether obfuscated death messages should affect every server death message or only messages concerning specific players/items.
- Desired configuration scope: server-wide default only, or per-player customization.
- Whether any other mods alter death messages or player rendering in the client’s modpack; these could affect compatibility testing.

