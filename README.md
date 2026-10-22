# Keras On-Device

Pick a photo, and this Android app runs a Keras image classifier on it, on the phone, with no server. It is a demo
app with a step-by-step guide, not a product: no model comes with it, so you export one yourself and copy it to the
phone.

<p>
<picture>
  <source media="(prefers-color-scheme: dark) and (max-width: 600px)" srcset="assets/path-narrow-dark.svg">
  <source media="(max-width: 600px)" srcset="assets/path-narrow-light.svg">
  <source media="(prefers-color-scheme: dark)" srcset="assets/path-dark.svg">
  <img src="assets/path-light.svg" width="100%" alt="The path in six steps. On the computer, a pretrained Keras model, MobileNetV3 Small, goes through one export call with format litert and becomes two files: the model and its settings file. adb push copies both to /data/local/tmp on the phone. The app finds the pair, resizes the photo to 224 by 224, scales each colour, runs the model on the phone's CPU and shows five labels.">
</picture>
</p>

**[Start the guide: one model on your phone →](docs/01-export-a-vision-model.md)**

## What you get

- **One model, start to finish.** One export call in Python to LiteRT, the format once called TensorFlow Lite; one
  small settings file; and four commands that copy the files to the phone and install the app.
- **A way to check the phone's answer.** Get the right answer in Python, check the exported file against it, then
  hold the phone to the same answer.
- **The traps before you hit them.** Five ways an export breaks, each with the public issue or pull request behind it.
- **Six screens to start from.** Classification, object detection, segmentation and depth, each with its run time
  in milliseconds, and two Gemma screens.

## Try it

You export the model and copy it to the phone yourself. Save the settings file from
[part 1](docs/01-export-a-vision-model.md#2-tell-the-app-how-to-feed-it), a small JSON file with the photo size and the
numbers to scale it by, as `/tmp/mobilenetv3.json`, then:

```bash
KERAS_BACKEND=torch python -c "
import keras_hub, torch
m = keras_hub.models.MobileNetImageClassifier.from_preset('mobilenet_v3_small_100_imagenet')
m.export('/tmp/mobilenetv3.tflite', format='litert', input_signature=[torch.randn(1,224,224,3)])
"
adb push /tmp/mobilenetv3.tflite /data/local/tmp/
adb push /tmp/mobilenetv3.json /data/local/tmp/
adb push app/src/androidTest/assets/models/*.txt /data/local/tmp/
./gradlew installDebug
```

`from_preset` loads a pretrained model by name from KerasHub, the Keras library of ready-made models.
`input_signature` fixes the input to one 224 by 224 colour photo, because an export on the PyTorch backend needs one
fixed size. Then open Classification, pick a photo and tap Run.

On 22 October 2026 the export ran on a computer's CPU with keras 3.15.1, keras-hub 0.32.0, litert-torch 0.10.0 and
ai-edge-litert 2.3.0. Installed from the main branches instead, it failed in the converter
([part 1](docs/01-export-a-vision-model.md#1-export-the-model) has the error and the install lines). The app built
with `./gradlew assembleDebug`; it was not tested on a phone for this page.

You need a recent Android Studio (Ladybug or newer), Android SDK 36, JDK 21, PyTorch and the pinned Python packages
above, and a phone that `adb` can reach.

## What the guide found

Bugs in this app's own code, still there as of
[3 July 2026](https://github.com/pctablet505/keras-ondevice-android/tree/b8d3e2f1baeb5f9d13697e6a0ee5e1f646f6da09): the
top-five list shows the five least likely labels, two tests cannot catch a wrong answer, and the sample settings and
labels do not fit the model ([part 1](docs/01-export-a-vision-model.md#before-you-trust-the-list),
[part 2](docs/02-check-against-the-notebook.md#two-tests-that-could-not-fail)).

## Limits

The vision screens run on the CPU only, through the Interpreter API of the older TensorFlow Lite runtime, which the
[LiteRT Android page](https://ai.google.dev/edge/litert/android) calls "maintained for backward compatibility". The
guide covers the vision models; making the model files for the two Gemma screens is not covered.

## The guide

- [Part 1: export a KerasHub vision model and run it on Android](docs/01-export-a-vision-model.md): the whole path,
  with the numbers the model needs.
- [Part 2: check the phone's answer against the notebook](docs/02-check-against-the-notebook.md): why two of this
  app's tests cannot catch a wrong answer, and how to write one that can.
- [Part 3: where conversion breaks, with the evidence](docs/03-traps.md): five traps, each with the public pull request
  or issue behind it.
- [Build, test and export: the reference](docs/reference.md): every build, test and export command, and how the code
  is laid out.

## Licence

Apache-2.0, see [LICENSE](LICENSE).

---

<picture><source media="(prefers-color-scheme: dark)" srcset="assets/mark-dark.svg"><img src="assets/mark-light.svg" width="16" height="16" alt=""></picture>
<sub>Built by [pctablet505](https://github.com/pctablet505).</sub>
