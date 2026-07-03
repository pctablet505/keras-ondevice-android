#!/usr/bin/env python3
"""Prepare test assets for Keras On-Device Android app.

This script recreates the bundled test models, labels, and golden outputs
under app/src/androidTest/assets/. Large models are downloaded or exported
when the required Python packages are available.
"""

import json
import os
import shutil
import urllib.request
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app/src/androidTest/assets"
MODELS = ASSETS / "models"
IMAGES = ASSETS / "images"
GOLDEN = ASSETS / "golden"


def ensure_dirs():
    for d in (MODELS, IMAGES, GOLDEN):
        d.mkdir(parents=True, exist_ok=True)


def download_ssd_mobilenet():
    url = (
        "https://storage.googleapis.com/download.tensorflow.org/models/tflite/"
        "coco_ssd_mobilenet_v1_1.0_quant_2018_06_29.zip"
    )
    archive = MODELS / "coco_ssd_mobilenet_v1.zip"
    if not archive.exists():
        print(f"Downloading {url}...")
        urllib.request.urlretrieve(url, archive)
    extract_dir = MODELS / "ssd_tmp"
    with zipfile.ZipFile(archive, "r") as z:
        z.extractall(extract_dir)
    shutil.move(extract_dir / "detect.tflite", MODELS / "ssd_mobilenet_v1_detect.tflite")
    shutil.move(extract_dir / "labelmap.txt", MODELS / "coco_labels.txt")
    shutil.rmtree(extract_dir)
    archive.unlink()


def write_ssd_sidecar():
    sidecar = {
        "schema_version": 1,
        "task": "object_detection",
        "format": "tflite",
        "image_width": 300,
        "image_height": 300,
        "input_dtype": "uint8",
        "color_channels": "rgb",
        "labels": "coco_labels.txt",
        "num_classes": 90,
        "max_detections": 10,
        "box_format": "ymin_xmin_ymax_xmax_normalized",
        "score_threshold": 0.5,
        "postprocess_family": "tflite_detection_postprocess",
    }
    (MODELS / "ssd_mobilenet_v1_detect.json").write_text(
        json.dumps(sidecar, indent=2)
    )


def write_mobilenetv3_sidecar():
    sidecar = {
        "task": "image_classification",
        "image_width": 224,
        "image_height": 224,
        "normalization": {"mean": [127.5, 127.5, 127.5], "std": [127.5, 127.5, 127.5]},
        "labels": "imagenet_labels.txt",
        "num_classes": 1000,
    }
    (MODELS / "mobilenetv3.json").write_text(json.dumps(sidecar, indent=2))


def generate_test_image():
    img = Image.new("RGB", (512, 512), color=(128, 128, 128))
    img.save(IMAGES / "test_image.jpg", quality=90)


def generate_goldens():
    print("Golden generation requires ai-edge-litert or TensorFlow; skipping.")
    (GOLDEN / "detection_golden.json").write_text(
        json.dumps({"note": "placeholder — regenerate with real inference"}, indent=2)
    )


def main():
    ensure_dirs()
    download_ssd_mobilenet()
    write_ssd_sidecar()
    write_mobilenetv3_sidecar()
    generate_test_image()
    generate_goldens()
    print("Done.")


if __name__ == "__main__":
    main()
