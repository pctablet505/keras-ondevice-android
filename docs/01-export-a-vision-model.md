# Export a KerasHub vision model and run it on Android

**A Keras model on a phone with one export call, not a rewrite.**

This page takes one KerasHub image classifier, MobileNetV3, to an Android phone. You need one export call in
Python, one JSON file for the app, and four commands for the phone. The format is LiteRT, once called
TensorFlow Lite, and the file still ends in `.tflite`.

<p>
<picture>
  <source media="(prefers-color-scheme: dark) and (max-width: 600px)" srcset="assets/01-diagram-narrow-dark.svg">
  <source media="(max-width: 600px)" srcset="assets/01-diagram-narrow-light.svg">
  <source media="(prefers-color-scheme: dark)" srcset="assets/01-diagram-dark.svg">
  <img src="assets/01-diagram-light.svg" width="100%" alt="The path on this page: a KerasHub preset, model.export with format litert, a .tflite file with its JSON sidecar, adb push to /data/local/tmp, and the app, which finds the pair and runs it on the CPU.">
</picture>
</p>

## Is it for you?

For an Android developer, or an ML engineer with a Keras model, who wants it on a real phone.

What it is not:

- **Not a tour of the export API.** The [keras.io LiteRT guide](https://keras.io/guides/litert_export/) covers
  model types, quantization and the signature runner.
- **Not tuned.** The app runs the model on the CPU, through the Interpreter API. The
  [LiteRT Android page](https://ai.google.dev/edge/litert/android) calls that API "maintained for backward
  compatibility".

## 1. Export the model

The keras.io guide installs Keras, KerasHub and litert-torch from their main branches, because the export path is
still moving. The guide runs these lines in a notebook, each with a leading `!`:

```bash
pip install -q git+https://github.com/keras-team/keras.git
pip install -q git+https://github.com/keras-team/keras-hub.git
pip install -q git+https://github.com/google-ai-edge/litert-torch.git
pip install -q ai-edge-litert
```

Then one call exports the model. This is the export command from this repository's README:

```bash
KERAS_BACKEND=torch python -c "
import keras_hub, torch
m = keras_hub.models.MobileNetImageClassifier.from_preset('mobilenet_v3_small_100_imagenet')
m.export('/tmp/mobilenetv3.tflite', format='litert', input_signature=[torch.randn(1,224,224,3)])
"
```

Two parts of it matter:

- `KERAS_BACKEND=torch` picks the PyTorch backend. The keras.io guide uses it because its output runs on the new
  LiteRT Android runtime without flex ops.
- `input_signature` fixes one shape: one image, 224 by 224, three channels. The PyTorch path needs a fixed shape
  ([keras#22997](https://github.com/keras-team/keras/pull/22997)).

## 2. Tell the app how to feed it

The exported file starts after preprocessing. KerasHub's own source says so: calling a model directly "does not
apply preprocessing" ([pipeline_model.py](https://github.com/keras-team/keras-hub/blob/3ba52a2fa10a841633bc8acb06e6fa0195a00077/keras_hub/src/utils/pipeline_model.py#L537-L553)).
So the app resizes and normalizes each photo itself
([ImagePreprocessor.kt](../app/src/main/java/com/example/kerasondevice/data/preprocessing/ImagePreprocessor.kt)).
It reads the numbers from a JSON sidecar beside the model. A model with no sidecar is skipped
([ModelLocator.kt](../app/src/main/java/com/example/kerasondevice/domain/model/ModelLocator.kt)).

This preset's numbers come from its own image converter
([preprocessor.json](https://huggingface.co/keras/mobilenet_v3_small_100_imagenet/blob/0c0c8625121a1725a05a0ec7d23f0ed22286b3a2/preprocessor.json)):
a `scale` and an `offset` for each colour channel. In the sidecar's terms, on pixels from 0 to 255, `mean` is
`-offset / scale` and `std` is `1 / scale`. Save this as `/tmp/mobilenetv3.json`:

```json
{
  "task": "image_classification",
  "image_width": 224,
  "image_height": 224,
  "normalization": {"mean": [123.675, 116.28, 103.53], "std": [58.395, 57.12, 57.375]},
  "labels": "imagenet_labels.txt",
  "num_classes": 1000
}
```

The sidecar in the app's test assets is marked as a placeholder, and it uses 127.5 for both numbers. That maps
pixels to the range -1 to 1, which is not the input this preset expects.

## 3. Put both on the phone

The app looks for models in its own files folder, then its external files folder, then `/data/local/tmp`. The
README stages models in `/data/local/tmp` with `adb push`, and Gradle's `installDebug` builds the app and installs
it on the connected phone:

```bash
adb push /tmp/mobilenetv3.tflite /data/local/tmp/
adb push /tmp/mobilenetv3.json /data/local/tmp/
adb push app/src/androidTest/assets/models/*.txt /data/local/tmp/
./gradlew installDebug
```

The third line copies the label files. The ImageNet one holds placeholders, `class_0` to `class_999`, so the app
shows class numbers. `keras_hub.utils.imagenet_id_to_name` turns a number into a name.

Open the app, choose Classification, and pick a photo.

## Before you trust the list

When I read the code for this page, I found that the five labels are the wrong five. The queue keeps the highest
score at its head. Each time it holds six, `poll()` removes that head
([lines 64 to 72](https://github.com/pctablet505/keras-ondevice-android/blob/b8d3e2f1baeb5f9d13697e6a0ee5e1f646f6da09/app/src/main/java/com/example/kerasondevice/inference/tasks/ImageClassificationTask.kt#L64-L72)).
So the screen shows the five least likely classes. The test for this screen checks only that one to five labels
come back
([line 66](https://github.com/pctablet505/keras-ondevice-android/blob/b8d3e2f1baeb5f9d13697e6a0ee5e1f646f6da09/app/src/androidTest/java/com/example/kerasondevice/ClassificationE2ETest.kt#L66)).
It cannot see this.

Why a check like that cannot fail, and the one that can, is the next page.

**[Next: check the phone's answer against the notebook →](02-check-against-the-notebook.md)**

---

<picture><source media="(prefers-color-scheme: dark)" srcset="../assets/mark-dark.svg"><img src="../assets/mark-light.svg" width="16" height="16" alt=""></picture>
<sub>Part 1 of the [Keras On-Device guide](../README.md#the-guide). Built by [pctablet505](https://github.com/pctablet505).</sub>
