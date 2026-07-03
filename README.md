# Keras On-Device

A production-grade public Android demo that showcases Keras/TensorFlow models running locally on-device via LiteRT and LiteRT-LM.

## What it demonstrates

| Surface | Task | Model | Runtime |
|---|---|---|---|
| Vision | Image classification | MobileNetV3 (KerasHub) | LiteRT (`tensorflow-lite`) |
| Vision | Object detection | D-Fine (KerasHub) | LiteRT (`tensorflow-lite`) |
| Vision | Image segmentation | DeepLabV3 (KerasHub) | LiteRT (`tensorflow-lite`) |
| Vision | Depth estimation | DepthAnything v2 (KerasHub) | LiteRT (`tensorflow-lite`) |
| Language | Text generation | Gemma3 270M IT | LiteRT (custom tokenization/sampling) |
| Language | Text generation | Gemma3 270M IT | LiteRT-LM (`litertlm-android`) |
| Multimodal | Engine validation | Gemma4 dummy | `litertlm-android` |

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

Stage large test models in `/data/local/tmp` (they are intentionally not bundled):

```bash
adb push /tmp/mobilenetv3.tflite /data/local/tmp/
adb push /tmp/deeplabv3.tflite /data/local/tmp/
adb push /tmp/midas.tflite /data/local/tmp/
adb push /tmp/dfine.tflite /data/local/tmp/
adb push app/src/androidTest/assets/models/*.json /data/local/tmp/
adb push app/src/androidTest/assets/models/*.txt /data/local/tmp/
```

Then run:

```bash
./gradlew connectedDebugAndroidTest
```

> Test assets (images, small SSD model, labels) live in `app/src/androidTest/assets`. The tests read them through the instrumentation context and copy required files to the target app's `filesDir`. Large models are discovered from `/data/local/tmp`.

## Model exports

Vision models are exported from KerasHub with the default Torch backend where possible. D-Fine's torch-backend LiteRT export fails with dynamic-shape errors, so it is exported via `tf_saved_model` and converted to TFLite.

Example exports:

```bash
cd /home/pctablet505/Projects/gemmademo-litert-export
source .venv/bin/activate

KERAS_BACKEND=torch python -c "
import keras_hub, torch
m = keras_hub.models.MobileNetImageClassifier.from_preset('mobilenet_v3_small_100_imagenet')
m.export('/tmp/mobilenetv3.tflite', format='litert', input_signature=[torch.randn(1,224,224,3)])
"

KERAS_BACKEND=torch python -c "
import keras_hub, torch
m = keras_hub.models.DeepLabV3ImageSegmenter.from_preset('deeplab_v3_plus_resnet50_pascalvoc')
m.export('/tmp/deeplabv3.tflite', format='litert', input_signature=[torch.randn(1,512,512,3)])
"

KERAS_BACKEND=torch python -c "
import keras_hub, torch
b = keras_hub.models.DepthAnythingBackbone.from_preset('depth_anything_v2_small')
m = keras_hub.models.DepthAnythingDepthEstimator(backbone=b, depth_estimation_type='relative')
m.export('/tmp/midas.tflite', format='litert', input_signature=[torch.randn(1,518,518,3)])
"

# D-Fine uses TensorFlow SavedModel as an intermediate step.
KERAS_BACKEND=tensorflow python -c "
import keras_hub, tensorflow as tf
m = keras_hub.models.DFineObjectDetector.from_preset('dfine_nano_coco')
m.export('/tmp/dfine_saved_model', format='tf_saved_model', input_signature=[tf.TensorSpec([1,640,640,3], tf.float32)])
"
python -c "
import tensorflow as tf
c = tf.lite.TFLiteConverter.from_saved_model('/tmp/dfine_saved_model')
c.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS, tf.lite.OpsSet.SELECT_TF_OPS]
open('/tmp/dfine.tflite','wb').write(c.convert())
"
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
- D-Fine is converted with SELECT_TF_OPS enabled because its torch-backend LiteRT export hits dynamic-shape errors in the current toolchain. GPU/NPU delegate validation is out of scope for this cycle.

## License

Apache 2.0 — see `LICENSE`.
