"""
Simple OpenVINO Demo
====================
This script demonstrates how to:
1. Build a small model using OpenVINO's opset builder (no download required)
2. Compile it on CPU / GPU / NPU
3. Run inference and inspect results

Requirements:
    pip install openvino numpy
"""

import numpy as np
import openvino as ov
from openvino import Type, Shape
import openvino.opset14 as ops

print("=" * 55)
print("  OpenVINO Simple Demo")
print(f"  Version: {ov.__version__}")
print("=" * 55)

# ─── Step 1: Initialize the Runtime ─────────────────────────────────────────
core = ov.Core()
devices = core.available_devices
print(f"\n[INFO] Available devices: {devices}")

# ─── Step 2: Build a tiny model (1×3×224×224 → softmax over 1000 classes) ───
print("\n[INFO] Building model with OpenVINO opset builder...")

# Input node  (batch=1, C=3, H=224, W=224)
param = ops.parameter(Shape([1, 3, 224, 224]), dtype=Type.f32, name="input")

# Flatten → (1, 150528)
target_shape_const = ops.constant(np.array([1, -1], dtype=np.int64))
reshape = ops.reshape(param, target_shape_const, special_zero=False)

# Dense layer: (1, 150528) @ (150528, 1000) + bias → (1, 1000)
np.random.seed(42)
W = np.random.randn(150528, 1000).astype(np.float32) * 0.01
b = np.zeros(1000, dtype=np.float32)

W_const = ops.constant(W)
b_const = ops.constant(b)

matmul = ops.matmul(reshape, W_const, transpose_a=False, transpose_b=False)
add    = ops.add(matmul, b_const)

# Softmax over the class axis
softmax = ops.softmax(add, axis=1)

# Wrap into an ov.Model
model = ov.Model(results=[softmax], parameters=[param], name="mini_classifier")
print("[INFO] Model built successfully.")

# ─── Step 3: Compile the model ───────────────────────────────────────────────
# Change device to "GPU" or "NPU" if you want to use those accelerators
DEVICE = "NPU"
print(f"\n[INFO] Compiling model on {DEVICE}...")
compiled = core.compile_model(model, device_name=DEVICE)

input_layer  = compiled.input(0)
output_layer = compiled.output(0)
print(f"[INFO] Input  shape : {input_layer.shape}")
print(f"[INFO] Output shape : {output_layer.shape}")

# ─── Step 4: Run Inference ───────────────────────────────────────────────────
print("\n[INFO] Creating a random test image (1, 3, 224, 224)...")
dummy_input = np.random.rand(1, 3, 224, 224).astype(np.float32)

print("[INFO] Running inference...")
result = compiled([dummy_input])
output = result[output_layer]          # shape (1, 1000)

top_idx   = int(np.argmax(output))
top_score = float(output[0, top_idx])

print(f"\n[RESULT] Output shape    : {output.shape}")
print(f"[RESULT] Top class index : {top_idx}")
print(f"[RESULT] Top confidence  : {top_score:.6f}")

# ─── Step 5: Top-5 predictions ───────────────────────────────────────────────
top5 = np.argsort(output[0])[::-1][:5]
print("\n[RESULT] Top-5 predicted class indices (random weights, for demo):")
for rank, idx in enumerate(top5, 1):
    print(f"  #{rank}  class {idx:4d}  score={output[0][idx]:.6f}")

# ─── Step 6: Try other devices ───────────────────────────────────────────────
print("\n[INFO] Attempting inference on all available devices...")
for dev in devices:
    try:
        c = core.compile_model(model, device_name=dev)
        r = c([dummy_input])
        top = int(np.argmax(r[c.output(0)]))
        print(f"  [OK]  {dev:<6s} -> top class {top}")
    except Exception as exc:
        print(f"  [!!]  {dev:<6s} -> {exc}")

print("\n[SUCCESS] OpenVINO is working perfectly on your machine!")
print("  -> Swap in a real ONNX / IR model with core.read_model()")
print("  -> Change DEVICE to 'GPU' or 'NPU' for hardware acceleration")

