#!/usr/bin/env python3
"""Prepare synthetic test assets and (optionally) golden outputs for E2E tests."""

import json
import os
from pathlib import Path

from PIL import Image, ImageDraw

REPO_ROOT = Path(__file__).resolve().parent.parent
ASSETS = REPO_ROOT / "app" / "src" / "androidTest" / "assets"
MODELS_DIR = ASSETS / "models"
IMAGES_DIR = ASSETS / "images"
GOLDEN_DIR = ASSETS / "golden"


def create_test_image() -> None:
    """Create a simple synthetic 512x512 test image."""
    IMAGES_DIR.mkdir(parents=True, exist_ok=True)
    img = Image.new("RGB", (512, 512), color=(135, 206, 235))  # sky blue
    draw = ImageDraw.Draw(img)
    # A red rectangle in the foreground-ish area
    draw.rectangle([160, 320, 352, 448], fill=(220, 20, 60))
    # A green rectangle higher up
    draw.rectangle([64, 96, 192, 224], fill=(34, 139, 34))
    # A yellow-ish circle/ellipse in the center
    draw.ellipse([224, 224, 288, 288], fill=(255, 215, 0))
    out = IMAGES_DIR / "test_image.jpg"
    img.save(out, "JPEG", quality=90)
    print(f"Created {out}")


def create_ssd_sidecar() -> None:
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    sidecar = {
        "task": "object_detection",
        "image_width": 300,
        "image_height": 300,
        "normalization": {"mean": [0.0, 0.0, 0.0], "std": [1.0, 1.0, 1.0]},
        "labels": "coco_labels.txt",
        "num_classes": 91,
        "input": "uint8_rgb_300x300",
        "outputs": {
            "detection_boxes": {"shape": [1, 10, 4], "format": "ymin_xmin_ymax_xmax_normalized"},
            "detection_classes": {"shape": [1, 10]},
            "detection_scores": {"shape": [1, 10]},
            "num_detections": {"shape": [1]}
        }
    }
    out = MODELS_DIR / "ssd_mobilenet_v1_detect.json"
    out.write_text(json.dumps(sidecar, indent=2) + "\n")
    print(f"Created {out}")


def create_mobilenetv3_placeholder() -> None:
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    out = MODELS_DIR / "mobilenetv3.json"
    placeholder = {
        "task": "image_classification",
        "image_width": 224,
        "image_height": 224,
        "normalization": {"mean": [127.5, 127.5, 127.5], "std": [127.5, 127.5, 127.5]},
        "labels": "imagenet_labels.txt",
        "num_classes": 1000,
        "_note": "MobileNetV3 tflite model is not bundled yet. Placeholder sidecar only."
    }
    out.write_text(json.dumps(placeholder, indent=2) + "\n")
    print(f"Created {out}")


def generate_golden_detection() -> None:
    """Generate golden detection outputs if TensorFlow / ai-edge-litert is available."""
    GOLDEN_DIR.mkdir(parents=True, exist_ok=True)
    out = GOLDEN_DIR / "detection_golden.json"
    try:
        # ai-edge-litert is the preferred runtime per the design spec.
        from ai_edge_litert import interpreter
        import numpy as np

        model_path = str(MODELS_DIR / "ssd_mobilenet_v1_detect.tflite")
        img = Image.open(IMAGES_DIR / "test_image.jpg").convert("RGB").resize((300, 300))
        input_data = np.array(img, dtype=np.uint8)[np.newaxis, ...]

        interp = interpreter.Interpreter(model_path=model_path)
        interp.allocate_tensors()
        input_details = interp.get_input_details()
        output_details = interp.get_output_details()
        interp.set_tensor(input_details[0]["index"], input_data)
        interp.invoke()

        names = [d["name"] for d in output_details]
        def tensor(name):
            for d in output_details:
                if d["name"] == name:
                    return interp.get_tensor(d["index"]).tolist()
            return None

        golden = {
            "model": "ssd_mobilenet_v1_detect.tflite",
            "image": "images/test_image.jpg",
            "input_shape": list(input_data.shape),
            "output_names": names,
            "detection_boxes": tensor("TFLite_Detection_PostProcess"),
            "detection_classes": tensor("TFLite_Detection_PostProcess:1"),
            "detection_scores": tensor("TFLite_Detection_PostProcess:2"),
            "num_detections": tensor("TFLite_Detection_PostProcess:3")
        }
        out.write_text(json.dumps(golden, indent=2) + "\n")
        print(f"Created {out}")
    except Exception as exc:  # noqa: BLE001
        note = {
            "_note": f"Golden generation skipped: {exc}",
            "model": "ssd_mobilenet_v1_detect.tflite",
            "image": "images/test_image.jpg"
        }
        out.write_text(json.dumps(note, indent=2) + "\n")
        print(f"Created placeholder {out} ({exc})")


if __name__ == "__main__":
    create_test_image()
    create_ssd_sidecar()
    create_mobilenetv3_placeholder()
    generate_golden_detection()
