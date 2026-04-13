#!/usr/bin/env python3
"""
Generate Android app icons and drawable resources from the logo image.
"""
from PIL import Image, ImageDraw
import os

BASE = "/Volumes/DATA/Projects/miniPOS"
SRC_ICON = os.path.join(BASE, "icon.png")
RES_DIR = os.path.join(BASE, "app/src/main/res")

def resize_and_save(img, size, path):
    """Resize image to size x size and save as PNG."""
    resized = img.resize((size, size), Image.LANCZOS)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    resized.save(path, "PNG")
    print(f"  ✓ {path} ({size}x{size})")

def make_round(img, size):
    """Create a circular version of the image."""
    resized = img.resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size - 1, size - 1), fill=255)
    result = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    result.paste(resized, (0, 0), mask)
    return result

def main():
    print("Loading source icon...")
    img = Image.open(SRC_ICON).convert("RGBA")
    print(f"  Source: {img.size[0]}x{img.size[1]}")

    # ── 1. Adaptive icon foreground (108dp with safe zone) ──
    # The foreground layer for adaptive icons should be 108dp,
    # with the icon content within the inner 72dp (66% area).
    # We need to place the logo centered on a 108dp canvas with padding.
    print("\n── Generating adaptive icon foreground layers ──")
    # mipmap density -> foreground size in px (108dp at each density)
    foreground_sizes = {
        "mipmap-mdpi": 108,
        "mipmap-hdpi": 162,
        "mipmap-xhdpi": 216,
        "mipmap-xxhdpi": 324,
        "mipmap-xxxhdpi": 432,
    }
    
    for folder, size in foreground_sizes.items():
        # Create a transparent canvas at 108dp equivalent
        canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        # Icon should be about 66% of the canvas (safe zone)
        icon_size = int(size * 0.62)
        icon_resized = img.resize((icon_size, icon_size), Image.LANCZOS)
        offset = (size - icon_size) // 2
        canvas.paste(icon_resized, (offset, offset), icon_resized)
        path = os.path.join(RES_DIR, folder, "ic_launcher_foreground.png")
        canvas.save(path, "PNG")
        print(f"  ✓ {folder}/ic_launcher_foreground.png ({size}x{size})")

    # ── 2. Legacy launcher icons (square with rounded corners) ──
    print("\n── Generating legacy launcher icons ──")
    launcher_sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    
    for folder, size in launcher_sizes.items():
        # ic_launcher.png - square icon
        path = os.path.join(RES_DIR, folder, "ic_launcher.png")
        resize_and_save(img, size, path)
        
        # ic_launcher_round.png - circular icon
        round_img = make_round(img, size)
        round_path = os.path.join(RES_DIR, folder, "ic_launcher_round.png")
        round_img.save(round_path, "PNG")
        print(f"  ✓ {folder}/ic_launcher_round.png ({size}x{size})")

    # ── 3. Play Store icon (512x512) ──
    print("\n── Generating Play Store icon ──")
    playstore_path = os.path.join(BASE, "app/src/main/ic_launcher-playstore.png")
    resize_and_save(img, 512, playstore_path)

    # ── 4. Drawable resources for in-app use ──
    print("\n── Generating drawable resources for in-app logo ──")
    drawable_sizes = {
        "drawable-mdpi": 48,
        "drawable-hdpi": 72,
        "drawable-xhdpi": 96,
        "drawable-xxhdpi": 144,
        "drawable-xxxhdpi": 192,
    }
    
    for folder, size in drawable_sizes.items():
        path = os.path.join(RES_DIR, folder, "app_logo.png")
        resize_and_save(img, size, path)

    print("\n✅ All icons generated successfully!")

if __name__ == "__main__":
    main()
