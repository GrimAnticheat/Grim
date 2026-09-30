# Adventure 5 compatibility fix

This fork updates the Fabric 26.2 module to use Adventure 5.2.0 directly instead of PacketEvents' Adventure 4-compatible Gson serializer API.

The affected path is Fabric261ConversionUtil.toNativeText(), which previously referenced PacketEvents' GsonComponentSerializer and could resolve net.kyori.adventure.util.Buildable$Builder on an Adventure 5 runtime.
