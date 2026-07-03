# Test Assets

This directory contains helper scripts for preparing bundled test fixtures.

## Usage

```bash
python3 scripts/prepare_test_assets.py
```

This recreates:
- `app/src/androidTest/assets/images/test_image.jpg`
- `app/src/androidTest/assets/models/ssd_mobilenet_v1_detect.json`
- `app/src/androidTest/assets/models/mobilenetv3.json`
- `app/src/androidTest/assets/golden/detection_golden.json` (if TensorFlow / ai-edge-litert is available)

## Models

### Object detection

`ssd_mobilenet_v1_detect.tflite` and `coco_labels.txt` are downloaded from the
official TensorFlow stock model archive and committed under
`app/src/androidTest/assets/models/`.

Source:
https://storage.googleapis.com/download.tensorflow.org/models/tflite/coco_ssd_mobilenet_v1_1.0_quant_2018_06_29.zip

### Classification

`mobilenetv3.json` is a placeholder sidecar. The MobileNetV3 Small `.tflite`
model is not bundled yet because the KerasHub torch-backend export artifact is
still being produced. The sidecar documents the expected metadata once the model
is available.

### Multimodal

`gemma4_multimodal_test.litertlm` is **not present** in the workspace. The
expected source path is
`/home/pctablet505/Projects/gemmademo-litert-export/gemma4_multimodal_test.litertlm`.
Only the export script (`export_gemma4_multimodal_test.py`) exists there; the
actual artifact must be generated before the multimodal config test can run
end-to-end.

## Golden outputs

Real golden outputs require `ai-edge-litert` or TensorFlow Lite to run the model
on the host. If neither is installed, `detection_golden.json` is written as a
placeholder that explains the skip reason.
