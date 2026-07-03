# Keras On-Device

A production-grade public Android demo that showcases Keras/TensorFlow models running locally on-device via LiteRT and LiteRT-LM.

## What it demonstrates

| Surface | Task | Runtime |
|---|---|---|
| Vision | Image classification (MobileNetV3) | LiteRT (`tensorflow-lite`) |
| Vision | Object detection (COCO SSD MobileNet V1) | LiteRT (`tensorflow-lite`) |
| Vision | Image segmentation (DeepLabV3) | LiteRT (`tensorflow-lite`) |
| Vision | Depth estimation (MiDaS) | LiteRT (`tensorflow-lite`) |
| Language | Text generation with custom tokenization/sampling | LiteRT (`tensorflow-lite`) |
| Language | Text generation with LiteRT-LM optimized engine | `litertlm-android` |
| Multimodal | Engine configuration validation for Gemma4 | `litertlm-android` |

## Build

Requirements:
- Android Studio (Ladybug or newer)
- Android SDK 36
- JDK 21

```bash
./gradlew assembleDebug
```

The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Run tests

### Unit tests (no device needed)

```bash
./gradlew testDebugUnitTest
```

### Instrumented tests (requires a device or emulator)

Push bundled test models to the device:

```bash
adb push app/src/androidTest/assets/models/* /sdcard/Android/data/com.example.kerasondevice/files/
```

Then run:

```bash
./gradlew connectedDebugAndroidTest
```

## Model setup

Models are discovered at runtime in this order:

1. `context.filesDir`
2. `context.getExternalFilesDir(null)`
3. `/data/local/tmp`

Each `.tflite` or `.litertlm` file must ship with a JSON sidecar (`<model>.json`) describing the task, input size, normalization, and labels file.

### Example sidecar for classification

```json
{
  "task": "image_classification",
  "image_width": 224,
  "image_height": 224,
  "normalization": {"mean": [127.5, 127.5, 127.5], "std": [127.5, 127.5, 127.5]},
  "labels": "imagenet_labels.txt",
  "num_classes": 1000
}
```

See `scripts/prepare_test_assets.py` for how the bundled test models are generated.

## Architecture

- `domain/` — pure Kotlin domain models, task contract, catalog, model discovery.
- `inference/` — interpreter wrappers and task-specific inference logic.
- `data/preprocessing/` — image/audio preprocessors.
- `ui/` — Jetpack Compose screens and renderers.

## Known limitations

- Real Gemma4 multimodal end-to-end image/audio inference is not yet supported; the KerasHub export pipeline currently produces a single `PREFILL_DECODE` model instead of separate `VISION_ENCODER`/`VISION_ADAPTER` sub-models required by the LiteRT-LM runtime.
- Object detection uses a stock quantized SSD MobileNet V1 TFLite model because KerasHub D-Fine/RetinaNet torch-backend LiteRT export fails with the current toolchain.
- GPU/NPU delegate validation is out of scope for this cycle.

## License

Apache 2.0 — see `LICENSE`.
