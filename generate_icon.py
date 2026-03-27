#!/usr/bin/env python3
"""
Generate all Android launcher icon sizes from the 512x512 Play Store icon.
Also generates foreground-only PNG for adaptive icons.

Android mipmap sizes:
  mdpi:    48x48
  hdpi:    72x72
  xhdpi:   96x96
  xxhdpi:  144x144
  xxxhdpi: 192x192

Adaptive icon foreground sizes (108dp with safe zone):
  mdpi:    108x108
  hdpi:    162x162
  xhdpi:   216x216
  xxhdpi:  324x324
  xxxhdpi: 432x432
"""

from PIL import Image, ImageDraw, ImageFilter
import os

BASE_DIR = "/Volumes/DATA/Projects/miniPOS/app/src/main/res"
SOURCE = "/Volumes/DATA/Projects/miniPOS/ic_launcher_playstore.png"

# Standard launcher icon sizes (square, with built-in rounded corners)
LAUNCHER_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Adaptive icon foreground sizes (108dp canvas, content in 66dp safe zone)
FOREGROUND_SIZES = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}


def create_round_mask(size):
    """Create a circular mask for round icons."""
    mask = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse([0, 0, size, size], fill=255)
    return mask


def create_rounded_rect_mask(size, radius_ratio=0.22):
    """Create a rounded rectangle mask (Google-style squircle)."""
    # Render at 4x for smooth edges
    big = size * 4
    mask = Image.new('L', (big, big), 0)
    draw = ImageDraw.Draw(mask)
    r = int(big * radius_ratio)
    draw.rounded_rectangle([0, 0, big, big], radius=r, fill=255)
    mask = mask.resize((size, size), Image.LANCZOS)
    return mask


def create_foreground_png(source_img, target_size):
    """
    Create a foreground PNG for adaptive icon.
    The foreground canvas is 108dp, with the icon content
    in the center 66dp safe zone (about 61% of canvas).
    """
    canvas = Image.new('RGBA', (target_size, target_size), (0, 0, 0, 0))

    # The icon content should fit within the safe zone (~61% of canvas)
    safe_zone_ratio = 0.61
    content_size = int(target_size * safe_zone_ratio)
    content = source_img.resize((content_size, content_size), Image.LANCZOS)

    # Center the content on the canvas
    offset = (target_size - content_size) // 2
    canvas.paste(content, (offset, offset), content)

    return canvas


def create_background_png(target_size):
    """
    Create a solid blue background PNG for adaptive icon.
    Matches the existing ic_launcher_background.xml design.
    """
    img = Image.new('RGBA', (target_size, target_size), (37, 99, 235, 255))
    draw = ImageDraw.Draw(img)

    # Subtle lighter circle for depth (matching existing vector)
    cx, cy = target_size // 2, target_size // 2
    r1 = int(target_size * 40 / 108)
    r2 = int(target_size * 28 / 108)

    # Lighter circle layer 1
    overlay1 = Image.new('RGBA', (target_size, target_size), (0, 0, 0, 0))
    d1 = ImageDraw.Draw(overlay1)
    d1.ellipse([cx-r1, cy-r1, cx+r1, cy+r1], fill=(59, 130, 246, 77))
    img = Image.alpha_composite(img, overlay1)

    # Lighter circle layer 2
    overlay2 = Image.new('RGBA', (target_size, target_size), (0, 0, 0, 0))
    d2 = ImageDraw.Draw(overlay2)
    d2.ellipse([cx-r2, cy-r2, cx+r2, cy+r2], fill=(96, 165, 250, 38))
    img = Image.alpha_composite(img, overlay2)

    return img


def main():
    if not os.path.exists(SOURCE):
        print(f"ERROR: Source icon not found: {SOURCE}")
        return

    src = Image.open(SOURCE).convert('RGBA')
    print(f"Source: {src.size[0]}x{src.size[1]}")

    # 1. Generate standard launcher icons (ic_launcher.png)
    print("\n=== Standard Launcher Icons (ic_launcher.png) ===")
    for folder, size in LAUNCHER_SIZES.items():
        out_dir = os.path.join(BASE_DIR, folder)
        os.makedirs(out_dir, exist_ok=True)

        # ic_launcher.png - square with slight rounding for legacy
        icon = src.resize((size, size), Image.LANCZOS)
        out_path = os.path.join(out_dir, "ic_launcher.png")
        icon.save(out_path, "PNG", optimize=True)
        print(f"  {folder}/ic_launcher.png ({size}x{size})")

        # ic_launcher_round.png - circular
        round_icon = src.resize((size, size), Image.LANCZOS)
        mask = create_round_mask(size)
        result = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        result.paste(round_icon, (0, 0), mask)
        out_path = os.path.join(out_dir, "ic_launcher_round.png")
        result.save(out_path, "PNG", optimize=True)
        print(f"  {folder}/ic_launcher_round.png ({size}x{size})")

    # 2. Generate adaptive icon foreground PNGs
    print("\n=== Adaptive Icon Foreground (ic_launcher_foreground.png) ===")
    for folder, size in FOREGROUND_SIZES.items():
        out_dir = os.path.join(BASE_DIR, folder)
        os.makedirs(out_dir, exist_ok=True)

        fg = create_foreground_png(src, size)
        out_path = os.path.join(out_dir, "ic_launcher_foreground.png")
        fg.save(out_path, "PNG", optimize=True)
        print(f"  {folder}/ic_launcher_foreground.png ({size}x{size})")

    print("\n✅ All icons generated successfully!")
    print("\nNote: The adaptive icon XML files (mipmap-anydpi-v26/) will be")
    print("updated to reference the PNG foreground instead of vector drawable.")


if __name__ == "__main__":
    main()
