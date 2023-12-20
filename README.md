## PacketEvents Generators

Utility generators for updating [packetevents](https://github.com/retrooper/packetevents/) to new minecraft versions.

### Usage for updating PacketEvents

1. Clone this repo
2. Set old version in `gradle.properties` and run `./gradlew generate`
3. Copy the old generated data, it's used for comparison in the new version:
   - Copy `generated/RegistryGenerator/item.json` to `generated/ItemTypesGenerator/input.json`
   - Copy `generated/RegistryGenerator/block.json` to `generated/StateTypesGenerator/input.json`
4. Set new version in `gradle.properties` and run `./gradlew generate`
5. Copy important stuff in the `generated` directory to the proper places in the packetevents project
