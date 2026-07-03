# Scripts

## `prepare_test_assets.py`

Regenerates bundled test assets under `app/src/androidTest/assets/`.

```bash
cd scripts
python prepare_test_assets.py
```

Requires: `PIL` (Pillow).

Optional for real golden outputs: `ai-edge-litert` or TensorFlow.

## Notes

- The script downloads a ~4 MB COCO SSD MobileNet V1 quantized model.
- Large models (MobileNetV3, DeepLabV3, MiDaS, Gemma3) are not downloaded by this script; they must be exported/obtained separately.
