from PIL import Image

# 检查图标尺寸
icons = [
    "e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\drawable\\ic_launcher_foreground.png",
    "e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\mipmap-hdpi\\ic_launcher.png",
    "e:\\Code\\Script\\Fuel_consumption_record\\app\\src\\main\\res\\mipmap-hdpi\\ic_launcher1.png",
]

for icon_path in icons:
    try:
        with Image.open(icon_path) as img:
            print(f"{icon_path}: {img.size[0]}x{img.size[1]}")
    except Exception as e:
        print(f"Error checking {icon_path}: {e}")
